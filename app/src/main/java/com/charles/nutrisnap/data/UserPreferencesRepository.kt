package com.charles.nutrisnap.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import com.charles.nutrisnap.ui.components.PipAccessory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class UnitSystem { METRIC, IMPERIAL }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPrefs(
    val onboardingComplete: Boolean,
    val goal: DailyGoal?,
    val units: UnitSystem,
    val modelVariant: ModelVariant,
    val themeMode: ThemeMode,
    val crashlyticsEnabled: Boolean = true,
    val performanceEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
)

@Singleton
open class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>?,
) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CALORIES = intPreferencesKey("goal_calories")
        val PROTEIN = intPreferencesKey("goal_protein_g")
        val CARBS = intPreferencesKey("goal_carbs_g")
        val FAT = intPreferencesKey("goal_fat_g")
        val UNITS = stringPreferencesKey("units")
        val MODEL_VARIANT = stringPreferencesKey("model_variant")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CRASHLYTICS_ENABLED = booleanPreferencesKey("crashlytics_enabled")
        val PERFORMANCE_ENABLED = booleanPreferencesKey("performance_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val PIP_SOUNDS_ENABLED = booleanPreferencesKey("pip_sounds_enabled")
        val PIP_VOICE_ENABLED = booleanPreferencesKey("pip_voice_enabled")
        val PIP_ACCESSORY = stringPreferencesKey("pip_accessory")
    }

    open val prefs: Flow<UserPrefs> by lazy { dataStore!!.data.map { p ->
        val calories = p[Keys.CALORIES]
        val goal = if (calories != null) {
            DailyGoal(
                calories = calories,
                proteinG = p[Keys.PROTEIN] ?: 0,
                carbsG = p[Keys.CARBS] ?: 0,
                fatG = p[Keys.FAT] ?: 0,
            )
        } else null
        UserPrefs(
            onboardingComplete = p[Keys.ONBOARDING_COMPLETE] ?: false,
            goal = goal,
            units = runCatching { UnitSystem.valueOf(p[Keys.UNITS] ?: "METRIC") }
                .getOrDefault(UnitSystem.METRIC),
            modelVariant = runCatching { ModelVariant.valueOf(p[Keys.MODEL_VARIANT] ?: "E2B") }
                .getOrDefault(ModelVariant.E2B),
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME_MODE] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            crashlyticsEnabled = p[Keys.CRASHLYTICS_ENABLED] ?: true,
            performanceEnabled = p[Keys.PERFORMANCE_ENABLED] ?: true,
            analyticsEnabled = p[Keys.ANALYTICS_ENABLED] ?: true,
        )
    }
    }

    open suspend fun saveGoal(goal: DailyGoal) {
        dataStore!!.edit { p ->
            p[Keys.CALORIES] = goal.calories
            p[Keys.PROTEIN] = goal.proteinG
            p[Keys.CARBS] = goal.carbsG
            p[Keys.FAT] = goal.fatG
        }
    }

    open suspend fun setUnits(units: UnitSystem) {
        dataStore!!.edit { it[Keys.UNITS] = units.name }
    }

    open suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore!!.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    open suspend fun setModelVariant(variant: ModelVariant) {
        dataStore!!.edit { it[Keys.MODEL_VARIANT] = variant.name }
    }

    open suspend fun setThemeMode(mode: ThemeMode) {
        dataStore!!.edit { it[Keys.THEME_MODE] = mode.name }
    }

    open suspend fun setCrashlyticsEnabled(enabled: Boolean) {
        dataStore!!.edit { it[Keys.CRASHLYTICS_ENABLED] = enabled }
    }

    open suspend fun setPerformanceEnabled(enabled: Boolean) {
        dataStore!!.edit { it[Keys.PERFORMANCE_ENABLED] = enabled }
    }

    open suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore!!.edit { it[Keys.ANALYTICS_ENABLED] = enabled }
    }

    open val pipSoundsEnabled: Flow<Boolean> by lazy { dataStore!!.data.map { p -> p[Keys.PIP_SOUNDS_ENABLED] ?: true } }
    open val pipVoiceEnabled: Flow<Boolean> by lazy { dataStore!!.data.map { p -> p[Keys.PIP_VOICE_ENABLED] ?: false } }
    open val pipAccessory: Flow<PipAccessory> by lazy { dataStore!!.data.map { p ->
        runCatching { PipAccessory.valueOf(p[Keys.PIP_ACCESSORY] ?: "NONE") }.getOrDefault(PipAccessory.NONE)
    } }

    open suspend fun setPipSoundsEnabled(enabled: Boolean) { dataStore!!.edit { it[Keys.PIP_SOUNDS_ENABLED] = enabled } }
    open suspend fun setPipVoiceEnabled(enabled: Boolean) { dataStore!!.edit { it[Keys.PIP_VOICE_ENABLED] = enabled } }
    open suspend fun setPipAccessory(accessory: PipAccessory) { dataStore!!.edit { it[Keys.PIP_ACCESSORY] = accessory.name } }
}
