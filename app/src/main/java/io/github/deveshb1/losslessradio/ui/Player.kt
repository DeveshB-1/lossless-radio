package io.github.deveshb1.losslessradio.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.deveshb1.losslessradio.AudioPath
import io.github.deveshb1.losslessradio.SessionInfo
import io.github.deveshb1.losslessradio.Station
import kotlinx.coroutines.delay

data class NowState(
    val connected: Boolean = false,
    val station: Station? = null,
    val wantsAudio: Boolean = false,
    val buffering: Boolean = false,
    val reconnecting: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val extras: Bundle = Bundle.EMPTY,
    val error: String? = null,
)

private fun NowState.status(): Pair<String, Color?> = when {
    error != null -> error to Warn
    reconnecting -> "Reconnecting…" to Warn
    buffering -> "Buffering…" to Warn
    wantsAudio -> "Live" to Color(0xFFFF5A5A)
    else -> "Stopped" to null
}

@Composable
fun MiniPlayer(now: NowState, onOpen: () -> Unit, onToggle: () -> Unit) {
    val station = now.station ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArt(station, 44.dp, corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(now.title ?: station.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val (status, _) = now.status()
            val sub = if (now.title != null) now.artist ?: station.name else status
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PlayStopButton(now.wantsAudio, 40.dp, onToggle, background = MaterialTheme.colorScheme.primary, ink = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun FullPlayer(
    now: NowState,
    favorite: Boolean,
    bitPerfectWanted: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onFavorite: () -> Unit,
    onSleep: (Int) -> Unit,
    onBitPerfect: (Boolean) -> Unit,
    onHomepage: (String) -> Unit,
) {
    val station = now.station ?: return
    val hue = ((station.name.hashCode() % 360) + 360) % 360
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Color.hsv(hue.toFloat(), 0.35f, 0.28f), 0.7f to Background)),
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = if (favorite) "Remove from favourites" else "Add to favourites", tint = if (favorite) Warn else Color.White.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(12.dp))
            StationArt(station, 220.dp, corner = 28.dp)
            Spacer(Modifier.height(24.dp))

            Text(
                now.title ?: station.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (now.title != null) now.artist ?: station.name else listOf(station.flag, station.formatLabel).filter { it.isNotBlank() }.joinToString("  "),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(24.dp))
            PlayStopButton(now.wantsAudio, 76.dp, onToggle)
            Spacer(Modifier.height(12.dp))
            val (status, dot) = now.status()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dot != null) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(8.dp))
                }
                Text(status, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))
            SleepTimer(now.extras.getLong(SessionInfo.SLEEP_AT), onSleep)
            Spacer(Modifier.height(16.dp))
            SignalPath(now.extras, station, bitPerfectWanted, onBitPerfect)

            if (station.homepage.isNotBlank()) {
                TextButton(onClick = { onHomepage(station.homepage) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Station website ↗")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SleepTimer(sleepAt: Long, onSleep: (Int) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepAt) {
        while (sleepAt > 0) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    Column(
        Modifier.widthIn(max = 440.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val minutesLeft = ((sleepAt - now) / 60_000 + 1).coerceAtLeast(1)
        Text(
            if (sleepAt > 0) "Sleep timer · stops in $minutesLeft min" else "Sleep timer",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "1h", 90 to "1½h").forEach { (minutes, label) ->
                FilterChip(
                    selected = if (minutes == 0) sleepAt == 0L else false,
                    onClick = { onSleep(minutes) },
                    label = { Text(label, fontSize = 13.sp) },
                )
            }
        }
    }
}

@Composable
private fun SignalPath(extras: Bundle, station: Station, bitPerfectWanted: Boolean, onBitPerfect: (Boolean) -> Unit) {
    val codec = extras.getString(SessionInfo.CODEC).orEmpty()
    val rate = extras.getInt(SessionInfo.SAMPLE_RATE)
    val bits = extras.getInt(SessionInfo.BITS)
    val outRate = extras.getInt(SessionInfo.OUT_SAMPLE_RATE)
    val kind = runCatching { AudioPath.Kind.valueOf(extras.getString(SessionInfo.DEVICE_KIND).orEmpty()) }.getOrDefault(AudioPath.Kind.OTHER)
    val deviceName = extras.getString(SessionInfo.DEVICE_NAME).orEmpty()
    val bitPerfect = extras.getBoolean(SessionInfo.BIT_PERFECT)
    val streamLossless = codec in setOf("FLAC", "ALAC")

    val source = when {
        codec.isEmpty() -> station.formatLabel + " (waiting for audio)"
        else -> buildList {
            add(codec)
            if (rate > 0) add("%.1f kHz".format(rate / 1000f))
            if (bits > 0 && streamLossless) add("$bits-bit")
        }.joinToString(" · ")
    }
    val kindLabel = when (kind) {
        AudioPath.Kind.BLUETOOTH -> "Bluetooth"
        AudioPath.Kind.USB -> "USB"
        AudioPath.Kind.WIRED -> "Wired"
        AudioPath.Kind.SPEAKER -> "Speaker"
        AudioPath.Kind.OTHER -> "Output"
    }
    val (verdictColor, verdict) = when {
        codec.isNotEmpty() && !streamLossless -> Warn to "This station sends compressed (lossy) audio. Look for the green FLAC badge for lossless stations."
        kind == AudioPath.Kind.USB && bitPerfect -> Good to "Bit-perfect to your DAC — nothing in between."
        kind == AudioPath.Kind.USB -> Warn to "Android's mixer may resample before the DAC. Turn on bit-perfect below."
        kind == AudioPath.Kind.BLUETOOTH -> Warn to "Re-encoded by the Bluetooth codec, so lossy — unless your phone and earbuds use aptX Lossless."
        kind == AudioPath.Kind.WIRED -> Warn to "Lossless into the phone's DAC; Android's mixer may resample to 48 kHz."
        kind == AudioPath.Kind.SPEAKER -> Warn to "Phone speaker — the stream is fine, the speaker is the limit."
        else -> Good to "Lossless stream."
    }

    Column(
        Modifier.widthIn(max = 440.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("SIGNAL PATH", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 2.sp)
        PathRow("Stream", source, if (codec.isEmpty()) Color.Gray else if (streamLossless) Good else Warn)
        PathRow("Decoder", if (outRate > 0) "PCM · %.1f kHz".format(outRate / 1000f) else "PCM", if (outRate == 0 || outRate == rate) Good else Warn)
        PathRow(kindLabel, deviceName.ifEmpty { "—" }, verdictColor)
        Text(verdict, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 18.sp)

        if (AudioPath.bitPerfectSupported) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bit-perfect USB", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Sends audio untouched to a USB DAC. Other app sounds are silenced while it plays.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = bitPerfectWanted,
                    onCheckedChange = onBitPerfect,
                    colors = SwitchDefaults.colors(checkedTrackColor = Good, checkedThumbColor = Background),
                )
            }
        }
    }
}

@Composable
private fun PathRow(label: String, value: String, dot: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp, modifier = Modifier.widthIn(min = 76.dp))
        Text(value, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
