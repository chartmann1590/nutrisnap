package com.charles.nutrisnap.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PremiumAccess, PurchasesUpdatedListener {

    private val _entitlement = MutableStateFlow(PremiumEntitlement())
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement

    private val _plans = MutableStateFlow<List<PremiumPlan>>(emptyList())
    override val plans: StateFlow<List<PremiumPlan>> = _plans

    private val _billingMessage = MutableStateFlow<String?>(null)
    override val billingMessage: StateFlow<String?> = _billingMessage

    private val productDetails = mutableMapOf<String, ProductDetails>()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        connect()
    }

    override fun refresh() {
        if (billingClient.isReady) {
            queryProducts()
            queryPurchases()
        } else {
            connect()
        }
    }

    override fun restorePurchases() {
        refresh()
        _billingMessage.value = "Checking Google Play purchases..."
    }

    override fun startPurchase(activity: Activity, plan: PremiumPlan) {
        val details = productDetails[plan.productId] ?: run {
            _billingMessage.value = "Premium is not available yet. Try again in a moment."
            refresh()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(plan.offerToken)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingMessage.value = result.debugMessage.ifBlank { "Google Play purchase could not start." }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _billingMessage.value = result.debugMessage.ifBlank { "Google Play purchase failed." }
        }
    }

    private fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                } else {
                    _billingMessage.value = result.debugMessage.ifBlank { "Google Play billing is unavailable." }
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingMessage.value = "Google Play billing disconnected."
            }
        })
    }

    private fun queryProducts() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _billingMessage.value = result.debugMessage.ifBlank { "Premium products could not be loaded." }
                return@queryProductDetailsAsync
            }
            val products = queryResult.productDetailsList
            productDetails.clear()
            products.forEach { productDetails[it.productId] = it }
            _plans.value = products.flatMap { it.toPremiumPlans() }
                .sortedWith(compareBy<PremiumPlan> { it.basePlanId != YEARLY_BASE_PLAN_ID }.thenBy { it.basePlanId })
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            } else {
                _billingMessage.value = result.debugMessage.ifBlank { "Purchases could not be restored." }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val active = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(PREMIUM_PRODUCT_ID)
        }
        _entitlement.value = PremiumEntitlement(
            isPremium = active != null,
            productId = active?.products?.firstOrNull(),
        )
        active?.let { acknowledgeIfNeeded(it) }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _billingMessage.value = result.debugMessage.ifBlank { "Purchase could not be acknowledged." }
            }
        }
    }
}

private fun ProductDetails.toPremiumPlans(): List<PremiumPlan> =
    subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
        val phase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return@mapNotNull null
        PremiumPlan(
            productId = productId,
            basePlanId = offer.basePlanId,
            offerToken = offer.offerToken,
            formattedPrice = phase.formattedPrice,
            billingPeriod = phase.billingPeriod,
        )
    }
