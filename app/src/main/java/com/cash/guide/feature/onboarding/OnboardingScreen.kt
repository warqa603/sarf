package com.cash.guide.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.feature.settings.PromoCodeRedeemDialog
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pagerState = rememberPagerState(pageCount = { 7 })
    val coroutineScope = rememberCoroutineScope()
    var showPromoDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = JournalPaper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Subtle notebook vertical margin line
                    val marginX = if (isRtl) size.width - 24.dp.toPx() else 24.dp.toPx()
                    drawLine(
                        color = JournalRule.copy(alpha = 0.40f),
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.5f
                    )
                }
        ) {
            // Top Navigation Header: Back button & Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onboarding_back),
                            tint = JournalInk,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                // Skip button (visible on slides 0..5)
                if (pagerState.currentPage < 6) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.skipOnboarding(onFinish)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Horizontal Pager: 7 Rich Handcrafted Slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> Slide1WelcomeAndLanguage(
                        selectedLanguage = uiState.selectedLanguage,
                        onSelectLanguage = { viewModel.selectLanguage(it) },
                        isRtl = isRtl
                    )
                    1 -> Slide2RuledPaperCalculations(isRtl = isRtl)
                    2 -> Slide3ChecklistsAndSharing(isRtl = isRtl)
                    3 -> Slide4QuickNotes(isRtl = isRtl)
                    4 -> Slide5ContactsAndArtisans(isRtl = isRtl)
                    5 -> Slide6AiVoiceAssistant(isRtl = isRtl)
                    6 -> Slide7PersonalizationAndLaunch(
                        userName = uiState.userName,
                        onNameChange = { viewModel.updateUserName(it) },
                        selectedCurrency = uiState.selectedCurrency,
                        onCurrencyChange = { viewModel.selectCurrency(it) },
                        onStart = { viewModel.completeOnboarding(onFinish) },
                        onOpenPromo = { showPromoDialog = true },
                        isRtl = isRtl
                    )
                }
            }

            // Bottom Navigation: 7 Indicator Dots & Next CTA Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 7 Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    repeat(7) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 7.dp,
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) JournalWritingInk
                                    else JournalRule.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        )
                    }
                }

                // Next Step Button
                if (pagerState.currentPage < 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ElevatedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = JournalWritingInk,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_next),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showPromoDialog) {
        PromoCodeRedeemDialog(
            onDismiss = { showPromoDialog = false },
            onSuccess = {
                showPromoDialog = false
                viewModel.completeOnboarding(onFinish)
            }
        )
    }
}

// ==========================================
// SLIDE 1: WELCOME & HERITAGE & LANGUAGE
// ==========================================
@Composable
private fun Slide1WelcomeAndLanguage(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    isRtl: Boolean
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Moroccan Leather Notebook Cover Emblem
        Box(
            modifier = Modifier
                .size(92.dp)
                .shadow(6.dp, RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B4D3E), Color(0xFF0F2E24))
                    ),
                    RoundedCornerShape(22.dp)
                )
                .border(2.dp, Color(0xFFD4AF37).copy(alpha = 0.75f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ورقة",
                    fontFamily = TajawalFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Text(
                    text = "WARQA",
                    fontFamily = PatrickHandFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s1_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_s1_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 22.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center,
            lineHeight = if (isRtl) 30.sp else 28.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_s1_subtitle),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 14.sp else 15.5.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.onboarding_s1_lang_prompt),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = JournalWritingInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
        )

        Spacer(modifier = Modifier.height(8.dp))

        val languages = listOf(
            Triple("dar", "🇲🇦 الدارجة المغربية", "Maghribiya (Darija)"),
            Triple("fr", "🇫🇷 Français", "Carnet artisanal"),
            Triple("en", "🇬🇧 English", "Moroccan Notebook")
        )

        languages.forEach { (code, title, desc) ->
            val isSelected = selectedLanguage == code || (code == "dar" && (selectedLanguage == "ar" || selectedLanguage == "dar"))
            LanguageOptionCard(
                title = title,
                subtitle = desc,
                isSelected = isSelected,
                onClick = { onSelectLanguage(code) },
                isRtl = isRtl
            )
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isRtl: Boolean
) {
    val bg = if (isSelected) HighlighterGreen.copy(alpha = 0.35f) else JournalPaper
    val borderCol = if (isSelected) JournalWritingInk else JournalRule.copy(alpha = 0.45f)
    val borderW = if (isSelected) 2.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(borderW, borderCol)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 16.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
                Text(
                    text = subtitle,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 12.sp,
                    color = JournalMutedInk
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(JournalWritingInk),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// SLIDE 2: RULED PAPER & CALCULATIONS
// ==========================================
@Composable
private fun Slide2RuledPaperCalculations(isRtl: Boolean) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simulated Notebook Paper Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = JournalPaper,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, JournalRule)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🛒 Taqadya dial Semaine",
                        fontFamily = PatrickHandFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk
                    )
                    Text(
                        text = "17/09",
                        fontFamily = PatrickHandFamily,
                        fontSize = 14.sp,
                        color = JournalMutedInk
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CalculLineItem(label = "Khodra & Dwas", amount = "65 DH", ruleColor = JournalRule)
                CalculLineItem(label = "Zit l-3oud (5L)", amount = "380 DH", ruleColor = JournalRule)
                CalculLineItem(label = "Kridi Hanout (Baqi)", amount = "-50 DH", ruleColor = JournalRule, isCredit = true)

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = HighlighterYellow,
                                start = Offset(0f, size.height - 2f),
                                end = Offset(size.width, size.height - 2f),
                                strokeWidth = 5f
                            )
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total صافي :",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk
                    )
                    Text(
                        text = "395 DH",
                        fontFamily = PatrickHandFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s2_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_s2_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 21.sp else 23.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_s2_subtitle),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 13.5.sp else 15.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureBullet(icon = "✍️", text = stringResource(R.string.onboarding_s2_feature1), isRtl = isRtl)
        FeatureBullet(icon = "🤝", text = stringResource(R.string.onboarding_s2_feature2), isRtl = isRtl)
        FeatureBullet(icon = "📤", text = stringResource(R.string.onboarding_s2_feature3), isRtl = isRtl)
    }
}

