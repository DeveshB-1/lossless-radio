package io.github.deveshb1.losslessradio.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.deveshb1.losslessradio.LibraryState
import io.github.deveshb1.losslessradio.Station

sealed interface Directory {
    data object Loading : Directory
    data class Failed(val message: String) : Directory
    data class Loaded(val stations: List<Station>) : Directory
}

@Composable
fun LibraryScreen(
    library: LibraryState,
    current: Station?,
    contentPadding: PaddingValues,
    onPlay: (Station) -> Unit,
    onFavorite: (Station) -> Unit,
    onDiscover: () -> Unit,
    onAddStation: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lossless Radio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onAddStation) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Add URL")
                }
            }
        }

        if (library.favorites.isEmpty() && library.recents.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("📻", fontSize = 48.sp)
                    Text("Your stations live here", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        "Find CD-quality FLAC stations from around the world, or add any stream by URL. Star the ones you love.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = onDiscover) { Text("Find lossless stations") }
                    OutlinedButton(onClick = onAddStation) { Text("Add a stream URL") }
                }
            }
        }

        if (library.favorites.isNotEmpty()) {
            item { SectionTitle("Favourites") }
            items(library.favorites, key = { "fav-" + it.id }) { s ->
                Box(Modifier.padding(horizontal = 4.dp)) {
                    StationRow(s, favorite = true, playing = s.id == current?.id, onPlay = { onPlay(s) }, onFavorite = { onFavorite(s) })
                }
            }
        }
        if (library.recents.isNotEmpty()) {
            item { SectionTitle("Recently played", Modifier.padding(top = 8.dp)) }
            items(library.recents, key = { "recent-" + it.id }) { s ->
                Box(Modifier.padding(horizontal = 4.dp)) {
                    StationRow(s, favorite = library.isFavorite(s), playing = s.id == current?.id, onPlay = { onPlay(s) }, onFavorite = { onFavorite(s) })
                }
            }
        }
    }
}

@Composable
fun DiscoverScreen(
    query: String,
    losslessOnly: Boolean,
    results: Directory,
    library: LibraryState,
    current: Station?,
    contentPadding: PaddingValues,
    onQuery: (String) -> Unit,
    onLosslessOnly: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onPlay: (Station) -> Unit,
    onFavorite: (Station) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Discover", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text(if (losslessOnly) "Filter lossless stations" else "Search 50,000+ stations") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = losslessOnly,
                        onClick = { onLosslessOnly(true) },
                        label = { Text("Lossless only") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Good, selectedLabelColor = Background),
                    )
                    FilterChip(selected = !losslessOnly, onClick = { onLosslessOnly(false) }, label = { Text("All stations") })
                }
            }
        }

        when (results) {
            Directory.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is Directory.Failed -> item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(results.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = onRetry) { Text("Try again") }
                }
            }
            is Directory.Loaded -> {
                if (results.stations.isEmpty()) {
                    item {
                        Text(
                            "No stations match.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    item {
                        Text(
                            if (losslessOnly) "${results.stations.size} lossless stations" else "${results.stations.size} stations",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(results.stations, key = { it.id }) { s ->
                        Box(Modifier.padding(horizontal = 4.dp)) {
                            StationRow(s, favorite = library.isFavorite(s), playing = s.id == current?.id, onPlay = { onPlay(s) }, onFavorite = { onFavorite(s) })
                        }
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
}
