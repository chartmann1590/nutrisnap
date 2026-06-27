package com.charles.nutrisnap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.charles.nutrisnap.ui.theme.NutriTheme

/** A small rounded label chip used for streaks, confidence, info notes, etc. */
@Composable
fun NutriPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = NutriTheme.colors.mangoTint,
    content: Color = NutriTheme.colors.streak,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun StreakPill(days: Int, modifier: Modifier = Modifier) =
    NutriPill(text = "🔥 $days", modifier = modifier)

@Composable
fun ConfidencePill(percent: Int, modifier: Modifier = Modifier) =
    NutriPill(
        text = "$percent% sure",
        modifier = modifier,
        container = NutriTheme.colors.mintTint,
        content = NutriTheme.colors.protein,
    )

@Composable
fun InfoPill(text: String, modifier: Modifier = Modifier) =
    NutriPill(
        text = text,
        modifier = modifier,
        container = NutriTheme.colors.skyTint,
        content = NutriTheme.colors.info,
    )
