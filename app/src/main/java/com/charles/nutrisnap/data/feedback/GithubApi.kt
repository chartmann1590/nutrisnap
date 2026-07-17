package com.charles.nutrisnap.data.feedback

import com.charles.nutrisnap.BuildConfig
import com.charles.nutrisnap.di.FeedbackJson
import com.charles.nutrisnap.di.GithubOkHttp
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the `workers/github-proxy` Cloudflare Worker rather than the GitHub API directly.
 * The Worker holds the real GitHub PAT server-side and forwards a limited set of operations
 * to a fixed repo, so no repo-write credential ever ships inside the APK. [proxySecret] is a
 * low-privilege, rotatable app identifier — not a GitHub credential — that only gates access
 * to this Worker's limited surface (issue/comment creation, feedback-asset upload).
 */
@Singleton
class GithubApi @Inject constructor(
    @GithubOkHttp private val client: OkHttpClient,
    @FeedbackJson private val json: Json,
) {
    private val proxyUrl: String get() = BuildConfig.GITHUB_PROXY_URL.trimEnd('/')
    private val proxySecret: String get() = BuildConfig.GITHUB_PROXY_SECRET

    val isConfigured: Boolean
        get() = proxyUrl.isNotBlank() && proxySecret.isNotBlank()

    val configError: String?
        get() = when {
            proxyUrl.isBlank() -> "GitHub proxy URL not configured.\nAdd github.proxy.url to local.properties or set GH_PROXY_URL secret."
            proxySecret.isBlank() -> "GitHub proxy secret not configured.\nAdd github.proxy.secret to local.properties or set GH_PROXY_SECRET secret."
            else -> null
        }

    private val jsonMediaType = "application/json".toMediaType()

    private fun Request.Builder.auth(): Request.Builder {
        header("X-App-Secret", proxySecret)
        return this
    }

    suspend fun createIssue(title: String, body: String): GithubIssue {
        val requestBody = json.encodeToString(
            CreateIssueRequest.serializer(),
            CreateIssueRequest(title = title, body = body),
        ).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$proxyUrl/issues")
            .auth()
            .post(requestBody)
            .build()
        return executeParse(request, GithubIssue.serializer())
    }

    suspend fun getIssue(number: Int): GithubIssue {
        val request = Request.Builder()
            .url("$proxyUrl/issues/$number")
            .auth()
            .get()
            .build()
        return executeParse(request, GithubIssue.serializer())
    }

    suspend fun getComments(issueNumber: Int): List<GithubComment> {
        val request = Request.Builder()
            .url("$proxyUrl/issues/$issueNumber/comments")
            .auth()
            .get()
            .build()
        return executeParseList(request, GithubComment.serializer())
    }

    suspend fun postComment(issueNumber: Int, body: String): GithubComment {
        val requestBody = json.encodeToString(
            PostCommentRequest.serializer(),
            PostCommentRequest(body = body),
        ).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$proxyUrl/issues/$issueNumber/comments")
            .auth()
            .post(requestBody)
            .build()
        return executeParse(request, GithubComment.serializer())
    }

    suspend fun uploadAsset(
        filename: String,
        base64Content: String,
        message: String = "Add feedback asset",
    ): UploadAssetResponse {
        val requestBody = json.encodeToString(
            UploadAssetRequest.serializer(),
            UploadAssetRequest(message = message, content = base64Content),
        ).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$proxyUrl/assets/$filename")
            .auth()
            .put(requestBody)
            .build()
        return executeParse(request, UploadAssetResponse.serializer())
    }

    fun generateAssetFilename(prefix: String = "issue"): String {
        val now = LocalDateTime.now()
        val ts = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val rand = Random.nextInt(0, 999999)
        return "$prefix-$ts-$rand.png"
    }

    private suspend inline fun <reified T> executeParse(
        request: Request,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: throw IOException("Empty response body")
        if (!response.isSuccessful) {
            throw IOException("GitHub API error ${response.code}: $bodyString")
        }
        return json.decodeFromString(serializer, bodyString)
    }

    private suspend inline fun <reified T> executeParseList(
        request: Request,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): List<T> {
        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: throw IOException("Empty response body")
        if (!response.isSuccessful) {
            throw IOException("GitHub API error ${response.code}: $bodyString")
        }
        return json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(serializer),
            bodyString,
        )
    }
}
