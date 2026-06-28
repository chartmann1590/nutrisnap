package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val dateEpochDay: Long,
    val challengeId: String,             // DailyChallengeType.name
    val completedAtMs: Long? = null
)
