package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_message ORDER BY timestampMs ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message ORDER BY timestampMs DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    @Query("DELETE FROM chat_message")
    suspend fun clearAll()
}
