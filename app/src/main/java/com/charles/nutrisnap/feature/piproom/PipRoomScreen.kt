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

private val RoomBg = Color(0xFFFFF3E0)
private val OnRoomText = Color(0xFF2C1A0E)
private val OnRoomTextMid = Color(0xFF6D4C2A)
private val ChatBubbleBg = Color(0xFFFFE0B2)

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
                        color = OnRoomText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = OnRoomText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoomBg,
                ),
            )
        },
        containerColor = RoomBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Hero Pip
            Pip(
                size = 180.dp,
                mood = PipMood.Celebrate,
                accessory = currentAccessory,
                animated = true,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Pip  •  Level $pipLevel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnRoomText,
                textAlign = TextAlign.Center,
            )

            Text(
                pipTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OnRoomTextMid,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "★".repeat(pipLevel) + "☆".repeat(5 - pipLevel),
                color = Mango,
                fontSize = 22.sp,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(28.dp))

            // Wardrobe section
            Text(
                "Wardrobe ✨",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnRoomText,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(PipAccessory.values()) { accessory ->
                    val isEarned = accessory in earnedAccessories
                    val isEquipped = accessory == currentAccessory
                    AccessoryCard(
                        accessory = accessory,
                        isEquipped = isEquipped,
                        isEarned = isEarned,
                        onClick = { if (isEarned) viewModel.equipAccessory(accessory) },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Messages section
            Text(
                "Pip says…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnRoomText,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            if (recentMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChatBubbleBg)
                        .padding(16.dp),
                ) {
                    Text(
                        "Chat with Pip to see messages here! 💬",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnRoomTextMid,
                    )
                }
            } else {
                recentMessages.forEach { message ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        Pip(
                            size = 36.dp,
                            mood = PipMood.Content,
                            animated = false,
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ChatBubbleBg)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column {
                                Text(
                                    message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnRoomText,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    pipRelativeTime(message.timestampMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnRoomTextMid,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "Chat with Pip 💬",
                onClick = onPipChat,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
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
    val bgColor = when {
        isEquipped -> Color(0xFFFFE0B2)
        isEarned -> Color(0xFFFFF3E0)
        else -> Color(0xFFEEEEEE)
    }

    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 96.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (isEquipped) Modifier.border(2.dp, Mango, shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isEarned) accessory.emoji.ifEmpty { "✨" } else "🔒",
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isEquipped) "Equipped" else accessory.displayName,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (isEquipped) Mango else if (isEarned) OnRoomText else Color(0xFF9E9E9E),
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
