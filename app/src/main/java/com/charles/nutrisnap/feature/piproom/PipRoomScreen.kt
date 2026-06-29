package com.charles.nutrisnap.feature.piproom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PipAccessory
import com.charles.nutrisnap.ui.components.PipMood
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.theme.Mango
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipRoomScreen(
    onBack: () -> Unit,
    onPipChat: () -> Unit,
    viewModel: PipRoomViewModel = hiltViewModel(),
) {
    val currentAccessory by viewModel.currentAccessory.collectAsStateWithLifecycle()
    val earnedAccessories by viewModel.earnedAccessories.collectAsStateWithLifecycle()
    val pipLevel by viewModel.pipLevel.collectAsStateWithLifecycle()
    val pipTitle by viewModel.pipTitle.collectAsStateWithLifecycle()
    val recentMessages by viewModel.recentPipMessages.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pip's Room 🏠",
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
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFF6EC), Color(0xFFFFE8D0))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                // Hero: Pip
                Pip(
                    size = 200.dp,
                    mood = PipMood.Celebrate,
                    accessory = currentAccessory,
                    animated = true,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Pip • Level $pipLevel  $pipTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "★".repeat(pipLevel) + "☆".repeat(5 - pipLevel),
                    color = Mango,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                )

                Spacer(Modifier.height(24.dp))

                // Wardrobe section
                Text(
                    "Pip's Wardrobe ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(PipAccessory.values()) { accessory ->
                        val isEarned = accessory in earnedAccessories
                        val isEquipped = accessory == currentAccessory
                        AccessoryCard(
                            accessory = accessory,
                            isEquipped = isEquipped,
                            isEarned = isEarned,
                            onClick = {
                                if (isEarned) viewModel.equipAccessory(accessory)
                            },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Messages from Pip
                Text(
                    "Pip says… 💬",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                if (recentMessages.isEmpty()) {
                    Text(
                        "Chat with Pip to see messages here! 💬",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    recentMessages.forEach { message ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            Pip(
                                size = 32.dp,
                                mood = PipMood.Content,
                                animated = false,
                            )
                            Spacer(Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x1F5BC0EB),
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        pipRelativeTime(message.timestampMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                PrimaryButton(
                    text = "Chat with Pip 💬",
                    onClick = onPipChat,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AccessoryCard(
    accessory: PipAccessory,
    isEquipped: Boolean,
    isEarned: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val alpha = if (isEarned) 1f else 0.4f

    Card(
        modifier = Modifier
            .size(width = 72.dp, height = 90.dp)
            .then(
                if (isEquipped) {
                    Modifier.border(2.dp, Mango, shape)
                } else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEarned) 4.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isEarned) accessory.emoji else "🔒",
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isEquipped) "Equipped" else accessory.displayName,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (isEquipped) Mango else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

private fun pipRelativeTime(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 2 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
