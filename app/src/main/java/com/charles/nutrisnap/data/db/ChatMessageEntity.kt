package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ChatRole { USER, PIP }

@Entity(
    tableName = "chat_message",
    indices = [Index(value = ["timestampMs"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val timestampMs: Long,
)
