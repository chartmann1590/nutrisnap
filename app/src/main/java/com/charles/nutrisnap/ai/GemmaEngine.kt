package com.charles.nutrisnap.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface GemmaEngine {
    suspend fun warmUp()
    suspend fun analyzeFood(image: Bitmap, hint: String? = null): Result<FoodEstimate>
    suspend fun estimateFromText(description: String): Result<FoodEstimate>
    fun isReady(): Boolean

    /** Start a multi-turn chat conversation seeded with [systemInstruction]. */
    suspend fun startChat(systemInstruction: String): ChatSession
}

/** A live multi-turn chat conversation backed by the on-device model. */
interface ChatSession {
    /**
     * Stream a reply to [userText]. Emits the cumulative reply text (each emission
     * is the full text so far). Completes when generation finishes; throws on error.
     */
    fun sendStreaming(userText: String): Flow<String>

    /** Cancel any in-flight generation and release the conversation. */
    fun close()
}
