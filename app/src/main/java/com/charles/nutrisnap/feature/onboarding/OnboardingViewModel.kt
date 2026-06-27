package com.charles.nutrisnap.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.UnitSystem
import com.charles.nutrisnap.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ONBOARDING_STEPS = 5

data class OnboardingUiState(
    val step: Int = 0,
    val sex: Sex = Sex.MALE,
    val age: Int = 28,
    val heightCm: Int = 175,
    val weightKg: Double = 74.0,
    val activity: ActivityLevel = ActivityLevel.ACTIVE,
    val goal: Goal = Goal.MAINTAIN,
    val rateKgPerWeek: Double = 0.5,
    val units: UnitSystem = UnitSystem.METRIC,
) {
    val input: OnboardingInput
        get() = OnboardingInput(sex, age, heightCm, weightKg, activity, goal, rateKgPerWeek)

    val preview: DailyGoal get() = GoalCalculator.calculate(input)
    val isLastStep: Boolean get() = step >= ONBOARDING_STEPS - 1
}

sealed interface OnboardingEvent {
    data object NavigateToDownload : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setSex(sex: Sex) = _state.update { it.copy(sex = sex) }
    fun setAge(age: Int) = _state.update { it.copy(age = age.coerceIn(13, 100)) }
    fun setHeightCm(cm: Int) = _state.update { it.copy(heightCm = cm.coerceIn(120, 230)) }
    fun setWeightKg(kg: Double) = _state.update { it.copy(weightKg = kg.coerceIn(30.0, 250.0)) }
    fun setActivity(a: ActivityLevel) = _state.update { it.copy(activity = a) }
    fun setGoal(g: Goal) = _state.update { it.copy(goal = g) }
    fun setUnits(u: UnitSystem) = _state.update { it.copy(units = u) }

    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(ONBOARDING_STEPS - 1)) }
    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun finish() {
        val s = _state.value
        viewModelScope.launch {
            prefs.saveGoal(s.preview)
            prefs.setUnits(s.units)
            prefs.setOnboardingComplete(true)
            _events.send(OnboardingEvent.NavigateToDownload)
        }
    }
}