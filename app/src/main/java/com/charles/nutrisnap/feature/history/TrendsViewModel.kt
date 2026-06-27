package com.charles.nutrisnap.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.WeightRepository
import com.charles.nutrisnap.data.db.DayTotalsWithEpochDay
import com.charles.nutrisnap.data.db.WeightEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TrendsUiState(
    val weekTotals: List<DayTotalsWithEpochDay> = emptyList(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val latestWeight: WeightEntryEntity? = null,
    val weightRange: List<WeightEntryEntity> = emptyList(),
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val weightRepository: WeightRepository,
) : ViewModel() {

    private val today = System.currentTimeMillis() / 86_400_000L

    val state: StateFlow<TrendsUiState> = combine(
        mealRepository.observeWeekTotals(),
        mealRepository.observeDistinctLoggedDays(),
        weightRepository.observeLatest(),
        weightRepository.observeRange(today - 6, today),
    ) { weekTotals, loggedDays, weight, weightRange ->
        TrendsUiState(
            weekTotals = weekTotals,
            streak = StreakCalculator.currentStreak(loggedDays.toSet(), today),
            bestStreak = StreakCalculator.bestStreak(loggedDays.toSet()),
            latestWeight = weight,
            weightRange = weightRange,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendsUiState(),
    )
}