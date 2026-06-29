package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestones")
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                    // MilestoneType.name
    val occurredAtMs: Long,
    val payload: String = ""
)
