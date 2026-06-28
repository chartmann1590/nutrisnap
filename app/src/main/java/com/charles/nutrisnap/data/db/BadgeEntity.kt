package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val badgeType: String,   // BadgeType.name
    val earnedAtMs: Long,
    val seen: Boolean = false
)
