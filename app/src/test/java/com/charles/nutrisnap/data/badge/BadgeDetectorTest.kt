package com.charles.nutrisnap.data.badge

import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.PipEvent
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.data.UnitSystem
import com.charles.nutrisnap.data.UserPrefs
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.db.BadgeDao
import com.charles.nutrisnap.data.db.BadgeEntity
import com.charles.nutrisnap.data.db.DailyChallengeDao
import com.charles.nutrisnap.data.db.DailyChallengeEntity
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.DayTotalsWithEpochDay
import com.charles.nutrisnap.data.db.MealDao
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.data.db.MilestoneDao
import com.charles.nutrisnap.data.db.MilestoneEntity
import com.charles.nutrisnap.data.milestone.MilestoneRepository
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BadgeDetectorTest {

    // ---- Fakes ----

    private fun fakeMealRepo(
        meals: List<MealEntity> = emptyList(),
        loggedDays: List<Long> = emptyList(),
        today: Long = 1000L,
    ) = object : MealRepository(null) {
        override fun todayEpochDay() = today
        override fun observeTodayMeals() = flowOf(meals)
        override fun observeTodayTotals() = flowOf(DayTotals())
        override fun observeDistinctLoggedDays() = flowOf(loggedDays)
        override fun observeMealsForDay(epochDay: Long) = flowOf(meals)
    }

    private fun fakeGoalRepo(remaining: Remaining? = null): GoalRepository {
        val prefs = object : UserPreferencesRepository(null) {
            override val prefs = MutableStateFlow(
                UserPrefs(false, null, UnitSystem.METRIC, ModelVariant.E2B, ThemeMode.SYSTEM)
            )
        }
        val mealRepo = fakeMealRepo()
        return object : GoalRepository(prefs, mealRepo) {
            override fun observeRemaining() = flowOf(remaining)
        }
    }

    private class FakeBadgeDao : BadgeDao {
        val awarded = mutableListOf<BadgeEntity>()
        override suspend fun insertOrIgnore(badge: BadgeEntity) {
            if (awarded.none { it.badgeType == badge.badgeType }) awarded.add(badge)
        }
        override fun getAll(): Flow<List<BadgeEntity>> = flowOf(awarded.toList())
        override suspend fun markSeen(type: String) {}
    }

    private class FakeMilestoneDao : MilestoneDao {
        val recorded = mutableListOf<MilestoneEntity>()
        override suspend fun insert(milestone: MilestoneEntity) { recorded.add(milestone) }
        override fun getAll(): Flow<List<MilestoneEntity>> = flowOf(recorded.toList())
    }

    private class FakeDailyChallengeDao(private val completedCount: Int = 0) : DailyChallengeDao {
        override suspend fun upsert(entity: DailyChallengeEntity) {}
        override suspend fun getForDay(day: Long): DailyChallengeEntity? = null
        override fun observeForDay(day: Long): Flow<DailyChallengeEntity?> = flowOf(null)
        override fun getCompleted(): Flow<List<DailyChallengeEntity>> = flowOf(emptyList())
        override suspend fun countCompleted(): Int = completedCount
    }

    private class FakeMealDao(private val count: Int = 0) : MealDao {
        override suspend fun insert(meal: MealEntity): Long = 0L
        override suspend fun update(meal: MealEntity) {}
        override suspend fun delete(meal: MealEntity) {}
        override suspend fun deleteById(id: Long) {}
        override fun observeRange(startMs: Long, endMs: Long): Flow<List<MealEntity>> = flowOf(emptyList())
        override fun observeDayTotals(startMs: Long, endMs: Long): Flow<DayTotals> = flowOf(DayTotals())
        override fun observeDayTotalsRange(startMs: Long, endMs: Long, offsetMs: Long): Flow<List<DayTotalsWithEpochDay>> = flowOf(emptyList())
        override fun observeDistinctLoggedLocalDays(offsetMs: Long): Flow<List<Long>> = flowOf(emptyList())
        override suspend fun countAll(): Int = count
        override suspend fun getById(id: Long): MealEntity? = null
    }

    private fun makeDetector(
        mealRepo: MealRepository = fakeMealRepo(),
        badgeDao: FakeBadgeDao = FakeBadgeDao(),
        milestoneDao: FakeMilestoneDao = FakeMilestoneDao(),
        goalRepo: GoalRepository = fakeGoalRepo(),
        pipBus: PipEventBus = PipEventBus(),
        challengeDao: FakeDailyChallengeDao = FakeDailyChallengeDao(),
        mealDao: FakeMealDao = FakeMealDao(),
    ) = BadgeDetector(
        mealRepository = mealRepo,
        badgeRepository = BadgeRepository(badgeDao),
        milestoneRepository = MilestoneRepository(milestoneDao),
        goalRepository = goalRepo,
        pipEventBus = pipBus,
        dailyChallengeDao = challengeDao,
        mealDao = mealDao,
    )

    /** Collects events from [bus] synchronously during [block] execution. */
    private suspend fun collectEvents(
        bus: PipEventBus,
        block: suspend () -> Unit,
    ): List<PipEvent> {
        val events = mutableListOf<PipEvent>()
        val job = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher()).launch {
            bus.events.collect { events.add(it) }
        }
        block()
        job.cancel()
        return events
    }

    // ---- Tests ----

    @Test
    fun `no meals - no badges awarded`() = runTest {
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(badgeDao = badgeDao)
        detector.checkAndAward()
        assertTrue("no badges should be awarded with no data", badgeDao.awarded.isEmpty())
    }

    @Test
    fun `one logged day awards FIRST_BITE`() = runTest {
        val today = 1000L
        val mealRepo = fakeMealRepo(loggedDays = listOf(today), today = today)
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao, mealDao = FakeMealDao(count = 1))
        detector.checkAndAward()

        assertTrue(
            "FIRST_BITE should be awarded after first logged day",
            badgeDao.awarded.any { it.badgeType == BadgeType.FIRST_BITE.name }
        )
    }

    @Test
    fun `FIRST_BITE triggers BadgeEarned event`() = runTest {
        val today = 1000L
        val mealRepo = fakeMealRepo(loggedDays = listOf(today), today = today)
        val bus = PipEventBus()
        val detector = makeDetector(mealRepo = mealRepo, pipBus = bus, mealDao = FakeMealDao(count = 1))

        val events = collectEvents(bus) { detector.checkAndAward() }

        assertTrue(
            "BadgeEarned(FIRST_BITE) should be emitted",
            events.any { it is PipEvent.BadgeEarned && it.badge == BadgeType.FIRST_BITE }
        )
    }

    @Test
    fun `streak 7 awards HOT_STREAK and emits StreakMilestone(7)`() = runTest {
        val today = 1006L
        val loggedDays = (1000L..1006L).toList()  // 7-day streak
        val mealRepo = fakeMealRepo(loggedDays = loggedDays, today = today)
        val badgeDao = FakeBadgeDao()
        val bus = PipEventBus()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao, pipBus = bus)

        val events = collectEvents(bus) { detector.checkAndAward() }

        assertTrue(
            "HOT_STREAK should be awarded for 7-day streak",
            badgeDao.awarded.any { it.badgeType == BadgeType.HOT_STREAK.name }
        )
        assertTrue(
            "StreakMilestone(7) should be emitted",
            events.any { it is PipEvent.StreakMilestone && it.days == 7 }
        )
    }

    @Test
    fun `streak 3 awards ON_A_ROLL but not HOT_STREAK`() = runTest {
        val today = 1002L
        val loggedDays = (1000L..1002L).toList()
        val mealRepo = fakeMealRepo(loggedDays = loggedDays, today = today)
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao)
        detector.checkAndAward()

        assertTrue(
            "ON_A_ROLL should be awarded for 3-day streak",
            badgeDao.awarded.any { it.badgeType == BadgeType.ON_A_ROLL.name }
        )
        assertFalse(
            "HOT_STREAK should NOT be awarded for 3-day streak",
            badgeDao.awarded.any { it.badgeType == BadgeType.HOT_STREAK.name }
        )
    }

    @Test
    fun `streak 14 awards HOT_STREAK and FORTNIGHT with milestone events`() = runTest {
        val today = 1013L
        val loggedDays = (1000L..1013L).toList()  // 14-day streak
        val mealRepo = fakeMealRepo(loggedDays = loggedDays, today = today)
        val badgeDao = FakeBadgeDao()
        val bus = PipEventBus()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao, pipBus = bus)

        val events = collectEvents(bus) { detector.checkAndAward() }

        assertTrue(badgeDao.awarded.any { it.badgeType == BadgeType.FORTNIGHT.name })
        assertTrue(events.any { it is PipEvent.StreakMilestone && it.days == 14 })
    }

    @Test
    fun `idempotent - calling checkAndAward twice does not double award`() = runTest {
        val today = 1000L
        val mealRepo = fakeMealRepo(loggedDays = listOf(today), today = today)
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao, mealDao = FakeMealDao(count = 1))

        detector.checkAndAward()
        detector.checkAndAward()

        val count = badgeDao.awarded.count { it.badgeType == BadgeType.FIRST_BITE.name }
        assertEquals("FIRST_BITE should be awarded exactly once", 1, count)
    }

    @Test
    fun `CHALLENGE_CHAMP awarded when completed challenge exists`() = runTest {
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(
            badgeDao = badgeDao,
            challengeDao = FakeDailyChallengeDao(completedCount = 1),
        )
        detector.checkAndAward()

        assertTrue(
            "CHALLENGE_CHAMP should be awarded when 1 challenge is completed",
            badgeDao.awarded.any { it.badgeType == BadgeType.CHALLENGE_CHAMP.name }
        )
    }

    @Test
    fun `CHALLENGE_CHAMP not awarded with no completed challenges`() = runTest {
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(
            badgeDao = badgeDao,
            challengeDao = FakeDailyChallengeDao(completedCount = 0),
        )
        detector.checkAndAward()

        assertFalse(
            "CHALLENGE_CHAMP should NOT be awarded when no challenges completed",
            badgeDao.awarded.any { it.badgeType == BadgeType.CHALLENGE_CHAMP.name }
        )
    }

    @Test
    fun `milestone recorded when badge awarded`() = runTest {
        val today = 1000L
        val mealRepo = fakeMealRepo(loggedDays = listOf(today), today = today)
        val milestoneDao = FakeMilestoneDao()
        val detector = makeDetector(mealRepo = mealRepo, milestoneDao = milestoneDao, mealDao = FakeMealDao(count = 1))
        detector.checkAndAward()

        assertTrue(
            "Milestone should be recorded for FIRST_BITE",
            milestoneDao.recorded.any { it.payload == BadgeType.FIRST_BITE.name }
        )
    }

    @Test
    fun `BALANCED_DAY awarded when all macros exceeded`() = runTest {
        val goal = DailyGoal(calories = 2000, proteinG = 100, carbsG = 250, fatG = 65)
        // All macros hit: protein=110 >= 100, carbs=260 >= 250, fat=70 >= 65
        val remaining = Remaining(goal = goal, totals = DayTotals(2000, proteinG = 110, carbsG = 260, fatG = 70))
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(badgeDao = badgeDao, goalRepo = fakeGoalRepo(remaining))
        detector.checkAndAward()

        assertTrue(
            "BALANCED_DAY should be awarded when all macros are met",
            badgeDao.awarded.any { it.badgeType == BadgeType.BALANCED_DAY.name }
        )
    }

    @Test
    fun `BALANCED_DAY not awarded when macros short`() = runTest {
        val goal = DailyGoal(calories = 2000, proteinG = 100, carbsG = 250, fatG = 65)
        val remaining = Remaining(goal = goal, totals = DayTotals(1500, proteinG = 50, carbsG = 150, fatG = 30))
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(badgeDao = badgeDao, goalRepo = fakeGoalRepo(remaining))
        detector.checkAndAward()

        assertFalse(
            "BALANCED_DAY should NOT be awarded when macros are not met",
            badgeDao.awarded.any { it.badgeType == BadgeType.BALANCED_DAY.name }
        )
    }

    @Test
    fun `DAILY_TRIPLE awarded when breakfast lunch and dinner all logged today`() = runTest {
        val today = 1000L
        val ts = System.currentTimeMillis()
        val meals = listOf(
            dummyMeal(MealType.BREAKFAST, ts),
            dummyMeal(MealType.LUNCH, ts + 1),
            dummyMeal(MealType.DINNER, ts + 2),
        )
        val mealRepo = fakeMealRepo(meals = meals, loggedDays = listOf(today), today = today)
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao)
        detector.checkAndAward()

        assertTrue(
            "DAILY_TRIPLE should be awarded when B+L+D logged today",
            badgeDao.awarded.any { it.badgeType == BadgeType.DAILY_TRIPLE.name }
        )
    }

    @Test
    fun `DAILY_TRIPLE not awarded when only breakfast and lunch logged`() = runTest {
        val today = 1000L
        val ts = System.currentTimeMillis()
        val meals = listOf(
            dummyMeal(MealType.BREAKFAST, ts),
            dummyMeal(MealType.LUNCH, ts + 1),
        )
        val mealRepo = fakeMealRepo(meals = meals, loggedDays = listOf(today), today = today)
        val badgeDao = FakeBadgeDao()
        val detector = makeDetector(mealRepo = mealRepo, badgeDao = badgeDao)
        detector.checkAndAward()

        assertFalse(
            "DAILY_TRIPLE should NOT be awarded without dinner",
            badgeDao.awarded.any { it.badgeType == BadgeType.DAILY_TRIPLE.name }
        )
    }

    // ---- Helpers ----

    private fun dummyMeal(
        mealType: MealType = MealType.LUNCH,
        timestampMs: Long = System.currentTimeMillis(),
    ) = MealEntity(
        timestampMs = timestampMs,
        mealType = mealType,
        name = "Test meal",
        totalKcal = 400,
        proteinG = 20,
        carbsG = 40,
        fatG = 15,
        source = MealSource.MANUAL,
    )
}
