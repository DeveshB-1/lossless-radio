package io.github.deveshb1.losslessradio.ui

import android.content.Context
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.deveshb1.losslessradio.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Each station gets a signature colour pulled from its logo, used to tint the UI around it. */
object StationColors {
    private val cache = LruCache<String, Color>(300)

    /** A pleasant colour derived from the name, for stations without a usable logo. */
    fun fallback(name: String): Color {
        val hue = ((name.hashCode() % 360) + 360) % 360
        return Color.hsv(hue.toFloat(), 0.55f, 0.85f)
    }

    fun cached(station: Station): Color = cache.get(station.id) ?: fallback(station.name)

    suspend fun extract(context: Context, station: Station): Color {
        cache.get(station.id)?.let { return it }
        val color = station.favicon.takeIf { it.startsWith("http") }?.let { fromLogo(context, it) } ?: fallback(station.name)
        cache.put(station.id, color)
        return color
    }

    private suspend fun fromLogo(context: Context, url: String): Color? = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(128).build()
            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return@runCatching null
            val palette = Palette.from(result.image.toBitmap()).generate()
            val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
            swatch?.let { normalize(Color(it.rgb)) }
        }.getOrNull()
    }

    /** Keep logo colours vivid enough to glow but never neon; near-greys get no say. */
    private fun normalize(color: Color): Color? {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        if (hsv[1] < 0.15f) return null
        hsv[1] = hsv[1].coerceIn(0.45f, 0.85f)
        hsv[2] = hsv[2].coerceIn(0.7f, 0.95f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}

@Composable
fun rememberStationColor(station: Station?): Color {
    val context = LocalContext.current
    var color by remember(station?.id) { mutableStateOf(station?.let(StationColors::cached) ?: Accent) }
    LaunchedEffect(station?.id) {
        if (station != null) color = StationColors.extract(context, station)
    }
    return animateColorAsState(color, tween(900), label = "stationColor").value
}
