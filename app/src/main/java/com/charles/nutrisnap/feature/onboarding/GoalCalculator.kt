package com.charles.nutrisnap.feature.onboarding

import kotlin.math.roundToInt

/**
 * Pure, side-effect-free calorie + macro math. Kept free of Android types so it can be unit tested
 * directly (see GoalCalculatorTest).
 *
 * Uses the Mifflin-St Jeor equation for BMR, an activity multiplier for TDEE, then applies a
 * goal-based calorie delta. Macros: protein scaled to body weight, fat as a share of calories,
 * carbs filling the remainder.
 */
object GoalCalculator {

    private const val KCAL_PER_KG_FAT = 7700.0
    private const val PROTEIN_G_PER_KG = 1.8
    private const val FAT_CALORIE_SHARE = 0.25
    private const val KCAL_PER_G_PROTEIN = 4.0
    private const val KCAL_PER_G_CARB = 4.0
    private const val KCAL_PER_G_FAT = 9.0

    /** Minimum sane daily intake floor to avoid unhealthy targets. */
    private const val MIN_CALORIES = 1200

    fun bmr(input: OnboardingInput): Double {
        val base = 10 * input.weightKg + 6.25 * input.heightCm - 5 * input.age
        return when (input.sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    fun tdee(input: OnboardingInput): Double = bmr(input) * input.activity.factor

    /** Daily calorie delta from the goal + target rate (negative for loss, positive for gain). */
    fun calorieDelta(input: OnboardingInput): Double {
        val perDay = input.rateKgPerWeek * KCAL_PER_KG_FAT / 7.0
        return when (input.goal) {
            Goal.LOSE -> -perDay
            Goal.MAINTAIN -> 0.0
            Goal.GAIN -> perDay
        }
    }

    fun calculate(input: OnboardingInput): DailyGoal {
        val calories = (tdee(input) + calorieDelta(input)).roundToInt().coerceAtLeast(MIN_CALORIES)

        val proteinG = (input.weightKg * PROTEIN_G_PER_KG).roundToInt()
        val fatG = (calories * FAT_CALORIE_SHARE / KCAL_PER_G_FAT).roundToInt()
        val remainingKcal = calories - proteinG * KCAL_PER_G_PROTEIN - fatG * KCAL_PER_G_FAT
        val carbsG = (remainingKcal / KCAL_PER_G_CARB).roundToInt().coerceAtLeast(0)

        return DailyGoal(
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
        )
    }
}
