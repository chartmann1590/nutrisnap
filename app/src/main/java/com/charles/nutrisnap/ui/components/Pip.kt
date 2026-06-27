package com.charles.nutrisnap.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charles.nutrisnap.ui.theme.Berry
import com.charles.nutrisnap.ui.theme.Cocoa
import com.charles.nutrisnap.ui.theme.Mango
import com.charles.nutrisnap.ui.theme.Mint
import androidx.compose.ui.graphics.Color

/**
 * Pip — the NutriSnap mascot: a cute round mango with a green leaf, big friendly eyes and rosy
 * cheeks. Drawn vectorially so it stays crisp at any size. Gently bobs when [animated] is true.
 */
@Composable
fun Pip(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "pip")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "bob",
    )

    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val dy = -bob * h * 0.06f
        fun x(p: Float) = w * p
        fun y(p: Float) = h * p + dy

        // soft shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(x(0.25f), h * 0.9f),
            size = Size(w * 0.5f, h * 0.06f),
        )
        // body
        val body = Path().apply {
            moveTo(x(0.5f), y(0.13f))
            cubicTo(x(0.84f), y(0.13f), x(0.92f), y(0.45f), x(0.92f), y(0.6f))
            cubicTo(x(0.92f), y(0.86f), x(0.72f), y(0.95f), x(0.5f), y(0.95f))
            cubicTo(x(0.28f), y(0.95f), x(0.08f), y(0.86f), x(0.08f), y(0.6f))
            cubicTo(x(0.08f), y(0.45f), x(0.16f), y(0.13f), x(0.5f), y(0.13f))
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
        // cheeks
        drawCircle(Berry.copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(x(0.33f), y(0.62f)))
        drawCircle(Berry.copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(x(0.67f), y(0.62f)))
        // eyes
        drawCircle(Color.White, radius = w * 0.06f, center = Offset(x(0.4f), y(0.52f)))
        drawCircle(Color.White, radius = w * 0.06f, center = Offset(x(0.6f), y(0.52f)))
        drawCircle(Cocoa, radius = w * 0.03f, center = Offset(x(0.41f), y(0.53f)))
        drawCircle(Cocoa, radius = w * 0.03f, center = Offset(x(0.61f), y(0.53f)))
        // smile
        val smile = Path().apply {
            moveTo(x(0.43f), y(0.64f))
            cubicTo(x(0.47f), y(0.70f), x(0.53f), y(0.70f), x(0.57f), y(0.64f))
        }
        drawPath(smile, Cocoa, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))
    }
}
