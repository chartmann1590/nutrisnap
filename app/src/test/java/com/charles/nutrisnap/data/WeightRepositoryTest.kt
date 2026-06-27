package com.charles.nutrisnap.data

import androidx.room.Room
import app.cash.turbine.test
import com.charles.nutrisnap.data.db.AppDatabase
import com.charles.nutrisnap.data.db.WeightEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WeightRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: WeightRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = WeightRepository(db.weightDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `observeLatest returns null when empty`() = runTest {
        repo.observeLatest().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `log and observe latest weight`() = runTest {
        repo.logWeight(WeightEntryEntity(dateEpochDay = 100L, weightKg = 75.0))

        repo.observeLatest().test {
            val entry = awaitItem()
            assertNotNull(entry)
            assertEquals(75.0, entry!!.weightKg, 0.01)
            assertEquals(100L, entry.dateEpochDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert replaces weight for same day`() = runTest {
        repo.logWeight(WeightEntryEntity(dateEpochDay = 100L, weightKg = 75.0))
        repo.logWeight(WeightEntryEntity(dateEpochDay = 100L, weightKg = 76.5))

        repo.observeLatest().test {
            assertEquals(76.5, awaitItem()!!.weightKg, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeRange returns entries in order`() = runTest {
        repo.logWeight(WeightEntryEntity(dateEpochDay = 100L, weightKg = 75.0))
        repo.logWeight(WeightEntryEntity(dateEpochDay = 101L, weightKg = 74.5))
        repo.logWeight(WeightEntryEntity(dateEpochDay = 102L, weightKg = 74.0))

        repo.observeRange(100L, 101L).test {
            val entries = awaitItem()
            assertEquals(2, entries.size)
            assertEquals(75.0, entries[0].weightKg, 0.01)
            assertEquals(74.5, entries[1].weightKg, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes weight entry`() = runTest {
        val id = repo.logWeight(WeightEntryEntity(dateEpochDay = 100L, weightKg = 75.0))
        repo.deleteWeight(id)

        repo.observeLatest().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
