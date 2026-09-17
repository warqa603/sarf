package com.cash.guide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.cash.guide.app.HssabiApp
import com.cash.guide.ui.theme.HisabiTheme
import android.app.Activity

class MainActivity : FragmentActivity() {
    companion object {
        var currentActivity: Activity? = null
    }

    private val deepLinkUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = this
        deepLinkUriState.value = intent?.data

        // Initialize Google Mobile Ads SDK (AdMob)
        com.cash.guide.domain.ads.AdMobManager.getInstance(this).initialize()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = false
        }
        setContent {
            HisabiTheme {
                HssabiApp(
                    deepLinkUri = deepLinkUriState.value,
                    onDeepLinkConsumed = { deepLinkUriState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUriState.value = intent.data
    }

    override fun onDestroy() {
        if (currentActivity == this) currentActivity = null
        super.onDestroy()
    }
}

