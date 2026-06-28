package com.charles.nutrisnap.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake engine for debug/preview use — returns a fixed plausible estimate.
 * The DI module switches to this when [com.charles.nutrisnap.BuildConfig.DEBUG] is true.
 */
class FakeGemmaEngine : GemmaEngine {

    private var ready = false

    override suspend fun warmUp() {
        ready = true
    }

    override suspend fun analyzeFood(image: Bitmap, hint: String?): Result<FoodEstimate> {
        ready = true
        return Result.success(
            FoodEstimate(
                name = "Creamy pasta",
                portionDescription = "~1 cup",
                grams = 240,
                kcal = 610,
                proteinG = 22,
                carbsG = 74,
                fatG = 26,
                confidence = 0.94f,
                items = listOf(
                    FoodItem(name = "Pasta", portionDescription = "~2 cups cooked", grams = 200, kcal = 440, proteinG = 16, carbsG = 66, fatG = 6),
                    FoodItem(name = "Cream sauce", portionDescription = "~1/3 cup", grams = 40, kcal = 170, proteinG = 6, carbsG = 8, fatG = 20),
                ),
            )
        )
    }

    override suspend fun estimateFromText(description: String): Result<FoodEstimate> {
        ready = true
        return Result.success(
            FoodEstimate(
                name = description,
                portionDescription = "estimated from description",
                grams = 200,
                kcal = 400,
                proteinG = 20,
                carbsG = 50,
                fatG = 15,
                confidence = 0.7f,
            )
        )
    }

    override fun isReady(): Boolean = ready

    /** When true, chat streams fail (for testing the ViewModel's error path). */
    var chatShouldFail: Boolean = false

    override suspend fun startChat(systemInstruction: String): ChatSession {
        ready = true
        val fail = chatShouldFail
        return object : ChatSession {
            override fun sendStreaming(userText: String): Flow<String> =
                if (fail) flow { throw RuntimeException("chat failure") }
                else flowOf("Hey", "Hey there", "Hey there!")
            override fun close() {}
        }
    }
}
