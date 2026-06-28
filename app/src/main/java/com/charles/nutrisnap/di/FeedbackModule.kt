package com.charles.nutrisnap.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

private val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feedback_bug_reports"
)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GithubOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeedbackJson

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeedbackDataStore

@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {

    @Provides
    @Singleton
    @FeedbackDataStore
    fun provideFeedbackDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.feedbackDataStore

    @Provides
    @Singleton
    @FeedbackJson
    fun provideFeedbackJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @GithubOkHttp
    fun provideGithubOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            // Redact any line that contains "Authorization" or "Bearer" or the token
            if (message.contains("Authorization", ignoreCase = true) ||
                message.contains("Bearer", ignoreCase = true)
            ) {
                // skip logging auth headers
                return@HttpLoggingInterceptor
            }
            android.util.Log.d("GitHubApi", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addNetworkInterceptor(logging)
            .build()
    }
}
