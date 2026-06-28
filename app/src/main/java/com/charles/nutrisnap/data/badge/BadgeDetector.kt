package com.charles.nutrisnap.data.badge

import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.PipEvent
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.db.DailyChallengeDao
import com.charles.nutrisnap.data.db.MealDao
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.data.milestone.MilestoneRepository
import com.charles.nutrisnap.data.milestone.MilestoneType
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeDetector @Inject constructor(
    private val mealRepository: MealRepository,
    private val badgeRepository: BadgeRepository,
    private val milestoneRepository: MilestoneRepository,
    private val goalRepository: GoalRepository,
    private val pipEventBus: PipEventBus,
    private val dailyChallengeDao: DailyChallengeDao,
    private val mealDao: MealDao,
) {
    /**
     * Checks all badge conditions and awards any newly earned badges.
     * Idempotent: safe to call multiple times — already-earned badges are skipped.
     */
    suspend fun checkAndAward() {
        val today = mealRepository.todayEpochDay()
        val loggedDays = mealRepository.observeDistinctLoggedDays().first().toSet()
        val todayMeals = mealRepository.observeTodayMeals().first()
        val remaining = goalRepository.observeRemaining().first()
        val earnedBadges = badgeRepository.getAll().first().map { it.badgeType }.toSet()
        val currentStreak = StreakCalculator.currentStreak(loggedDays, today)

        // Use total meal entries as the meal-count metric
        val mealCount = mealDao.countAll()

        // Helper: award a badge only if condition is met and badge not yet earned
        suspend fun awardIf(type: BadgeType, condition: Boolean) {
            if (condition && type.name !in earnedBadges) {
                badgeRepository.awardIfNew(type)
                milestoneRepository.record(MilestoneType.BADGE_EARNED, type.name)
                pipEventBus.emit(PipEvent.BadgeEarned(type))
            }
        }

        // --- Meal count badges ---
        awardIf(BadgeType.FIRST_BITE, mealCount >= 1)
        awardIf(BadgeType.SEASONED, mealCount >= 10)
        awardIf(BadgeType.CHEF_HAT, mealCount >= 25)
        awardIf(BadgeType.HALF_CENTURY, mealCount >= 50)
        awardIf(BadgeType.CENTURY, mealCount >= 100)

        // --- Streak badges (also emit StreakMilestone on first award) ---
        awardIf(BadgeType.ON_A_ROLL, currentStreak >= 3)

        if (currentStreak >= 7 && BadgeType.HOT_STREAK.name !in earnedBadges) {
            badgeRepository.awardIfNew(BadgeType.HOT_STREAK)
            milestoneRepository.record(MilestoneType.BADGE_EARNED, BadgeType.HOT_STREAK.name)
            pipEventBus.emit(PipEvent.BadgeEarned(BadgeType.HOT_STREAK))
            pipEventBus.emit(PipEvent.StreakMilestone(7))
        }

        if (currentStreak >= 14 && BadgeType.FORTNIGHT.name !in earnedBadges) {
            badgeRepository.awardIfNew(BadgeType.FORTNIGHT)
            milestoneRepository.record(MilestoneType.BADGE_EARNED, BadgeType.FORTNIGHT.name)
            pipEventBus.emit(PipEvent.BadgeEarned(BadgeType.FORTNIGHT))
            pipEventBus.emit(PipEvent.StreakMilestone(14))
        }

        if (currentStreak >= 30 && BadgeType.UNSTOPPABLE.name !in earnedBadges) {
            badgeRepository.awardIfNew(BadgeType.UNSTOPPABLE)
            milestoneRepository.record(MilestoneType.BADGE_EARNED, BadgeType.UNSTOPPABLE.name)
            pipEventBus.emit(PipEvent.BadgeEarned(BadgeType.UNSTOPPABLE))
            pipEventBus.emit(PipEvent.StreakMilestone(30))
        }

        // --- Time-based badges ---
        val hasEarlyBreakfast = todayMeals.any { meal ->
            meal.mealType == MealType.BREAKFAST &&
                Calendar.getInstance().apply { timeInMillis = meal.timestampMs }
                    .get(Calendar.HOUR_OF_DAY) < 9
        }
        awardIf(BadgeType.EARLY_BIRD, hasEarlyBreakfast)

        val hasLateDinner = todayMeals.any { meal ->
            meal.mealType == MealType.DINNER &&
                Calendar.getInstance().apply { timeInMillis = meal.timestampMs }
                    .get(Calendar.HOUR_OF_DAY) >= 20
        }
        awardIf(BadgeType.NIGHT_OWL, hasLateDinner)

        // --- Meal variety badges ---
        val todayTypes = todayMeals.map { it.mealType }.toSet()
        val hasTriple = MealType.BREAKFAST in todayTypes &&
            MealType.LUNCH in todayTypes &&
            MealType.DINNER in todayTypes
        awardIf(BadgeType.DAILY_TRIPLE, hasTriple)

        // --- Nutrition badges ---
        if (remaining != null) {
            val allMacrosHit = remaining.proteinRemaining <= 0 &&
                remaining.carbsRemaining <= 0 &&
                remaining.fatRemaining <= 0
            awardIf(BadgeType.BALANCED_DAY, allMacrosHit)
        }

        // --- Challenge badge ---
        val completedChallenges = dailyChallengeDao.countCompleted()
        awardIf(BadgeType.CHALLENGE_CHAMP, completedChallenges >= 1)

    }
}
