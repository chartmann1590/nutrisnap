package com.charles.nutrisnap.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.ui.components.CalorieRing
import com.charles.nutrisnap.ui.components.ConfettiBurst
import com.charles.nutrisnap.ui.components.EmptyState
import com.charles.nutrisnap.ui.components.MacroBar
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PipMood
import com.charles.nutrisnap.ui.components.StreakPill
import com.charles.nutrisnap.ui.theme.NutriTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onOpenMeal: (Long) -> Unit = {},
    onAddMeal: () -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConfetti by remember { mutableStateOf(false) }
    var previousMealCount by remember { mutableStateOf(state.todayMeals.size) }

    LaunchedEffect(state.streak) {
        if (state.streak > 0 && state.streak % 7 == 0) {
            showConfetti = true
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(3000)
            showConfetti = false
        }
    }

    LaunchedEffect(state.todayMeals.size) {
        if (state.todayMeals.size > previousMealCount && previousMealCount >= 0) {
            showConfetti = true
        }
        previousMealCount = state.todayMeals.size
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 56.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeaderSection(state, celebrating = showConfetti)
            }

            item {
                if (state.remaining != null) {
                    CalorieCard(state)
                }
            }

            item {
                Text(
                    "Today's meals",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }

            if (state.todayMeals.isEmpty()) {
                item {
                    EmptyState(
                        pipSize = 100.dp,
                        title = "No meals yet",
                        subtitle = "Snap a photo to get started!",
                        modifier = Modifier,
                        mood = PipMood.Sleepy,
                    )
                }
            } else {
                items(state.todayMeals, key = { it.id }) { meal ->
                    MealRow(meal, onClick = { onOpenMeal(meal.id) })
                }
            }
        }

        ConfettiBurst(visible = showConfetti, modifier = Modifier)
    }
}

@Composable
private fun HeaderSection(state: DashboardUiState, celebrating: Boolean) {
    val mood = if (celebrating) PipMood.Celebrate else pipMoodFor(state)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Pip(size = 56.dp, mood = mood, animated = true)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hey there!", style = MaterialTheme.typography.headlineMedium)
            Text(
                dayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.streak > 0) {
            StreakPill(days = state.streak)
        }
    }
}

@Composable
private fun CalorieCard(state: DashboardUiState) {
    val remaining = state.remaining ?: return
    val ring = state.ringProgress
    val kcalRemaining = remaining.kcalRemaining.coerceAtLeast(0)

    NutriCard(cornerRadius = 24.dp, padding = 18.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CalorieRing(
                progress = ring.kcalFraction,
                centerValue = formatKcal(kcalRemaining),
                centerLabel = "left",
                size = 96.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                MacroBar(
                    "Protein",
                    "${remaining.totals.proteinG}/${remaining.goal.proteinG}g",
                    ring.proteinFraction,
                    NutriTheme.colors.protein,
                )
                MacroBar(
                    "Carbs",
                    "${remaining.totals.carbsG}/${remaining.goal.carbsG}g",
                    ring.carbsFraction,
                    NutriTheme.colors.carbs,
                )
                MacroBar(
                    "Fat",
                    "${remaining.totals.fatG}/${remaining.goal.fatG}g",
                    ring.fatFraction,
                    NutriTheme.colors.fat,
                )
            }
        }
    }
}

@Composable
private fun MealRow(meal: MealEntity, onClick: () -> Unit) {
    NutriCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp, padding = 11.dp, shadowElevation = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (meal.photoUri != null) {
                AsyncImage(
                    model = meal.photoUri,
                    contentDescription = "Meal photo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(NutriTheme.colors.mangoTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(meal.mealType.name.take(2), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    meal.mealType.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${meal.totalKcal}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatKcal(kcal: Int): String {
    if (kcal >= 1000) "${kcal / 1000},${(kcal % 1000).toString().padStart(3, '0')}"
    return kcal.toString()
}

private fun dayLabel(): String {
    val now = System.currentTimeMillis()
    val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    return dateFormat.format(Date(now))
}
