package io.github.deveshb1.losslessradio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.deveshb1.losslessradio.LibraryState
import io.github.deveshb1.losslessradio.Station

sealed interface Directory {
    data object Loading : Directory
    data class Failed(val message: String) : Directory
    data class Loaded(val stations: List<Station>) : Directory
}

/** Real genres worth a chip; directory tags are otherwise full of places and marketing words. */
private val GENRES = listOf(
    "rock", "jazz", "classical", "electronic", "lounge", "ambient", "chillout", "blues", "pop", "metal", "alternative",
    "indie", "folk", "world", "soul", "funk", "dance", "house", "techno", "trance", "reggae", "latin", "country",
    "hip hop", "oldies", "80s", "70s", "60s", "eclectic", "news", "talk",
)

/** Genres present among [stations], most common first. */
fun topGenres(stations: List<Station>, max: Int = 12): List<String> {
    val counts = GENRES.associateWith { g -> stations.count { s -> s.tags.split(',').any { it.trim().lowercase().contains(g) } } }
    return counts.filterValues { it > 0 }.entries.sortedByDescending { it.value }.take(max).map { it.key }
}

/** One station per broadcaster, so a carousel isn't five channels of the same network. */
fun featured(stations: List<Station>, max: Int = 8): List<Station> =
    stations.distinctBy { s ->
        s.homepage.substringAfter("://").substringBefore('/').removePrefix("www.").lowercase()
            .ifBlank { s.name.lowercase().split(' ').take(2).joinToString(" ") }
    }.take(max)

@Composable
private fun ScreenHeader(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displaySmall.copy(brush = AccentBrush))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        action?.invoke()
    }
}

@Composable
fun LibraryScreen(
    library: LibraryState,
    now: NowState,
    contentPadding: PaddingValues,
    onPlay: (Station) -> Unit,
    onFavorite: (Station) -> Unit,
    onDiscover: () -> Unit,
    onAddStation: () -> Unit,
    onOpenPlayer: () -> Unit,
    onToggle: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            ScreenHeader("Lossless", "Radio in CD quality") {
                IconButton(
                    onClick = onAddStation,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.07f)),
                ) { Icon(Icons.Filled.Add, contentDescription = "Add a station by URL") }
            }
        }

        now.station?.let { station -> item { NowPlayingHero(now, station, onOpenPlayer, onToggle) } }

        if (library.favorites.isEmpty() && library.recents.isEmpty()) {
            item { EmptyLibrary(onDiscover, onAddStation) }
        }

        if (library.favorites.isNotEmpty()) {
            item { SectionTitle("Favourites", trailing = "${library.favorites.size}") }
            items(library.favorites.chunked(2), key = { row -> "fav-" + row.joinToString { it.id } }) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { s ->
                        FavoriteTile(s, playing = s.id == now.station?.id && now.wantsAudio, modifier = Modifier.weight(1f), onClick = { onPlay(s) }, onLongStar = { onFavorite(s) })
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        val recents = library.recents.filterNot { r -> library.favorites.any { it.id == r.id } }
        if (recents.isNotEmpty()) {
            item { SectionTitle("Recently played") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recents, key = { "recent-" + it.id }) { s ->
                        StationCard(s, playing = s.id == now.station?.id && now.wantsAudio, width = 150.dp, onClick = { onPlay(s) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun NowPlayingHero(now: NowState, station: Station, onOpen: () -> Unit, onToggle: () -> Unit) {
    val color = rememberStationColor(station)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.15f), Surface1)))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(28.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArt(station, 76.dp, corner = 20.dp, glow = color)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Equalizer(now.wantsAudio && !now.buffering, Color.White, bars = 4, barWidth = 2.5.dp, height = 11.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (now.wantsAudio) "NOW PLAYING" else "PAUSED",
                    fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(now.title ?: station.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (now.title != null) now.artist ?: station.name else station.formatLabel,
                color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        GlowPlayButton(now.wantsAudio, 52.dp, color, onToggle)
    }
}

@Composable
private fun FavoriteTile(station: Station, playing: Boolean, modifier: Modifier, onClick: () -> Unit, onLongStar: () -> Unit) {
    val color = rememberStationColor(station)
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.42f), color.copy(alpha = 0.08f), Surface1)))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            StationArt(station, 58.dp, corner = 16.dp, glow = color)
            Spacer(Modifier.weight(1f))
            if (playing) Equalizer(true, Color.White, bars = 3, barWidth = 2.5.dp, height = 12.dp)
        }
        Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormatBadge(station)
            Spacer(Modifier.weight(1f))
            Text(
                "★",
                color = Warn,
                fontSize = 16.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onLongStar).padding(4.dp),
            )
        }
    }
}

