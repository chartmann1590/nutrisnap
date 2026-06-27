package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntryEntity): Long

    @Query("SELECT * FROM weight_entry_entity ORDER BY dateEpochDay DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>

    @Query("""
        SELECT * FROM weight_entry_entity 
        WHERE dateEpochDay >= :startDay AND dateEpochDay <= :endDay 
        ORDER BY dateEpochDay ASC
    """)
    fun observeRange(startDay: Long, endDay: Long): Flow<List<WeightEntryEntity>>

    @Query("DELETE FROM weight_entry_entity WHERE id = :id")
    suspend fun deleteById(id: Long)
}
