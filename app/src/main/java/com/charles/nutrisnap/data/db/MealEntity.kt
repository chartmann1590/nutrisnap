package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_entity",
    indices = [Index(value = ["timestampMs"])]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long,
    val mealType: MealType,
    val name: String,
    val totalKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val photoUri: String? = null,
    val source: MealSource,
    val confidence: Float? = null,
)
