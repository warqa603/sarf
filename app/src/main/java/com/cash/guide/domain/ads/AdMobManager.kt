package com.cash.guide.domain.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdMobManager private constructor(private val appContext: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = false

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialAdLoading = false
    
    // Action counter to avoid spamming the user with interstitial ads
    private var majorActionCounter = 0
    private var viewActionCounter = 0
    private var isRecentlyModified = false

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    fun initialize() {
        try {
            MobileAds.initialize(appContext) {
                Log.d(TAG, "Google Mobile Ads SDK initialized successfully")
                loadRewardedAd()
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadRewardedAd() {
        if (rewardedAd != null || isRewardedAdLoading) return

        isRewardedAdLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            appContext,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    _isRewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message} (code: ${error.code})")
                    rewardedAd = null
                    isRewardedAdLoading = false
                    _isRewardedReady.value = false
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "Rewarded ad not ready yet, attempting to reload")
            loadRewardedAd()
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd = null
                _isRewardedReady.value = false
                loadRewardedAd() // Preload the next one immediately
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Failed to show rewarded ad: ${error.message}")
                rewardedAd = null
                _isRewardedReady.value = false
                loadRewardedAd()
                onAdDismissed()
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewardEarned()
        }
    }

    fun loadInterstitialAd() {
        if (interstitialAd != null || isInterstitialAdLoading) return

        isInterstitialAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isInterstitialAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message} (code: ${error.code})")
                    interstitialAd = null
                    isInterstitialAdLoading = false
                }
            }
        )
    }

    fun reportModification() {
        isRecentlyModified = true
    }

    /**
     * Call this when the user navigates back.
     * Uses the isRecentlyModified flag to determine if it was a modification or just a view.
     */
    fun showInterstitialIfThresholdMet(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (isRecentlyModified) {
            majorActionCounter++
            isRecentlyModified = false
            if (majorActionCounter >= 3) {
                if (interstitialAd != null) {
                    showInterstitialAd(activity, onAdDismissed)
                    majorActionCounter = 0
                    viewActionCounter = 0
                } else {
                    loadInterstitialAd()
                    onAdDismissed()
                }
                return
            }
        } else {
            viewActionCounter++
            if (viewActionCounter >= 5) {
                if (interstitialAd != null) {
                    showInterstitialAd(activity, onAdDismissed)
                    viewActionCounter = 0
                    majorActionCounter = 0
                } else {
                    loadInterstitialAd()
                    onAdDismissed()
                }
                return
            }
        }
        
        onAdDismissed()
    }

    private fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "Interstitial ad not ready, proceeding immediately")
            loadInterstitialAd()
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                loadInterstitialAd() // Preload the next one immediately
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Failed to show interstitial ad: ${error.message}")
                interstitialAd = null
                loadInterstitialAd()
                onAdDismissed()
            }
        }

        ad.show(activity)
    }

    companion object {
        private const val TAG = "AdMobManager"

        // Official Google AdMob Test Unit IDs (100% safe for development/testing)
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        @Volatile
        private var instance: AdMobManager? = null

        fun getInstance(context: Context): AdMobManager {
            return instance ?: synchronized(this) {
                instance ?: AdMobManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
