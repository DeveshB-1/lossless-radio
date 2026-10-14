package io.github.deveshb1.losslessradio.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt(), hsv)
    return Color.hsv((hsv[0] + degrees + 360) % 360, hsv[1], hsv[2])
}

/**
 * Slowly drifting light in the station's colour: three soft radial glows over near-black,
 * fading into the background at the bottom. Static when [animated] is false.
 */
@Composable
fun AuroraBackground(color: Color, animated: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "aurora")
    fun drift(ms: Int) = infiniteRepeatable<Float>(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    val a by t.animateFloat(0f, 1f, drift(13_000), label = "a")
    val b by t.animateFloat(0f, 1f, drift(17_000), label = "b")
    val c by t.animateFloat(0f, 1f, drift(21_000), label = "c")
    val (pa, pb, pc) = if (animated) Triple(a, b, c) else Triple(0.5f, 0.5f, 0.5f)
    val second = remember(color) { shiftHue(color, 40f) }
    val third = remember(color) { shiftHue(color, -50f) }

    Box(modifier.background(Background)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun glow(c: Color, x: Float, y: Float, r: Float, alpha: Float) = drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = alpha), Color.Transparent), center = Offset(x, y), radius = r),
                radius = r,
                center = Offset(x, y),
            )
            glow(color, w * (0.2f + 0.5f * pa), h * (0.12f + 0.1f * pb), w * 0.95f, 0.55f)
            glow(second, w * (0.9f - 0.4f * pb), h * (0.32f + 0.12f * pc), w * 0.8f, 0.38f)
            glow(third, w * (0.1f + 0.3f * pc), h * (0.48f - 0.08f * pa), w * 0.7f, 0.25f)
            drawRect(Brush.verticalGradient(0.35f to Color.Transparent, 0.9f to Background))
        }
        content()
    }
}

/** Equaliser bars that dance while audio plays and rest when it doesn't. Decorative only. */
@Composable
fun Equalizer(playing: Boolean, color: Color, modifier: Modifier = Modifier, bars: Int = 4, barWidth: Dp = 3.dp, height: Dp = 16.dp) {
    val t = rememberInfiniteTransition(label = "eq")
    val speeds = listOf(420, 560, 380, 640, 500)
    val levels = (0 until bars).map { i ->
        t.animateFloat(0.15f, 1f, infiniteRepeatable(tween(speeds[i % speeds.size], easing = LinearEasing), RepeatMode.Reverse), label = "bar$i")
    }
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(barWidth * 0.8f), verticalAlignment = Alignment.Bottom) {
        levels.forEach { level ->
            val target = if (playing) level.value else 0.2f
            val shown by animateFloatAsState(target, spring(), label = "level")
            Box(
                Modifier
                    .width(barWidth)
                    .fillMaxHeight(shown)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(color),
            )
        }
    }
}

/** Big round button whose icon morphs between play and stop, with a glow in the station colour. */
@Composable
fun GlowPlayButton(playing: Boolean, size: Dp, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f), label = "press")
    val morph by animateFloatAsState(if (playing) 1f else 0f, tween(260), label = "morph")
    val ink = Background

    Box(
        modifier
            .scale(scale)
            .size(size)
            .shadow(size / 2.5f, CircleShape, ambientColor = color, spotColor = color)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(lerp(color, Color.White, 0.35f), color)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = if (playing) "Stop" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.36f)) {
            val s = this.size.width
            // Play is drawn as a quad with two coincident tips so it can morph into the stop square.
            val play = listOf(0.26f to 0.10f, 0.95f to 0.5f, 0.95f to 0.5f, 0.26f to 0.90f)
            val stop = listOf(0.14f to 0.14f, 0.86f to 0.14f, 0.86f to 0.86f, 0.14f to 0.86f)
            val pts = play.zip(stop) { p, q -> Offset(s * (p.first + (q.first - p.first) * morph), s * (p.second + (q.second - p.second) * morph)) }
            drawPath(Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }, ink)
        }
    }
}

/** Rings that ripple outward from the artwork while the station is live. */
@Composable
fun PulseRings(active: Boolean, color: Color, corner: Dp, modifier: Modifier = Modifier) {
    if (!active) return
    val t = rememberInfiniteTransition(label = "pulse")
    val p1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p1")
    Canvas(modifier) {
        listOf(p1, (p1 + 0.5f) % 1f).forEach { p ->
            val grow = 1f + 0.18f * p
            val w = size.width * grow
            val h = size.height * grow
            drawRoundRect(
                color = color.copy(alpha = 0.45f * (1f - p)),
                topLeft = Offset((size.width - w) / 2, (size.height - h) / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(corner.toPx() * grow),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, tint: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f))))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** "LIVE" pill with a breathing dot, or a status message when not playing cleanly. */
@Composable
fun StatusPill(text: String, dot: Color?, breathing: Boolean) {
    val t = rememberInfiniteTransition(label = "breathe")
    val alpha by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot.copy(alpha = if (breathing) alpha else 1f)))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
    }
}

/** Gradient-outlined quality badge, e.g. "FLAC · 44.1 kHz · 16-bit". */
@Composable
fun QualityPill(text: String, lossless: Boolean) {
    val brush = if (lossless) AccentBrush else Brush.linearGradient(listOf(Warn, Warn))
    Box(
        Modifier
            .clip(CircleShape)
            .border(BorderStroke(1.dp, brush), CircleShape)
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            style = androidx.compose.ui.text.TextStyle(brush = brush),
        )
    }
}

@Composable
fun Glow(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))))
}

@Composable
fun FillerSpace(height: Dp) = Spacer(Modifier.fillMaxWidth().height(height))
