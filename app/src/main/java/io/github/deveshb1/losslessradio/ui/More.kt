package io.github.deveshb1.losslessradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.deveshb1.losslessradio.TipJar

@Composable
fun AboutScreen(
    tipJar: TipJar,
    version: String,
    contentPadding: PaddingValues,
    onTip: (TipJar.Tip) -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

        Card {
            Text("Lossless Radio", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Internet radio in CD quality. Lossless stations stream FLAC — every bit of the original — " +
                    "and the signal-path view shows exactly what happens to the audio on your phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Text("Version $version", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        if (tipJar.status == TipJar.Status.READY) {
            Card {
                Text("Support the app", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Lossless Radio is free with no ads. A tip helps keep it going — it unlocks nothing, it's just a thank-you.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tipJar.tips.forEachIndexed { i, tip ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onTip(tip) }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(listOf("☕", "🍕", "🎧").getOrElse(i) { "💛" }, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(tip.price, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                tipJar.message?.let { Text(it, color = Good, fontSize = 13.sp) }
            }
        }

        Card {
            Text("Stations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Station listings come from radio-browser.info, a free, community-run directory. Streams belong to their " +
                    "broadcasters — many are listener-supported, so consider supporting the ones you enjoy.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Link("radio-browser.info ↗") { onOpen("https://www.radio-browser.info") }
        }

        Card {
            Text("Privacy", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "No accounts, no ads, no tracking. Your favourites stay on your phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Link("Privacy policy ↗") { onOpen("https://deveshb-1.github.io/lossless-radio/privacy.html") }
            Link("Source code ↗") { onOpen("https://github.com/DeveshB-1/lossless-radio") }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp))
}

@Composable
fun AddStationDialog(onDismiss: () -> Unit, onAdd: (name: String, url: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val valid = url.trim().let { it.startsWith("http://") || it.startsWith("https://") } && url.trim().length > 10
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a station") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste a stream link — direct streams, .pls and .m3u playlists, and HLS all work.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (optional)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onAdd(name.trim(), url.trim()) }) { Text("Add & play") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
