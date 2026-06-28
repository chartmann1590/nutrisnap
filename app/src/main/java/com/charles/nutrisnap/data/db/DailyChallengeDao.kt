package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyChallengeEntity)

    @Query("SELECT * FROM daily_challenges WHERE dateEpochDay = :day")
    suspend fun getForDay(day: Long): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges WHERE completedAtMs IS NOT NULL")
    fun getCompleted(): Flow<List<DailyChallengeEntity>>
}
