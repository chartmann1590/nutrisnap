package com.charles.nutrisnap.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.db.DayTotalsWithEpochDay
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.ui.components.EmptyState
import com.charles.nutrisnap.ui.components.LoadingState
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.StreakPill
import com.charles.nutrisnap.ui.theme.NutriTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state.remaining == null
    val hasMeals = state.mealsByType.values.any { it.isNotEmpty() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { viewModel.goBack() }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Previous day")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDate(state.epochDay),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Goal: ${state.remaining?.goal?.calories ?: 0} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { viewModel.goForward() }) {
                Icon(Icons.Rounded.ArrowForward, contentDescription = "Next day")
            }
        }

        Spacer(Modifier.height(8.dp))

        val total = state.dayTotal
        val goalCal = state.remaining?.goal?.calories ?: 0
        if (goalCal > 0) {
            val progress = (total.totalKcal.toFloat() / goalCal).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NutriTheme.colors.ringTrack),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${total.totalKcal} / $goalCal kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            LoadingState(modifier = Modifier.fillMaxSize())
        } else if (!hasMeals) {
            EmptyState(
                pipSize = 100.dp,
                title = "No history yet",
                subtitle = "Meals you log will appear here",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.forEach { type ->
                    val meals = state.mealsByType[type] ?: emptyList()
                    if (meals.isNotEmpty()) {
                        item {
                            MealSection(type, meals)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun MealSection(type: MealType, meals: List<MealEntity>) {
    NutriCard(cornerRadius = 16.dp, padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            val sectionKcal = meals.sumOf { it.totalKcal }
            Text("$sectionKcal kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        meals.forEach { meal ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(meal.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${meal.totalKcal}", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "+ Add food",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { /* navigate to entry */ },
        )
    }
}

@Composable
fun TrendsScreen(
    modifier: Modifier = Modifier,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Text("Trends", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Weekly calories", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            WeekBarChart(weekTotals = state.weekTotals)
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${state.streak}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("days", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Best: ${state.bestStreak}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Scale, contentDescription = "Weight", tint = NutriTheme.colors.protein, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        val weight = state.latestWeight
                        if (weight != null) {
                            Text(
                                String.format("%.1f", weight.weightKg),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("kg", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("--", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    if (state.weightRange.size >= 2) {
                        val first = state.weightRange.first()
                        val last = state.weightRange.last()
                        val diff = last.weightKg - first.weightKg
                        Text(
                            String.format("%+.1f kg", diff),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (diff > 0) NutriTheme.colors.fat else NutriTheme.colors.protein,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun WeekBarChart(weekTotals: List<DayTotalsWithEpochDay>) {
    val maxKcal = weekTotals.maxOfOrNull { it.totalKcal } ?: 1
    val barColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val today = com.charles.nutrisnap.data.localEpochDay()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            ((today - 6)..today).forEach { day ->
                val total = weekTotals.find { it.epochDay == day }?.totalKcal ?: 0
                val fraction = (total.toFloat() / maxKcal).coerceIn(0f, 1f)
                val (dayStartMs, _) = com.charles.nutrisnap.data.localDayRangeMs(day)
                val dayOfWeek = java.util.Calendar.getInstance().apply {
                    timeInMillis = dayStartMs + com.charles.nutrisnap.data.MS_PER_DAY / 2
                }.get(java.util.Calendar.DAY_OF_WEEK)
                val label = when (dayOfWeek) {
                    java.util.Calendar.MONDAY -> "Mon"
                    java.util.Calendar.TUESDAY -> "Tue"
                    java.util.Calendar.WEDNESDAY -> "Wed"
                    java.util.Calendar.THURSDAY -> "Thu"
                    java.util.Calendar.FRIDAY -> "Fri"
                    java.util.Calendar.SATURDAY -> "Sat"
                    java.util.Calendar.SUNDAY -> "Sun"
                    else -> ""
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height((fraction * 120).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatDate(epochDay: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val (startMs, _) = com.charles.nutrisnap.data.localDayRangeMs(epochDay)
    return sdf.format(Date(startMs))
}
