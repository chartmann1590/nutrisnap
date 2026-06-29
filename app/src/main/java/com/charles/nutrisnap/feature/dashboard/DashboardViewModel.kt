package com.charles.nutrisnap.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.PipEvent
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.PipReactionMapper
import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.RingProgress
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.challenge.DailyChallengeRepository
import com.charles.nutrisnap.data.challenge.DailyChallengeState
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.ui.components.PipAccessory
import com.charles.nutrisnap.ui.components.PipMood
import com.charles.nutrisnap.ui.sound.PipSound
import com.charles.nutrisnap.ui.sound.PipSoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val remaining: Remaining? = null,
    val ringProgress: RingProgress = RingProgress(0f, 0f, 0f, 0f),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val todayMeals: List<MealEntity> = emptyList(),
    val currentAccessory: PipAccessory = PipAccessory.NONE,
    val pipReaction: PipReactionState? = null,
    val todayChallenge: DailyChallengeState? = null,
)

data class PipReactionState(
    val mood: PipMood,
    val speech: String,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val prefs: UserPreferencesRepository,
    private val pipEventBus: PipEventBus,
    private val pipSoundManager: PipSoundManager,
    private val dailyChallengeRepository: DailyChallengeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var goalHitEmittedToday = false

    init {
        // Collect base data flows
        viewModelScope.launch {
            combine(
                goalRepository.observeRemaining(),
                goalRepository.observeRingProgress(),
                mealRepository.observeTodayMeals(),
                mealRepository.observeDistinctLoggedDays(),
                prefs.prefs,
            ) { remaining, ring, meals, loggedDays, _ ->
                val today = mealRepository.todayEpochDay()
                _uiState.update {
                    it.copy(
                        remaining = remaining,
                        ringProgress = ring,
                        todayMeals = meals,
                        streak = StreakCalculator.currentStreak(loggedDays.toSet(), today),
                        bestStreak = StreakCalculator.bestStreak(loggedDays.toSet()),
                    )
                }
                val kcalRemaining = remaining?.kcalRemaining ?: Int.MAX_VALUE
                if (kcalRemaining <= 0 && !goalHitEmittedToday) {
                    goalHitEmittedToday = true
                    pipEventBus.emit(PipEvent.GoalHit)
                } else if (kcalRemaining > 0) {
                    goalHitEmittedToday = false
                }
            }.collect {}
        }

        // Collect accessory pref
        viewModelScope.launch {
            prefs.pipAccessory.collect { accessory ->
                _uiState.update { it.copy(currentAccessory = accessory) }
            }
        }

        // Collect pip events and show reactions
        viewModelScope.launch {
            pipEventBus.events.collect { event ->
                val (mood, speech) = PipReactionMapper.map(event)
                val sound = when (event) {
                    is PipEvent.BadgeEarned -> PipSound.CHIME
                    is PipEvent.ChallengeComplete -> PipSound.POP
                    is PipEvent.StreakMilestone -> PipSound.CELEBRATE
                    is PipEvent.GoalHit -> PipSound.REACT
                    else -> PipSound.REACT
                }
                pipSoundManager.play(sound)
                _uiState.update { it.copy(pipReaction = PipReactionState(mood, speech)) }
                delay(4500)
                _uiState.update { it.copy(pipReaction = null) }
            }
        }

        // Ensure today's challenge exists, then observe it
        viewModelScope.launch {
            dailyChallengeRepository.ensureTodayChallenge()
            dailyChallengeRepository.getToday().collect { challenge ->
                _uiState.update { it.copy(todayChallenge = challenge) }
            }
        }
    }

    fun dismissReaction() {
        _uiState.update { it.copy(pipReaction = null) }
    }
}