@Composable
private fun CalculLineItem(label: String, amount: String, ruleColor: Color, isCredit: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = ruleColor.copy(alpha = 0.35f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• $label",
            fontFamily = PatrickHandFamily,
            fontSize = 15.sp,
            color = JournalInk
        )
        Text(
            text = amount,
            fontFamily = PatrickHandFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCredit) Color(0xFFD66860) else JournalWritingInk
        )
    }
}

// ==========================================
// SLIDE 3: CHECKLISTS & WHATSAPP SHARING
// ==========================================
@Composable
private fun Slide3ChecklistsAndSharing(isRtl: Boolean) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Checklist & WhatsApp Mockup Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = JournalPaper,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HighlighterGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Checklist Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 La liste d Te9diya",
                        fontFamily = PatrickHandFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk
                    )
                    // WhatsApp share badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "💬", fontSize = 12.sp)
                            Text(
                                text = "WhatsApp ↗",
                                fontFamily = PatrickHandFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E7E34)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checklist Items
                ChecklistMockRow(text = "2 kg Batata & 1 kg Maticha", isChecked = true)
                ChecklistMockRow(text = "Formaj rouge & 2 L 7lib", isChecked = true)
                ChecklistMockRow(text = "Bakya d Ataye & Sekkar qaleb", isChecked = false)
                ChecklistMockRow(text = "Djej m9atta3 & 1 kg Bssla", isChecked = false)

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Speech Bubble Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCF8C6),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB9E49A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📲", fontSize = 14.sp)
                        Text(
                            text = "« هاهي لا ليست د التقضية جيبها معاك وأنت راجع للدار »",
                            fontFamily = TajawalFamily,
                            fontSize = 12.5.sp,
                            color = Color(0xFF075E54)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s3_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_s3_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 21.sp else 23.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_s3_subtitle),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 13.5.sp else 15.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureBullet(icon = "☑️", text = stringResource(R.string.onboarding_s3_feature1), isRtl = isRtl)
        FeatureBullet(icon = "💬", text = stringResource(R.string.onboarding_s3_feature2), isRtl = isRtl)
        FeatureBullet(icon = "⏰", text = stringResource(R.string.onboarding_s3_feature3), isRtl = isRtl)
    }
}

@Composable
private fun ChecklistMockRow(text: String, isChecked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isChecked) JournalWritingInk else Color.White)
                .border(1.5.dp, if (isChecked) JournalWritingInk else JournalRule, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = text,
            fontFamily = PatrickHandFamily,
            fontSize = 15.sp,
            color = if (isChecked) JournalMutedInk else JournalInk
        )
    }
}

