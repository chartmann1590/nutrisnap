package com.charles.nutrisnap.feature.onboarding

/** Inputs collected during the onboarding questionnaire. */
enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val label: String, val emoji: String, val factor: Double, val blurb: String) {
    SEDENTARY("Sedentary", "🛋️", 1.20, "Office job, little movement"),
    LIGHT("Lightly active", "🚶", 1.375, "Light exercise 1–3 days/wk"),
    ACTIVE("Active", "🏃", 1.55, "Exercise 3–5 days/wk"),
    VERY_ACTIVE("Very active", "🔥", 1.725, "Hard exercise 6–7 days/wk"),
}

enum class Goal(val label: String) {
    LOSE("Lose"),
    MAINTAIN("Maintain"),
    GAIN("Gain"),
}

/** Raw answers from the questionnaire. Height in cm, weight in kg (converted at the edge). */
data class OnboardingInput(
    val sex: Sex,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val activity: ActivityLevel,
    val goal: Goal,
    /** Target rate of weight change in kg/week (always positive; sign comes from [goal]). */
    val rateKgPerWeek: Double = 0.5,
)

/** Computed daily targets persisted as the user's goal. */
data class DailyGoal(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
)
