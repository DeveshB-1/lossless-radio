package io.github.deveshb1.losslessradio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.deveshb1.losslessradio.Station

val Background = Color(0xFF0F1115)
val Surface1 = Color(0xFF181B21)
val Accent = Color(0xFF7DD3C0)
val Good = Color(0xFF5BD68A)
val Warn = Color(0xFFFFB547)

val AppColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1F4D45),
    onPrimaryContainer = Color(0xFFB8F1E3),
    secondaryContainer = Color(0xFF26302E),
    onSecondaryContainer = Color(0xFFD5E8E3),
    background = Background,
    surface = Background,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Color(0xFF1F232A),
    surfaceVariant = Color(0xFF242830),
)

/** Station logo, or a coloured monogram when there's no usable favicon. */
@Composable
fun StationArt(station: Station, size: Dp, corner: Dp = 12.dp) {
    var failed by remember(station.favicon) { mutableStateOf(station.favicon.isBlank()) }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(monogramBrush(station.name)),
        contentAlignment = Alignment.Center,
    ) {
        if (failed) {
            Text(
                station.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪",
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = station.favicon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    onError = { failed = true },
                    modifier = Modifier.fillMaxSize().padding(size * 0.08f),
                )
            }
        }
    }
}

private fun monogramBrush(name: String): Brush {
    val hue = ((name.hashCode() % 360) + 360) % 360
    val a = Color.hsv(hue.toFloat(), 0.45f, 0.55f)
    val b = Color.hsv(((hue + 40) % 360).toFloat(), 0.55f, 0.35f)
    return Brush.linearGradient(listOf(a, b))
}

@Composable
fun FormatBadge(station: Station) {
    val lossless = station.lossless
    Text(
        station.formatLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (lossless) Color(0xFF0B2E22) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (lossless) Good else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun StationRow(
    station: Station,
    favorite: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (playing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArt(station, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormatBadge(station)
                val place = listOf(station.flag, station.countryCode.uppercase()).filter { it.isNotBlank() }.joinToString(" ")
                val tags = station.tags.split(',').map { it.trim() }.filter { it.isNotBlank() && !it.contains("flac", true) }.take(2)
                val detail = (listOfNotNull(place.ifBlank { null }) + tags).joinToString(" · ")
                if (detail.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        IconButton(onClick = onFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                tint = if (favorite) Warn else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

/** Round play/stop button; stop rather than pause, since this is live radio. */
@Composable
fun PlayStopButton(playing: Boolean, size: Dp, onClick: () -> Unit, background: Color = Color.White, ink: Color = Background) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (playing) "Stop" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.34f)) {
            val s = this.size.width
            if (playing) {
                drawRoundRect(ink, Offset(s * 0.1f, s * 0.1f), Size(s * 0.8f, s * 0.8f), CornerRadius(s * 0.12f))
            } else {
                drawPath(
                    Path().apply {
                        moveTo(s * 0.22f, s * 0.08f)
                        lineTo(s * 0.95f, s * 0.5f)
                        lineTo(s * 0.22f, s * 0.92f)
                        close()
                    },
                    ink,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
