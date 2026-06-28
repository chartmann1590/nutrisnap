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
import java.util.TimeZone

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
        val epochDay = localEpochDay()
        val meal = MealEntity(
            timestampMs = localDayRangeMs(epochDay).first + 3600000,
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
        val epochDay = localEpochDay()
        val dayStart = localDayRangeMs(epochDay).first
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
        val epochDay = localEpochDay()
        val id = repo.logMeal(
            MealEntity(
                timestampMs = localDayRangeMs(epochDay).first,
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
    fun `meal logged late evening local time buckets under local day not utc`() = runTest {
        // Reproduces the "History tab blank" bug: with UTC day-keys, an evening meal
        // west of UTC lands on the next epoch-day and vanishes from today's diary.
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York")) // UTC-5 / -4
        try {
            val day = localEpochDay()
            // 23:00 local — this instant is already the *next* calendar day in UTC.
            val elevenPmLocal = localDayRangeMs(day).first + 23 * 3_600_000L
            repo.logMeal(
                MealEntity(
                    timestampMs = elevenPmLocal, mealType = MealType.DINNER, name = "Late dinner",
                    totalKcal = 600, proteinG = 30, carbsG = 40, fatG = 25, source = MealSource.MANUAL,
                )
            )

            repo.observeMealsForDay(day).test {
                assertEquals("meal must appear under the local day it was logged", 1, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }
            repo.observeMealsForDay(day + 1).test {
                assertTrue("meal must NOT leak into the next (UTC) day", awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `distinct logged days`() = runTest {
        val today = localEpochDay()
        val yesterdayStart = localDayRangeMs(today - 1).first
        val todayStart = localDayRangeMs(today).first
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
