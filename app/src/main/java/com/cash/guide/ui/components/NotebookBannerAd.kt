package com.cash.guide.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cash.guide.domain.ads.AdMobManager
import com.cash.guide.domain.billing.BillingManager
import com.cash.guide.ui.notebook.JournalPaper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun NotebookBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.TEST_BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    val billingManager = remember(context) { BillingManager.getInstance(context) }
    val isPremium by billingManager.isPremium.collectAsState()

    if (isPremium) {
        return // Hide ads for premium users
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(JournalPaper)
            .padding(top = 4.dp, bottom = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx: Context ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
