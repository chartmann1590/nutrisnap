package com.charles.nutrisnap.data.challenge

import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.PipEvent
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.badge.BadgeDetector
import com.charles.nutrisnap.data.db.DailyChallengeDao
import com.charles.nutrisnap.data.db.DailyChallengeEntity
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class DailyChallengeState(
    val type: DailyChallengeType,
    val isComplete: Boolean,
    val progress: Float,          // 0f..1f
    val rewardBadge: com.charles.nutrisnap.data.badge.BadgeType?,
)

@Singleton
class DailyChallengeRepository @Inject constructor(
    private val dao: DailyChallengeDao,
    private val mealRepository: MealRepository,
    private val goalRepository: GoalRepository,
    private val pipEventBus: PipEventBus,
    private val badgeDetector: BadgeDetector,
) {
    /**
     * Returns a deterministic [DailyChallengeType] for the given epoch-day.
     * Same input always returns the same output.
     */
    fun generateForDay(epochDay: Long): DailyChallengeType =
        DailyChallengeType.values()[epochDay.toInt().mod(DailyChallengeType.values().size)]

    /**
     * Inserts a challenge row for today if one does not already exist.
     */
    suspend fun ensureTodayChallenge() {
        val today = mealRepository.todayEpochDay()
        if (dao.getForDay(today) == null) {
            dao.upsert(
                DailyChallengeEntity(
                    dateEpochDay = today,
                    challengeId = generateForDay(today).name,
                )
            )
        }
    }

    /** Observe today's challenge state, combining DB row with live meal/goal data. */
    fun getToday(): Flow<DailyChallengeState?> {
        val today = mealRepository.todayEpochDay()
        return combine(
            dao.observeForDay(today),
            mealRepository.observeTodayMeals(),
            goalRepository.observeRemaining(),
        ) { entity, meals, remaining ->
            entity ?: return@combine null
            val type = DailyChallengeType.valueOf(entity.challengeId)
            val isComplete = entity.completedAtMs != null
            val progress = if (isComplete) 1f else computeProgress(type, meals, remaining)
            DailyChallengeState(type, isComplete, progress, type.rewardBadge)
        }
    }

    // --- Legacy methods kept for backwards compatibility ---

    fun getCompleted(): Flow<List<DailyChallengeEntity>> = dao.getCompleted()

    suspend fun getForDay(dateEpochDay: Long): DailyChallengeEntity? =
        dao.getForDay(dateEpochDay)

    suspend fun upsert(entity: DailyChallengeEntity) {
        dao.upsert(entity)
    }

    /**
     * Evaluates whether today's challenge is complete. If newly complete, persists
     * the completion timestamp, triggers badge detection, and emits [PipEvent.ChallengeComplete].
     */
    suspend fun checkAndComplete(epochDay: Long) {
        val entity = dao.getForDay(epochDay) ?: return
        if (entity.completedAtMs != null) return  // already marked complete

        val type = DailyChallengeType.valueOf(entity.challengeId)
        val meals = mealRepository.observeMealsForDay(epochDay).first()
        val remaining = goalRepository.observeRemaining().first()

        val isComplete = evaluateCompletion(type, meals, remaining)

        if (isComplete) {
            dao.upsert(entity.copy(completedAtMs = System.currentTimeMillis()))
            badgeDetector.checkAndAward()
            pipEventBus.emit(PipEvent.ChallengeComplete(type))
        }
    }

    // ---- private helpers ----

    private fun evaluateCompletion(
        type: DailyChallengeType,
        meals: List<MealEntity>,
        remaining: Remaining?,
    ): Boolean = when (type) {
        DailyChallengeType.LOG_BEFORE_NOON -> meals.any { meal ->
            hour(meal.timestampMs) < 12
        }
        DailyChallengeType.HIT_PROTEIN ->
            remaining != null && remaining.proteinRemaining <= 0
        DailyChallengeType.LOG_THREE_MEALS -> {
            val types = meals.map { it.mealType }.toSet()
            MealType.BREAKFAST in types && MealType.LUNCH in types && MealType.DINNER in types
        }
        DailyChallengeType.BALANCED_MACROS -> {
            if (remaining == null) false
            else {
                val goal = remaining.goal
                val totals = remaining.totals
                totals.proteinG >= goal.proteinG * 0.9 &&
                    totals.carbsG >= goal.carbsG * 0.9 &&
                    totals.fatG >= goal.fatG * 0.9
            }
        }
        DailyChallengeType.EARLY_BREAKFAST -> meals.any { meal ->
            meal.mealType == MealType.BREAKFAST && hour(meal.timestampMs) < 8
        }
        DailyChallengeType.HIT_CALORIE_GOAL ->
            remaining != null && remaining.kcalRemaining in (-100..100)
        DailyChallengeType.LIGHT_DAY ->
            remaining != null &&
                remaining.totals.totalKcal <= (remaining.goal.calories * 0.8).toInt()
    }

    private fun computeProgress(
        type: DailyChallengeType,
        meals: List<MealEntity>,
        remaining: Remaining?,
    ): Float = when (type) {
        DailyChallengeType.LOG_BEFORE_NOON -> {
            if (meals.any { hour(it.timestampMs) < 12 }) 1f
            else if (meals.isNotEmpty()) 0.5f
            else 0f
        }
        DailyChallengeType.HIT_PROTEIN -> {
            val goal = remaining?.goal?.proteinG ?: 0
            if (goal <= 0) 0f
            else (remaining!!.totals.proteinG.toFloat() / goal).coerceIn(0f, 1f)
        }
        DailyChallengeType.LOG_THREE_MEALS -> {
            val mealTypes = meals.map { it.mealType }.toSet()
            val hit = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER).count { it in mealTypes }
            (hit / 3f).coerceIn(0f, 1f)
        }
        DailyChallengeType.BALANCED_MACROS -> {
            if (remaining == null) 0f
            else {
                val goal = remaining.goal
                val totals = remaining.totals
                val pFrac = if (goal.proteinG > 0) (totals.proteinG.toFloat() / goal.proteinG).coerceIn(0f, 1f) else 1f
                val cFrac = if (goal.carbsG > 0) (totals.carbsG.toFloat() / goal.carbsG).coerceIn(0f, 1f) else 1f
                val fFrac = if (goal.fatG > 0) (totals.fatG.toFloat() / goal.fatG).coerceIn(0f, 1f) else 1f
                minOf(pFrac, cFrac, fFrac)
            }
        }
        DailyChallengeType.EARLY_BREAKFAST ->
            if (meals.any { it.mealType == MealType.BREAKFAST && hour(it.timestampMs) < 8 }) 1f else 0f
        DailyChallengeType.HIT_CALORIE_GOAL -> {
            val goal = remaining?.goal?.calories ?: 0
            if (goal <= 0) 0f
            else (remaining!!.totals.totalKcal.toFloat() / goal).coerceIn(0f, 1f)
        }
        DailyChallengeType.LIGHT_DAY -> {
            if (remaining == null) 0f
            else {
                val goal = remaining.goal.calories
                val threshold = goal * 0.8f
                val logged = remaining.totals.totalKcal.toFloat()
                if (logged <= 0f) 0f
                else (threshold / logged).coerceIn(0f, 1f)
            }
        }
    }

    private fun hour(timestampMs: Long): Int =
        Calendar.getInstance().apply { timeInMillis = timestampMs }.get(Calendar.HOUR_OF_DAY)
}
