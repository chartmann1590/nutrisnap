package com.charles.nutrisnap.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.nutrisnap.data.challenge.DailyChallengeState

private val ButterFill = Color(0xFFFFF8E1)
private val ButterBorder = Color(0xFFFFB300)
private val ChallengeCorner = RoundedCornerShape(16.dp)
private val TextDark = Color(0xFF2C1A0E)
private val TextMid = Color(0xFF5C3D1E)

@Composable
fun DailyChallengeCard(
    challenge: DailyChallengeState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .border(width = 1.5.dp, color = ButterBorder, shape = ChallengeCorner)
            .clickable(onClick = onTap),
        shape = ChallengeCorner,
        color = ButterFill,
        shadowElevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // Emoji badge circle instead of Pip
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ButterBorder.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (challenge.isComplete) "✅" else challenge.type.emoji,
                    fontSize = 22.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's Challenge",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMid,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = challenge.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                )
                Text(
                    text = challenge.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid,
                )
                if (challenge.isComplete) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Complete! 🎉",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { challenge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ButterBorder,
                        trackColor = Color(0xFFFFE082),
                    )
                }
            }
        }
    }
}
