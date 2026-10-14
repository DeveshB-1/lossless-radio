package io.github.deveshb1.losslessradio.ui

import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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

private data class Status(val text: String, val dot: Color?, val breathing: Boolean)

private fun NowState.status(): Status = when {
    error != null -> Status(error, Warn, false)
    reconnecting -> Status("RECONNECTING", Warn, true)
    buffering -> Status("BUFFERING", Warn, true)
    wantsAudio -> Status("LIVE", LiveRed, true)
    else -> Status("STOPPED", null, false)
}

@Composable
fun MiniPlayer(now: NowState, onOpen: () -> Unit, onToggle: () -> Unit) {
    val station = now.station ?: return
    val color = rememberStationColor(station)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.38f), Surface1.copy(alpha = 0.96f))))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArt(station, 46.dp, corner = 14.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(now.title ?: station.name, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val status = now.status()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (now.wantsAudio && now.error == null && !now.buffering) {
                    Equalizer(true, color, bars = 3, barWidth = 2.dp, height = 10.dp)
                    Spacer(Modifier.width(6.dp))
                }
                val sub = when {
                    now.error != null -> now.error
                    now.title != null -> now.artist ?: station.name
                    else -> status.text.lowercase().replaceFirstChar { it.uppercase() }
                }
                Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        GlowPlayButton(now.wantsAudio, 44.dp, color, onToggle)
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
    val color = rememberStationColor(station)
    val live = now.wantsAudio && !now.buffering && now.error == null
    AuroraBackground(color, animated = live, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close player", modifier = Modifier.size(30.dp)) }
                Text(
                    "NOW PLAYING",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp, letterSpacing = 2.4.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f),
                )
                IconButton(onClick = onFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = if (favorite) "Remove from favourites" else "Add to favourites", tint = if (favorite) Warn else Color.White.copy(alpha = 0.45f))
                }
            }

            Spacer(Modifier.height(28.dp))
            Box(contentAlignment = Alignment.Center) {
                PulseRings(live, color, corner = 40.dp, modifier = Modifier.size(260.dp))
                StationArt(station, 248.dp, corner = 40.dp, glow = color)
            }
            Spacer(Modifier.height(34.dp))

            Text(
                now.title ?: station.name,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (now.title != null) now.artist ?: station.name else station.subtitle(),
                fontSize = 17.sp,
                color = color,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (now.title != null) {
                Text(
                    "on ${station.name}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val status = now.status()
                StatusPill(status.text, status.dot, status.breathing)
                QualityPill(qualityLabel(now.extras, station), lossless = isLossless(now.extras, station))
            }

            Spacer(Modifier.height(30.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Equalizer(live, color.copy(alpha = 0.4f), bars = 5, barWidth = 4.dp, height = 28.dp)
                Spacer(Modifier.width(32.dp))
                GlowPlayButton(now.wantsAudio, 92.dp, color, onToggle)
                Spacer(Modifier.width(32.dp))
                Equalizer(live, color.copy(alpha = 0.4f), bars = 5, barWidth = 4.dp, height = 28.dp)
            }

            Spacer(Modifier.height(36.dp))
            SleepTimer(now.extras.getLong(SessionInfo.SLEEP_AT), color, onSleep)
            Spacer(Modifier.height(14.dp))
            SignalPath(now.extras, station, bitPerfectWanted, onBitPerfect)

            if (station.homepage.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                PillButton("Station website ↗", filled = false) { onHomepage(station.homepage) }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun isLossless(extras: Bundle, station: Station): Boolean {
    val codec = extras.getString(SessionInfo.CODEC).orEmpty()
    return if (codec.isEmpty()) station.lossless else codec in setOf("FLAC", "ALAC")
}

private fun qualityLabel(extras: Bundle, station: Station): String {
    val codec = extras.getString(SessionInfo.CODEC).orEmpty()
    val rate = extras.getInt(SessionInfo.SAMPLE_RATE)
    val bits = extras.getInt(SessionInfo.BITS)
    if (codec.isEmpty()) return station.formatLabel
    return buildList {
        add(codec)
        if (rate > 0) add("%.1f kHz".format(rate / 1000f))
        if (bits > 0 && codec in setOf("FLAC", "ALAC")) add("$bits-bit")
    }.joinToString(" · ")
}

@Composable
private fun SleepTimer(sleepAt: Long, color: Color, onSleep: (Int) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepAt) {
        while (sleepAt > 0) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    GlassCard(Modifier.widthIn(max = 460.dp).fillMaxWidth(), tint = color) {
        val minutesLeft = ((sleepAt - now) / 60_000 + 1).coerceAtLeast(1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sleep timer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (sleepAt > 0) Text("stops in $minutesLeft min", fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "1h", 90 to "1½h").forEach { (minutes, label) ->
                val selected = minutes == 0 && sleepAt == 0L
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = if (selected) Background else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (selected) color else Color.White.copy(alpha = 0.07f))
                        .clickable { onSleep(minutes) }
                        .padding(vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun SignalPath(extras: Bundle, station: Station, bitPerfectWanted: Boolean, onBitPerfect: (Boolean) -> Unit) {
    val codec = extras.getString(SessionInfo.CODEC).orEmpty()
    val rate = extras.getInt(SessionInfo.SAMPLE_RATE)
    val outRate = extras.getInt(SessionInfo.OUT_SAMPLE_RATE)
    val kind = runCatching { AudioPath.Kind.valueOf(extras.getString(SessionInfo.DEVICE_KIND).orEmpty()) }.getOrDefault(AudioPath.Kind.OTHER)
    val deviceName = extras.getString(SessionInfo.DEVICE_NAME).orEmpty()
    val bitPerfect = extras.getBoolean(SessionInfo.BIT_PERFECT)
    val streamLossless = codec in setOf("FLAC", "ALAC")

    val kindLabel = when (kind) {
        AudioPath.Kind.BLUETOOTH -> "Bluetooth"
        AudioPath.Kind.USB -> "USB"
        AudioPath.Kind.WIRED -> "Wired"
        AudioPath.Kind.SPEAKER -> "Speaker"
        AudioPath.Kind.OTHER -> "Output"
    }
    val (verdictColor, verdict) = when {
        codec.isNotEmpty() && !streamLossless -> Warn to "This station sends compressed (lossy) audio. Look for the FLAC badge for lossless stations."
        kind == AudioPath.Kind.USB && bitPerfect -> Good to "Bit-perfect to your DAC — nothing in between."
        kind == AudioPath.Kind.USB -> Warn to "Android's mixer may resample before the DAC. Turn on bit-perfect below."
        kind == AudioPath.Kind.BLUETOOTH -> Warn to "Re-encoded by the Bluetooth codec, so lossy — unless your phone and earbuds use aptX Lossless."
        kind == AudioPath.Kind.WIRED -> Warn to "Lossless into the phone's DAC; Android's mixer may resample to 48 kHz."
        kind == AudioPath.Kind.SPEAKER -> Warn to "Phone speaker — the stream is fine, the speaker is the limit."
        else -> Good to "Lossless stream."
    }

    GlassCard(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
        Text("Signal path", style = MaterialTheme.typography.titleMedium)
        PathStep("Stream", qualityLabel(extras, station), if (codec.isEmpty()) Color.Gray else if (streamLossless) Good else Warn, last = false)
        PathStep("Decoder", if (outRate > 0) "PCM · %.1f kHz".format(outRate / 1000f) else "PCM", if (outRate == 0 || outRate == rate) Good else Warn, last = false)
        PathStep(kindLabel, deviceName.ifEmpty { "—" }, verdictColor, last = true)
        Text(verdict, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 18.sp)

        if (AudioPath.bitPerfectSupported) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Bit-perfect USB", style = MaterialTheme.typography.titleSmall, fontSize = 15.sp)
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
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Background),
                )
            }
        }
    }
}

/** One hop of the signal chain, joined to the next by a thin line. */
@Composable
private fun PathStep(label: String, value: String, dot: Color, last: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(14.dp)) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            if (!last) Box(Modifier.width(2.dp).height(22.dp).background(Color.White.copy(alpha = 0.12f)))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
