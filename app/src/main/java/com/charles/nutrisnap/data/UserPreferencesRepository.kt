package com.charles.nutrisnap.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charles.nutrisnap.feature.onboarding.DailyGoal
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
}