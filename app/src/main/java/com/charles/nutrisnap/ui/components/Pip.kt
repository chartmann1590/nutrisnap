package com.charles.nutrisnap.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charles.nutrisnap.ui.theme.Berry
import com.charles.nutrisnap.ui.theme.Cocoa
import com.charles.nutrisnap.ui.theme.Mango
import com.charles.nutrisnap.ui.theme.Mint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pip's expressions. Drives posture, eyes and mouth in [Pip]. Mood-specific
 * branches are opt-in: the `when` blocks fall back to [Content] via `else`.
 */
enum class PipMood {
    Content,
    Celebrate,
    Proud,
    Sleepy,
    Stuffed,
    Thinking,
}

/**
 * Pip — the NutriSnap mascot, a cute mango with a leaf, big eyes and rosy cheeks.
 * Drawn vectorially so it stays crisp at any size. Pip reacts to [mood]: posture,
 * eyes and mouth change per mood. Always-on touches when [animated]: a bob with
 * squash-and-stretch, and periodic blinking. Tapping Pip triggers a poke wobble
 * and calls [onPoke].
 */
@Composable
fun Pip(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    mood: PipMood = PipMood.Content,
    accessory: PipAccessory = PipAccessory.NONE,
    animated: Boolean = true,
    onPoke: () -> Unit = {},
) {
    // Bob speed/height vary by mood (Celebrate is fast & big; Sleepy is slow & small).
    val bobPeriodMs = when (mood) {
        PipMood.Celebrate -> 520
        PipMood.Sleepy -> 3400
        PipMood.Stuffed -> 3000
        else -> 2400
    }
    val bobHeight = when (mood) {
        PipMood.Celebrate -> 0.16f
        PipMood.Sleepy -> 0.03f
        else -> 0.06f
    }

    val transition = rememberInfiniteTransition(label = "pip")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(bobPeriodMs), RepeatMode.Reverse),
        label = "bob",
    )
    // Slow woozy sway for the Stuffed mood (degrees).
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "sway",
    )
    // Slow orbit for CONFETTI_HALO dots.
    val haloRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "haloRotation",
    )

    // Blink: eyes are open (1f) most of the time, snapping shut briefly at random.
    val eyeOpen = remember { Animatable(1f) }
    if (animated) {
        LaunchedEffect(mood) {
            // Sleepy keeps eyes half-closed and blinks rarely.
            val base = if (mood == PipMood.Sleepy) 0.35f else 1f
            eyeOpen.snapTo(base)
            while (true) {
                delay(Random.nextLong(2400, 4800))
                eyeOpen.animateTo(0.08f, tween(70))
                eyeOpen.animateTo(base, tween(110))
            }
        }
    }

    // Tap-to-poke: one-shot 0->1->0 that adds extra squish + a small spin.
    val poke = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Canvas(
        modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onPoke()
                        scope.launch {
                            poke.snapTo(0f)
                            poke.animateTo(1f, tween(120))
                            poke.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 320f))
                        }
                    },
                )
            },
    ) {
        val w = this.size.width
        val h = this.size.height
        val dy = -bob * h * bobHeight

        // Squash-and-stretch: stretch tall near the top of the bob, squash wide at the
        // bottom. Poke adds an extra squash pulse.
        val stretch = if (animated) (bob - 0.5f) * 2f else 0f
        val pokeSquash = sin(poke.value * Math.PI.toFloat())
        val sx = 1f + 0.05f * stretch + 0.10f * pokeSquash
        val sy = 1f - 0.05f * stretch - 0.10f * pokeSquash

        val swayDeg = if (mood == PipMood.Stuffed) sway * 4f else 0f
        val pokeSpinDeg = poke.value * 8f

        fun x(p: Float) = w * p
        fun y(p: Float) = h * p + dy

        // soft shadow (drawn outside the squash transform so the ground stays put)
        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(x(0.25f), h * 0.9f),
            size = Size(w * 0.5f, h * 0.06f),
        )

        rotate(degrees = swayDeg + pokeSpinDeg, pivot = Offset(w * 0.5f, h * 0.6f)) {
            scale(scaleX = sx, scaleY = sy, pivot = Offset(w * 0.5f, h * 0.95f)) {
                // body — Stuffed mood is a touch wider
                val widen = if (mood == PipMood.Stuffed) 0.04f else 0f
                val body = Path().apply {
                    moveTo(x(0.5f), y(0.13f))
                    cubicTo(x(0.84f + widen), y(0.13f), x(0.92f + widen), y(0.45f), x(0.92f + widen), y(0.6f))
                    cubicTo(x(0.92f + widen), y(0.86f), x(0.72f), y(0.95f), x(0.5f), y(0.95f))
                    cubicTo(x(0.28f), y(0.95f), x(0.08f - widen), y(0.86f), x(0.08f - widen), y(0.6f))
                    cubicTo(x(0.08f - widen), y(0.45f), x(0.16f - widen), y(0.13f), x(0.5f), y(0.13f))
                    close()
                }
                drawPath(body, Mango)

                // leaf
                val leaf = Path().apply {
                    moveTo(x(0.52f), y(0.14f))
                    cubicTo(x(0.54f), y(0.02f), x(0.66f), y(0.0f), x(0.66f), y(0.0f))
                    cubicTo(x(0.66f), y(0.0f), x(0.66f), y(0.1f), x(0.58f), y(0.13f))
                    cubicTo(x(0.54f), y(0.145f), x(0.52f), y(0.14f), x(0.52f), y(0.14f))
                    close()
                }
                drawPath(leaf, Mint)

                // Proud mood: a little sparkle by the leaf
                if (mood == PipMood.Proud) {
                    val spark = w * 0.03f
                    val cx = x(0.78f)
                    val cy = y(0.16f)
                    drawLine(Color.White, Offset(cx - spark, cy), Offset(cx + spark, cy), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(cx, cy - spark), Offset(cx, cy + spark), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
                }

                // cheeks
                drawCircle(Berry.copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(x(0.33f), y(0.62f)))
                drawCircle(Berry.copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(x(0.67f), y(0.62f)))

                // eyes — pupils look up for Thinking; eyes squeeze to happy arcs for Celebrate
                val open = eyeOpen.value
                val pupilDy = if (mood == PipMood.Thinking) -w * 0.02f else 0f
                if (mood == PipMood.Celebrate) {
                    // Celebrate uses fixed happy arcs; the blink loop has no visual effect here.
                    // happy upturned arcs ^ ^
                    val arc = w * 0.05f
                    drawArc(
                        color = Cocoa,
                        startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(x(0.4f) - arc, y(0.52f) - arc),
                        size = Size(arc * 2, arc * 2),
                        style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Cocoa,
                        startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(x(0.6f) - arc, y(0.52f) - arc),
                        size = Size(arc * 2, arc * 2),
                        style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
                    )
                } else {
                    // round eyes that close vertically when blinking (scaled by `open`)
                    val eyeR = w * 0.06f
                    drawOval(
                        Color.White,
                        topLeft = Offset(x(0.4f) - eyeR, y(0.52f) - eyeR * open),
                        size = Size(eyeR * 2, eyeR * 2 * open),
                    )
                    drawOval(
                        Color.White,
                        topLeft = Offset(x(0.6f) - eyeR, y(0.52f) - eyeR * open),
                        size = Size(eyeR * 2, eyeR * 2 * open),
                    )
                    if (open > 0.4f) {
                        drawCircle(Cocoa, radius = w * 0.03f, center = Offset(x(0.41f), y(0.53f) + pupilDy))
                        drawCircle(Cocoa, radius = w * 0.03f, center = Offset(x(0.61f), y(0.53f) + pupilDy))
                    }
                }

                // mouth per mood
                when (mood) {
                    PipMood.Celebrate -> {
                        // big open happy mouth
                        val mouth = Path().apply {
                            moveTo(x(0.43f), y(0.64f))
                            cubicTo(x(0.47f), y(0.74f), x(0.53f), y(0.74f), x(0.57f), y(0.64f))
                            close()
                        }
                        drawPath(mouth, Cocoa)
                    }
                    PipMood.Sleepy -> {
                        // small yawning "o"
                        drawCircle(Cocoa, radius = w * 0.025f, center = Offset(x(0.5f), y(0.67f)))
                    }
                    PipMood.Stuffed -> {
                        // flat wavy line
                        val mouth = Path().apply {
                            moveTo(x(0.43f), y(0.66f))
                            cubicTo(x(0.47f), y(0.63f), x(0.53f), y(0.69f), x(0.57f), y(0.66f))
                        }
                        drawPath(mouth, Cocoa, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))
                    }
                    PipMood.Thinking -> {
                        // tiny pursed line
                        drawLine(Cocoa, Offset(x(0.46f), y(0.66f)), Offset(x(0.54f), y(0.66f)), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
                    }
                    else -> {
                        // gentle smile (Content, Proud)
                        val smile = Path().apply {
                            moveTo(x(0.43f), y(0.64f))
                            cubicTo(x(0.47f), y(0.70f), x(0.53f), y(0.70f), x(0.57f), y(0.64f))
                        }
                        drawPath(smile, Cocoa, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))
                    }
                }

                // Draw accessory on top of Pip body; bobs/squashes with Pip.
                when (accessory) {
                    PipAccessory.NONE -> { /* nothing */ }
                    PipAccessory.CHEF_HAT -> drawChefHat(w, h, dy)
                    PipAccessory.PARTY_CROWN -> drawPartyCrown(w, h, dy)
                    PipAccessory.GOLDEN_LEAF -> drawGoldenLeaf(w, h, dy)
                    PipAccessory.HEART_GLASSES -> drawHeartGlasses(w, h, dy)
                    PipAccessory.CONFETTI_HALO -> drawConfettiHalo(w, h, dy, haloRotation)
                    PipAccessory.RAINBOW_HALO -> drawRainbowHalo(w, h, dy)
                }
            }
        }
    }
}

