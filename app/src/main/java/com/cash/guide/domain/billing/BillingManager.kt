package com.cash.guide.domain.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager private constructor(private val context: Context) : PurchasesUpdatedListener {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private var isGooglePlayPremium = false
    private var isVipCodeActive = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    companion object {
        const val PREMIUM_YEARLY_ID = "warqa_premium_yearly"
        const val PREMIUM_LIFETIME_ID = "warqa_premium_lifetime"
        
        @Volatile
        private var INSTANCE: BillingManager? = null
        
        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        startConnection()
        observeVipCodeStatus()
    }

    private fun observeVipCodeStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            com.cash.guide.data.SettingsRepository(context).isVipUnlocked.collect { unlocked ->
                isVipCodeActive = unlocked
                updatePremiumState()
            }
        }
    }

    fun setVipUnlocked(unlocked: Boolean) {
        isVipCodeActive = unlocked
        updatePremiumState()
    }

    private fun updatePremiumState() {
        _isPremium.value = isGooglePlayPremium || isVipCodeActive
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart connection later
            }
        })
    }

    private fun queryProductDetails() {
        // Query INAPP (Lifetime)
        val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME_ID)
                .setProductType(BillingClient.ProductType.INAPP).build())
        ).build()

        billingClient.queryProductDetailsAsync(inAppParams) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                productDetailsList.forEach { productDetailsMap[it.productId] = it }
            }
        }

        // Query SUBS (Yearly)
        val subsParams = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY_ID)
                .setProductType(BillingClient.ProductType.SUBS).build())
        ).build()

        billingClient.queryProductDetailsAsync(subsParams) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                productDetailsList.forEach { productDetailsMap[it.productId] = it }
            }
        }
    }

    private fun queryPurchases() {
        var premiumFound = false

        // Check INAPP
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchasesList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    if (purchase.products.contains(PREMIUM_LIFETIME_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        premiumFound = true
                        if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                    }
                }
                if (premiumFound) {
                    isGooglePlayPremium = true
                    updatePremiumState()
                }
            }
        }

        // Check SUBS
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchasesList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    if (purchase.products.contains(PREMIUM_YEARLY_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        premiumFound = true
                        if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                    }
                }
                if (premiumFound) {
                    isGooglePlayPremium = true
                    updatePremiumState()
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val details = productDetailsMap[productId]
        if (details != null) {
            val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
            
            // If it's a subscription, we must set the offer token (using the first available base plan offer)
            if (details.productType == BillingClient.ProductType.SUBS) {
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken != null) {
                    productDetailsParamsBuilder.setOfferToken(offerToken)
                }
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        } else {
            // Product not ready, attempt reconnection
            startConnection()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PREMIUM_LIFETIME_ID) || purchase.products.contains(PREMIUM_YEARLY_ID)) {
                isGooglePlayPremium = true
                updatePremiumState()
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        CoroutineScope(Dispatchers.IO).launch {
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isGooglePlayPremium = true
                    updatePremiumState()
                }
            }
        }
    }
}
