package com.charles.nutrisnap.feature.milestones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.db.MilestoneEntity
import com.charles.nutrisnap.data.milestone.MilestoneType
import com.charles.nutrisnap.ui.components.EmptyState
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PipMood
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestonesScreen(
    onBack: () -> Unit,
    viewModel: MilestonesViewModel = hiltViewModel(),
) {
    val milestones by viewModel.milestones.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Your Moments ✨",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                "Every step of your journey with Pip",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (milestones.isEmpty()) {
                EmptyState(
                    pipSize = 100.dp,
                    title = "No moments yet",
                    subtitle = "Start logging to make memories! 🌱",
                    mood = PipMood.Sleepy,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    itemsIndexed(milestones) { index, milestone ->
                        MilestoneCard(
                            milestone = milestone,
                            isFirst = index == 0,
                            isLast = index == milestones.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneCard(
    milestone: MilestoneEntity,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val type = runCatching { MilestoneType.valueOf(milestone.type) }
        .getOrDefault(MilestoneType.BADGE_EARNED)
    val color = Color(android.graphics.Color.parseColor(type.colorHex))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Timeline column
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(120.dp)
                .drawBehind {
                    val centerX = size.width / 2f
                    val circleRadius = 16.dp.toPx()
                    val circleCenter = circleRadius

                    // Draw dashed line above circle (unless first)
                    if (!isFirst) {
                        drawLine(
                            color = color.copy(alpha = 0.4f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, circleCenter - circleRadius),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                        )
                    }

                    // Draw dashed line below circle (unless last)
                    if (!isLast) {
                        drawLine(
                            color = color.copy(alpha = 0.4f),
                            start = Offset(centerX, circleCenter + circleRadius),
                            end = Offset(centerX, size.height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            // Colored circle dot
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color = color, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = milestoneEmoji(type),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Card content
        NutriCard(
            modifier = Modifier.weight(1f),
            cornerRadius = 16.dp,
            padding = 12.dp,
            shadowElevation = 4.dp,
        ) {
            val title = milestoneTitle(type)
            val body = milestoneBody(type, milestone.payload)

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = relativeDate(milestone.occurredAtMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )

                if (type == MilestoneType.STREAK_MILESTONE) {
                    Pip(
                        size = 28.dp,
                        mood = PipMood.Celebrate,
                        animated = false,
                    )
                }
            }
        }
    }
}

private fun milestoneEmoji(type: MilestoneType): String = when (type) {
    MilestoneType.BADGE_EARNED -> "🏅"
    MilestoneType.STREAK_MILESTONE -> "🔥"
    MilestoneType.CHALLENGE_COMPLETE -> "⭐"
    MilestoneType.PERSONAL_BEST -> "💪"
    MilestoneType.FIRST_EVER -> "🎉"
}

private fun milestoneTitle(type: MilestoneType): String = when (type) {
    MilestoneType.BADGE_EARNED -> "Badge earned! 🏅"
    MilestoneType.STREAK_MILESTONE -> "Streak milestone! 🔥"
    MilestoneType.CHALLENGE_COMPLETE -> "Challenge complete! ⭐"
    MilestoneType.PERSONAL_BEST -> "Personal best! 💪"
    MilestoneType.FIRST_EVER -> "First ever! 🎉"
}

private fun milestoneBody(type: MilestoneType, payload: String): String = when (type) {
    MilestoneType.BADGE_EARNED -> payload
    MilestoneType.STREAK_MILESTONE -> "$payload-day streak"
    MilestoneType.CHALLENGE_COMPLETE -> payload
    MilestoneType.PERSONAL_BEST -> payload
    MilestoneType.FIRST_EVER -> payload
}

private fun relativeDate(epochMs: Long): String {
    val days = (System.currentTimeMillis() - epochMs) / 86400000L
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 30 -> "$days days ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
