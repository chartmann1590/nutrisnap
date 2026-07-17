package com.charles.nutrisnap.ai

import android.content.Context
import android.graphics.Bitmap
import com.charles.nutrisnap.BuildConfig
import com.charles.nutrisnap.data.ModelRepository
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtGemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
) : GemmaEngine {

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var warmedUp = false

    /** Wraps [block] in a Firebase Performance trace. Inline so suspend calls inside [block]
     *  are still allowed even though the parameter isn't typed `suspend`. */
    private inline fun <T> traced(name: String, block: () -> T): T {
        val trace = FirebasePerformance.getInstance().newTrace(name)
        trace.start()
        try {
            return block()
        } finally {
            trace.stop()
        }
    }

    override suspend fun warmUp(): Unit = traced("gemma_warm_up") {
        withContext(Dispatchers.Default) {
            mutex.withLock {
                if (warmedUp) return@withLock
                engine = createEngine()
                warmedUp = true
            }
        }
    }

    override suspend fun analyzeFood(image: Bitmap, hint: String?): Result<FoodEstimate> =
        traced("gemma_analyze_food") {
        withContext(Dispatchers.Default) {
            val eng = ensureEngine()
            val modelPath = resolveModelPath()
                ?: return@withContext Result.failure(IllegalStateException("Model not ready"))
            val conv = eng.createConversation(ConversationConfig(
                systemInstruction = Contents.of("You are a nutritionist. Return ONLY compact JSON.")
            ))
            var imageFile: File? = null
            try {
                imageFile = bitmapToTempFile(image)
                val prompt = buildMultimodalPrompt(hint)
                val response = conv.sendMessage(
                    Contents.of(Content.ImageFile(imageFile.absolutePath), Content.Text(prompt))
                )
                val text = extractText(response)
                android.util.Log.d("LiteRtGemmaEngine", "analyzeFood raw response: $text")
                parseFoodEstimate(text).also {
                    android.util.Log.d("LiteRtGemmaEngine", "analyzeFood parsed: $it")
                }
            } catch (e: Exception) {
                android.util.Log.e("LiteRtGemmaEngine", "analyzeFood error", e)
                Result.failure(e)
            } finally {
                conv.close()
                imageFile?.delete()
            }
        }
        }

    override suspend fun estimateFromText(description: String): Result<FoodEstimate> =
        traced("gemma_estimate_from_text") {
        withContext(Dispatchers.Default) {
            val eng = ensureEngine()
            val conv = eng.createConversation(ConversationConfig())
            try {
                val prompt = buildTextPrompt(description)
                val response = conv.sendMessage(prompt)
                val text = extractText(response)
                parseFoodEstimate(text)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                conv.close()
            }
        }
        }

    override fun isReady(): Boolean = warmedUp

    override suspend fun startChat(systemInstruction: String): ChatSession =
        withContext(Dispatchers.Default) {
            val eng = ensureEngine()
            val conv = eng.createConversation(
                ConversationConfig(systemInstruction = Contents.of(systemInstruction))
            )
            LiteRtChatSession(conv, mutex)
        }

    suspend fun verifyModel(): Result<Unit> = withContext(Dispatchers.Default) {
        val eng = ensureEngine()
        val conv = eng.createConversation(ConversationConfig())
        try {
            val response = conv.sendMessage("Reply OK")
            val text = extractText(response)
            if (text.isBlank()) throw IllegalStateException("Empty response from model")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conv.close()
        }
    }

    fun release() {
        if (!mutex.tryLock()) return
        try {
            engine?.close()
            engine = null
            warmedUp = false
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun ensureEngine(): Engine = withContext(Dispatchers.Default) {
        mutex.withLock {
            engine ?: createEngine().also {
                engine = it
                warmedUp = true
            }
        }
    }

    private fun createEngine(): Engine {
        val modelPath = resolveModelPath()
            ?: throw IllegalStateException("Model not ready — warmUp() after download")
        // Gemma 4 is multimodal: the text decoder AND the vision encoder each need a
        // backend. visionBackend defaults to null — leaving it unset makes the loader
        // skip the vision encoder, and sending an image then segfaults in native code.
        //
        // Prefer the GPU for speed, but the available GPU delegate varies by device (some
        // Mali GPUs expose no working OpenCL/OpenGL path and engine init throws). Fall back
        // to CPU (XNNPack) so the app still works everywhere rather than crashing.
        return runCatching { buildEngine(modelPath, Backend.GPU()) }
            .getOrElse { gpuError ->
                android.util.Log.w(
                    "LiteRtGemmaEngine",
                    "GPU backend unavailable, falling back to CPU: ${gpuError.message}",
                )
                buildEngine(modelPath, Backend.CPU())
            }
    }

    private fun buildEngine(modelPath: String, backend: Backend): Engine {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            visionBackend = backend,
            cacheDir = context.cacheDir.path,
        )
        return Engine(config).also { it.initialize() }
    }

    private fun resolveModelPath(): String? {
        return if (modelRepository.isReady()) {
            File(File(context.filesDir, "models"), BuildConfig.MODEL_FILE_NAME).absolutePath
        } else null
    }

    private fun bitmapToTempFile(bitmap: Bitmap): File {
        val file = File(context.cacheDir, "analyze_${System.nanoTime()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file
    }

    private fun buildMultimodalPrompt(hint: String?): String {
        val hintPart = if (!hint.isNullOrBlank()) "\nUser hint: $hint" else ""
        return """Analyze this food photo and return ONLY compact JSON with these fields:
name (string), portionDescription (string), grams (int), kcal (int),
proteinG (int), carbsG (int), fatG (int), confidence (float 0-1).
Optional: items (array) for multi-component meals, each with the same fields.$hintPart

JSON:"""
    }

    private fun buildTextPrompt(description: String): String {
        return """Estimate nutrition for: $description
Return ONLY compact JSON with: name, portionDescription, grams, kcal,
proteinG, carbsG, fatG, confidence (float 0-1).

JSON:"""
    }

    private fun extractText(msg: com.google.ai.edge.litertlm.Message): String {
        return msg.contents.contents.joinToString(separator = "") { content ->
            when (content) {
                is Content.Text -> content.text
                else -> ""
            }
        }
    }

    private class LiteRtChatSession(
        private val conv: Conversation,
        private val mutex: Mutex,
    ) : ChatSession {

        override fun sendStreaming(userText: String): Flow<String> = flow {
            mutex.withLock {
                val sb = StringBuilder()
                conv.sendMessageAsync(userText).collect { message ->
                    val delta = message.contents.contents.joinToString(separator = "") { content ->
                        when (content) {
                            is Content.Text -> content.text
                            else -> ""
                        }
                    }
                    sb.append(delta)
                    emit(sb.toString())
                }
            }
        }.flowOn(Dispatchers.Default)

        override fun close() {
            runCatching { conv.cancelProcess() }
            runCatching { conv.close() }
        }
    }
}
