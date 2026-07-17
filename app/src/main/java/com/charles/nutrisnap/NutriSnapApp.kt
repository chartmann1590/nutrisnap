package com.charles.nutrisnap

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Provides Hilt's object graph and a Hilt-aware [HiltWorkerFactory]
 * so the model-download worker (Phase 2) can have dependencies injected.
 */
@HiltAndroidApp
class NutriSnapApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Firebase resets collection-enabled state to its manifest/SDK default on every cold
        // start; re-apply the user's saved opt-out choice here so it actually sticks across
        // app restarts rather than only taking effect until the process is killed.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val prefs = userPreferencesRepository.prefs.first()
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(prefs.crashlyticsEnabled)
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(prefs.performanceEnabled)
            FirebaseAnalytics.getInstance(this@NutriSnapApp).setAnalyticsCollectionEnabled(prefs.analyticsEnabled)
        }
    }
}
