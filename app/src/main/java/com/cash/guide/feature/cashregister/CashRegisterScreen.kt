package com.cash.guide.feature.cashregister

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            // Modern Notebook Top Bar (54dp) matching CalculsScreen & RemindersOverviewScreen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HisabiMetrics.TopBarHeight)
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

                    // Currency toggle chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.70f))
                            .clickable(role = Role.Button) { viewModel.toggleCurrency() }
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

            // Main content switcher
            when (state.step) {
                CashRegisterStep.CALCULATOR -> {
                    CashRegisterCalculatorContent(
                        state = state,
                        currencySuffix = currencySuffix,
                        isRtl = isRtl,
                        onKeyClick = { viewModel.applyCalculatorKey(it) },
                        onNext = { viewModel.goToChangeReturn() },
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
                        onPresetSelect = { viewModel.selectPresetReceived(it) },
                        onBackToCalc = { viewModel.goToCalculator() },
                        onNextClient = { viewModel.clear() },
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

    Column(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp)
    ) {
        // TOP: Soft Notebook Ledger Slip (Fiche de calcul / سجل الحسابات)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.65f))
                .border(
                    width = 0.6.dp,
                    color = JournalInk.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Row: Label + Clear Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Calculator,
                        contentDescription = null,
                        tint = JournalMutedInk,
                        size = 15.dp
                    )
                    val subheader = if (isRtl) "حساب السلعة والمشتريات" else "Articles & Calcul"
                    Text(
                        text = subheader,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                if (state.calcExpression.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEE2E2))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyClick("C")
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCoral
                            )
                            val clearText = stringResource(R.string.cash_register_clear_input)
                            Text(
                                text = clearText,
                                fontFamily = resolveJournalFont(clearText, isRtl),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCoral,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expression area
            val expressionText = state.calcExpression.ifBlank {
                stringResource(R.string.cash_register_calc_hint)
            }
            Text(
                text = expressionText,
                fontFamily = if (state.calcExpression.isBlank()) resolveJournalFont(expressionText, isRtl) else PatrickHandFamily,
                fontSize = if (state.calcExpression.isBlank()) 14.5.sp else 28.sp,
                fontWeight = if (state.calcExpression.isBlank()) FontWeight.Normal else FontWeight.Medium,
                color = if (state.calcExpression.isBlank()) JournalMutedInk.copy(alpha = 0.50f) else JournalWritingInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtle divider line
            HorizontalDivider(
                color = JournalRule.copy(alpha = 0.35f),
                thickness = 0.7.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Evaluated Total and Secondary Currency Row
            val displayTotal = state.purchaseText.ifBlank { "0" }
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                    Text(
                        text = currencySuffix,
                        fontFamily = resolveJournalFont(currencySuffix, isRtl),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
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
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Double notebook underline under total
            Canvas(
                modifier = Modifier
                    .width(130.dp)
                    .height(6.dp)
            ) {
                val strokeW = 1.2.dp.toPx()
                drawLine(
                    color = HighlighterPink.copy(alpha = 0.75f),
                    start = Offset(0f, 1.dp.toPx()),
                    end = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = HighlighterPink.copy(alpha = 0.75f),
                    start = Offset(0f, 4.dp.toPx()),
                    end = Offset(size.width, 4.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // BOTTOM: Modern Tactile Keypad + Action button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: C, ÷, ×, ⌫
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(
                    text = "C",
                    backgroundColor = Color(0xFFFFE4E6),
                    textColor = Color(0xFFE11D48),
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick("C") },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileCalcKey(
                    text = "÷",
                    backgroundColor = HighlighterBlue.copy(alpha = 0.50f),
                    textColor = JournalWritingInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick("÷") },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileCalcKey(
                    text = "×",
                    backgroundColor = HighlighterGreen.copy(alpha = 0.50f),
                    textColor = JournalWritingInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick("×") },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileBackspaceKey(
                    backgroundColor = Color.White.copy(alpha = 0.85f),
                    onClick = { onKeyClick("⌫") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: 7, 8, 9, −
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "7", onClick = { onKeyClick("7") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "8", onClick = { onKeyClick("8") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "9", onClick = { onKeyClick("9") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(
                    text = "−",
                    backgroundColor = HighlighterYellow.copy(alpha = 0.55f),
                    textColor = JournalWritingInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick("−") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: 4, 5, 6, +
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "4", onClick = { onKeyClick("4") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "5", onClick = { onKeyClick("5") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "6", onClick = { onKeyClick("6") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(
                    text = "+",
                    backgroundColor = Color(0xFFFFEDD5),
                    textColor = Color(0xFFC2410C),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick("+") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: 1, 2, 3, .
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "1", onClick = { onKeyClick("1") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "2", onClick = { onKeyClick("2") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(text = "3", onClick = { onKeyClick("3") }, modifier = Modifier.weight(1f))
                JournalTactileCalcKey(
                    text = ".",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick(".") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 5: 0 (weight 2f), 00 (weight 1f), = (weight 1f)
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JournalTactileCalcKey(text = "0", onClick = { onKeyClick("0") }, modifier = Modifier.weight(2f))
                JournalTactileCalcKey(
                    text = "00",
                    fontSize = 20.sp,
                    onClick = {
                        onKeyClick("0")
                        onKeyClick("0")
                    },
                    modifier = Modifier.weight(1f)
                )
                JournalTactileCalcKey(
                    text = "=",
                    backgroundColor = Color(0xFFFFEDD5),
                    textColor = Color(0xFFC2410C),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    onClick = { onKeyClick("=") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Primary Action button to advance to Change Return
            val hasPurchase = state.purchaseCentimes > 0L
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasPurchase) Color(0xFFFFEDD5)
                        else Color.White.copy(alpha = 0.65f)
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
                        size = 18.dp,
                        tint = if (hasPurchase) Color(0xFFC2410C) else JournalMutedInk
                    )
                    Text(
                        text = buttonText,
                        fontFamily = resolveJournalFont(buttonText, isRtl),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPurchase) Color(0xFFC2410C) else JournalMutedInk,
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
    backgroundColor: Color = Color.White.copy(alpha = 0.85f),
    textColor: Color = JournalWritingInk,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = 0.6.dp,
                color = JournalInk.copy(alpha = 0.08f),
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
    backgroundColor: Color = Color.White.copy(alpha = 0.85f),
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = 0.6.dp,
                color = JournalInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp, 17.dp)) {
            val strokeW = 1.2.dp.toPx()
            val ink = JournalInk.copy(alpha = 0.85f)
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
            drawPath(p, color = ink, style = Stroke(width = strokeW))

            // Inner cross '×'
            val cx = w * 0.64f
            val cy = h / 2f
            val d = 3.5.dp.toPx()
            drawLine(ink, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeW, StrokeCap.Round)
            drawLine(ink, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeW, StrokeCap.Round)
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

        // Rule 3 (29dp): Montant reçu du client (Header sitting directly on Rule 3)
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

        // Rule 4 (29dp): Received input row resting strictly on Rule 4
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

        // Rule 5: Preset Banknote Chips [20 DH] [50 DH] [100 DH] [200 DH]
        val presetNotes = listOf(20L, 50L, 100L, 200L)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetNotes.forEach { noteDh ->
                val chipText = if (state.currencyUnit == MoneyUnit.DIRHAM) {
                    "$noteDh DH"
                } else {
                    "${noteDh * 20} ريال"
                }
                val isSelected = state.receivedCentimes == (noteDh * 100L)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) HighlighterYellow.copy(alpha = 0.70f)
                            else Color.White.copy(alpha = 0.75f)
                        )
                        .then(
                            if (!isSelected) {
                                Modifier.border(
                                    width = 0.8.dp,
                                    color = JournalInk.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            } else Modifier
                        )
                        .clickable { onPresetSelect(noteDh) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chipText,
                        fontFamily = PatrickHandFamily,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }

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

        // Bottom bar: Client suivant action button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFEDD5))
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
                    Text("↺", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                    Text(
                        text = nextText,
                        fontFamily = resolveJournalFont(nextText, isRtl),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC2410C),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}
