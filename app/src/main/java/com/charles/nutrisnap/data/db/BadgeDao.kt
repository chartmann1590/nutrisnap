package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(badge: BadgeEntity)

    @Query("SELECT * FROM badges ORDER BY earnedAtMs DESC")
    fun getAll(): Flow<List<BadgeEntity>>

    @Query("UPDATE badges SET seen = 1 WHERE badgeType = :type")
    suspend fun markSeen(type: String)
}