// ── Accessory draw helpers ────────────────────────────────────────────────────
// All are DrawScope extension functions so they share the Canvas drawing API.
// They define local x()/y() helpers matching the ones inside the Pip Canvas.

private val Butter = Color(0xFFFFD66B)
private val Sky = Color(0xFF5BC0EB)

/** White toque with puffed dome and cylinder brim. */
private fun DrawScope.drawChefHat(w: Float, h: Float, dy: Float) {
    fun x(p: Float) = w * p
    fun y(p: Float) = h * p + dy
    val white = Color.White
    val outline = Color(0xFF333333)
    val strokeW = w * 0.018f

    // Puffed dome (ellipse above the leaf)
    val domeRx = w * 0.19f
    val domeRy = h * 0.13f
    val domeCx = x(0.5f)
    val domeCy = y(0.01f)
    drawOval(white, topLeft = Offset(domeCx - domeRx, domeCy - domeRy), size = Size(domeRx * 2, domeRy * 2))
    drawOval(outline, topLeft = Offset(domeCx - domeRx, domeCy - domeRy), size = Size(domeRx * 2, domeRy * 2), style = Stroke(width = strokeW))

    // Cylinder (band just above body top)
    val cylLeft = x(0.25f)
    val cylTop = y(0.1f)
    val cylW = w * 0.5f
    val cylH = h * 0.09f
    drawRoundRect(white, topLeft = Offset(cylLeft, cylTop), size = Size(cylW, cylH), cornerRadius = CornerRadius(w * 0.025f))
    drawRoundRect(outline, topLeft = Offset(cylLeft, cylTop), size = Size(cylW, cylH), cornerRadius = CornerRadius(w * 0.025f), style = Stroke(width = strokeW))

    // Gray brim line at the bottom of cylinder
    drawLine(Color(0xFFAAAAAA), Offset(cylLeft, cylTop + cylH), Offset(cylLeft + cylW, cylTop + cylH), strokeWidth = strokeW * 1.5f)
}

