package io.github.deveshb1.losslessradio.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.deveshb1.losslessradio.Station

/** Station logo on white, or a gradient monogram when there's no usable logo. Optional coloured glow. */
@Composable
fun StationArt(station: Station, size: Dp, corner: Dp = 14.dp, glow: Color? = null) {
    var failed by remember(station.favicon) { mutableStateOf(station.favicon.isBlank()) }
    val shape = RoundedCornerShape(corner)
    val base = StationColors.cached(station)
    Box(
        Modifier
            .size(size)
            .then(if (glow != null) Modifier.shadow(size / 6, shape, ambientColor = glow, spotColor = glow) else Modifier)
            .clip(shape)
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.55f).compositeOver(Background)))),
        contentAlignment = Alignment.Center,
    ) {
        if (failed) {
            Text(
                station.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪",
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontFamily = Display,
                fontWeight = FontWeight.Bold,
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

private fun Color.compositeOver(background: Color): Color = Color(
    red = red * alpha + background.red * (1 - alpha),
    green = green * alpha + background.green * (1 - alpha),
    blue = blue * alpha + background.blue * (1 - alpha),
)

@Composable
fun FormatBadge(station: Station) {
    if (station.lossless) {
        Text(
            station.formatLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = Background,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentBrush).padding(horizontal = 7.dp, vertical = 2.dp),
        )
    } else {
        Text(
            station.formatLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

fun Station.subtitle(): String {
    val place = listOf(flag, countryCode.uppercase()).filter { it.isNotBlank() }.joinToString(" ")
    val genres = tags.split(',').map { it.trim() }.filter { it.isNotBlank() && !it.contains("flac", true) && !it.contains("lossless", true) }.take(2)
    return (listOfNotNull(place.ifBlank { null }) + genres).joinToString(" · ")
}

@Composable
fun StationRow(
    station: Station,
    favorite: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
) {
    val color = rememberStationColor(station)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (playing) Brush.horizontalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArt(station, 54.dp, corner = 16.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playing) {
                    Equalizer(true, color, bars = 3, barWidth = 2.5.dp, height = 12.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (playing) color else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormatBadge(station)
                val detail = station.subtitle()
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
                tint = if (favorite) Warn else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }
    }
}

/** Tall card for carousels: station colour wash, glowing logo, name and badge. */
@Composable
fun StationCard(station: Station, playing: Boolean, width: Dp, onClick: () -> Unit) {
    val color = rememberStationColor(station)
    Column(
        Modifier
            .width(width)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.12f), Surface1)))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            StationArt(station, width - 28.dp, corner = 18.dp, glow = color)
            if (playing) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Background.copy(alpha = 0.75f)).padding(6.dp),
                ) { Equalizer(true, color, bars = 4, barWidth = 3.dp, height = 14.dp) }
            }
        }
        Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormatBadge(station)
            if (station.flag.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(station.flag, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
