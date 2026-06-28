package com.charles.nutrisnap.feature.dashboard

import app.cash.turbine.test
import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UnitSystem
import com.charles.nutrisnap.data.UserPrefs
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.challenge.DailyChallengeRepository
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import com.charles.nutrisnap.ui.components.PipAccessory
import com.charles.nutrisnap.ui.sound.PipSoundManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
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
    fun `emits empty state when no goal set`() = runTest {
        val prefs = fakePrefs(UserPrefs(onboardingComplete = true, goal = null, units = UnitSystem.METRIC, modelVariant = ModelVariant.E2B, themeMode = ThemeMode.SYSTEM))
        val mealRepo = fakeMealRepo()
        val vm = DashboardViewModel(GoalRepository(prefs, mealRepo), mealRepo, prefs, PipEventBus(), fakeSoundManager(), fakeChallengeRepo())
        vm.state.test {
            val s = awaitItem()
            assert(s.remaining == null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shows remaining when goal set`() = runTest {
        val goal = DailyGoal(calories = 2000, proteinG = 100, carbsG = 250, fatG = 65)
        val prefs = fakePrefs(UserPrefs(onboardingComplete = true, goal = goal, units = UnitSystem.METRIC, modelVariant = ModelVariant.E2B, themeMode = ThemeMode.SYSTEM))
        val mealRepo = fakeMealRepo(totals = DayTotals(totalKcal = 1000, proteinG = 50, carbsG = 125, fatG = 32))
        val vm = DashboardViewModel(GoalRepository(prefs, mealRepo), mealRepo, prefs, PipEventBus(), fakeSoundManager(), fakeChallengeRepo())
        vm.state.test {
            val s = awaitItem()
            assert(s.remaining != null)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun fakePrefs(userPrefs: UserPrefs) = object : UserPreferencesRepository(null) {
    override val prefs = MutableStateFlow(userPrefs)
    override val pipSoundsEnabled: Flow<Boolean> = flowOf(true)
    override val pipVoiceEnabled: Flow<Boolean> = flowOf(false)
    override val pipAccessory: Flow<PipAccessory> = flowOf(PipAccessory.NONE)
}

private fun fakeSoundManager(): PipSoundManager = mockk(relaxed = true)

private fun fakeChallengeRepo(): DailyChallengeRepository = mockk<DailyChallengeRepository> {
    coEvery { ensureTodayChallenge() } just Runs
    every { getToday() } returns flowOf(null)
}

private fun fakeMealRepo(
    meals: List<MealEntity> = emptyList(),
    totals: DayTotals = DayTotals(),
    loggedDays: List<Long> = emptyList(),
) = object : MealRepository(null) {
    override fun observeTodayMeals() = flowOf(meals)
    override fun observeTodayTotals() = flowOf(totals)
    override fun observeDistinctLoggedDays() = flowOf(loggedDays)
}