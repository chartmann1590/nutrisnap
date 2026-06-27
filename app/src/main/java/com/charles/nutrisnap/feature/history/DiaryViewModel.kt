package com.charles.nutrisnap.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryUiState(
    val epochDay: Long = System.currentTimeMillis() / 86_400_000L,
    val mealsByType: Map<MealType, List<MealEntity>> = emptyMap(),
    val dayTotal: DayTotals = DayTotals(),
    val remaining: Remaining? = null,
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _epochDay = MutableStateFlow(System.currentTimeMillis() / 86_400_000L)
    val epochDay: StateFlow<Long> = _epochDay.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DiaryUiState> = _epochDay.flatMapLatest { day ->
        combine(
            mealRepository.observeMealsForDay(day),
            mealRepository.observeDayTotals(day),
            goalRepository.observeRemaining(),
        ) { meals, totals, remaining ->
            DiaryUiState(
                epochDay = day,
                mealsByType = meals.groupBy { it.mealType },
                dayTotal = totals,
                remaining = remaining,
            )
        }
    }.let { flow ->
        MutableStateFlow(DiaryUiState()).also { s ->
            viewModelScope.launch {
                flow.collect { s.value = it }
            }
        }
    }

    fun setDay(epochDay: Long) {
        _epochDay.value = epochDay
    }

    fun goBack() {
        _epochDay.update { it - 1 }
    }

    fun goForward() {
        _epochDay.update { it + 1 }
    }
}