// ==========================================
// SLIDE 4: QUICK NOTES & IDEAS
// ==========================================
@Composable
private fun Slide4QuickNotes(isRtl: Boolean) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pastel Post-it Notepad Sheet Mockup
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(5.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFDE7), // Soft pastel yellow note
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFF59D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Tape / Pin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📌 Fikra & Masrouf Chhar",
                        fontFamily = PatrickHandFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                    Text(
                        text = "Note #1",
                        fontFamily = PatrickHandFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF8D6E63)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Highlighted Rule 50/30/20
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HighlighterPink.copy(alpha = 0.40f)
                ) {
                    Text(
                        text = "• Règle d'or: 50% Dar, 30% Chhar, 20% Tawkir",
                        fontFamily = PatrickHandFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "• Wajibat l-Madrasa dial Drari: Khlass 9bel 28 sept.",
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.5.sp,
                    color = JournalInk
                )
                Text(
                    text = "• Wasfa dial Tajine l-7out b chermoula d Fès 🍲",
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.5.sp,
                    color = JournalInk
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s4_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_s4_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 21.sp else 23.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_s4_subtitle),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 13.5.sp else 15.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureBullet(icon = "📄", text = stringResource(R.string.onboarding_s4_feature1), isRtl = isRtl)
        FeatureBullet(icon = "🖍️", text = stringResource(R.string.onboarding_s4_feature2), isRtl = isRtl)
        FeatureBullet(icon = "🔍", text = stringResource(R.string.onboarding_s4_feature3), isRtl = isRtl)
    }
}

// ==========================================
// SLIDE 5: CONTACTS & TRUSTED ARTISANS
// ==========================================
@Composable
private fun Slide5ContactsAndArtisans(isRtl: Boolean) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Artisans Directory Mockup Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = JournalPaper,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HighlighterBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "👥 Dalil l-M3elmin & 7irafiyin",
                    fontFamily = PatrickHandFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk
                )

                Spacer(modifier = Modifier.height(10.dp))

                ContactMockRow(
                    icon = "🔧",
                    name = "Si Mohamed (Plombier)",
                    category = "Dépannage Dar",
                    badgeText = "Payé ✓",
                    badgeColor = HighlighterGreen
                )
                ContactMockRow(
                    icon = "⚡",
                    name = "M3elem Brahim (Électricien)",
                    category = "Trakib & Dwas",
                    badgeText = "Avance: 200 DH",
                    badgeColor = HighlighterYellow
                )
                ContactMockRow(
                    icon = "🥬",
                    name = "Moul L7anout (Si Allal)",
                    category = "Epicerie l-7ouma",
                    badgeText = "Kridi: 80 DH",
                    badgeColor = HighlighterPink
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s5_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_s5_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 21.sp else 23.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_s5_subtitle),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 13.5.sp else 15.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureBullet(icon = "🏷️", text = stringResource(R.string.onboarding_s5_feature1), isRtl = isRtl)
        FeatureBullet(icon = "📞", text = stringResource(R.string.onboarding_s5_feature2), isRtl = isRtl)
        FeatureBullet(icon = "💰", text = stringResource(R.string.onboarding_s5_feature3), isRtl = isRtl)
    }
}

@Composable
private fun ContactMockRow(
    icon: String,
    name: String,
    category: String,
    badgeText: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = JournalRule.copy(alpha = 0.35f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = name,
                    fontFamily = PatrickHandFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
                Text(
                    text = category,
                    fontFamily = PatrickHandFamily,
                    fontSize = 12.sp,
                    color = JournalMutedInk
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeColor.copy(alpha = 0.35f)
        ) {
            Text(
                text = badgeText,
                fontFamily = PatrickHandFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = JournalWritingInk,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ==========================================
// SLIDE 6: AI VOICE ASSISTANT (DARIJA)
// ==========================================
@Composable
private fun Slide6AiVoiceAssistant(isRtl: Boolean) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Voice Mic Visual Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFCE4EC),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF48FB1).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mic Icon with Pulsing Halo
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE91E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Audio Wave
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val heights = listOf(8.dp, 16.dp, 24.dp, 12.dp, 28.dp, 18.dp, 10.dp, 22.dp, 14.dp, 6.dp)
                    heights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE91E63).copy(alpha = 0.7f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Darija speech sample bubble
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF48FB1).copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "« قيد ليا جوج كيلو دجاج ب 40 درهم و كيلو تفاح و باكية أتاي »",
                        fontFamily = TajawalFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF880E4F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s6_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_s6_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 21.sp else 23.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_s6_subtitle),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 13.5.sp else 15.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureBullet(icon = "🎙️", text = stringResource(R.string.onboarding_s6_feature1), isRtl = isRtl)
        FeatureBullet(icon = "⚡", text = stringResource(R.string.onboarding_s6_feature2), isRtl = isRtl)
        FeatureBullet(icon = "📝", text = stringResource(R.string.onboarding_s6_feature3), isRtl = isRtl)
    }
}

