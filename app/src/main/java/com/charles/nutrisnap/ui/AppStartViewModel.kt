package com.charles.nutrisnap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.ModelRepository
import com.charles.nutrisnap.data.PremiumAccess
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val modelRepository: ModelRepository,
    private val premiumAccess: PremiumAccess,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = prefs.prefs.map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Premium removes ads entirely. */
    val showAds: StateFlow<Boolean> = premiumAccess.entitlement
        .map { !it.isPremium }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            val onboarded = prefs.prefs.first().onboardingComplete
            _startRoute.value = when {
                !onboarded -> Routes.ONBOARDING
                !modelRepository.isReady() -> Routes.DOWNLOAD
                else -> Routes.HOME
            }
        }
    }
}