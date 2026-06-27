package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Remaining(
    val goal: DailyGoal,
    val totals: DayTotals,
) {
    val kcalRemaining: Int get() = goal.calories - totals.totalKcal
    val proteinRemaining: Int get() = goal.proteinG - totals.proteinG
    val carbsRemaining: Int get() = goal.carbsG - totals.carbsG
    val fatRemaining: Int get() = goal.fatG - totals.fatG
}

data class RingProgress(
    val kcalFraction: Float,
    val proteinFraction: Float,
    val carbsFraction: Float,
    val fatFraction: Float,
)

@Singleton
open class GoalRepository @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mealRepository: MealRepository,
) {
    open fun observeRemaining(): Flow<Remaining?> = combine(
        userPreferencesRepository.prefs.map { it.goal },
        mealRepository.observeTodayTotals(),
    ) { goal, totals ->
        if (goal == null) null else Remaining(goal, totals)
    }.distinctUntilChanged()

    open fun observeRingProgress(): Flow<RingProgress> = combine(
        userPreferencesRepository.prefs.map { it.goal },
        mealRepository.observeTodayTotals(),
    ) { goal, totals ->
        if (goal == null) {
            RingProgress(0f, 0f, 0f, 0f)
        } else {
            RingProgress(
                kcalFraction = (totals.totalKcal.toFloat() / goal.calories).coerceIn(0f, 1f),
                proteinFraction = if (goal.proteinG > 0) (totals.proteinG.toFloat() / goal.proteinG).coerceIn(0f, 1f) else 1f,
                carbsFraction = if (goal.carbsG > 0) (totals.carbsG.toFloat() / goal.carbsG).coerceIn(0f, 1f) else 1f,
                fatFraction = if (goal.fatG > 0) (totals.fatG.toFloat() / goal.fatG).coerceIn(0f, 1f) else 1f,
            )
        }
    }.distinctUntilChanged()
}
