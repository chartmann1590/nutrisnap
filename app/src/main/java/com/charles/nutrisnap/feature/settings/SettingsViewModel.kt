package com.charles.nutrisnap.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.ModelRepository
import com.charles.nutrisnap.data.ModelState
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentVariant: ModelVariant = ModelVariant.E2B,
    val modelState: ModelState = ModelState.NotDownloaded,
    val goal: DailyGoal? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        prefs.prefs,
        modelRepository.state,
    ) { prefs, modelState ->
        SettingsUiState(
            currentVariant = prefs.modelVariant,
            modelState = modelState,
            goal = prefs.goal,
            themeMode = prefs.themeMode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setVariant(variant: ModelVariant) {
        viewModelScope.launch {
            prefs.setModelVariant(variant)
            modelRepository.retryDownload(variant)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun retryDownload() {
        val s = state.value
        modelRepository.retryDownload(s.currentVariant)
    }
}