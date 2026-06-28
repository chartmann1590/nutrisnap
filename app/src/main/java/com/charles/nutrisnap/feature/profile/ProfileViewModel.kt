package com.charles.nutrisnap.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val goalSummary: String = "",
    val streak: Int = 0,
    val latestWeight: String = "--",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    prefs: UserPreferencesRepository,
    mealRepository: MealRepository,
    weightRepository: WeightRepository,
) : ViewModel() {

    private val today = mealRepository.todayEpochDay()

    val state: StateFlow<ProfileUiState> = combine(
        prefs.prefs,
        mealRepository.observeDistinctLoggedDays(),
        weightRepository.observeLatest(),
    ) { prefs, loggedDays, weight ->
        val goal = prefs.goal
        val goalSummary = if (goal != null) {
            "${goal.calories} kcal - P${goal.proteinG}/C${goal.carbsG}/F${goal.fatG}"
        } else {
            "No goal set"
        }
        ProfileUiState(
            goalSummary = goalSummary,
            streak = StreakCalculator.currentStreak(loggedDays.toSet(), today),
            latestWeight = weight?.let { String.format("%.1f kg", it.weightKg) } ?: "--",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )
}