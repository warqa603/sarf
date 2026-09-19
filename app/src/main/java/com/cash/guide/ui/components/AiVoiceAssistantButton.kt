package com.cash.guide.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.cash.guide.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.cash.guide.domain.ads.AdMobManager
import com.cash.guide.domain.ads.AiCreditManager
import com.cash.guide.domain.ai.AiOutputScript
import com.cash.guide.domain.ai.AiOutputScriptManager
import com.cash.guide.domain.ai.AiVoiceOnboardingManager
import com.cash.guide.domain.ai.CalculationAiResult
import com.cash.guide.domain.ai.ChecklistAiResult
import com.cash.guide.domain.ai.ExistingCalculationRowContext
import com.cash.guide.domain.ai.GeminiDarijaService
import com.cash.guide.domain.speech.SpeechRecognizerHelper
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Wraps a bottom action/input row with an in-place AI voice button and a docked Manga speech bubble above.
 * Used in ChecklistScreen to keep the input row full-width without squishing.
 */
@Composable
fun AiVoiceRowContainer(
    target: AiVoiceInputTarget,
    existingRows: List<ExistingCalculationRowContext> = emptyList(),
    onChecklistResult: (ChecklistAiResult) -> Unit = {},
    onCalculationResult: (CalculationAiResult) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val activity = com.cash.guide.MainActivity.currentActivity ?: context.findActivity()
    val scope = rememberCoroutineScope()

    val creditManager = remember { AiCreditManager.getInstance(context) }
    val adMobManager = remember { AdMobManager.getInstance(context) }
    val onboardingManager = remember { AiVoiceOnboardingManager.getInstance(context) }
    val scriptManager = remember { AiOutputScriptManager.getInstance(context) }

    val credits by creditManager.credits.collectAsState()
    val hasSeenOnboarding by onboardingManager.hasSeenOnboarding.collectAsState()
    val currentScript by scriptManager.selectedScript.collectAsState()

    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0)
    val appLang = currentLocale?.language ?: "ar"

    LaunchedEffect(appLang) {
        scriptManager.syncWithAppLanguage(appLang)
    }

    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showRewardedAdDialog by remember { mutableStateOf(false) }

    var buttonState by remember { mutableStateOf(AiVoiceButtonState.IDLE) }
    var recordedTranscript by remember { mutableStateOf("") }
    var pendingChecklistResult by remember { mutableStateOf<ChecklistAiResult?>(null) }
    var pendingCalculationResult by remember { mutableStateOf<CalculationAiResult?>(null) }

    val speechHelper = remember {
        SpeechRecognizerHelper(context).apply {
            preferredScript = currentScript
            onSpeechResult = { text ->
                if (text.isNotBlank()) {
                    recordedTranscript = text
                    buttonState = AiVoiceButtonState.ANALYZING
                    scope.launch {
                        try {
                            if (target == AiVoiceInputTarget.CHECKLIST) {
                                val result = GeminiDarijaService.parseChecklistFromDarija(text, currentScript)
                                if (result != null && result.items.isNotEmpty()) {
                                    pendingChecklistResult = result
                                } else {
                                    Toast.makeText(context, "تعذر استخراج العناصر، عاود جرب وتحدث بوضوح", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val result = GeminiDarijaService.parseCalculationFromDarija(text, currentScript, existingRows)
                                if (result != null && result.entries.isNotEmpty()) {
                                    pendingCalculationResult = result
                                } else {
                                    Toast.makeText(context, "تعذر استخراج الحسابات، عاود جرب وتحدث بوضوح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "حدث خطأ في الاتصال بالذكاء الاصطناعي", Toast.LENGTH_SHORT).show()
                        } finally {
                            buttonState = AiVoiceButtonState.IDLE
                        }
                    }
                } else {
                    buttonState = AiVoiceButtonState.IDLE
                }
            }
        }
    }

    DisposableEffect(speechHelper) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val haptic = LocalHapticFeedback.current
    var secondsRemaining by remember { mutableIntStateOf(45) }

    LaunchedEffect(buttonState) {
        if (buttonState == AiVoiceButtonState.RECORDING) {
            secondsRemaining = 45
            while (secondsRemaining > 0 && buttonState == AiVoiceButtonState.RECORDING) {
                delay(1000L)
                if (buttonState == AiVoiceButtonState.RECORDING) {
                    secondsRemaining--
                }
            }
            if (buttonState == AiVoiceButtonState.RECORDING) {
                val currentText = speechHelper.getBestTranscript()
                if (currentText.isBlank()) {
                    buttonState = AiVoiceButtonState.IDLE
                    speechHelper.stopListening()
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    buttonState = AiVoiceButtonState.ANALYZING
                    speechHelper.stopAndDeliver()
                }
            }
        } else {
            secondsRemaining = 45
        }
    }

    LaunchedEffect(currentScript) {
        speechHelper.updateScript(currentScript)
    }

    val partialText by speechHelper.partialText.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted && (AiCreditManager.IS_TEST_UNLIMITED || credits > 0)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            buttonState = AiVoiceButtonState.RECORDING
            speechHelper.startListening(currentScript)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
        }
    }

    fun handleMicClick() {
        when (buttonState) {
            AiVoiceButtonState.IDLE -> {
                if (!isNetworkAvailable(context)) {
                    val noNetMsg = when {
                        appLang == "fr" -> "Connexion Internet requise pour l'assistant vocal 📡"
                        appLang == "en" -> "Internet connection required for Voice Assistant 📡"
                        else -> "خاصك اتصال بالإنترنت باش يخدم المساعد الذكي 📡"
                    }
                    Toast.makeText(context, noNetMsg, Toast.LENGTH_SHORT).show()
                    return
                }
                if (!hasSeenOnboarding) {
                    showOnboardingDialog = true
                    return
                }
                if (!AiCreditManager.IS_TEST_UNLIMITED && credits <= 0) {
                    showRewardedAdDialog = true
                    return
                }
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                buttonState = AiVoiceButtonState.RECORDING
                speechHelper.startListening(currentScript)
            }

            AiVoiceButtonState.RECORDING -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val currentText = speechHelper.getBestTranscript()
                if (currentText.isBlank()) {
                    buttonState = AiVoiceButtonState.IDLE
                    speechHelper.stopListening()
                } else {
                    buttonState = AiVoiceButtonState.ANALYZING
                    speechHelper.stopAndDeliver()
                }
            }

            AiVoiceButtonState.ANALYZING -> {}
        }
    }

    Column(
        modifier = modifier
    ) {
        // 1. Manga Speech Bubble docked above the row
        AnimatedVisibility(
            visible = buttonState == AiVoiceButtonState.RECORDING || buttonState == AiVoiceButtonState.ANALYZING,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            MangaVoiceSpeechBubble(
                liveTranscript = partialText,
                isAnalyzing = buttonState == AiVoiceButtonState.ANALYZING,
                currentScript = currentScript,
                secondsRemaining = secondsRemaining,
                onScriptSelected = {
                    scriptManager.setScript(it)
                    speechHelper.updateScript(it)
                },
                onCancel = {
                    buttonState = AiVoiceButtonState.IDLE
                    speechHelper.stopListening()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // 2. Action row containing caller's content + in-place AiVoiceButton
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()

            Spacer(modifier = Modifier.width(8.dp))

            AiVoiceButton(
                state = buttonState,
                onClick = { handleMicClick() }
            )
        }
    }

    // Dialogs
    RenderAiDialogs(
        activity = activity,
        showOnboardingDialog = showOnboardingDialog,
        onDismissOnboarding = {
            onboardingManager.markSeen()
            showOnboardingDialog = false
            if (AiCreditManager.IS_TEST_UNLIMITED || credits > 0) {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    buttonState = AiVoiceButtonState.RECORDING
                    speechHelper.startListening(currentScript)
                }
            } else {
                showRewardedAdDialog = true
            }
        },
        showRewardedAdDialog = showRewardedAdDialog,
        onDismissRewardedAd = { showRewardedAdDialog = false },
        onRewardSuccess = {
            creditManager.addRewardCredits(3)
            showRewardedAdDialog = false
            buttonState = AiVoiceButtonState.RECORDING
            speechHelper.startListening(currentScript)
        },
        pendingChecklistResult = pendingChecklistResult,
        onDismissChecklistReview = { pendingChecklistResult = null },
        onConfirmChecklist = { confirmedItems ->
            creditManager.consumeCredit()
            pendingChecklistResult?.let { onChecklistResult(it.copy(items = confirmedItems)) }
            pendingChecklistResult = null
        },
        pendingCalculationResult = pendingCalculationResult,
        onDismissCalculationReview = { pendingCalculationResult = null },
        onConfirmCalculation = { confirmedEntries ->
            creditManager.consumeCredit()
            pendingCalculationResult?.let { onCalculationResult(it.copy(entries = confirmedEntries)) }
            pendingCalculationResult = null
        },
        recordedTranscript = recordedTranscript
    )
}

/**
 * Pinned Bottom-Right AI voice assistant button with Manga speech bubble docked above it.
 * Designed specifically for CalculationEditorScreen to sit permanently at the bottom right.
 */
@Composable
fun AiVoiceDockedBottomButton(
    target: AiVoiceInputTarget = AiVoiceInputTarget.CALCULATION,
    existingRows: List<ExistingCalculationRowContext> = emptyList(),
    onChecklistResult: (ChecklistAiResult) -> Unit = {},
    onCalculationResult: (CalculationAiResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = com.cash.guide.MainActivity.currentActivity ?: context.findActivity()
    val scope = rememberCoroutineScope()

    val creditManager = remember { AiCreditManager.getInstance(context) }
    val adMobManager = remember { AdMobManager.getInstance(context) }
    val onboardingManager = remember { AiVoiceOnboardingManager.getInstance(context) }
    val scriptManager = remember { AiOutputScriptManager.getInstance(context) }

    val credits by creditManager.credits.collectAsState()
    val hasSeenOnboarding by onboardingManager.hasSeenOnboarding.collectAsState()
    val currentScript by scriptManager.selectedScript.collectAsState()

    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0)
    val appLang = currentLocale?.language ?: "ar"

    LaunchedEffect(appLang) {
        scriptManager.syncWithAppLanguage(appLang)
    }

    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showRewardedAdDialog by remember { mutableStateOf(false) }

    var buttonState by remember { mutableStateOf(AiVoiceButtonState.IDLE) }
    var recordedTranscript by remember { mutableStateOf("") }
    var pendingChecklistResult by remember { mutableStateOf<ChecklistAiResult?>(null) }
    var pendingCalculationResult by remember { mutableStateOf<CalculationAiResult?>(null) }

    val speechHelper = remember {
        SpeechRecognizerHelper(context).apply {
            preferredScript = currentScript
            onSpeechResult = { text ->
                if (text.isNotBlank()) {
                    recordedTranscript = text
                    buttonState = AiVoiceButtonState.ANALYZING
                    scope.launch {
                        try {
                            if (target == AiVoiceInputTarget.CHECKLIST) {
                                val result = GeminiDarijaService.parseChecklistFromDarija(text, currentScript)
                                if (result != null && result.items.isNotEmpty()) {
                                    pendingChecklistResult = result
                                } else {
                                    Toast.makeText(context, "تعذر استخراج العناصر، عاود جرب وتحدث بوضوح", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val result = GeminiDarijaService.parseCalculationFromDarija(text, currentScript, existingRows)
                                if (result != null && result.entries.isNotEmpty()) {
                                    pendingCalculationResult = result
                                } else {
                                    Toast.makeText(context, "تعذر استخراج الحسابات، عاود جرب وتحدث بوضوح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "حدث خطأ في الاتصال بالذكاء الاصطناعي", Toast.LENGTH_SHORT).show()
                        } finally {
                            buttonState = AiVoiceButtonState.IDLE
                        }
                    }
                } else {
                    buttonState = AiVoiceButtonState.IDLE
                }
            }
        }
    }

    DisposableEffect(speechHelper) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val haptic = LocalHapticFeedback.current
    var secondsRemaining by remember { mutableIntStateOf(45) }

    LaunchedEffect(buttonState) {
        if (buttonState == AiVoiceButtonState.RECORDING) {
            secondsRemaining = 45
            while (secondsRemaining > 0 && buttonState == AiVoiceButtonState.RECORDING) {
                delay(1000L)
                if (buttonState == AiVoiceButtonState.RECORDING) {
                    secondsRemaining--
                }
            }
            if (buttonState == AiVoiceButtonState.RECORDING) {
                val currentText = speechHelper.getBestTranscript()
                if (currentText.isBlank()) {
                    buttonState = AiVoiceButtonState.IDLE
                    speechHelper.stopListening()
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    buttonState = AiVoiceButtonState.ANALYZING
                    speechHelper.stopAndDeliver()
                }
            }
        } else {
            secondsRemaining = 45
        }
    }

    LaunchedEffect(currentScript) {
        speechHelper.updateScript(currentScript)
    }

    val partialText by speechHelper.partialText.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted && (AiCreditManager.IS_TEST_UNLIMITED || credits > 0)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            buttonState = AiVoiceButtonState.RECORDING
            speechHelper.startListening(currentScript)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
        }
    }

    fun handleMicClick() {
        when (buttonState) {
            AiVoiceButtonState.IDLE -> {
                if (!isNetworkAvailable(context)) {
                    val noNetMsg = when {
                        appLang == "fr" -> "Connexion Internet requise pour l'assistant vocal 📡"
                        appLang == "en" -> "Internet connection required for Voice Assistant 📡"
                        else -> "خاصك اتصال بالإنترنت باش يخدم المساعد الذكي 📡"
                    }
                    Toast.makeText(context, noNetMsg, Toast.LENGTH_SHORT).show()
                    return
                }
                if (!hasSeenOnboarding) {
                    showOnboardingDialog = true
                    return
                }
                if (!AiCreditManager.IS_TEST_UNLIMITED && credits <= 0) {
                    showRewardedAdDialog = true
                    return
                }
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                buttonState = AiVoiceButtonState.RECORDING
                speechHelper.startListening(currentScript)
            }

            AiVoiceButtonState.RECORDING -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val currentText = speechHelper.getBestTranscript()
                if (currentText.isBlank()) {
                    buttonState = AiVoiceButtonState.IDLE
                    speechHelper.stopListening()
                } else {
                    buttonState = AiVoiceButtonState.ANALYZING
                    speechHelper.stopAndDeliver()
                }
            }

            AiVoiceButtonState.ANALYZING -> {}
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Manga Speech Bubble docked above the button
        AnimatedVisibility(
            visible = buttonState == AiVoiceButtonState.RECORDING || buttonState == AiVoiceButtonState.ANALYZING,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            MangaVoiceSpeechBubble(
                liveTranscript = partialText,
                isAnalyzing = buttonState == AiVoiceButtonState.ANALYZING,
                currentScript = currentScript,
                secondsRemaining = secondsRemaining,
                onScriptSelected = {
                    scriptManager.setScript(it)
                    speechHelper.updateScript(it)
                },
                forceRightArrow = true, // Points directly down at bottom-right mic button
                onCancel = {
                    buttonState = AiVoiceButtonState.IDLE
                    speechHelper.stopListening()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // 2. The Button sitting at the physical bottom right
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                AiVoiceButton(
                    state = buttonState,
                    onClick = { handleMicClick() }
                )
            }
        }
    }

    // Dialogs
    RenderAiDialogs(
        activity = activity,
        showOnboardingDialog = showOnboardingDialog,
        onDismissOnboarding = {
            onboardingManager.markSeen()
            showOnboardingDialog = false
            if (AiCreditManager.IS_TEST_UNLIMITED || credits > 0) {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    buttonState = AiVoiceButtonState.RECORDING
                    speechHelper.startListening(currentScript)
                }
            } else {
                showRewardedAdDialog = true
            }
        },
        showRewardedAdDialog = showRewardedAdDialog,
        onDismissRewardedAd = { showRewardedAdDialog = false },
        onRewardSuccess = {
            creditManager.addRewardCredits(3)
            showRewardedAdDialog = false
            buttonState = AiVoiceButtonState.RECORDING
            speechHelper.startListening(currentScript)
        },
        pendingChecklistResult = pendingChecklistResult,
        onDismissChecklistReview = { pendingChecklistResult = null },
        onConfirmChecklist = { confirmedItems ->
            creditManager.consumeCredit()
            pendingChecklistResult?.let { onChecklistResult(it.copy(items = confirmedItems)) }
            pendingChecklistResult = null
        },
        pendingCalculationResult = pendingCalculationResult,
        onDismissCalculationReview = { pendingCalculationResult = null },
        onConfirmCalculation = { confirmedEntries ->
            creditManager.consumeCredit()
            pendingCalculationResult?.let { onCalculationResult(it.copy(entries = confirmedEntries)) }
            pendingCalculationResult = null
        },
        recordedTranscript = recordedTranscript
    )
}

@Composable
private fun RenderAiDialogs(
    activity: Activity?,
    showOnboardingDialog: Boolean,
    onDismissOnboarding: () -> Unit,
    showRewardedAdDialog: Boolean,
    onDismissRewardedAd: () -> Unit,
    onRewardSuccess: () -> Unit,
    pendingChecklistResult: ChecklistAiResult?,
    onDismissChecklistReview: () -> Unit,
    onConfirmChecklist: (List<String>) -> Unit,
    pendingCalculationResult: CalculationAiResult?,
    onDismissCalculationReview: () -> Unit,
    onConfirmCalculation: (List<com.cash.guide.domain.ai.CalculationAiEntry>) -> Unit,
    recordedTranscript: String
) {
    val context = LocalContext.current
    val adMobManager = remember { AdMobManager.getInstance(context) }

    // 1. Educational Onboarding Dialog
    if (showOnboardingDialog) {
        AiVoiceOnboardingDialog(onDismiss = onDismissOnboarding)
    }

    // 2. Rewarded Ad Dialog (when not unlimited and credits == 0)
    if (showRewardedAdDialog && !AiCreditManager.IS_TEST_UNLIMITED) {
        Dialog(onDismissRequest = onDismissRewardedAd) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = JournalPaper),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismissRewardedAd, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = JournalMutedInk)
                        }
                    }

                    Text(
                        text = stringResource(R.string.ai_voice_limit_reached_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFF57F17),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.ai_voice_limit_reached_desc),
                        fontSize = 13.5.sp,
                        color = JournalInk,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        onClick = {
                            if (activity != null) {
                                if (!adMobManager.isRewardedReady.value) {
                                    Toast.makeText(context, context.getString(R.string.ai_voice_ad_not_ready), Toast.LENGTH_SHORT).show()
                                    adMobManager.loadRewardedAd()
                                } else {
                                    adMobManager.showRewardedAd(
                                        activity = activity,
                                        onRewardEarned = onRewardSuccess
                                    )
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.ai_voice_system_error), Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1B7A4B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.ai_voice_watch_ad_btn),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. Review Dialog for Checklist
    val checklistRes = pendingChecklistResult
    if (checklistRes != null) {
        AiChecklistReviewDialog(
            originalSpeech = recordedTranscript,
            initialItems = checklistRes.items,
            onDismiss = onDismissChecklistReview,
            onConfirm = onConfirmChecklist
        )
    }

    // 4. Review Dialog for Calculations
    val calcRes = pendingCalculationResult
    if (calcRes != null) {
        AiCalculationReviewDialog(
            originalSpeech = recordedTranscript,
            initialEntries = calcRes.entries,
            onDismiss = onDismissCalculationReview,
            onConfirm = onConfirmCalculation
        )
    }
}

/**
 * Standalone button wrapper for backwards compatibility if needed.
 */
@Composable
fun AiVoiceAssistantButton(
    target: AiVoiceInputTarget,
    existingRows: List<ExistingCalculationRowContext> = emptyList(),
    onChecklistResult: (ChecklistAiResult) -> Unit = {},
    onCalculationResult: (CalculationAiResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AiVoiceDockedBottomButton(
        target = target,
        existingRows = existingRows,
        onChecklistResult = onChecklistResult,
        onCalculationResult = onCalculationResult,
        modifier = modifier
    )
}

internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
