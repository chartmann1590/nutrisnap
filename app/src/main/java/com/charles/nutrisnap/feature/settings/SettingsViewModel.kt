package com.charles.nutrisnap.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.ModelRepository
import com.charles.nutrisnap.data.ModelState
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.PremiumAccess
import com.charles.nutrisnap.data.PremiumEntitlement
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.WeightRepository
import com.charles.nutrisnap.data.db.WeightEntryEntity
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.perf.FirebasePerformance
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
    val latestWeightKg: Double? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val crashlyticsEnabled: Boolean = true,
    val performanceEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
    val premiumEntitlement: PremiumEntitlement = PremiumEntitlement(),
    val billingMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val modelRepository: ModelRepository,
    private val prefs: UserPreferencesRepository,
    private val premiumAccess: PremiumAccess,
    private val weightRepository: WeightRepository,
) : AndroidViewModel(app) {

    val state: StateFlow<SettingsUiState> = combine(
        prefs.prefs,
        modelRepository.state,
        premiumAccess.entitlement,
        premiumAccess.billingMessage,
        weightRepository.observeLatest(),
    ) { prefs, modelState, premiumEntitlement, billingMessage, weight ->
        SettingsUiState(
            currentVariant = prefs.modelVariant,
            modelState = modelState,
            goal = prefs.goal,
            latestWeightKg = weight?.weightKg,
            themeMode = prefs.themeMode,
            crashlyticsEnabled = prefs.crashlyticsEnabled,
            performanceEnabled = prefs.performanceEnabled,
            analyticsEnabled = prefs.analyticsEnabled,
            premiumEntitlement = premiumEntitlement,
            billingMessage = billingMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun saveGoal(calories: Int, proteinG: Int, carbsG: Int, fatG: Int) {
        viewModelScope.launch {
            prefs.saveGoal(DailyGoal(calories = calories, proteinG = proteinG, carbsG = carbsG, fatG = fatG))
        }
    }

    fun saveWeight(kg: Double) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toEpochDay()
            weightRepository.logWeight(WeightEntryEntity(dateEpochDay = today, weightKg = kg))
        }
    }

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

    fun setCrashlyticsEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        viewModelScope.launch { prefs.setCrashlyticsEnabled(enabled) }
    }

    fun setPerformanceEnabled(enabled: Boolean) {
        FirebasePerformance.getInstance().setPerformanceCollectionEnabled(enabled)
        viewModelScope.launch { prefs.setPerformanceEnabled(enabled) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        FirebaseAnalytics.getInstance(app).setAnalyticsCollectionEnabled(enabled)
        viewModelScope.launch { prefs.setAnalyticsEnabled(enabled) }
    }

    fun resetFirebaseId() {
        FirebaseInstallations.getInstance().delete()
    }

    val pipSoundsEnabled: StateFlow<Boolean> = prefs.pipSoundsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val pipVoiceEnabled: StateFlow<Boolean> = prefs.pipVoiceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPipSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setPipSoundsEnabled(enabled) }
    }

    fun setPipVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setPipVoiceEnabled(enabled) }
    }

    fun restorePurchases() {
        premiumAccess.restorePurchases()
    }
}
