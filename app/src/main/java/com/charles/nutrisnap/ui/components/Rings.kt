package com.charles.nutrisnap.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charles.nutrisnap.ui.theme.NutriTheme

/**
 * The hero calorie ring: a thick rounded arc that animates to [progress] (0f..1f), with the
 * remaining-calorie number centered inside.
 */
@Composable
fun CalorieRing(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 13.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = NutriTheme.colors.ringTrack,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "ring",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val inset = s / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - s, this.size.height - s)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = s, cap = StrokeCap.Round),
            )
            drawArc(
                color = color, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = s, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.headlineMedium)
            Text(
                centerLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A labeled macro progress bar (Protein / Carbs / Fat). */
@Composable
fun MacroBar(
    label: String,
    valueText: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "macro-$label",
    )
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
        Box(
            Modifier
                .weight(1f)
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(NutriTheme.colors.ringTrack),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(valueText, style = MaterialTheme.typography.labelLarge)
    }
}
