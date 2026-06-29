package com.charles.nutrisnap.feature.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.badge.BadgeType
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.theme.Berry
import com.charles.nutrisnap.ui.theme.Butter
import com.charles.nutrisnap.ui.theme.Mint
import com.charles.nutrisnap.ui.theme.Mango
import com.charles.nutrisnap.ui.theme.Grape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    onPipChat: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val earnedCount = badges.count { it.earnedAtMs != null }
    val earnedBadges = badges.filter { it.earnedAtMs != null }
    val lockedBadges = badges.filter { it.earnedAtMs == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pip's Badges ✨",
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
                "You've earned $earnedCount of ${BadgeType.entries.size} badges",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (earnedBadges.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "Earned",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(earnedBadges) { item ->
                        BadgeCell(item = item)
                    }
                }

                if (lockedBadges.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "Still to earn…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(lockedBadges) { item ->
                        BadgeCell(item = item)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                PrimaryButton(
                    text = "Chat with Pip about badges",
                    onClick = onPipChat,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun BadgeCell(item: BadgeDisplayItem, modifier: Modifier = Modifier) {
    val color = badgeColor(item.type)
    val isEarned = item.earnedAtMs != null

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 100.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            if (isEarned) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = color, spotColor = color)
                        .background(color = color, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.type.emoji,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .alpha(0.5f)
                            .drawWithContent {
                                val paint = Paint().apply {
                                    colorFilter = ColorFilter.colorMatrix(
                                        ColorMatrix().apply { setToSaturation(0f) }
                                    )
                                }
                                drawContext.canvas.saveLayer(
                                    androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                                    paint
                                )
                                drawContent()
                                drawContext.canvas.restore()
                            }
                            .background(color = color, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.type.emoji,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = "🔒",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = item.type.displayName,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isEarned) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }

        if (item.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Berry, shape = CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    "NEW!",
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

fun badgeColor(type: BadgeType): Color = when (type) {
    BadgeType.FIRST_BITE, BadgeType.CHEF_HAT, BadgeType.CENTURY -> Mango
    BadgeType.ON_A_ROLL, BadgeType.HOT_STREAK, BadgeType.FORTNIGHT,
    BadgeType.MONTHLY, BadgeType.UNSTOPPABLE -> Berry
    BadgeType.BALANCED_DAY, BadgeType.THREE_PEAT, BadgeType.LIGHT_EATER -> Grape
    BadgeType.CHALLENGE_CHAMP -> Butter
    else -> Mint
}
