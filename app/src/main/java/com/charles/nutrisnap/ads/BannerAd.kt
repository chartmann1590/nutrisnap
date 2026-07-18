package com.charles.nutrisnap.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.charles.nutrisnap.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Anchored adaptive banner. Renders nothing if no banner unit ID is configured. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (BuildConfig.ADMOB_BANNER_ID.isBlank()) return

    val context = LocalContext.current
    val adView = remember {
        val view = AdView(context)
        view.setAdSize(AdSize.BANNER)
        view.adUnitId = BuildConfig.ADMOB_BANNER_ID
        view
    }

    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose { adView.destroy() }
    }

    AndroidView(modifier = modifier.fillMaxWidth(), factory = { adView })
}
