package com.charles.nutrisnap.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.RingProgress
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.db.MealEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val remaining: Remaining? = null,
    val ringProgress: RingProgress = RingProgress(0f, 0f, 0f, 0f),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val todayMeals: List<MealEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    goalRepository: GoalRepository,
    mealRepository: MealRepository,
    prefs: UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        goalRepository.observeRemaining(),
        goalRepository.observeRingProgress(),
        mealRepository.observeTodayMeals(),
        mealRepository.observeDistinctLoggedDays(),
        prefs.prefs,
    ) { remaining, ring, meals, loggedDays, userPrefs ->
        val today = mealRepository.todayEpochDay()
        DashboardUiState(
            remaining = remaining,
            ringProgress = ring,
            streak = StreakCalculator.currentStreak(loggedDays.toSet(), today),
            bestStreak = StreakCalculator.bestStreak(loggedDays.toSet()),
            todayMeals = meals,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )
}