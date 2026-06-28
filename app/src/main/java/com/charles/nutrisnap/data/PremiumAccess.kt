package com.charles.nutrisnap.data

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

const val PREMIUM_PRODUCT_ID = "nutrisnap_premium"
const val MONTHLY_BASE_PLAN_ID = "premium_monthly"
const val YEARLY_BASE_PLAN_ID = "premium_yearly"

data class PremiumEntitlement(
    val isPremium: Boolean = false,
    val productId: String? = null,
)

data class PremiumPlan(
    val productId: String,
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriod: String,
) {
    val label: String
        get() = when (basePlanId) {
            MONTHLY_BASE_PLAN_ID -> "Monthly"
            YEARLY_BASE_PLAN_ID -> "Yearly"
            else -> basePlanId.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
}

interface PremiumAccess {
    val entitlement: StateFlow<PremiumEntitlement>
    val plans: StateFlow<List<PremiumPlan>>
    val billingMessage: StateFlow<String?>

    fun refresh()
    fun restorePurchases()
    fun startPurchase(activity: Activity, plan: PremiumPlan)
}
