package com.charles.nutrisnap.ai

import kotlinx.serialization.Serializable

@Serializable
data class FoodItem(
    val name: String,
    val portionDescription: String = "",
    val grams: Int = 0,
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
)

@Serializable
data class FoodEstimate(
    val name: String,
    val portionDescription: String = "",
    val grams: Int = 0,
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val confidence: Float = 0f,
    val items: List<FoodItem> = emptyList(),
)
