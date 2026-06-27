package com.charles.nutrisnap

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Provides Hilt's object graph and a Hilt-aware [HiltWorkerFactory]
 * so the model-download worker (Phase 2) can have dependencies injected.
 * Also initializes Firebase services (Crashlytics, Remote Config, FCM channel).
 */
@HiltAndroidApp
class NutriSnapApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // ---- Firebase Crashlytics ----
        // Crashlytics auto-initializes via content provider; send any reports captured
        // during the previous cold start without a crash.
        FirebaseCrashlytics.getInstance().sendUnsentReports()

        // ---- Firebase Remote Config ----
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(43200L) // 12 hours (Spark: 5 fetches/hr/device)
                .build()
        )
        remoteConfig.fetch(43200L).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                remoteConfig.activate()
            }
        }

        // ---- FCM default notification channel (Android 8+) ----
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nutrisnap_default",
                "NutriSnap Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General NutriSnap notifications"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
