package com.charles.nutrisnap.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.ai.FoodEstimate
import com.charles.nutrisnap.ai.GemmaEngine
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.PipEvent
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.badge.BadgeDetector
import com.charles.nutrisnap.data.challenge.DailyChallengeRepository
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EntryUiState {
    data object Idle : EntryUiState
    data object Estimating : EntryUiState
    data class EstimateReady(val estimate: FoodEstimate) : EntryUiState
    data object Logging : EntryUiState
    data object Done : EntryUiState
}

sealed interface EntryEvent {
    data object Logged : EntryEvent
}

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val gemmaEngine: GemmaEngine,
    private val mealRepository: MealRepository,
    private val badgeDetector: BadgeDetector,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val pipEventBus: PipEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<EntryUiState>(EntryUiState.Idle)
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EntryEvent>()
    val events: SharedFlow<EntryEvent> = _events.asSharedFlow()

    fun estimateFromText(description: String) {
        if (description.isBlank()) return
        _state.value = EntryUiState.Estimating
        viewModelScope.launch(Dispatchers.Default) {
            val result = gemmaEngine.estimateFromText(description)
            result.onSuccess { estimate ->
                _state.value = EntryUiState.EstimateReady(estimate)
            }.onFailure {
                _state.value = EntryUiState.Idle
            }
        }
    }

    fun logMeal(
        estimate: FoodEstimate,
        mealType: MealType,
        source: MealSource = MealSource.MANUAL,
        photoUri: String? = null,
    ) {
        _state.value = EntryUiState.Logging
        viewModelScope.launch {
            val isFirstMeal = mealRepository.observeTodayMeals().first().isEmpty()
            val now = System.currentTimeMillis()
            val meal = MealEntity(
                timestampMs = now,
                mealType = mealType,
                name = estimate.name,
                totalKcal = estimate.kcal,
                proteinG = estimate.proteinG,
                carbsG = estimate.carbsG,
                fatG = estimate.fatG,
                photoUri = photoUri,
                source = source,
                confidence = estimate.confidence,
            )
            mealRepository.logMeal(meal)
            _state.value = EntryUiState.Done
            // Gamification
            if (isFirstMeal) pipEventBus.emit(PipEvent.FirstMealOfDay)
            pipEventBus.emit(PipEvent.MealLogged)
            badgeDetector.checkAndAward()
            dailyChallengeRepository.checkAndComplete(mealRepository.todayEpochDay())
            _events.emit(EntryEvent.Logged)
        }
    }

    fun reset() {
        _state.value = EntryUiState.Idle
    }
}