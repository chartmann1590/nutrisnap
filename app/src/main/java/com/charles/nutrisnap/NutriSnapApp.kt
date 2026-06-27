package com.charles.nutrisnap

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Provides Hilt's object graph and a Hilt-aware [HiltWorkerFactory]
 * so the model-download worker (Phase 2) can have dependencies injected.
 */
@HiltAndroidApp
class NutriSnapApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
