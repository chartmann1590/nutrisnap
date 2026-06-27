package com.charles.nutrisnap.feature.pip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipContextBuilderTest {

    private fun snapshot(
        goalKcal: Int? = 2000,
        eatenKcal: Int = 900,
        streakDays: Int = 3,
        recentMeals: List<PipMealBrief> = listOf(PipMealBrief("Pizza", 450)),
        latestWeightKg: Double? = 75.0,
        weightTrend: WeightTrend = WeightTrend.DOWN,
    ) = PipTrackingSnapshot(
        goalKcal = goalKcal, eatenKcal = eatenKcal,
        goalProteinG = 133, eatenProteinG = 30,
        goalCarbsG = 361, eatenCarbsG = 90,
        goalFatG = 73, eatenFatG = 40,
        streakDays = streakDays, recentMeals = recentMeals,
        latestWeightKg = latestWeightKg, weightTrend = weightTrend,
    )

    @Test
    fun `includes calories remaining and macros when goal set`() {
        val text = PipContextBuilder.build(snapshot())
        assertTrue(text.contains("2000"))
        assertTrue(text.contains("1100")) // 2000 - 900 remaining
        assertTrue(text.contains("30/133"))
    }

    @Test
    fun `no goal omits calorie math`() {
        val text = PipContextBuilder.build(snapshot(goalKcal = null))
        assertTrue(text.contains("No calorie goal"))
        assertFalse(text.contains("left of"))
    }

    @Test
    fun `no meals says so`() {
        val text = PipContextBuilder.build(snapshot(recentMeals = emptyList()))
        assertTrue(text.contains("No meals logged today"))
    }

    @Test
    fun `no streak omits streak brag`() {
        val text = PipContextBuilder.build(snapshot(streakDays = 0))
        assertTrue(text.contains("No active streak"))
    }

    @Test
    fun `weight omitted when absent`() {
        val text = PipContextBuilder.build(snapshot(latestWeightKg = null))
        assertFalse(text.lowercase().contains("weight"))
    }

    @Test
    fun `weight trend rendered when present`() {
        val text = PipContextBuilder.build(snapshot(latestWeightKg = 75.0, weightTrend = WeightTrend.DOWN))
        assertTrue(text.contains("75"))
        assertTrue(text.lowercase().contains("down"))
    }
}
