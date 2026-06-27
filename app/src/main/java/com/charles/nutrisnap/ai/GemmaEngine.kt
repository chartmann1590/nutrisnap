package com.charles.nutrisnap.ai

import android.graphics.Bitmap

interface GemmaEngine {
    suspend fun warmUp()
    suspend fun analyzeFood(image: Bitmap, hint: String? = null): Result<FoodEstimate>
    suspend fun estimateFromText(description: String): Result<FoodEstimate>
    fun isReady(): Boolean
}
