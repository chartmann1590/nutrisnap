package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DayTotals(
    val totalKcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
)

data class DayTotalsWithEpochDay(
    val epochDay: Long,
    val totalKcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
)

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("DELETE FROM meal_entity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM meal_entity 
        WHERE timestampMs >= :startMs AND timestampMs < :endMs 
        ORDER BY timestampMs DESC
    """)
    fun observeRange(startMs: Long, endMs: Long): Flow<List<MealEntity>>

    @Query("""
        SELECT 
            COALESCE(SUM(totalKcal), 0) AS totalKcal,
            COALESCE(SUM(proteinG), 0) AS proteinG,
            COALESCE(SUM(carbsG), 0) AS carbsG,
            COALESCE(SUM(fatG), 0) AS fatG
        FROM meal_entity
        WHERE timestampMs >= :startMs AND timestampMs < :endMs
    """)
    fun observeDayTotals(startMs: Long, endMs: Long): Flow<DayTotals>

    @Query("""
        SELECT 
            timestampMs / 86400000 AS epochDay,
            COALESCE(SUM(totalKcal), 0) AS totalKcal,
            COALESCE(SUM(proteinG), 0) AS proteinG,
            COALESCE(SUM(carbsG), 0) AS carbsG,
            COALESCE(SUM(fatG), 0) AS fatG
        FROM meal_entity
        WHERE timestampMs >= :startMs AND timestampMs < :endMs
        GROUP BY epochDay
        ORDER BY epochDay ASC
    """)
    fun observeDayTotalsRange(startMs: Long, endMs: Long): Flow<List<DayTotalsWithEpochDay>>

    @Query("""
        SELECT DISTINCT timestampMs / 86400000 AS epochDay 
        FROM meal_entity 
        ORDER BY epochDay DESC
    """)
    fun observeDistinctLoggedDays(): Flow<List<Long>>
}
