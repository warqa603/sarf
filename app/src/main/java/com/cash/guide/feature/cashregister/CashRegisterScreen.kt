package com.cash.guide.feature.cashregister

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyPiece
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.ColorCoral
import com.cash.guide.ui.notebook.ColorOrange
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalDockBg
import com.cash.guide.ui.notebook.JournalDoubleUnderline
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalKeyDigitStyle
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.HisabiMetrics
import com.cash.guide.ui.notebook.JournalDenominationsBoard
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalOperatorDab
import com.cash.guide.ui.notebook.rememberBanknoteImage
import com.cash.guide.ui.notebook.resolveJournalFont

private val ColorEmerald = Color(0xFF2E7D32)

@Composable
fun CashRegisterScreen(
    viewModel: CashRegisterViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    // Handle system back navigation: return to calculator first if in change return step
    BackHandler(enabled = true) {
        if (state.step == CashRegisterStep.CHANGE_RETURN) {
            viewModel.goToCalculator()
        } else {
            onNavigateBack()
        }
    }

    // Sound effect setup
    var isSoundEnabled by remember { mutableStateOf(true) }
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {
            null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (_: Exception) {}
        }
    }

    val playSound: (String) -> Unit = remember(isSoundEnabled, toneGenerator) {
        { key ->
            if (isSoundEnabled && toneGenerator != null) {
                try {
                    when (key) {
                        "C" -> toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 35)
                        "=" -> toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 40)
                        "+", "−", "-", "×", "*", "÷", "/" -> toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT, 30)
                        "⌫" -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 20)
                        else -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val currencySuffix = if (state.currencyUnit == MoneyUnit.DIRHAM) {
        stringResource(R.string.currency_dirham)
    } else {
        stringResource(R.string.currency_rial)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .drawBehind {
                // 1. Subtle tactile paper grain / flecks
                val dotColor = JournalInk.copy(alpha = 0.022f)
                var px = 18f
                while (px < size.width) {
                    var py = 22f
                    while (py < size.height) {
                        drawCircle(
                            color = dotColor,
                            radius = 0.9f,
                            center = Offset(px, py)
                        )
                        py += 64f
                    }
                    px += 48f
                }

                // 2. 29dp Horizontal Rules
                val rowHeightPx = JournalRuleSpacing.roundToPx().toFloat()
                var y = rowHeightPx
                while (y <= size.height) {
                    drawLine(
                        color = JournalRule.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.6.dp.toPx()
                    )
                    y += rowHeightPx
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Notebook Top Bar (58dp = 2 rules)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        playSound("back")
                                        if (state.step == CashRegisterStep.CHANGE_RETURN) {
                                            viewModel.goToCalculator()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Back,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = JournalInk,
                                size = 20.dp
                            )
                        }

                        val titleText = if (state.step == CashRegisterStep.CALCULATOR) {
                            stringResource(R.string.cash_register_calc_title)
                        } else {
                            stringResource(R.string.cash_register_title)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.step == CashRegisterStep.CALCULATOR) HighlighterGreen.copy(alpha = 0.40f)
                                    else HighlighterBlue.copy(alpha = 0.40f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (state.step == CashRegisterStep.CALCULATOR) Color(0xFF10B981)
                                            else Color(0xFF38BDF8),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = titleText,
                                    fontFamily = resolveJournalFont(titleText, isRtl),
                                    fontSize = if (isRtl) 15.sp else 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }

                    // Actions: Sound Toggle + Currency Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Sound feedback toggle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.70f))
                                .clickable(role = Role.Button) {
                                    isSoundEnabled = !isSoundEnabled
                                    if (isSoundEnabled) {
                                        try {
                                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
                                        } catch (_: Exception) {}
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSoundEnabled) "🔊" else "🔇",
                                fontSize = 13.5.sp
                            )
                        }

                        // Currency toggle chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.70f))
                                .clickable(role = Role.Button) {
                                    playSound("currency")
                                    viewModel.toggleCurrency()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currencySuffix,
                                fontFamily = resolveJournalFont(currencySuffix, isRtl),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                            Text(
                                text = "⇅",
                                fontSize = 12.sp,
                                color = JournalMutedInk
                            )
                        }
                    }
                }
            }

            // Main content switcher
            when (state.step) {
                CashRegisterStep.CALCULATOR -> {
                    CashRegisterCalculatorContent(
                        state = state,
                        currencySuffix = currencySuffix,
                        isRtl = isRtl,
                        onKeyClick = { key ->
                            playSound(key)
                            viewModel.applyCalculatorKey(key)
                        },
                        onNext = {
                            playSound("=")
                            viewModel.goToChangeReturn()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                CashRegisterStep.CHANGE_RETURN -> {
                    CashRegisterChangeReturnContent(
                        state = state,
                        currencySuffix = currencySuffix,
                        isRtl = isRtl,
                        onPurchaseChange = { viewModel.setPurchaseText(it) },
                        onReceivedChange = { viewModel.setReceivedText(it) },
                        onPresetSelect = { noteDh ->
                            playSound(noteDh.toString())
                            viewModel.selectPresetReceived(noteDh)
                        },
                        onBackToCalc = {
                            playSound("back")
                            viewModel.goToCalculator()
                        },
                        onNextClient = {
                            playSound("C")
                            viewModel.clear()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// STEP 1: CALCULATOR VIEW (AUTHENTIC JOURNAL KEYPAD & LEDGER DISPLAY)
// -----------------------------------------------------------------------------

private fun Modifier.journalMultilineOnRules(
    firstLineHeight: Dp = JournalRuleSpacing,
    totalLines: Int = 3
): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
    )
    val baseline = placeable[FirstBaseline]
    val rowHeight = firstLineHeight.roundToPx()
    val totalHeightPx = (firstLineHeight * totalLines).roundToPx()
    val yOffset = if (baseline != AlignmentLine.Unspecified) {
        rowHeight - baseline
    } else {
        0
    }
    layout(placeable.width, totalHeightPx) {
        placeable.placeRelative(0, yOffset)
    }
}

@Composable
private fun CashRegisterCalculatorContent(
    state: CashRegisterUiState,
    currencySuffix: String,
    isRtl: Boolean,
    onKeyClick: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Operator and ink palette matching the notebook design system
    val OpRed = Color(0xFFE11D48)       // C
    val OpBlue = Color(0xFF0284C7)      // ÷
    val OpGreen = Color(0xFF16A34A)     // ×
    val OpSlate = Color(0xFF475569)     // ⌫
    val OpYellow = Color(0xFFD97706)    // −
    val OpOrange = Color(0xFFEA580C)    // + & =
    val NumInk = JournalWritingInk.copy(alpha = 0.82f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        val totalLines = (maxHeight / JournalRuleSpacing).toInt()
        // Fixed vertical budget:
        // Display:
        //   - Header (1 line = 29dp)
        //   - Operations area (3 lines = 87dp)
        //   - Gap between operations & result (1 line = 29dp)
        //   - Result (1 line = 29dp)
        //   Total display = 6 lines (174dp)
        // Keypad: 5 rows * 2 lines = 10 lines (290dp)
        // Bottom Action Card: 2 lines (58dp)
        // Total fixed lines = 6 + 10 + 2 = 18 lines (522dp)
        val spacerLines = maxOf(0, totalLines - 18)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // -----------------------------------------------------------------
            // TOP DISPLAY: Directly on notebook paper rules (lketba fo9 stoura)
            // -----------------------------------------------------------------

            // Rule 1: Header Row (Articles & Calcul + Clear Action)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Calculator,
                        contentDescription = null,
                        tint = JournalMutedInk,
                        size = 15.dp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                    val subheader = if (isRtl) "حساب السلعة والمشتريات" else "Articles & Calcul"
                    Text(
                        text = subheader,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                if (state.calcExpression.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyClick("C")
                            },
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorCoral,
                            modifier = Modifier.journalBaselineOnRule()
                        )
                        val clearText = stringResource(R.string.cash_register_clear_input)
                        Text(
                            text = clearText,
                            fontFamily = resolveJournalFont(clearText, isRtl),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorCoral,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                }
            }

            // Rules 2, 3 & 4: Math Expression (takes 3 notebook spaces = 87dp)
            // Comfortable vertical space for operations as requested: "o lblassa dial l3amalyat khalli fiha espace verticaly"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 3)
                    .padding(horizontal = 2.dp)
            ) {
                val expressionText = state.calcExpression.ifBlank {
                    stringResource(R.string.cash_register_calc_hint)
                }
                Text(
                    text = expressionText,
                    fontFamily = if (state.calcExpression.isBlank()) resolveJournalFont(expressionText, isRtl) else PatrickHandFamily,
                    fontSize = if (state.calcExpression.isBlank()) 15.sp else 28.sp,
                    lineHeight = 29.sp,
                    fontWeight = if (state.calcExpression.isBlank()) FontWeight.Normal else FontWeight.Bold,
                    color = if (state.calcExpression.isBlank()) JournalMutedInk.copy(alpha = 0.45f) else JournalWritingInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalMultilineOnRules(firstLineHeight = JournalRuleSpacing, totalLines = 3)
                )
            }

            // Rule 5: Breathing gap rule separating operations from result as requested: "o resultat ba3edha hta hya chwya 3la l3amalyat"
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Rule 6: Evaluated Total & Secondary Currency (1 line = 29dp)
            val displayTotal = state.purchaseText.ifBlank { "0" }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "= $displayTotal",
                        fontFamily = PatrickHandFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorOrange,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                    Text(
                        text = currencySuffix,
                        fontFamily = resolveJournalFont(currencySuffix, isRtl),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                if (state.purchaseCentimes > 0L) {
                    val secondaryValue = if (state.currencyUnit == MoneyUnit.DIRHAM) {
                        val rials = MoneyMath.fromCentimes(state.purchaseCentimes, MoneyUnit.RIAL)
                        "$rials ${stringResource(R.string.currency_rial)}"
                    } else {
                        val dh = MoneyMath.fromCentimes(state.purchaseCentimes, MoneyUnit.DIRHAM)
                        "$dh ${stringResource(R.string.currency_dirham)}"
                    }
                    Text(
                        text = "≈ $secondaryValue",
                        fontFamily = resolveJournalFont(secondaryValue, isRtl),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
            }

            // -----------------------------------------------------------------
            // SPACER: Fills integer notebook lines so the keypad rows align
            // -----------------------------------------------------------------
            if (spacerLines > 0) {
                Spacer(modifier = Modifier.height(JournalRuleSpacing * spacerLines))
            }

            // -----------------------------------------------------------------
            // KEYPAD: 5 Rows, each taking exactly 2 ruled lines (58dp)
            // -----------------------------------------------------------------

            // Row 1: C, ÷, ×, ⌫
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(
                    text = "C",
                    borderColor = OpRed,
                    borderWidth = 1.4.dp,
                    textColor = OpRed,
                    onClick = { onKeyClick("C") },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileCalcKey(
                    text = "÷",
                    borderColor = OpBlue,
                    borderWidth = 1.4.dp,
                    textColor = OpBlue,
                    fontSize = 30.sp,
                    onClick = { onKeyClick("÷") },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileCalcKey(
                    text = "×",
                    borderColor = OpGreen,
                    borderWidth = 1.4.dp,
                    textColor = OpGreen,
                    fontSize = 26.sp,
                    onClick = { onKeyClick("×") },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileBackspaceKey(
                    borderColor = OpSlate,
                    borderWidth = 1.3.dp,
                    iconColor = OpSlate,
                    onClick = { onKeyClick("⌫") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: 7, 8, 9, −
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "7", borderColor = NumInk, onClick = { onKeyClick("7") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "8", borderColor = NumInk, onClick = { onKeyClick("8") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "9", borderColor = NumInk, onClick = { onKeyClick("9") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(
                    text = "−",
                    borderColor = OpYellow,
                    borderWidth = 1.4.dp,
                    textColor = OpYellow,
                    fontSize = 30.sp,
                    onClick = { onKeyClick("−") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: 4, 5, 6, +
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "4", borderColor = NumInk, onClick = { onKeyClick("4") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "5", borderColor = NumInk, onClick = { onKeyClick("5") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "6", borderColor = NumInk, onClick = { onKeyClick("6") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(
                    text = "+",
                    borderColor = OpOrange,
                    borderWidth = 1.5.dp,
                    textColor = OpOrange,
                    fontSize = 30.sp,
                    onClick = { onKeyClick("+") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: 1, 2, 3, .
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "1", borderColor = NumInk, onClick = { onKeyClick("1") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "2", borderColor = NumInk, onClick = { onKeyClick("2") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "3", borderColor = NumInk, onClick = { onKeyClick("3") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(
                    text = ".",
                    borderColor = NumInk,
                    fontSize = 30.sp,
                    onClick = { onKeyClick(".") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 5: 0 (weight 2f), 00 (weight 1f), = (weight 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "0", borderColor = NumInk, onClick = { onKeyClick("0") }, modifier = Modifier.weight(2f))
                JournalTactileCalcKey(
                    text = "00",
                    borderColor = NumInk,
                    fontSize = 22.sp,
                    onClick = {
                        onKeyClick("0")
                        onKeyClick("0")
                    },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileCalcKey(
                    text = "=",
                    borderColor = OpOrange,
                    borderWidth = 1.6.dp,
                    textColor = OpOrange,
                    backgroundColor = OpOrange.copy(alpha = 0.08f),
                    fontSize = 30.sp,
                    onClick = { onKeyClick("=") },
                    modifier = Modifier.weight(1f)
                )
            }

            // -----------------------------------------------------------------
            // BOTTOM ACTION CARD: Takes exactly 2 lines (58dp)
            // -----------------------------------------------------------------
            val hasPurchase = state.purchaseCentimes > 0L
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2)
                    .padding(vertical = 3.5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasPurchase) Color(0xFFEA580C).copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (hasPurchase) 1.5.dp else 1.2.dp,
                        color = if (hasPurchase) Color(0xFFEA580C) else JournalWritingInk.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(role = Role.Button) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                val buttonText = stringResource(R.string.cash_register_go_to_change)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Wallet,
                        contentDescription = null,
                        size = 19.dp,
                        tint = if (hasPurchase) Color(0xFFEA580C) else JournalMutedInk
                    )
                    Text(
                        text = buttonText,
                        fontFamily = resolveJournalFont(buttonText, isRtl),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPurchase) Color(0xFFEA580C) else JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalTactileCalcKey(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = JournalWritingInk.copy(alpha = 0.80f),
    borderWidth: Dp = 1.3.dp,
    textColor: Color = JournalWritingInk,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 3.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = PatrickHandFamily,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            style = TextStyle(platformStyle = NoFontPadding)
        )
    }
}

@Composable
private fun JournalTactileBackspaceKey(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color(0xFF475569),
    borderWidth: Dp = 1.3.dp,
    iconColor: Color = Color(0xFF475569),
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 3.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp, 17.dp)) {
            val strokeW = 1.3.dp.toPx()
            val w = size.width
            val h = size.height

            // Pointed tag shape
            val p = Path().apply {
                moveTo(w * 0.32f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w * 0.32f, h)
                lineTo(0f, h / 2f)
                close()
            }
            drawPath(p, color = iconColor, style = Stroke(width = strokeW))

            // Inner cross '×'
            val cx = w * 0.64f
            val cy = h / 2f
            val d = 3.5.dp.toPx()
            drawLine(iconColor, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeW, StrokeCap.Round)
            drawLine(iconColor, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeW, StrokeCap.Round)
        }
    }
}

// -----------------------------------------------------------------------------
// STEP 2: CHANGE RETURN VIEW (CAISSE & RENDU + TSTIFA)
// -----------------------------------------------------------------------------

@Composable
private fun CashRegisterChangeReturnContent(
    state: CashRegisterUiState,
    currencySuffix: String,
    isRtl: Boolean,
    onPurchaseChange: (String) -> Unit,
    onReceivedChange: (String) -> Unit,
    onPresetSelect: (Long) -> Unit,
    onBackToCalc: () -> Unit,
    onNextClient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // Rule 1 (29dp): Total des achats (Header sitting directly on Rule 1)
        val labelTotal = stringResource(R.string.cash_register_purchase_total)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Page,
                    contentDescription = null,
                    tint = JournalInk,
                    size = 16.dp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
                Text(
                    text = labelTotal,
                    fontFamily = resolveJournalFont(labelTotal, isRtl),
                    fontSize = if (isRtl) 14.sp else 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.purchaseText.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.cash_register_clear_input),
                        fontFamily = resolveJournalFont(stringResource(R.string.cash_register_clear_input), isRtl),
                        fontSize = 12.sp,
                        color = ColorCoral,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier
                            .clickable { onPurchaseChange("") }
                            .journalBaselineOnRule()
                    )
                }

                // Link back to calculator
                Row(
                    modifier = Modifier
                        .height(JournalRuleSpacing)
                        .clickable(role = Role.Button, onClick = onBackToCalc),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Calculator,
                        contentDescription = null,
                        tint = JournalWritingInk,
                        size = 13.dp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                    Text(
                        text = stringResource(R.string.cash_register_back_to_calc),
                        fontFamily = resolveJournalFont(stringResource(R.string.cash_register_back_to_calc), isRtl),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
            }
        }

        // Rule 2 (29dp): Purchase input row resting strictly on Rule 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .drawBehind {
                    // Crisp dividing line right on the blue rule (size.height)
                    drawLine(
                        color = JournalWritingInk.copy(alpha = 0.38f),
                        start = Offset(14.dp.toPx(), size.height),
                        end = Offset(size.width - 14.dp.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = state.purchaseText,
                onValueChange = onPurchaseChange,
                modifier = Modifier
                    .weight(1f)
                    .journalBaselineOnRule(),
                textStyle = TextStyle(
                    fontFamily = PatrickHandFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isPurchaseValid) JournalWritingInk else ColorCoral,
                    platformStyle = NoFontPadding
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(JournalInk),
                decorationBox = { innerTextField ->
                    if (state.purchaseText.isEmpty()) {
                        Text(
                            text = "0.00",
                            fontFamily = PatrickHandFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk.copy(alpha = 0.35f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    innerTextField()
                }
            )

            Text(
                text = currencySuffix,
                fontFamily = resolveJournalFont(currencySuffix, isRtl),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }

        // Rule spacing: 1 empty notebook line between Total des achats and Montant reçu
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Montant reçu du client (Header sitting on rule)
        val labelReceived = stringResource(R.string.cash_register_amount_received)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Wallet,
                    contentDescription = null,
                    tint = JournalInk,
                    size = 16.dp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
                Text(
                    text = labelReceived,
                    fontFamily = resolveJournalFont(labelReceived, isRtl),
                    fontSize = if (isRtl) 14.sp else 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            if (state.receivedText.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.cash_register_clear_input),
                    fontFamily = resolveJournalFont(stringResource(R.string.cash_register_clear_input), isRtl),
                    fontSize = 12.sp,
                    color = ColorCoral,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .clickable { onReceivedChange("") }
                        .journalBaselineOnRule()
                )
            }
        }

        // Received input row resting strictly on rule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .drawBehind {
                    // Crisp dividing line right on the blue rule (size.height)
                    drawLine(
                        color = JournalWritingInk.copy(alpha = 0.25f),
                        start = Offset(14.dp.toPx(), size.height),
                        end = Offset(size.width - 14.dp.toPx(), size.height),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = state.receivedText,
                onValueChange = onReceivedChange,
                modifier = Modifier
                    .weight(1f)
                    .journalBaselineOnRule(),
                textStyle = TextStyle(
                    fontFamily = PatrickHandFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isReceivedValid) JournalWritingInk else ColorCoral,
                    platformStyle = NoFontPadding
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(JournalInk),
                decorationBox = { innerTextField ->
                    if (state.receivedText.isEmpty()) {
                        Text(
                            text = "0.00",
                            fontFamily = PatrickHandFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk.copy(alpha = 0.35f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    innerTextField()
                }
            )

            Text(
                text = currencySuffix,
                fontFamily = resolveJournalFont(currencySuffix, isRtl),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }

        // Spacing before preset banknote chips
        Spacer(modifier = Modifier.height(10.dp))

        // 8 Preset Banknote Chips (4 over 4)
        val presetRow1 = listOf(20L, 50L, 100L, 200L)
        val presetRow2 = listOf(300L, 500L, 800L, 1000L)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Row 1: 20, 50, 100, 200
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetRow1.forEach { noteDh ->
                    val chipText = if (state.currencyUnit == MoneyUnit.DIRHAM) {
                        "$noteDh DH"
                    } else {
                        "${noteDh * 20} ريال"
                    }
                    val isSelected = state.receivedCentimes == (noteDh * 100L)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) HighlighterYellow.copy(alpha = 0.70f)
                                else Color.White.copy(alpha = 0.85f)
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 0.9.dp,
                                color = if (isSelected) Color(0xFFD97706) else JournalWritingInk.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onPresetSelect(noteDh) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipText,
                            fontFamily = PatrickHandFamily,
                            fontSize = if (state.currencyUnit != MoneyUnit.DIRHAM && noteDh >= 800L) 12.sp else 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            maxLines = 1
                        )
                    }
                }
            }

            // Row 2: 300, 500, 800, 1000
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetRow2.forEach { noteDh ->
                    val chipText = if (state.currencyUnit == MoneyUnit.DIRHAM) {
                        "$noteDh DH"
                    } else {
                        "${noteDh * 20} ريال"
                    }
                    val isSelected = state.receivedCentimes == (noteDh * 100L)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) HighlighterYellow.copy(alpha = 0.70f)
                                else Color.White.copy(alpha = 0.85f)
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 0.9.dp,
                                color = if (isSelected) Color(0xFFD97706) else JournalWritingInk.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onPresetSelect(noteDh) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipText,
                            fontFamily = PatrickHandFamily,
                            fontSize = if (state.currencyUnit != MoneyUnit.DIRHAM && noteDh >= 800L) 12.sp else 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Rules 6-8: Change Due Result Band
        if (state.changeCentimes > 0L) {
            val changeDh = MoneyMath.fromCentimes(state.changeCentimes, MoneyUnit.DIRHAM)
            val changeRial = MoneyMath.fromCentimes(state.changeCentimes, MoneyUnit.RIAL)
            val changeFormatted = if (state.currencyUnit == MoneyUnit.DIRHAM) changeDh else changeRial
            val changeLabel = stringResource(R.string.cash_register_change_due)

            // Rule 6 (29dp): Change Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = changeLabel,
                    fontFamily = resolveJournalFont(changeLabel, isRtl),
                    fontSize = if (isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Rule 7 (29dp): Primary Change Amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "= $changeFormatted",
                        fontFamily = PatrickHandFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorOrange,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                    Text(
                        text = currencySuffix,
                        fontFamily = resolveJournalFont(currencySuffix, isRtl),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
            }

            // Rule 8 (29dp): Secondary Currency Representation
            val secondaryText = if (state.currencyUnit == MoneyUnit.DIRHAM) {
                "= $changeRial ${stringResource(R.string.currency_rial)}"
            } else {
                "= $changeDh ${stringResource(R.string.currency_dirham)}"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = secondaryText,
                    fontFamily = resolveJournalFont(secondaryText, isRtl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        } else if (state.isExactAmount) {
            val exactText = stringResource(R.string.cash_register_exact_amount)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Check,
                    contentDescription = null,
                    tint = ColorEmerald,
                    size = 17.dp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
                Text(
                    text = exactText,
                    fontFamily = resolveJournalFont(exactText, isRtl),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorEmerald,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        } else if (state.isInsufficient && state.shortageCentimes > 0L) {
            val shortageDh = MoneyMath.fromCentimes(state.shortageCentimes, state.currencyUnit)
            val shortageMsg = stringResource(R.string.cash_register_insufficient, "$shortageDh $currencySuffix")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Exclamation,
                    contentDescription = null,
                    tint = ColorCoral,
                    size = 17.dp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
                Text(
                    text = shortageMsg,
                    fontFamily = resolveJournalFont(shortageMsg, isRtl),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorCoral,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        }

        // 1 empty notebook line spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // TSTIFA TRAY: Banknotes & Coins Board
        if (state.pieces.isNotEmpty()) {
            JournalDenominationsBoard(
                pieces = state.pieces,
                isRtl = isRtl
            )
            Spacer(modifier = Modifier.height(JournalRuleSpacing))
        }

        // Bottom bar: Client suivant action button (takes exactly 2 lines = 58dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing * 2)
                .padding(horizontal = 14.dp)
                .padding(vertical = 3.5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEA580C).copy(alpha = 0.08f))
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFEA580C),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(role = Role.Button) {
                    onNextClient()
                },
            contentAlignment = Alignment.Center
        ) {
            val nextText = stringResource(R.string.cash_register_next_client)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("↺", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                Text(
                    text = nextText,
                    fontFamily = resolveJournalFont(nextText, isRtl),
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEA580C),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}
