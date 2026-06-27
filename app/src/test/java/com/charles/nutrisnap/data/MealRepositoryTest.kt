package com.charles.nutrisnap.data

import androidx.room.Room
import app.cash.turbine.test
import com.charles.nutrisnap.data.db.AppDatabase
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MealRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: MealRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = MealRepository(db.mealDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `log and observe meals for day`() = runTest {
        val epochDay = System.currentTimeMillis() / 86400000L
        val meal = MealEntity(
            timestampMs = epochDay * 86400000L + 3600000,
            mealType = MealType.BREAKFAST,
            name = "Oatmeal",
            totalKcal = 350,
            proteinG = 12,
            carbsG = 50,
            fatG = 8,
            source = MealSource.MANUAL,
        )
        repo.logMeal(meal)

        repo.observeMealsForDay(epochDay).test {
            val meals = awaitItem()
            assertEquals(1, meals.size)
            assertEquals("Oatmeal", meals.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `day totals match logged meals`() = runTest {
        val epochDay = System.currentTimeMillis() / 86400000L
        val dayStart = epochDay * 86400000L
        repo.logMeal(
            MealEntity(
                timestampMs = dayStart + 1, mealType = MealType.BREAKFAST, name = "A",
                totalKcal = 200, proteinG = 10, carbsG = 20, fatG = 5, source = MealSource.MANUAL,
            )
        )
        repo.logMeal(
            MealEntity(
                timestampMs = dayStart + 2, mealType = MealType.LUNCH, name = "B",
                totalKcal = 300, proteinG = 15, carbsG = 30, fatG = 8, source = MealSource.MANUAL,
            )
        )

        repo.observeDayTotals(epochDay).test {
            val totals = awaitItem()
            assertEquals(500, totals.totalKcal)
            assertEquals(25, totals.proteinG)
            assertEquals(50, totals.carbsG)
            assertEquals(13, totals.fatG)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete meal removes it`() = runTest {
        val epochDay = System.currentTimeMillis() / 86400000L
        val id = repo.logMeal(
            MealEntity(
                timestampMs = epochDay * 86400000L,
                mealType = MealType.SNACK,
                name = "Chips",
                totalKcal = 150,
                proteinG = 2,
                carbsG = 15,
                fatG = 9,
                source = MealSource.MANUAL,
            )
        )
        repo.deleteMeal(id)

        repo.observeMealsForDay(epochDay).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distinct logged days`() = runTest {
        val today = System.currentTimeMillis() / 86400000L
        val yesterdayStart = (today - 1) * 86400000L
        val todayStart = today * 86400000L
        repo.logMeal(
            MealEntity(
                timestampMs = yesterdayStart + 1, mealType = MealType.DINNER, name = "X",
                totalKcal = 100, proteinG = 0, carbsG = 0, fatG = 0, source = MealSource.MANUAL,
            )
        )
        repo.logMeal(
            MealEntity(
                timestampMs = todayStart + 1, mealType = MealType.BREAKFAST, name = "Y",
                totalKcal = 100, proteinG = 0, carbsG = 0, fatG = 0, source = MealSource.MANUAL,
            )
        )

        repo.observeDistinctLoggedDays().test {
            val days = awaitItem()
            assertTrue(days.contains(today))
            assertTrue(days.contains(today - 1))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