/** Butter-yellow zigzag crown with 3 coloured tips. */
private fun DrawScope.drawPartyCrown(w: Float, h: Float, dy: Float) {
    fun x(p: Float) = w * p
    fun y(p: Float) = h * p + dy
    val outline = Color(0xFF333333)
    val strokeW = w * 0.015f

    // Base of crown sits just above body top; three upward points.
    val crown = Path().apply {
        moveTo(x(0.22f), y(0.1f))
        lineTo(x(0.22f), y(0.05f))
        lineTo(x(0.3f), y(-0.06f))   // left tip
        lineTo(x(0.38f), y(0.03f))
        lineTo(x(0.5f), y(-0.11f))   // centre tip
        lineTo(x(0.62f), y(0.03f))
        lineTo(x(0.7f), y(-0.06f))   // right tip
        lineTo(x(0.78f), y(0.05f))
        lineTo(x(0.78f), y(0.1f))
        close()
    }
    drawPath(crown, Butter)
    drawPath(crown, outline, style = Stroke(width = strokeW, cap = StrokeCap.Round))

    // Small coloured gems on each tip
    drawCircle(Berry, radius = w * 0.027f, center = Offset(x(0.3f), y(-0.06f)))
    drawCircle(Mint,  radius = w * 0.027f, center = Offset(x(0.5f), y(-0.11f)))
    drawCircle(Mango, radius = w * 0.027f, center = Offset(x(0.7f), y(-0.06f)))
}

