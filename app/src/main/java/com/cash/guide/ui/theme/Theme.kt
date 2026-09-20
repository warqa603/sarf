package com.cash.guide.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cash.guide.ui.notebook.CreamFrothFamily
import com.cash.guide.ui.notebook.Ink
import com.cash.guide.ui.notebook.InkTone
import com.cash.guide.ui.notebook.JournalFontManager
import com.cash.guide.ui.notebook.JournalTheme
import com.cash.guide.ui.notebook.LocalJournalTheme
import com.cash.guide.ui.notebook.MutedInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.Paper
import com.cash.guide.ui.notebook.PaperWarm
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.Rule

@Composable
fun HisabiTheme(content: @Composable () -> Unit) {
    val currentPalette = JournalTheme.currentPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !currentPalette.isDark
                insetsController.isAppearanceLightNavigationBars = !currentPalette.isDark
            }
        }
    }

    val hisabiColors = if (currentPalette.isDark) {
        darkColorScheme(
            primary = currentPalette.accent,
            onPrimary = Color.Black,
            background = currentPalette.paper,
            onBackground = currentPalette.ink,
            surface = currentPalette.cardBg,
            onSurface = currentPalette.ink,
            surfaceVariant = currentPalette.dockBg,
            onSurfaceVariant = currentPalette.mutedInk,
            outline = currentPalette.rule
        )
    } else {
        lightColorScheme(
            primary = currentPalette.accent,
            onPrimary = Color.White,
            background = currentPalette.paper,
            onBackground = currentPalette.ink,
            surface = currentPalette.cardBg,
            onSurface = currentPalette.ink,
            surfaceVariant = currentPalette.dockBg,
            onSurfaceVariant = currentPalette.mutedInk,
            outline = currentPalette.rule
        )
    }

    val currentArabicFont = JournalFontManager.currentArabicFont
    val currentLatinFont = JournalFontManager.currentLatinFont
    val globalFontScale = JournalFontManager.globalFontScale
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val defaultFont = if (isRtl) currentArabicFont.family else currentLatinFont.family
    val fontOpticalScale = if (isRtl) currentArabicFont.opticalScale else currentLatinFont.opticalScale

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity.density, currentDensity.fontScale, fontOpticalScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontOpticalScale
        )
    }

    val defaultTextStyle = TextStyle(
        fontFamily = defaultFont,
        fontWeight = FontWeight.Normal,
        platformStyle = NoFontPadding
    )
    val hisabiTypography = Typography(
        displayLarge = defaultTextStyle.copy(fontSize = 30.sp),
        displayMedium = defaultTextStyle.copy(fontSize = 24.sp),
        displaySmall = defaultTextStyle.copy(fontSize = 20.sp),
        headlineLarge = defaultTextStyle.copy(fontSize = 19.sp),
        headlineMedium = defaultTextStyle.copy(fontSize = 17.5.sp),
        headlineSmall = defaultTextStyle.copy(fontSize = 16.sp),
        titleLarge = defaultTextStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = defaultTextStyle.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Medium),
        titleSmall = defaultTextStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
        bodyLarge = defaultTextStyle.copy(fontSize = 15.sp),
        bodyMedium = defaultTextStyle.copy(fontSize = 14.sp),
        bodySmall = defaultTextStyle.copy(fontSize = 12.5.sp),
        labelLarge = defaultTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = defaultTextStyle.copy(fontSize = 12.5.sp),
        labelSmall = defaultTextStyle.copy(fontSize = 11.sp)
    )

    CompositionLocalProvider(
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = hisabiColors,
            typography = hisabiTypography
        ) {
            CompositionLocalProvider(
                LocalJournalTheme provides currentPalette,
                LocalTextStyle provides defaultTextStyle.copy(fontSize = 14.5.sp, color = currentPalette.ink)
            ) {
                content()
            }
        }
    }
}