@Composable
private fun EmptyLibrary(onDiscover: () -> Unit, onAddStation: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Glow(Accent, Modifier.size(200.dp))
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.35f), Violet.copy(alpha = 0.35f))))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Equalizer(true, Color.White, bars = 5, barWidth = 6.dp, height = 40.dp) }
        }
        Text("Every bit of the music", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Find stations streaming lossless FLAC from around the world, or add any stream by URL. Star the ones you love.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        PillButton("Find lossless stations", filled = true, onClick = onDiscover)
        PillButton("Add a stream URL", filled = false, onClick = onAddStation)
    }
}

@Composable
fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(AccentBrush)
                else Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 13.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp, color = if (filled) Background else Color.White)
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) Background else Color.White.copy(alpha = 0.85f),
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(AccentBrush)
                else Modifier.background(Color.White.copy(alpha = 0.06f)).border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun DiscoverScreen(
    query: String,
    losslessOnly: Boolean,
    genre: String?,
    genres: List<String>,
    results: Directory,
    library: LibraryState,
    now: NowState,
    contentPadding: PaddingValues,
    onQuery: (String) -> Unit,
    onLosslessOnly: (Boolean) -> Unit,
    onGenre: (String?) -> Unit,
    onRetry: () -> Unit,
    onPlay: (Station) -> Unit,
    onFavorite: (Station) -> Unit,
) {
    val current = now.station?.takeIf { now.wantsAudio }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { ScreenHeader("Discover", if (losslessOnly) "Lossless stations worldwide" else "50,000+ stations worldwide") }
        item {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text(if (losslessOnly) "Filter by name, genre, country" else "Search all stations") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") } },
                singleLine = true,
                shape = CircleShape,
                textStyle = TextStyle(fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Lossless only", losslessOnly) { onLosslessOnly(true) }
                Chip("All stations", !losslessOnly) { onLosslessOnly(false) }
            }
        }

        when (results) {
            Directory.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(56.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            }
            is Directory.Failed -> item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(results.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    PillButton("Try again", filled = false, onClick = onRetry)
                }
            }
            is Directory.Loaded -> {
                val browsing = losslessOnly && query.isBlank() && genre == null
                if (browsing && results.stations.size > 6) {
                    item { SectionTitle("Featured") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(featured(results.stations), key = { "feat-" + it.id }) { s ->
                                StationCard(s, playing = s.id == current?.id, width = 168.dp, onClick = { onPlay(s) })
                            }
                        }
                    }
                }
                if (losslessOnly && genres.isNotEmpty() && query.isBlank()) {
                    item { SectionTitle("Genres") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(genres, key = { "genre-$it" }) { g ->
                                Chip(g.replaceFirstChar { it.uppercase() }, genre == g) { onGenre(if (genre == g) null else g) }
                            }
                        }
                    }
                }
                item {
                    SectionTitle(
                        when {
                            genre != null -> genre.replaceFirstChar { it.uppercase() }
                            losslessOnly -> "All lossless"
                            else -> "Results"
                        },
                        trailing = "${results.stations.size} stations",
                    )
                }
                if (results.stations.isEmpty()) {
                    item {
                        Text(
                            "No stations match.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(results.stations, key = { it.id }) { s ->
                    StationRow(s, favorite = library.isFavorite(s), playing = s.id == current?.id, onPlay = { onPlay(s) }, onFavorite = { onFavorite(s) })
                }
                item {
                    Text(
                        "Station directory: radio-browser.info, a free community project.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
        }
    }
}
