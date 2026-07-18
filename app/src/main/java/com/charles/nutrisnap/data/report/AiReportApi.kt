package com.charles.nutrisnap.data.report

import com.charles.nutrisnap.BuildConfig
import com.charles.nutrisnap.ai.FoodEstimate
import com.charles.nutrisnap.di.FeedbackJson
import com.charles.nutrisnap.di.GithubOkHttp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class AiReportCategory(val label: String) {
    WRONG_FOOD("Wrong food identified"),
    WRONG_NUTRITION("Wrong calories/macros"),
    INAPPROPRIATE("Inappropriate or offensive content"),
    OTHER("Something else"),
}

@Serializable
data class AiReportRequest(
    val category: String,
    val description: String,
    val foodName: String,
    val estimateJson: String,
    val appVersion: String,
)

/**
 * Lets users flag a bad/inappropriate AI food-estimate response. Goes through the same
 * Cloudflare Worker proxy as the GitHub feedback reporter (see workers/github-proxy) — the
 * app never talks to any report storage directly.
 */
@Singleton
class AiReportApi @Inject constructor(
    @GithubOkHttp private val client: OkHttpClient,
    @FeedbackJson private val json: Json,
) {
    private val proxyUrl: String get() = BuildConfig.GITHUB_PROXY_URL.trimEnd('/')
    private val proxySecret: String get() = BuildConfig.GITHUB_PROXY_SECRET

    val isConfigured: Boolean
        get() = proxyUrl.isNotBlank() && proxySecret.isNotBlank()

    suspend fun submitReport(
        category: AiReportCategory,
        description: String,
        estimate: FoodEstimate,
    ) {
        val request = AiReportRequest(
            category = category.name.lowercase(),
            description = description,
            foodName = estimate.name,
            estimateJson = json.encodeToString(FoodEstimate.serializer(), estimate),
            appVersion = BuildConfig.VERSION_NAME,
        )
        val requestBody = json.encodeToString(AiReportRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url("$proxyUrl/reports")
            .header("X-App-Secret", proxySecret)
            .post(requestBody)
            .build()
        val response = client.newCall(httpRequest).execute()
        response.use {
            if (!it.isSuccessful) {
                throw IOException("Report submission failed: ${it.code}")
            }
        }
    }
}
