package com.charles.nutrisnap.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Google's User Messaging Platform (UMP) SDK so ad personalization consent (GDPR/UK ATT
 * equivalent) is gathered before any ad request is made — required by AdMob policy, not optional.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    private var mobileAdsInitialized = false

    /**
     * Requests current consent status, shows the UMP consent form if the user's region
     * requires it, then initializes the Mobile Ads SDK. [onReady] fires once ad requests are
     * safe to make — never before, and never if consent is still outstanding.
     */
    fun gatherConsentAndInitialize(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // A non-null formError here means the form failed to show; canRequestAds()
                    // below is still the source of truth for whether ads may load.
                    initializeIfReady(onReady)
                }
            },
            { initializeIfReady(onReady) },
        )
    }

    private fun initializeIfReady(onReady: () -> Unit) {
        if (!consentInformation.canRequestAds()) return
        if (!mobileAdsInitialized) {
            mobileAdsInitialized = true
            MobileAds.initialize(context) {}
        }
        onReady()
    }

    /** Lets a Settings screen offer a persistent "manage ad consent" entry point, per UMP policy. */
    fun showPrivacyOptionsForm(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }
}
