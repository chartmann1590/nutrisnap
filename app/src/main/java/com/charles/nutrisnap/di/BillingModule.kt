package com.charles.nutrisnap.di

import android.app.Activity
import com.charles.nutrisnap.data.BillingRepository
import com.charles.nutrisnap.data.PremiumAccess
import com.charles.nutrisnap.data.PremiumEntitlement
import com.charles.nutrisnap.data.PremiumPlan
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun providePremiumAccess(play: Provider<BillingRepository>): PremiumAccess =
        // Amazon Appstore build: no Google Play Billing on Fire devices, so the app is free + ads (never Premium).
        if (com.charles.nutrisnap.BuildConfig.STORE == "amazon") NoBillingPremiumAccess else play.get()
}

/** Amazon Appstore build: Play Billing is never started; Premium is never active; purchases do nothing. */
object NoBillingPremiumAccess : PremiumAccess {
    override val entitlement: StateFlow<PremiumEntitlement> = MutableStateFlow(PremiumEntitlement(isPremium = false))
    override val plans: StateFlow<List<PremiumPlan>> = MutableStateFlow(emptyList())
    override val billingMessage: StateFlow<String?> = MutableStateFlow(null)
    override fun refresh() {}
    override fun restorePurchases() {}
    override fun startPurchase(activity: Activity, plan: PremiumPlan) {}
}
