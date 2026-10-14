package io.github.deveshb1.losslessradio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.deveshb1.losslessradio.R

val Background = Color(0xFF07080B)
val Surface1 = Color(0xFF12141A)
val Accent = Color(0xFF7DD3C0)
val Violet = Color(0xFFA78BFA)
val Good = Color(0xFF5BD68A)
val Warn = Color(0xFFFFB547)
val LiveRed = Color(0xFFFF5A6A)

val AccentBrush = Brush.linearGradient(listOf(Accent, Violet))

@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: Int) = Font(
    R.font.space_grotesk,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Space Grotesk for headings; the system font stays for body text. */
val Display = FontFamily(spaceGrotesk(400), spaceGrotesk(500), spaceGrotesk(600), spaceGrotesk(700))

private val Base = Typography()
private val AppTypography = Base.copy(
    displayLarge = Base.displayLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    displaySmall = Base.displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontFamily = Display, fontWeight = FontWeight.Medium),
    labelLarge = Base.labelLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
)

private val AppColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1F4D45),
    onPrimaryContainer = Color(0xFFB8F1E3),
    secondary = Violet,
    secondaryContainer = Color(0xFF2A2440),
    onSecondaryContainer = Color(0xFFE4DBFF),
    background = Background,
    surface = Background,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Color(0xFF1A1D25),
    surfaceVariant = Color(0xFF22252E),
    onSurfaceVariant = Color(0xFFA3A9B6),
    outline = Color(0xFF3A3F4B),
)

@Composable
fun LosslessTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
