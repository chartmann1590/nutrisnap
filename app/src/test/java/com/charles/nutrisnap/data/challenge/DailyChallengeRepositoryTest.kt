package com.charles.nutrisnap.data.challenge

import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UnitSystem
import com.charles.nutrisnap.data.UserPrefs
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.badge.BadgeDetector
import com.charles.nutrisnap.data.badge.BadgeRepository
import com.charles.nutrisnap.data.db.BadgeDao
import com.charles.nutrisnap.data.db.BadgeEntity
import com.charles.nutrisnap.data.db.DailyChallengeDao
import com.charles.nutrisnap.data.db.DailyChallengeEntity
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.DayTotalsWithEpochDay
import com.charles.nutrisnap.data.db.MealDao
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MilestoneDao
import com.charles.nutrisnap.data.db.MilestoneEntity
import com.charles.nutrisnap.data.milestone.MilestoneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for [DailyChallengeRepository.generateForDay] — verifies determinism and full cycling.
 * Uses the real [DailyChallengeRepository] class with minimal stub dependencies.
 * generateForDay is a pure function that uses no injected dependencies, so the stubs
 * only need to construct without throwing.
 */
class DailyChallengeRepositoryTest {

    private val types = DailyChallengeType.values()

    // ---- Minimal stubs ----

    private class StubDailyChallengeDao : DailyChallengeDao {
        override suspend fun upsert(entity: DailyChallengeEntity) {}
        override suspend fun getForDay(day: Long): DailyChallengeEntity? = null
        override fun observeForDay(day: Long): Flow<DailyChallengeEntity?> = flowOf(null)
        override fun getCompleted(): Flow<List<DailyChallengeEntity>> = flowOf(emptyList())
        override suspend fun countCompleted(): Int = 0
    }

    private class StubBadgeDao : BadgeDao {
        override suspend fun insertOrIgnore(badge: BadgeEntity) {}
        override fun getAll(): Flow<List<BadgeEntity>> = flowOf(emptyList())
        override suspend fun markSeen(type: String) {}
    }

    private class StubMilestoneDao : MilestoneDao {
        override suspend fun insert(milestone: MilestoneEntity) {}
        override fun getAll(): Flow<List<MilestoneEntity>> = flowOf(emptyList())
    }

    private class StubMealDao : MealDao {
        override suspend fun insert(meal: MealEntity): Long = 0L
        override suspend fun update(meal: MealEntity) {}
        override suspend fun delete(meal: MealEntity) {}
        override suspend fun deleteById(id: Long) {}
        override fun observeRange(startMs: Long, endMs: Long): Flow<List<MealEntity>> = flowOf(emptyList())
        override fun observeDayTotals(startMs: Long, endMs: Long): Flow<DayTotals> = flowOf(DayTotals())
        override fun observeDayTotalsRange(startMs: Long, endMs: Long, offsetMs: Long): Flow<List<DayTotalsWithEpochDay>> = flowOf(emptyList())
        override fun observeDistinctLoggedLocalDays(offsetMs: Long): Flow<List<Long>> = flowOf(emptyList())
        override suspend fun countAll(): Int = 0
        override suspend fun getById(id: Long): MealEntity? = null
    }

    private fun stubMealRepo() = object : MealRepository(null) {
        override fun todayEpochDay() = 0L
        override fun observeTodayMeals() = flowOf(emptyList<MealEntity>())
        override fun observeTodayTotals() = flowOf(DayTotals())
        override fun observeDistinctLoggedDays() = flowOf(emptyList<Long>())
        override fun observeMealsForDay(epochDay: Long) = flowOf(emptyList<MealEntity>())
    }

    private fun stubGoalRepo(mealRepo: MealRepository): GoalRepository {
        val prefs = object : UserPreferencesRepository(null) {
            override val prefs = MutableStateFlow(
                UserPrefs(false, null, UnitSystem.METRIC, ModelVariant.E2B, ThemeMode.SYSTEM)
            )
        }
        return object : GoalRepository(prefs, mealRepo) {
            override fun observeRemaining() = flowOf(null)
        }
    }

    /**
     * Builds the real [DailyChallengeRepository] with stub dependencies.
     * generateForDay does not invoke any of these; they exist only to satisfy construction.
     */
    private fun repo(): DailyChallengeRepository {
        val mealRepo = stubMealRepo()
        val goalRepo = stubGoalRepo(mealRepo)
        val pipBus = PipEventBus()
        val badgeDetector = BadgeDetector(
            mealRepository = mealRepo,
            badgeRepository = BadgeRepository(StubBadgeDao()),
            milestoneRepository = MilestoneRepository(StubMilestoneDao()),
            goalRepository = goalRepo,
            pipEventBus = pipBus,
            dailyChallengeDao = StubDailyChallengeDao(),
            mealDao = StubMealDao(),
        )
        return DailyChallengeRepository(
            dao = StubDailyChallengeDao(),
            mealRepository = mealRepo,
            goalRepository = goalRepo,
            pipEventBus = pipBus,
            badgeDetector = badgeDetector,
        )
    }

    // ---- Tests ----

    @Test
    fun `generateForDay is deterministic - same input same output`() {
        val r = repo()
        val epochDay = 19_000L
        val first = r.generateForDay(epochDay)
        val second = r.generateForDay(epochDay)
        assertEquals("generateForDay must return same result for same input", first, second)
    }

    @Test
    fun `generateForDay returns valid DailyChallengeType`() {
        val r = repo()
        for (day in 0L..100L) {
            val type = r.generateForDay(day)
            assertNotNull("generateForDay($day) should never be null", type)
        }
    }

    @Test
    fun `generateForDay cycles through all types`() {
        val r = repo()
        val count = types.size
        val seen = mutableSetOf<DailyChallengeType>()
        for (day in 0L until count) {
            seen.add(r.generateForDay(day))
        }
        assertEquals(
            "All ${count} DailyChallengeTypes should appear in one cycle",
            count,
            seen.size
        )
    }

    @Test
    fun `generateForDay repeats cycle after period`() {
        val r = repo()
        val count = types.size.toLong()
        for (day in 0L..20L) {
            val typeA = r.generateForDay(day)
            val typeB = r.generateForDay(day + count)
            assertEquals(
                "generateForDay($day) and generateForDay(${day + count}) must match",
                typeA,
                typeB
            )
        }
    }

    @Test
    fun `generateForDay epoch 0 maps to first type`() {
        val r = repo()
        assertEquals(types[0], r.generateForDay(0L))
    }

    @Test
    fun `generateForDay epoch 1 maps to second type`() {
        val r = repo()
        assertEquals(types[1], r.generateForDay(1L))
    }

    @Test
    fun `generateForDay epoch count minus 1 maps to last type`() {
        val r = repo()
        val last = types.size.toLong() - 1
        assertEquals(types[last.toInt()], r.generateForDay(last))
    }

    @Test
    fun `generateForDay is consistent for large epoch day values`() {
        val r = repo()
        val largeDay = 100_000L
        val expected = types[(largeDay.toInt()).mod(types.size)]
        assertEquals(expected, r.generateForDay(largeDay))
    }

    @Test
    fun `consecutive days produce different challenge types`() {
        // With 7 types and cycling, consecutive days should differ unless cycle size is 1
        val r = repo()
        val day0 = r.generateForDay(0)
        val day1 = r.generateForDay(1)
        // They should differ since we have 7 types and each gets its own slot
        if (types.size > 1) {
            org.junit.Assert.assertNotEquals(
                "Adjacent days should produce different challenges",
                day0,
                day1
            )
        }
    }
}
