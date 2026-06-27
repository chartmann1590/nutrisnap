package com.charles.nutrisnap.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charles.nutrisnap.ui.theme.Mango
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AnimatedRing(
    progress: Float,
    color: Color,
    track: Color,
    size: Dp,
    stroke: Dp,
    modifier: Modifier,
) {
    val animatedProgress = remember { Animatable(0f) }
    val target = progress.coerceIn(0f, 1f)

    LaunchedEffect(target) {
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        )
    }

    Canvas(modifier.size(size)) {
        val s = stroke.toPx()
        val inset = s / 2
        val arcSize = androidx.compose.ui.geometry.Size(
            this.size.width - s,
            this.size.height - s,
        )
        val topLeft = Offset(inset, inset)

        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = s, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress.value,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = s, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun StreakFlame(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    intensity: Float = 1f,
) {
    val i = intensity.coerceIn(0f, 1f)
    val periodMs = (820 - 320 * i).toInt() // hotter streak = faster flicker
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f + 0.1f * i,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val flame = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            cubicTo(w * 0.55f, h * 0.3f, w * 0.7f, h * 0.45f, w * 0.7f, h * 0.6f)
            cubicTo(w * 0.7f, h * 0.75f, w * 0.6f, h * 0.9f, w * 0.5f, h * 0.9f)
            cubicTo(w * 0.4f, h * 0.9f, w * 0.3f, h * 0.75f, w * 0.3f, h * 0.6f)
            cubicTo(w * 0.3f, h * 0.45f, w * 0.45f, h * 0.3f, w * 0.5f, h * 0.1f)
            close()
        }
        drawPath(flame, Mango.copy(alpha = alpha))

        val glow = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.52f, h * 0.3f, w * 0.6f, h * 0.4f, w * 0.6f, h * 0.55f)
            cubicTo(w * 0.6f, h * 0.7f, w * 0.55f, h * 0.8f, w * 0.5f, h * 0.8f)
            cubicTo(w * 0.45f, h * 0.8f, w * 0.4f, h * 0.7f, w * 0.4f, h * 0.55f)
            cubicTo(w * 0.4f, h * 0.4f, w * 0.48f, h * 0.3f, w * 0.5f, h * 0.15f)
            close()
        }
        drawPath(glow, Color.White.copy(alpha = alpha * (0.4f + 0.3f * i)))
    }
}

@Composable
fun ConfettiBurst(
    visible: Boolean,
    modifier: Modifier,
) {
    val alphas = remember { List(12) { Animatable(0f) } }
    val offsets = remember {
        val rng = Random(42)
        List(12) {
            val angle = rng.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val dist = 30f + rng.nextFloat() * 60f
            Offset(cos(angle) * dist, sin(angle) * dist)
        }
    }
    val colors = remember {
        val rng = Random(7)
        List(12) {
            Color(
                red = rng.nextFloat(),
                green = rng.nextFloat(),
                blue = rng.nextFloat(),
                alpha = 1f,
            )
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            alphas.forEach { it.snapTo(1f) }
            alphas.forEach { it.animateTo(0f, animationSpec = tween(1000)) }
        } else {
            alphas.forEach { it.snapTo(0f) }
        }
    }

    Box(modifier) {
        Canvas(Modifier.size(200.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            alphas.forEachIndexed { i, anim ->
                val a = anim.value
                if (a > 0.001f) {
                    val o = offsets[i]
                    drawCircle(
                        color = colors[i].copy(alpha = a),
                        radius = 4f + 3f * a,
                        center = Offset(cx + o.x * (1f - a), cy + o.y * (1f - a)),
                    )
                }
            }
        }
    }
}