/** Oversized leaf in golden colour with shimmer highlights. */
private fun DrawScope.drawGoldenLeaf(w: Float, h: Float, dy: Float) {
    fun x(p: Float) = w * p
    fun y(p: Float) = h * p + dy
    // Use scale() (already imported) to draw the standard leaf 1.3× larger.
    scale(1.3f, pivot = Offset(x(0.59f), y(0.07f))) {
        val leaf = Path().apply {
            moveTo(x(0.52f), y(0.14f))
            cubicTo(x(0.54f), y(0.02f), x(0.66f), y(0.0f),  x(0.66f), y(0.0f))
            cubicTo(x(0.66f), y(0.0f),  x(0.66f), y(0.1f),  x(0.58f), y(0.13f))
            cubicTo(x(0.54f), y(0.145f),x(0.52f), y(0.14f), x(0.52f), y(0.14f))
            close()
        }
        drawPath(leaf, Butter)
        // Two subtle shimmer lines diagonally across the leaf
        drawLine(Color.White.copy(alpha = 0.55f),
            start = Offset(x(0.54f), y(0.12f)), end = Offset(x(0.63f), y(0.03f)),
            strokeWidth = w * 0.012f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.3f),
            start = Offset(x(0.56f), y(0.13f)), end = Offset(x(0.65f), y(0.05f)),
            strokeWidth = w * 0.008f, cap = StrokeCap.Round)
    }
}

/** Berry-pink heart shapes over each eye. */
private fun DrawScope.drawHeartGlasses(w: Float, h: Float, dy: Float) {
    fun x(p: Float) = w * p
    fun y(p: Float) = h * p + dy
    val outline = Color(0xFF333333)
    val strokeW = w * 0.013f
    // Eye centres (mirroring the Pip eye x positions)
    for (ex in listOf(x(0.41f), x(0.61f))) {
        val ey = y(0.52f)
        val path = pipHeartPath(ex, ey, w * 0.09f)
        drawPath(path, Berry)
        drawPath(path, outline, style = Stroke(width = strokeW))
    }
}

private fun pipHeartPath(cx: Float, cy: Float, size: Float): Path {
    val s = size * 0.5f
    return Path().apply {
        moveTo(cx, cy + s * 0.8f)
        cubicTo(cx - s * 1.2f, cy,           cx - s * 1.2f, cy - s * 0.8f, cx, cy - s * 0.3f)
        cubicTo(cx + s * 1.2f, cy - s * 0.8f, cx + s * 1.2f, cy,           cx, cy + s * 0.8f)
        close()
    }
}

/** 8 coloured dots orbiting above Pip's head; animates when animated=true. */
private fun DrawScope.drawConfettiHalo(w: Float, h: Float, dy: Float, rotationDeg: Float) {
    fun x(p: Float) = w * p
    fun y(p: Float) = h * p + dy
    val radius = w * 0.38f
    val cx = x(0.5f)
    val cy = y(0.08f)
    val dotRadius = w * 0.034f
    val dotColors = listOf(Butter, Berry, Mint, Mango, Sky, Butter, Berry, Mint)
    // 8 dots evenly spaced across the upper arc (210° → 330°)
    val baseAngles = List(8) { i -> 210f + i * (120f / 7f) }
    for (i in baseAngles.indices) {
        val deg = (baseAngles[i] + rotationDeg).toDouble()
        val rad = deg * PI / 180.0
        drawCircle(
            color = dotColors[i],
            radius = dotRadius,
            center = Offset((cx + radius * cos(rad)).toFloat(), (cy + radius * sin(rad)).toFloat()),
        )
    }
}

/** Six-colour arc rainbow above Pip's head. */
private fun DrawScope.drawRainbowHalo(w: Float, h: Float, dy: Float) {
    fun x(p: Float) = w * p
    fun y(p: Float) = h * p + dy
    val radius = w * 0.42f
    val cx = x(0.5f)
    val cy = y(0.08f)
    val topLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val rainbowColors = listOf(
        Color(0xFFFF3B30), // Red
        Color(0xFFFF9500), // Orange
        Color(0xFFFFCC00), // Yellow
        Color(0xFF34C759), // Green
        Color(0xFF007AFF), // Blue
        Color(0xFFAF52DE), // Violet
    )
    val startAngle = 210f        // lower-left, sweeping clockwise through 270° (top)
    val totalSweep = 120f        // ends at 330° (lower-right)
    val bandSweep = totalSweep / rainbowColors.size
    for (i in rainbowColors.indices) {
        drawArc(
            color = rainbowColors[i],
            startAngle = startAngle + i * bandSweep,
            sweepAngle = bandSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round),
        )
    }
}
