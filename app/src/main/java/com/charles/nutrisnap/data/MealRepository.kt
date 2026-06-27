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
    private companion object {
        private const val MS_PER_DAY = 86_400_000L
    }

    private fun epochDayToMs(epochDay: Long): Pair<Long, Long> {
        val startMs = epochDay * MS_PER_DAY
        return startMs to startMs + MS_PER_DAY
    }

    private fun todayEpochDay(): Long =
        System.currentTimeMillis() / MS_PER_DAY

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
        val startMs = (today - 6) * MS_PER_DAY
        val endMs = (today + 1) * MS_PER_DAY
        return mealDao!!.observeDayTotalsRange(startMs, endMs)
    }

    open fun observeDistinctLoggedDays(): Flow<List<Long>> =
        mealDao!!.observeDistinctLoggedDays()

    open suspend fun logMeal(meal: MealEntity): Long = mealDao!!.insert(meal)

    open suspend fun deleteMeal(id: Long) = mealDao!!.deleteById(id)
}
