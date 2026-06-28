package com.charles.nutrisnap.feature.history

import app.cash.turbine.test
import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UnitSystem
import com.charles.nutrisnap.data.UserPrefs
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `diary shows meals logged today when goal is set`() = runTest {
        val goal = DailyGoal(calories = 2000, proteinG = 100, carbsG = 250, fatG = 65)
        val prefs = fakePrefs(
            UserPrefs(
                onboardingComplete = true, goal = goal, units = UnitSystem.METRIC,
                modelVariant = ModelVariant.E2B, themeMode = ThemeMode.SYSTEM,
            ),
        )
        val meal = MealEntity(
            id = 1, timestampMs = System.currentTimeMillis(), mealType = MealType.LUNCH,
            name = "Sandwich", totalKcal = 500, proteinG = 25, carbsG = 60, fatG = 18,
            source = MealSource.MANUAL,
        )
        val mealRepo = fakeMealRepo(
            mealsForDay = listOf(meal),
            totals = DayTotals(totalKcal = 500, proteinG = 25, carbsG = 60, fatG = 18),
        )
        val vm = DiaryViewModel(mealRepo, GoalRepository(prefs, mealRepo))

        vm.state.test {
            // Wait for a state where the goal has loaded (not the spinner).
            var s = awaitItem()
            while (s.remaining == null) s = awaitItem()
            val hasMeals = s.mealsByType.values.any { it.isNotEmpty() }
            assertTrue("Diary should show today's logged meals but mealsByType=${s.mealsByType}", hasMeals)
            assertEquals("Sandwich", s.mealsByType[MealType.LUNCH]?.first()?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun fakePrefs(userPrefs: UserPrefs) = object : UserPreferencesRepository(null) {
    override val prefs = MutableStateFlow(userPrefs)
}

private fun fakeMealRepo(
    mealsForDay: List<MealEntity> = emptyList(),
    totals: DayTotals = DayTotals(),
) = object : MealRepository(null) {
    override fun observeMealsForDay(epochDay: Long) = flowOf(mealsForDay)
    override fun observeDayTotals(epochDay: Long) = flowOf(totals)
    override fun observeTodayTotals() = flowOf(totals)
}
