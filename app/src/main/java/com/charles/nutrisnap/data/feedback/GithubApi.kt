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

@Singleton
class GithubApi @Inject constructor(
    @GithubOkHttp private val client: OkHttpClient,
    @FeedbackJson private val json: Json,
) {
    private val token: String get() = BuildConfig.GITHUB_API_TOKEN
    private val owner: String get() = BuildConfig.GITHUB_REPO_OWNER
    private val repo: String get() = BuildConfig.GITHUB_REPO_NAME

    val isConfigured: Boolean
        get() = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()

    val configError: String?
        get() = when {
            token.isBlank() -> "GitHub API token not configured.\nAdd github.api.token to local.properties or set GH_API_TOKEN secret."
            owner.isBlank() -> "GitHub repo owner not configured.\nAdd github.repo.owner to local.properties or set GH_REPO_OWNER secret."
            repo.isBlank() -> "GitHub repo name not configured.\nAdd github.repo.name to local.properties or set GH_REPO_NAME secret."
            else -> null
        }

    private val baseUrl = "https://api.github.com/"
    private val jsonMediaType = "application/json".toMediaType()

    private fun Request.Builder.auth(): Request.Builder {
        if (token.isNotBlank()) {
            header("Authorization", "Bearer $token")
        }
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("User-Agent", "NutriSnap-Android/0.1")
        return this
    }

    suspend fun createIssue(title: String, body: String): GithubIssue {
        val requestBody = json.encodeToString(
            CreateIssueRequest.serializer(),
            CreateIssueRequest(title = title, body = body),
        ).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${baseUrl}repos/$owner/$repo/issues")
            .auth()
            .post(requestBody)
            .build()
        return executeParse(request, GithubIssue.serializer())
    }

    suspend fun getIssue(number: Int): GithubIssue {
        val request = Request.Builder()
            .url("${baseUrl}repos/$owner/$repo/issues/$number")
            .auth()
            .get()
            .build()
        return executeParse(request, GithubIssue.serializer())
    }

    suspend fun getComments(issueNumber: Int): List<GithubComment> {
        val request = Request.Builder()
            .url("${baseUrl}repos/$owner/$repo/issues/$issueNumber/comments")
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
            .url("${baseUrl}repos/$owner/$repo/issues/$issueNumber/comments")
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
        val path = "${BuildConfig.FEEDBACK_ASSETS_DIR}/$filename"
        val requestBody = json.encodeToString(
            UploadAssetRequest.serializer(),
            UploadAssetRequest(message = message, content = base64Content),
        ).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${baseUrl}repos/$owner/$repo/contents/$path")
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
