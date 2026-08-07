package com.charles.nutrisnap.data

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Meals successfully logged before we ever ask for a review. Early asks convert worse. */
private const val LOGS_BEFORE_FIRST_ASK = 3

/**
 * Wraps Google's official In-App Review API. Google's own quota silently caps how often the
 * dialog can actually appear (roughly once a year regardless of what we request), so this only
 * needs to avoid asking on someone's very first log and avoid re-requesting every single time
 * after that — Play handles the rest.
 */
@Singleton
open class ReviewPrompter @Inject constructor(
    private val dataStore: DataStore<Preferences>?,
) {
    private object Keys {
        val LOGGED_COUNT = intPreferencesKey("review_prompt_meal_count")
        val REQUESTED = booleanPreferencesKey("review_prompt_requested")
    }

    /**
     * Call this from a moment of genuine success (a meal was just logged), never from an error
     * or loading state. Safe to call every time — it no-ops until the threshold is hit and only
     * ever triggers the real Play dialog once per install.
     */
    open suspend fun maybeRequestReview(activity: Activity) {
        val shouldRequest = dataStore?.edit { prefs ->
            val alreadyRequested = prefs[Keys.REQUESTED] ?: false
            val count = (prefs[Keys.LOGGED_COUNT] ?: 0) + 1
            prefs[Keys.LOGGED_COUNT] = count
            if (!alreadyRequested && count >= LOGS_BEFORE_FIRST_ASK) {
                prefs[Keys.REQUESTED] = true
            }
        }?.let { prefs ->
            (prefs[Keys.REQUESTED] ?: false) && (prefs[Keys.LOGGED_COUNT] == LOGS_BEFORE_FIRST_ASK)
        } ?: false

        if (!shouldRequest) return

        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
        }
        // Deliberately swallow failures — this is a nice-to-have engagement flow, never worth
        // surfacing an error or interrupting the meal-logging flow it's attached to.
    }
}
