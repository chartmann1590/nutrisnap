package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.DayTotalsWithEpochDay
import com.charles.nutrisnap.data.db.MealDao
import com.charles.nutrisnap.data.db.MealEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MealRepository @Inject constructor(
    private val mealDao: MealDao?,
) {
    private fun epochDayToMs(epochDay: Long): Pair<Long, Long> =
        localDayRangeMs(epochDay)

    /** Today as a local epoch-day (local calendar day, not UTC). */
    open fun todayEpochDay(): Long = localEpochDay()

    open fun observeMealsForDay(epochDay: Long): Flow<List<MealEntity>> {
        val (startMs, endMs) = epochDayToMs(epochDay)
        return mealDao!!.observeRange(startMs, endMs)
    }

    open fun observeTodayMeals(): Flow<List<MealEntity>> =
        observeMealsForDay(todayEpochDay())

    open fun observeDayTotals(epochDay: Long): Flow<DayTotals> {
        val (startMs, endMs) = epochDayToMs(epochDay)
        return mealDao!!.observeDayTotals(startMs, endMs)
    }

    open fun observeTodayTotals(): Flow<DayTotals> =
        observeDayTotals(todayEpochDay())

    open fun observeWeekTotals(): Flow<List<DayTotalsWithEpochDay>> {
        val today = todayEpochDay()
        val (startMs, _) = epochDayToMs(today - 6)
        val (_, endMs) = epochDayToMs(today)
        return mealDao!!.observeDayTotalsRange(startMs, endMs, tzOffsetMs())
    }

    open fun observeDistinctLoggedDays(): Flow<List<Long>> =
        mealDao!!.observeDistinctLoggedLocalDays(tzOffsetMs())

    open suspend fun logMeal(meal: MealEntity): Long = mealDao!!.insert(meal)

    open suspend fun deleteMeal(id: Long) = mealDao!!.deleteById(id)
}