// ==========================================
// SLIDE 7: PERSONALIZATION & READY TO LAUNCH
// ==========================================
@Composable
private fun Slide7PersonalizationAndLaunch(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedCurrency: MoneyUnit,
    onCurrencyChange: (MoneyUnit) -> Unit,
    onStart: () -> Unit,
    onOpenPromo: () -> Unit,
    isRtl: Boolean
) {
    val scrollState = rememberScrollState()
    val previewName = userName.ifBlank { "..." }
    val previewPrefix = stringResource(R.string.onboarding_s7_preview_prefix)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Live Notebook Cover Title Preview
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = JournalPaper,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📖 $previewPrefix $previewName",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 20.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk
                )
                Text(
                    text = if (selectedCurrency == MoneyUnit.RIAL) "Devise : Rial (ريال مغربي)" else "Devise : Dirham (DH)",
                    fontFamily = PatrickHandFamily,
                    fontSize = 13.sp,
                    color = JournalMutedInk
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OnboardingBadge(text = stringResource(R.string.onboarding_s7_badge), isRtl = isRtl)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_s7_title),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 20.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Name input label & box
        Text(
            text = stringResource(R.string.onboarding_s7_name_label),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = JournalInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
        )

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, JournalWritingInk.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (userName.isEmpty()) {
                Text(
                    text = stringResource(R.string.onboarding_s7_name_hint),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 15.sp,
                    color = JournalMutedInk.copy(alpha = 0.55f)
                )
            }
            BasicTextField(
                value = userName,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JournalInk
                ),
                singleLine = true,
                cursorBrush = SolidColor(JournalWritingInk),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Currency selection label
        Text(
            text = stringResource(R.string.onboarding_s7_currency_label),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = JournalInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Dirham vs Rial selector cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CurrencyCard(
                title = stringResource(R.string.onboarding_s7_currency_dh),
                desc = stringResource(R.string.onboarding_s7_currency_dh_desc),
                isSelected = selectedCurrency == MoneyUnit.DIRHAM,
                onClick = { onCurrencyChange(MoneyUnit.DIRHAM) },
                isRtl = isRtl,
                modifier = Modifier.weight(1f)
            )

            CurrencyCard(
                title = stringResource(R.string.onboarding_s7_currency_rial),
                desc = stringResource(R.string.onboarding_s7_currency_rial_desc),
                isSelected = selectedCurrency == MoneyUnit.RIAL,
                onClick = { onCurrencyChange(MoneyUnit.RIAL) },
                isRtl = isRtl,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security / Offline badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecurityBadge(
                icon = "🔒",
                text = stringResource(R.string.onboarding_s7_badge_offline),
                modifier = Modifier.weight(1f),
                isRtl = isRtl
            )
            SecurityBadge(
                icon = "♾️",
                text = stringResource(R.string.onboarding_s7_badge_unlimited),
                modifier = Modifier.weight(1f),
                isRtl = isRtl
            )
            SecurityBadge(
                icon = "📱",
                text = stringResource(R.string.onboarding_s7_badge_backup),
                modifier = Modifier.weight(1f),
                isRtl = isRtl
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big Primary CTA Button: Ouvrir mon carnet
        ElevatedButton(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF1B4D3E),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_start),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 17.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // VIP / Promo Code Link
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenPromo)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "🎁", fontSize = 14.sp)
            Text(
                text = stringResource(R.string.onboarding_s7_vip_prompt),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 13.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = JournalWritingInk
            )
        }
    }
}

@Composable
private fun CurrencyCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) HighlighterYellow.copy(alpha = 0.35f) else Color.White
    val borderCol = if (isSelected) JournalWritingInk else JournalRule.copy(alpha = 0.45f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 15.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = JournalInk,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 11.sp,
                color = JournalMutedInk,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun SecurityBadge(icon: String, text: String, modifier: Modifier = Modifier, isRtl: Boolean) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 11.sp,
                color = JournalInk,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

// Reusable Components
@Composable
private fun OnboardingBadge(text: String, isRtl: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(JournalMutedInk.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
    }
}

@Composable
private fun FeatureBullet(icon: String, text: String, isRtl: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = text,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 13.5.sp else 15.sp,
            color = JournalInk
        )
    }
}
