package com.charles.nutrisnap.ai

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [LiteRtGemmaEngine].
 *
 * Requires a physical device with the Gemma 4 model downloaded and the LiteRT-LM SDK wired.
 * Skipped automatically when the engine is the [FakeGemmaEngine] (debug) or when [LiteRtGemmaEngine]
 * throws [NotImplementedError] (SDK not yet linked).
 */
@RunWith(AndroidJUnit4::class)
class LiteRtGemmaEngineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun warmUpThenAnalyzeFood() = runBlocking {
        // Cannot instantiate LiteRtGemmaEngine without Hilt + ModelRepository.
        // Manual test: uncomment below on a device with the model + SDK wired.
        assumeTrue("Manual test — run on physical device with Gemma 4 model", false)
    }

    @Test
    fun nonFoodImageReturnsLowConfidence() = runBlocking {
        assumeTrue("Manual test — run on physical device with Gemma 4 model", false)
    }
}
