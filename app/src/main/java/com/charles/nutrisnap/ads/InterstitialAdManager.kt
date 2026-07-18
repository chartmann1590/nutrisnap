package com.charles.nutrisnap.ads

import android.app.Activity
import android.content.Context
import com.charles.nutrisnap.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preloads a single interstitial at a time and shows it opportunistically. Callers never block
 * waiting for an ad — [showIfReady] just no-ops (and kicks off a fresh preload) if none is ready.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var actionsSinceLastShow = 0

    fun preload() {
        if (ad != null || loading || BuildConfig.ADMOB_INTERSTITIAL_ID.isBlank()) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loading = false
                    ad = interstitialAd
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    ad = null
                }
            },
        )
    }

    fun showIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        val current = ad
        if (current == null) {
            preload()
            onDismissed()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                preload()
                onDismissed()
            }
        }
        current.show(activity)
    }

    /**
     * Shows an interstitial at most once every [SHOW_EVERY_N_ACTIONS] calls, so users aren't
     * interrupted on every single meal log — just an occasional one. [onDismissed] always
     * fires, whether or not an ad was actually shown, so callers can chain navigation off it
     * unconditionally.
     */
    fun maybeShow(activity: Activity, onDismissed: () -> Unit = {}) {
        actionsSinceLastShow++
        if (actionsSinceLastShow < SHOW_EVERY_N_ACTIONS) {
            onDismissed()
            return
        }
        actionsSinceLastShow = 0
        showIfReady(activity, onDismissed)
    }

    private companion object {
        const val SHOW_EVERY_N_ACTIONS = 3
    }
}
