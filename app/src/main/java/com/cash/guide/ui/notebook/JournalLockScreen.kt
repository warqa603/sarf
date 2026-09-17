package com.cash.guide.ui.notebook

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import com.cash.guide.R
import com.cash.guide.data.SecurityRepository
import com.cash.guide.domain.BiometricHelper
import com.cash.guide.domain.findFragmentActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun JournalLockScreen(
    securityRepository: SecurityRepository,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBiometricSupported by remember { mutableStateOf(false) }
    var useBiometricsPref by remember { mutableStateOf(true) }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        coroutineScope.launch {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    (-14f) at 50
                    14f at 100
                    (-10f) at 150
                    10f at 200
                    (-6f) at 250
                    6f at 300
                    0f at 350
                }
            )
        }
    }

    var isPromptShowing by remember { mutableStateOf(false) }

    fun promptBiometric() {
        if (isPromptShowing) return
        val activity = context.findFragmentActivity()
        if (activity != null && isBiometricSupported && useBiometricsPref) {
            isPromptShowing = true
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = context.getString(R.string.app_name),
                subtitle = context.getString(R.string.lock_screen_subtitle_bio),
                negativeButtonText = "PIN",
                onSuccess = {
                    isPromptShowing = false
                    onUnlock()
                },
                onError = { errorCode, errString ->
                    isPromptShowing = false
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        errorMessage = errString
                        triggerShake()
                    }
                },
                onFailed = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        isBiometricSupported = BiometricHelper.isBiometricAvailable(context)
        useBiometricsPref = securityRepository.useBiometrics.first()

        if (isBiometricSupported && useBiometricsPref) {
            delay(250)
            promptBiometric()
        }
    }

    fun onDigit(digit: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorMessage = null
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}

            if (newPin.length == 4) {
                coroutineScope.launch {
                    val isValid = securityRepository.verifyPin(newPin)
                    if (isValid) {
                        onUnlock()
                    } else {
                        errorMessage = context.getString(R.string.lock_screen_pin_error)
                        triggerShake()
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    // Intercept back button so user cannot back out of lock screen
    BackHandler(enabled = true) {
        context.findFragmentActivity()?.finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JournalPaper)
            .drawBehind {
                val stepPx = 29.dp.toPx()
                var y = stepPx
                while (y < size.height) {
                    drawLine(
                        color = JournalRule.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.6.dp.toPx()
                    )
                    y += stepPx
                }
            }
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Handwritten lock sketch icon
            HisabiSketchIcon(
                symbol = HisabiSymbol.Lock,
                contentDescription = null,
                tint = JournalInk,
                size = 48.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pastel Badge "حسابات محمية / Dossier Privé"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighlighterPink.copy(alpha = 0.35f))
                    .border(1.dp, JournalInk.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.lock_screen_badge),
                    style = TextStyle(
                        fontFamily = JournalHandFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${stringResource(R.string.app_name)} - ${stringResource(R.string.app_tagline)}",
                style = TextStyle(
                    fontFamily = JournalHandFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isBiometricSupported && useBiometricsPref)
                    stringResource(R.string.lock_screen_subtitle_bio)
                else
                    stringResource(R.string.lock_screen_subtitle_pin),
                style = TextStyle(
                    fontFamily = JournalHandFamily,
                    fontSize = 15.sp,
                    color = JournalMutedInk,
                    textAlign = TextAlign.Center
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = TextStyle(
                        fontFamily = JournalHandFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalCreditRed,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) HighlighterPink.copy(alpha = 0.90f) else JournalPaper
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) JournalInk else JournalRule,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Notebook Keypad Grid (3x4)
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "⌫")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keypad.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    if (isBiometricSupported && useBiometricsPref) {
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .background(HighlighterYellow.copy(alpha = 0.25f))
                                                .border(1.dp, JournalRule.copy(alpha = 0.60f), CircleShape)
                                                .clickable(
                                                    role = Role.Button,
                                                    onClickLabel = "Empreinte",
                                                    onClick = { promptBiometric() }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HisabiSketchIcon(
                                                symbol = HisabiSymbol.Fingerprint,
                                                contentDescription = "Empreinte",
                                                tint = JournalInk,
                                                size = 28.dp
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(62.dp))
                                    }
                                }

                                "⌫" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(HighlighterYellow.copy(alpha = 0.25f))
                                            .border(1.dp, JournalRule.copy(alpha = 0.60f), CircleShape)
                                            .clickable(
                                                role = Role.Button,
                                                onClickLabel = "Effacer",
                                                onClick = { onBackspace() }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⌫",
                                            style = TextStyle(
                                                fontFamily = JournalHandFamily,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = JournalInk
                                            )
                                        )
                                    }
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(HighlighterYellow.copy(alpha = 0.20f))
                                            .border(1.dp, JournalRule.copy(alpha = 0.60f), CircleShape)
                                            .clickable(
                                                role = Role.Button,
                                                onClick = { onDigit(key) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = TextStyle(
                                                fontFamily = JournalHandFamily,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = JournalInk
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
