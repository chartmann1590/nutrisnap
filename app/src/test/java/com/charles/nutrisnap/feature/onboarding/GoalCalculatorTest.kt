package com.charles.nutrisnap.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCalculatorTest {

    private val maleMaintain = OnboardingInput(
        sex = Sex.MALE, age = 28, heightCm = 178, weightKg = 74.0,
        activity = ActivityLevel.ACTIVE, goal = Goal.MAINTAIN,
    )

    @Test
    fun `bmr matches Mifflin-St Jeor for male`() {
        // 10*74 + 6.25*178 - 5*28 + 5 = 740 + 1112.5 - 140 + 5 = 1717.5
        assertEquals(1717.5, GoalCalculator.bmr(maleMaintain), 0.01)
    }

    @Test
    fun `bmr matches Mifflin-St Jeor for female`() {
        val female = maleMaintain.copy(sex = Sex.FEMALE)
        // 1717.5 - 5 (male bonus) - 161 (female offset) = 1551.5
        assertEquals(1551.5, GoalCalculator.bmr(female), 0.01)
    }

    @Test
    fun `tdee applies activity factor`() {
        // 1717.5 * 1.55 = 2662.125
        assertEquals(2662.125, GoalCalculator.tdee(maleMaintain), 0.01)
    }

    @Test
    fun `maintain has no calorie delta`() {
        assertEquals(0.0, GoalCalculator.calorieDelta(maleMaintain), 0.0)
        assertEquals(2662, GoalCalculator.calculate(maleMaintain).calories)
    }

    @Test
    fun `lose subtracts a deficit based on rate`() {
        val lose = maleMaintain.copy(goal = Goal.LOSE, rateKgPerWeek = 0.5)
        // delta = -(0.5 * 7700 / 7) = -550 → 2662 - 550 = 2112
        assertEquals(2112, GoalCalculator.calculate(lose).calories)
    }

    @Test
    fun `gain adds a surplus based on rate`() {
        val gain = maleMaintain.copy(goal = Goal.GAIN, rateKgPerWeek = 0.25)
        // delta = +(0.25 * 7700 / 7) = +275 → 2662 + 275 = 2937
        assertEquals(2937, GoalCalculator.calculate(gain).calories)
    }

    @Test
    fun `calories never drop below the floor`() {
        val extreme = OnboardingInput(
            sex = Sex.FEMALE, age = 60, heightCm = 150, weightKg = 50.0,
            activity = ActivityLevel.SEDENTARY, goal = Goal.LOSE, rateKgPerWeek = 1.0,
        )
        assertTrue(GoalCalculator.calculate(extreme).calories >= 1200)
    }

    @Test
    fun `protein scales with body weight`() {
        // 74kg * 1.8 = 133.2 → 133
        assertEquals(133, GoalCalculator.calculate(maleMaintain).proteinG)
    }

    @Test
    fun `macros roughly reconstruct the calorie target`() {
        val g = GoalCalculator.calculate(maleMaintain)
        val reconstructed = g.proteinG * 4 + g.carbsG * 4 + g.fatG * 9
        // within rounding tolerance of the calorie target
        assertTrue(kotlin.math.abs(reconstructed - g.calories) <= 12)
    }
}
