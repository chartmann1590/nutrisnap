package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_entry_entity",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Double,
)
