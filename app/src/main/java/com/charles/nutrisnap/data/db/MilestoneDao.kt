package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Insert
    suspend fun insert(milestone: MilestoneEntity)

    @Query("SELECT * FROM milestones ORDER BY occurredAtMs DESC")
    fun getAll(): Flow<List<MilestoneEntity>>
}
