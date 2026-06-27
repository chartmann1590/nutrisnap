package com.charles.nutrisnap.feature.dashboard

import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import com.charles.nutrisnap.ui.components.PipMood
import org.junit.Assert.assertEquals
import org.junit.Test

class PipMoodForTest {

    private fun remaining(goalKcal: Int, eatenKcal: Int) = Remaining(
        goal = DailyGoal(calories = goalKcal, proteinG = 100, carbsG = 200, fatG = 60),
        totals = DayTotals(totalKcal = eatenKcal, proteinG = 0, carbsG = 0, fatG = 0),
    )

    private fun meal() = MealEntity(
        timestampMs = 0L,
        mealType = MealType.LUNCH,
        name = "Test",
        totalKcal = 500,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
        source = MealSource.MANUAL,
    )

    @Test
    fun `no goal yet is Content`() {
        val state = DashboardUiState(remaining = null)
        assertEquals(PipMood.Content, pipMoodFor(state))
    }

    @Test
    fun `over calorie goal is Stuffed`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 2200),
            todayMeals = listOf(meal()),
        )
        assertEquals(PipMood.Stuffed, pipMoodFor(state))
    }

    @Test
    fun `under goal with no meals is Sleepy`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 0),
            todayMeals = emptyList(),
        )
        assertEquals(PipMood.Sleepy, pipMoodFor(state))
    }

    @Test
    fun `meals logged with active streak is Proud`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 800),
            todayMeals = listOf(meal()),
            streak = 5,
        )
        assertEquals(PipMood.Proud, pipMoodFor(state))
    }

    @Test
    fun `meals logged with short streak is Content`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 800),
            todayMeals = listOf(meal()),
            streak = 1,
        )
        assertEquals(PipMood.Content, pipMoodFor(state))
    }

    @Test
    fun `over goal beats active streak`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 2500),
            todayMeals = listOf(meal()),
            streak = 9,
        )
        assertEquals(PipMood.Stuffed, pipMoodFor(state))
    }
}
