package com.charles.nutrisnap.feature.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.nutrisnap.data.challenge.DailyChallengeState
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PipMood

private val ButterFill = Color(0x33FFD66B)
private val ButterBorder = Color(0xFFFFD66B)
private val ChallengeCorner = RoundedCornerShape(20.dp)

@Composable
fun DailyChallengeCard(
    challenge: DailyChallengeState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .rotate(1f)
            .border(width = 1.5.dp, color = ButterBorder, shape = ChallengeCorner)
            .clickable(onClick = onTap),
        shape = ChallengeCorner,
        color = ButterFill,
        shadowElevation = 4.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Pip(
                size = 36.dp,
                mood = if (challenge.isComplete) PipMood.Celebrate else PipMood.Thinking,
                animated = false,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⭐ Today's Challenge",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3A2A21),
                    letterSpacing = 0.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${challenge.type.emoji} ${challenge.type.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3A2A21),
                )
                Text(
                    text = challenge.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5A4A41),
                )
                if (challenge.isComplete) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "✅ Done!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A2A21),
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { challenge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ButterBorder,
                        trackColor = Color(0x66FFD66B),
                    )
                }
            }
        }
    }
}
