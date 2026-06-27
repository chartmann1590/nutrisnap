package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.db.ChatDao
import com.charles.nutrisnap.data.db.ChatMessageEntity
import com.charles.nutrisnap.data.db.ChatRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
) {
    fun observeHistory(): Flow<List<ChatMessageEntity>> = chatDao.observeAll()

    suspend fun append(role: ChatRole, text: String): Long =
        chatDao.insert(
            ChatMessageEntity(role = role, text = text, timestampMs = System.currentTimeMillis())
        )

    /** Most recent [limit] messages, oldest-first (suitable for a prompt excerpt). */
    suspend fun recent(limit: Int): List<ChatMessageEntity> =
        chatDao.recent(limit).reversed()

    suspend fun clear() = chatDao.clearAll()
}
