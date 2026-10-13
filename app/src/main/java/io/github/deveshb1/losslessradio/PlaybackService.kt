package io.github.deveshb1.losslessradio

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Session extras the UI reads: the signal path plus the sleep timer. */
object SessionInfo {
    const val CODEC = "codec"
    const val SAMPLE_RATE = "sampleRate"
    const val BITS = "bits"
    const val OUT_SAMPLE_RATE = "outSampleRate"
    const val DEVICE_NAME = "deviceName"
    const val DEVICE_KIND = "deviceKind"
    const val BIT_PERFECT = "bitPerfect"
    /** Wall-clock millis when the sleep timer stops playback; 0 when off. */
    const val SLEEP_AT = "sleepAt"

    const val ACTION_SLEEP = "sleep"
    const val ARG_MINUTES = "minutes"
    val SLEEP_COMMAND = SessionCommand(ACTION_SLEEP, Bundle.EMPTY)
}

private const val MAX_RETRIES = 6

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var retryJob: Job? = null
    private var sleepJob: Job? = null
    private var sleepAt = 0L
    private var pausedAt = 0L
    private var retries = 0

    private var inputFormat: Format? = null
    private var outSampleRate = 0

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
            if (added.any { AudioPath.kindOf(it.type) == AudioPath.Kind.USB }) AudioPath.applyBitPerfect(this@PlaybackService)
            publishExtras()
        }

        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = publishExtras()
    }

    override fun onCreate() {
        super.onCreate()
        AudioPath.applyBitPerfect(this)

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(RadioBrowser.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
        val sources = DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http))
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(sources)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(playerListener)
        player.addAnalyticsListener(analyticsListener)

        session = MediaSession.Builder(this, StationPlayer(player))
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setCallback(sessionCallback)
            .build()

        getSystemService(AudioManager::class.java).registerAudioDeviceCallback(deviceCallback, null)
        publishExtras()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(deviceCallback)
        scope.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(SessionInfo.SLEEP_COMMAND).build(),
                )
                .build()

        /** Controllers can't send URIs directly; the stream URL travels in requestMetadata. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
            mediaItems.mapNotNull { item ->
                val uri = item.requestMetadata.mediaUri ?: return@mapNotNull null
                item.buildUpon()
                    .setUri(uri)
                    .apply { if (Station.isHls(uri.toString())) setMimeType(MimeTypes.APPLICATION_M3U8) }
                    .build()
            }.toMutableList(),
        )

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == SessionInfo.ACTION_SLEEP) {
                setSleepTimer(args.getInt(SessionInfo.ARG_MINUTES))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepAt = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0L
        if (minutes > 0) {
            sleepJob = scope.launch {
                delay(minutes * 60_000L)
                player.pause()
                sleepAt = 0L
                publishExtras()
            }
        }
        publishExtras()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                pausedAt = SystemClock.elapsedRealtime()
                retryJob?.cancel()
                return
            }
            // Live radio: after a real pause, jump back to "now" instead of playing stale buffer.
            if (pausedAt != 0L && SystemClock.elapsedRealtime() - pausedAt > 10_000) player.seekToDefaultPosition()
            pausedAt = 0L
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            inputFormat = null
            outSampleRate = 0
            publishExtras()
        }

        override fun onPlayerError(error: PlaybackException) {
            retryJob?.cancel()
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition()
                player.prepare()
                return
            }
            if (!isTransient(error) || retries >= MAX_RETRIES) {
                // Dead link or unplayable stream: stop and let the UI explain, rather than spin forever.
                retries = 0
                player.pause()
                return
            }
            retryJob = scope.launch {
                delay(2_000L shl retries.coerceAtMost(4))
                retries++
                if (player.playWhenReady && player.playbackState == Player.STATE_IDLE) player.prepare()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) retries = 0
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?,
        ) {
            inputFormat = format
            publishExtras()
        }

        override fun onAudioTrackInitialized(eventTime: AnalyticsListener.EventTime, audioTrackConfig: AudioSink.AudioTrackConfig) {
            outSampleRate = audioTrackConfig.sampleRate
            publishExtras()
        }
    }

    private fun publishExtras() {
        val device = AudioPath.currentOutput(this)
        val format = inputFormat
        session.setSessionExtras(
            Bundle().apply {
                putString(SessionInfo.CODEC, format?.sampleMimeType?.let(::codecName).orEmpty())
                putInt(SessionInfo.SAMPLE_RATE, format?.sampleRate?.takeIf { it > 0 } ?: 0)
                putInt(SessionInfo.BITS, bitsOf(format))
                putInt(SessionInfo.OUT_SAMPLE_RATE, outSampleRate)
                putString(SessionInfo.DEVICE_NAME, device?.productName?.toString().orEmpty())
                putString(SessionInfo.DEVICE_KIND, device?.let { AudioPath.kindOf(it.type).name } ?: AudioPath.Kind.OTHER.name)
                putBoolean(SessionInfo.BIT_PERFECT, AudioPath.bitPerfectActive(this@PlaybackService))
                putLong(SessionInfo.SLEEP_AT, sleepAt)
            },
        )
    }

    /** Network blips and server hiccups are worth retrying; 4xx responses and bad formats are not. */
    private fun isTransient(error: PlaybackException): Boolean {
        val http = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()
        if (http != null) return http.responseCode >= 500
        return error.errorCode in setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )
    }

    private fun codecName(mime: String): String = when (mime) {
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_ALAC -> "ALAC"
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_MPEG -> "MP3"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_VORBIS -> "Vorbis"
        else -> mime.substringAfter('/').uppercase()
    }

    private fun bitsOf(format: Format?): Int = when (format?.pcmEncoding) {
        C.ENCODING_PCM_16BIT -> 16
        C.ENCODING_PCM_24BIT -> 24
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
        else -> 0
    }
}

/**
 * Shapes what the notification and lock screen show. Streams report the song (if at all) as a
 * single title like "Artist - Song"; we split that and fall back to the station name.
 */
@OptIn(UnstableApi::class)
private class StationPlayer(player: Player) : ForwardingPlayer(player) {
    override fun getMediaMetadata(): MediaMetadata {
        val merged = super.getMediaMetadata()
        val stationName = currentMediaItem?.mediaMetadata?.station?.toString()
        val (artist, title) = NowPlaying.split(merged.artist?.toString(), merged.title?.toString())
        return merged.buildUpon()
            .setTitle(title ?: stationName)
            .setArtist(if (title != null) artist ?: stationName else null)
            .build()
    }
}
