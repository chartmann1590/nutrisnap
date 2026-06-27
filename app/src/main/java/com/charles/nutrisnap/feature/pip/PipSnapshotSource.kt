package com.charles.nutrisnap.feature.pip

import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.WeightRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface PipSnapshotSource {
    suspend fun snapshot(): PipTrackingSnapshot
}

class DefaultPipSnapshotSource @Inject constructor(
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val weightRepository: WeightRepository,
) : PipSnapshotSource {

    override suspend fun snapshot(): PipTrackingSnapshot {
        val remaining = goalRepository.observeRemaining().first()
        val meals = mealRepository.observeTodayMeals().first()
        val loggedDays = mealRepository.observeDistinctLoggedDays().first().toSet()
        val todayEpochDay = System.currentTimeMillis() / 86_400_000L
        val streak = StreakCalculator.currentStreak(loggedDays, todayEpochDay)

        val recentMeals = meals
            .sortedByDescending { it.timestampMs }
            .take(3)
            .map { PipMealBrief(name = it.name, kcal = it.totalKcal) }

        val weights = weightRepository.observeRange(todayEpochDay - 30, todayEpochDay).first()
        val latestWeight = weights.lastOrNull()?.weightKg
        val trend = when {
            weights.size < 2 -> WeightTrend.NONE
            else -> {
                val prev = weights[weights.size - 2].weightKg
                val last = weights[weights.size - 1].weightKg
                when {
                    last > prev + 0.05 -> WeightTrend.UP
                    last < prev - 0.05 -> WeightTrend.DOWN
                    else -> WeightTrend.FLAT
                }
            }
        }

        val goal = remaining?.goal
        return PipTrackingSnapshot(
            goalKcal = goal?.calories,
            eatenKcal = meals.sumOf { it.totalKcal },
            goalProteinG = goal?.proteinG ?: 0,
            eatenProteinG = meals.sumOf { it.proteinG },
            goalCarbsG = goal?.carbsG ?: 0,
            eatenCarbsG = meals.sumOf { it.carbsG },
            goalFatG = goal?.fatG ?: 0,
            eatenFatG = meals.sumOf { it.fatG },
            streakDays = streak,
            recentMeals = recentMeals,
            latestWeightKg = latestWeight,
            weightTrend = trend,
        )
    }
}
