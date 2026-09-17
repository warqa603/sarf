package com.cash.guide.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.cash.guide.domain.ads.AdMobManager
import com.cash.guide.domain.ads.AiCreditManager
import com.cash.guide.domain.ai.AiVoiceOnboardingManager
import com.cash.guide.domain.ai.CalculationAiResult
import com.cash.guide.domain.ai.ChecklistAiResult
import com.cash.guide.domain.ai.GeminiDarijaService
import com.cash.guide.domain.speech.SpeechRecognizerHelper
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import kotlinx.coroutines.launch

enum class AiVoiceInputTarget {
    CHECKLIST,
    CALCULATION
}

private enum class VoiceFlowStep {
    ONBOARDING,
    REWARDED_AD,
    LISTENING,
    ANALYZING,
    REVIEW_CHECKLIST,
    REVIEW_CALCULATION,
    ERROR
}

@Composable
fun AiVoiceInputDialog(
    target: AiVoiceInputTarget,
    onDismiss: () -> Unit,
    onChecklistResult: (ChecklistAiResult) -> Unit = {},
    onCalculationResult: (CalculationAiResult) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val creditManager = remember { AiCreditManager.getInstance(context) }
    val adMobManager = remember { AdMobManager.getInstance(context) }
    val onboardingManager = remember { AiVoiceOnboardingManager.getInstance(context) }

    val credits by creditManager.credits.collectAsState()
    val hasSeenOnboarding by onboardingManager.hasSeenOnboarding.collectAsState()

    var flowStep by remember {
        mutableStateOf(
            when {
                !hasSeenOnboarding -> VoiceFlowStep.ONBOARDING
                credits <= 0 -> VoiceFlowStep.REWARDED_AD
                else -> VoiceFlowStep.LISTENING
            }
        )
    }

    var recordedTranscript by remember { mutableStateOf("") }
    var extractedChecklistResult by remember { mutableStateOf<ChecklistAiResult?>(null) }
    var extractedCalculationResult by remember { mutableStateOf<CalculationAiResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val speechHelper = remember {
        SpeechRecognizerHelper(context).apply {
            onSpeechResult = { text ->
                if (text.isNotBlank() && flowStep == VoiceFlowStep.LISTENING) {
                    recordedTranscript = text
                    flowStep = VoiceFlowStep.ANALYZING
                    scope.launch {
                        try {
                            if (target == AiVoiceInputTarget.CHECKLIST) {
                                val result = GeminiDarijaService.parseChecklistFromDarija(text)
                                if (result != null && result.items.isNotEmpty()) {
                                    creditManager.consumeCredit()
                                    extractedChecklistResult = result
                                    flowStep = VoiceFlowStep.REVIEW_CHECKLIST
                                } else {
                                    errorMessage = "تعذر استخراج العناصر من الأوديو، عاود جرب وتحدث بوضوح"
                                    flowStep = VoiceFlowStep.ERROR
                                }
                            } else {
                                val result = GeminiDarijaService.parseCalculationFromDarija(text)
                                if (result != null && result.entries.isNotEmpty()) {
                                    creditManager.consumeCredit()
                                    extractedCalculationResult = result
                                    flowStep = VoiceFlowStep.REVIEW_CALCULATION
                                } else {
                                    errorMessage = "تعذر استخراج الحسابات من الأوديو، عاود جرب وتحدث بوضوح"
                                    flowStep = VoiceFlowStep.ERROR
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "حدث خطأ في الاتصال بالذكاء الاصطناعي: ${e.message}"
                            flowStep = VoiceFlowStep.ERROR
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(speechHelper) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val partialText by speechHelper.partialText.collectAsState()
    val speechError by speechHelper.errorMessage.collectAsState()

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
        if (isGranted && credits > 0) {
            speechHelper.reset()
            speechHelper.startListening()
        }
    }

    // Start listening automatically when entering LISTENING state
    LaunchedEffect(flowStep, hasAudioPermission, credits) {
        if (flowStep == VoiceFlowStep.LISTENING) {
            if (!hasAudioPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (credits > 0) {
                speechHelper.reset()
                speechHelper.startListening()
            } else {
                flowStep = VoiceFlowStep.REWARDED_AD
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
        }
    }

    // Mic Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Render corresponding screen based on flowStep
    when (flowStep) {
        VoiceFlowStep.ONBOARDING -> {
            AiVoiceOnboardingDialog(
                onDismiss = {
                    onboardingManager.markSeen()
                    if (credits > 0) {
                        flowStep = VoiceFlowStep.LISTENING
                    } else {
                        flowStep = VoiceFlowStep.REWARDED_AD
                    }
                }
            )
        }

        VoiceFlowStep.REWARDED_AD -> {
            Dialog(onDismissRequest = onDismiss) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = JournalMutedInk)
                            }
                        }

                        Text(
                            text = "سلاو ليك المحاولات اليومية (3/3) ⏳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFF57F17),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "تفرج فإشهار فيديو قصير (15-30 ثانية) وربح 3 محاولات إضافية فوراً لتسجيل السلعة والحسابات!",
                            fontSize = 13.5.sp,
                            color = JournalInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1B7A4B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (activity != null) {
                                        if (adMobManager.isRewardedReady.value) {
                                            adMobManager.showRewardedAd(
                                                activity = activity,
                                                onRewardEarned = {
                                                    creditManager.addRewardCredits(3)
                                                    flowStep = VoiceFlowStep.LISTENING
                                                }
                                            )
                                        } else {
                                            android.widget.Toast.makeText(context, "الإعلان غير جاهز بعد، المرجو المحاولة مرة أخرى", android.widget.Toast.LENGTH_SHORT).show()
                                            adMobManager.loadRewardedAd()
                                        }
                                    }
                                }
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
                                    text = "مشاهدة إعلان وربح 3 محاولات 🎁",
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

        VoiceFlowStep.LISTENING -> {
            Dialog(onDismissRequest = onDismiss) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = JournalPaper),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with Credits Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE8F5E9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                            ) {
                                Text(
                                    text = "⚡ $credits محاولات متبقية",
                                    color = Color(0xFF1B5E20),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = JournalMutedInk)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Mic Pulse
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F).copy(alpha = 0.2f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F))
                                    .clickable {
                                        speechHelper.stopAndDeliver()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "كنسمع ليك دابا... تكلم بالدارجة 🎙️",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (target == AiVoiceInputTarget.CHECKLIST) "هضر بكل راحة، سمي كاع السلعة لي خاصاك دفعة واحدة" else "هضر بكل راحة، سمي المصاريف أو السلعة بالأثمنة أو بلا أثمنة",
                            fontSize = 12.5.sp,
                            color = JournalMutedInk,
                            textAlign = TextAlign.Center
                        )

                        // Live transcript
                        if (partialText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF48FB1).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF48FB1).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "💬 \"$partialText\"",
                                    fontSize = 13.5.sp,
                                    color = JournalInk,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: Finish & Review or Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDismiss() }
                            ) {
                                Text(
                                    text = "إلغاء",
                                    color = JournalInk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1B7A4B),
                                modifier = Modifier
                                    .weight(2f)
                                    .clickable {
                                        speechHelper.stopAndDeliver()
                                    }
                            ) {
                                Text(
                                    text = "سالي ومراجعة ✨",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        VoiceFlowStep.ANALYZING -> {
            Dialog(onDismissRequest = {}) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = JournalPaper),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1B7A4B),
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "جاري التحليل بالذكاء الاصطناعي... 🤖",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = JournalInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gemini 2.5 Flash كيستخرج العناصر وكيقادهم...",
                            fontSize = 12.5.sp,
                            color = JournalMutedInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        VoiceFlowStep.REVIEW_CHECKLIST -> {
            val res = extractedChecklistResult
            if (res != null) {
                AiChecklistReviewDialog(
                    originalSpeech = recordedTranscript,
                    initialItems = res.items,
                    onDismiss = onDismiss,
                    onConfirm = { confirmedItems ->
                        creditManager.consumeCredit()
                        onChecklistResult(res.copy(items = confirmedItems))
                        onDismiss()
                    }
                )
            }
        }

        VoiceFlowStep.REVIEW_CALCULATION -> {
            val res = extractedCalculationResult
            if (res != null) {
                AiCalculationReviewDialog(
                    originalSpeech = recordedTranscript,
                    initialEntries = res.entries,
                    onDismiss = onDismiss,
                    onConfirm = { confirmedEntries ->
                        creditManager.consumeCredit()
                        onCalculationResult(res.copy(entries = confirmedEntries))
                        onDismiss()
                    }
                )
            }
        }

        VoiceFlowStep.ERROR -> {
            Dialog(onDismissRequest = onDismiss) {
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
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ لم نتمكن من المعالجة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFC62828),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: speechError ?: "يرجى المحاولة مرة أخرى والتحدث بوضوح",
                            fontSize = 13.sp,
                            color = JournalInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDismiss() }
                            ) {
                                Text(
                                    text = "إلغاء",
                                    color = JournalInk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 11.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1B7A4B),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        errorMessage = null
                                        flowStep = VoiceFlowStep.LISTENING
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "إعادة المحاولة",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
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
