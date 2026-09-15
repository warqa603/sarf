package com.cash.guide.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.ui.notebook.*
import kotlin.math.roundToInt

/**
 * Interactive 7-mode Diagnostic Simulator.
 * All computation is pure Kotlin — no DB queries, no internet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticSimulatorSheet(
    diagnostic: FullDiagnosticResult,
    goal: SavingsGoalEntity,
    onClose: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var selectedMode by remember { mutableStateOf(SimulatorMode.LEAK_BREAKDOWN) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = JournalPaper,
        scrimColor = Color(0x66000000),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JournalMutedInk.copy(alpha = 0.35f))
            )
        },
        properties = ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ── Simulator title ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(JournalRuleSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text(
                    text = if (isRtl) "🔮 حاسبة التشخيص التفاعلية" else "🔮 Simulateur de diagnostic",
                    fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding)
                )
                androidx.compose.material3.Text(
                    text = "✕",
                    fontFamily = TajawalFamily, fontSize = 16.sp, color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // ── Mode tab bar (horizontal scroll) ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SimulatorMode.values().forEach { mode ->
                    val (emoji, shortAr, shortFr) = modeLabel(mode)
                    val label = if (isRtl) "$emoji $shortAr" else "$emoji $shortFr"
                    val isSelected = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HighlighterYellow.copy(alpha = 0.55f) else JournalPaper)
                            .border(1.dp, if (isSelected) JournalWritingInk.copy(0.5f) else JournalMutedInk.copy(0.20f), RoundedCornerShape(8.dp))
                            .clickable(role = Role.Tab) { selectedMode = mode }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = label,
                            fontFamily = TajawalFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(0.8.dp).padding(horizontal = 16.dp).background(JournalRule))

            // ── Mode Content ───────────────────────────────────────────────
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (selectedMode) {
                    SimulatorMode.LEAK_BREAKDOWN -> ModeLeakBreakdown(diagnostic, isRtl)
                    SimulatorMode.REDUCE_LEAKS -> ModeReduceLeaks(diagnostic, goal, isRtl)
                    SimulatorMode.EXTEND_DEADLINE -> ModeExtendDeadline(diagnostic, goal, isRtl)
                    SimulatorMode.INCREASE_SAVINGS -> ModeIncreaseSavings(diagnostic, goal, isRtl)
                    SimulatorMode.TOUGH_MONTH -> ModeToughMonth(diagnostic, goal, isRtl)
                    SimulatorMode.ANNUAL_EXPENSE -> ModeAnnualExpense(diagnostic, goal, isRtl)
                    SimulatorMode.EMERGENCY_FUND -> ModeEmergencyFund(diagnostic, goal, isRtl)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun modeLabel(mode: SimulatorMode): Triple<String, String, String> = when (mode) {
    SimulatorMode.LEAK_BREAKDOWN -> Triple("💸", "فين الفلوس؟", "Où va l'argent ?")
    SimulatorMode.REDUCE_LEAKS -> Triple("✂️", "إذا قللت", "Si je réduis")
    SimulatorMode.EXTEND_DEADLINE -> Triple("📅", "مد المدة", "Allonger")
    SimulatorMode.INCREASE_SAVINGS -> Triple("📈", "زد التوفير", "Épargner +")
    SimulatorMode.TOUGH_MONTH -> Triple("🌧️", "شهر صعيب", "Mois difficile")
    SimulatorMode.ANNUAL_EXPENSE -> Triple("📅", "مصروف سنوي", "Dép. annuelle")
    SimulatorMode.EMERGENCY_FUND -> Triple("🛡️", "الطوارئ", "Urgence")
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 1: Leak Breakdown — where does money go
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeLeakBreakdown(d: FullDiagnosticResult, isRtl: Boolean) {
    SimTitle(if (isRtl) "💸 فين كيمشي الفلوس؟" else "💸 Où va votre argent ?")
    Spacer(Modifier.height(10.dp))

    val m = d.metrics
    val items = listOfNotNull(
        if (m.protectedEssentialsCentimes > 0) (if (isRtl) "🛡️ الأساسيات المحمية" else "🛡️ Charges protégées") to m.protectedEssentialsCentimes else null,
        if (m.debtPaymentsCentimes > 0) (if (isRtl) "📋 أداءات وديون" else "📋 Mensualités") to m.debtPaymentsCentimes else null,
        if (m.familyCommitmentsCentimes > 0) (if (isRtl) "👨‍👩‍👧 التزامات عائلية" else "👨‍👩‍👧 Obligations familiales") to m.familyCommitmentsCentimes else null,
        if (m.flexibleSpendingCentimes > 0) (if (isRtl) "💸 مصاريف مرنة" else "💸 Dépenses flexibles") to m.flexibleSpendingCentimes else null,
        if (m.irregularMonthlyReserveCentimes > 0) (if (isRtl) "📅 احتياط موسمي" else "📅 Provision saisonnière") to m.irregularMonthlyReserveCentimes else null
    )

    items.forEach { (label, centimes) ->
        val dh = centimes / 100
        val pct = if (m.monthlyIncomeCentimes > 0) (centimes.toFloat() / m.monthlyIncomeCentimes * 100).roundToInt() else 0
        NotebookRuledSimRow(label, "$dh DH ($pct%)", isRtl = isRtl)
    }

    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(JournalRule))
    Spacer(Modifier.height(6.dp))
    val fcf = m.freeCashFlowCentimes / 100
    NotebookRuledSimRow(
        label = if (isRtl) "🟢 الهامش الحر الشهري" else "🟢 Marge libre mensuelle",
        value = "$fcf DH",
        bold = true,
        color = if (fcf >= 0) HighlighterGreen else HighlighterPink,
        isRtl = isRtl
    )
    NotebookRuledSimRow(
        label = if (isRtl) "🎯 القسط المطلوب للهدف" else "🎯 Mensualité objectif requise",
        value = "${m.requiredMonthlyCentimes / 100} DH",
        bold = true,
        color = JournalWritingInk,
        isRtl = isRtl
    )

    // Leak details
    if (d.topLeaks.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        SimSubtitle(if (isRtl) "تفاصيل أبواب التسرب:" else "Détail des fuites :")
        d.topLeaks.forEach { leak ->
            NotebookRuledSimRow(
                label = if (isRtl) leak.titleAr else leak.titleFr,
                value = "${leak.monthlyDrainDh.toLong()} DH/${if (isRtl) "شهر" else "mois"}",
                isRtl = isRtl
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 2: Reduce Leaks — slider 0/25/50/75%
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeReduceLeaks(d: FullDiagnosticResult, goal: SavingsGoalEntity, isRtl: Boolean) {
    var reductionPct by remember { mutableIntStateOf(50) }
    SimTitle(if (isRtl) "✂️ إذا قللت المصاريف المرنة؟" else "✂️ Et si vous réduisiez vos dépenses flexibles ?")
    Spacer(Modifier.height(10.dp))

    val topLeak = d.topLeaks.firstOrNull()
    val totalLeakMonthly = d.topLeaks.sumOf { it.monthlyDrainDh }
    val saved = totalLeakMonthly * reductionPct / 100
    val savedAnnual = saved * 12
    val newCapacity = d.metrics.realisticCapacityCentimes / 100.0 + saved
    val required = d.metrics.requiredMonthlyCentimes / 100.0
    val monthsGain = if (newCapacity > required && goal.targetMonths > 0) {
        val goalAmountDh = (goal.targetAmountCentimes - goal.currentAmountCentimes) / 100.0
        val originalMonths = goal.targetMonths
        val newMonths = if (newCapacity > 0) (goalAmountDh / newCapacity).toInt() else originalMonths
        originalMonths - newMonths.coerceAtLeast(1)
    } else 0

    Text(
        text = if (isRtl) "نسبة التخفيض: $reductionPct%" else "Taux de réduction : $reductionPct%",
        fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding)
    )
    Spacer(Modifier.height(6.dp))
    Slider(
        value = reductionPct.toFloat(),
        onValueChange = { reductionPct = it.roundToInt() },
        valueRange = 0f..75f,
        steps = 2,
        colors = SliderDefaults.colors(
            thumbColor = JournalInk,
            activeTrackColor = HighlighterYellow,
            inactiveTrackColor = JournalMutedInk.copy(0.3f)
        )
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(0, 25, 50, 75).forEach { v ->
            androidx.compose.material3.Text(
                "$v%", fontFamily = TajawalFamily, fontSize = 11.sp, color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    ResultCard {
        NotebookRuledSimRow(if (isRtl) "💰 ربح شهري" else "💰 Gain mensuel", "${saved.toLong()} DH", bold = true, color = HighlighterGreen, isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "📅 ربح سنوي" else "📅 Gain annuel", "${savedAnnual.toLong()} DH", bold = true, color = HighlighterGreen, isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "🎯 القدرة الشهرية الجديدة" else "🎯 Nouvelle capacité mensuelle", "${newCapacity.toLong()} DH", isRtl = isRtl)
        if (monthsGain > 0) {
            NotebookRuledSimRow(
                if (isRtl) "⏱️ توفير في الوقت" else "⏱️ Gain de temps",
                "-$monthsGain ${if (isRtl) "شهر" else "mois"}",
                bold = true, color = HighlighterGreen, isRtl = isRtl
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 3: Extend Deadline — slider +1 to +12 months
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeExtendDeadline(d: FullDiagnosticResult, goal: SavingsGoalEntity, isRtl: Boolean) {
    var extraMonths by remember { mutableIntStateOf(0) }
    SimTitle(if (isRtl) "📅 إذا مددت الموعد النهائي؟" else "📅 Et si vous allongiez le délai ?")
    Spacer(Modifier.height(10.dp))

    val goalRemainingDh = (goal.targetAmountCentimes - goal.currentAmountCentimes).coerceAtLeast(0L) / 100.0
    val originalMonths = goal.targetMonths.coerceAtLeast(1)
    val newMonths = originalMonths + extraMonths
    val newMonthly = if (newMonths > 0) goalRemainingDh / newMonths else goalRemainingDh
    val originalMonthly = if (originalMonths > 0) goalRemainingDh / originalMonths else goalRemainingDh
    val saving = originalMonthly - newMonthly

    Text(
        text = if (isRtl) "تمديد إضافي: +$extraMonths ${if (isRtl) "شهر" else "mois"}" else "+$extraMonths mois supplémentaires",
        fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding)
    )
    Spacer(Modifier.height(6.dp))
    Slider(
        value = extraMonths.toFloat(),
        onValueChange = { extraMonths = it.roundToInt() },
        valueRange = 0f..12f,
        steps = 11,
        colors = SliderDefaults.colors(
            thumbColor = JournalInk,
            activeTrackColor = HighlighterBlue,
            inactiveTrackColor = JournalMutedInk.copy(0.3f)
        )
    )
    Spacer(Modifier.height(12.dp))

    ResultCard {
        NotebookRuledSimRow(if (isRtl) "📅 المدة الجديدة" else "📅 Nouvelle durée", "$newMonths ${if (isRtl) "شهر" else "mois"}", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "💳 القسط الأصلي" else "💳 Mensualité actuelle", "${originalMonthly.toLong()} DH", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "💚 القسط الجديد" else "💚 Nouvelle mensualité", "${newMonthly.toLong()} DH", bold = true, color = HighlighterGreen, isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "📉 تخفيض في القسط" else "📉 Réduction mensuelle", "-${saving.toLong()} DH", bold = true, color = HighlighterGreen, isRtl = isRtl)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 4: Increase Savings — +100/200/500/custom
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeIncreaseSavings(d: FullDiagnosticResult, goal: SavingsGoalEntity, isRtl: Boolean) {
    var extraDh by remember { mutableLongStateOf(200L) }
    SimTitle(if (isRtl) "📈 إذا زدت التوفير الشهري؟" else "📈 Et si vous épargniez davantage ?")
    Spacer(Modifier.height(10.dp))

    val goalRemaining = (goal.targetAmountCentimes - goal.currentAmountCentimes).coerceAtLeast(0L) / 100.0
    val currentMonthly = d.metrics.requiredMonthlyCentimes / 100.0
    val newMonthly = currentMonthly + extraDh
    val newMonths = if (newMonthly > 0) (goalRemaining / newMonthly).toInt().coerceAtLeast(1) else goal.targetMonths
    val monthsGain = (goal.targetMonths - newMonths).coerceAtLeast(0)

    Text(if (isRtl) "إضافة شهرية:" else "Ajout mensuel :", fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(100L, 200L, 500L, 1000L).forEach { v ->
            val label = if (v >= 1000) "${v/1000}K DH" else "+$v DH"
            Box(
                modifier = Modifier.weight(1f).height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (extraDh == v) HighlighterYellow.copy(0.55f) else JournalPaper)
                    .border(1.dp, if (extraDh == v) JournalWritingInk.copy(0.5f) else JournalMutedInk.copy(0.2f), RoundedCornerShape(8.dp))
                    .clickable { extraDh = v },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(label, fontFamily = TajawalFamily, fontSize = 12.sp,
                    fontWeight = if (extraDh == v) FontWeight.Bold else FontWeight.Normal,
                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    ResultCard {
        NotebookRuledSimRow(if (isRtl) "💳 القسط الحالي" else "💳 Mensualité actuelle", "${currentMonthly.toLong()} DH", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "💚 القسط الجديد" else "💚 Nouvelle mensualité", "${newMonthly.toLong()} DH", bold = true, color = HighlighterGreen, isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "📅 مدة الهدف الجديدة" else "📅 Nouvelle durée", "$newMonths ${if (isRtl) "شهر" else "mois"}", isRtl = isRtl)
        if (monthsGain > 0) {
            NotebookRuledSimRow(if (isRtl) "⏱️ تقليص في المدة" else "⏱️ Gain de temps", "-$monthsGain ${if (isRtl) "شهر" else "mois"}", bold = true, color = HighlighterGreen, isRtl = isRtl)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 5: Tough Month — income -10% / -20%
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeToughMonth(d: FullDiagnosticResult, goal: SavingsGoalEntity, isRtl: Boolean) {
    var shockPct by remember { mutableIntStateOf(0) }
    SimTitle(if (isRtl) "🌧️ شهر صعيب — شنو اللي يتحمل؟" else "🌧️ Mois difficile — qu'est-ce qui tient ?")
    Spacer(Modifier.height(10.dp))

    val income = d.metrics.monthlyIncomeCentimes / 100.0
    val required = d.metrics.requiredMonthlyCentimes / 100.0
    val essentials = d.metrics.protectedEssentialsCentimes / 100.0
    val reducedIncome = income * (100 - shockPct) / 100.0
    val canPay = reducedIncome - essentials - d.metrics.debtPaymentsCentimes / 100.0
    val planSurvives = canPay >= required

    Text(if (isRtl) "صدمة الدخل:" else "Choc de revenu :", fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 10, 20).forEach { v ->
            val label = if (v == 0) (if (isRtl) "شهر عادي" else "Normal") else "-$v%"
            Box(
                modifier = Modifier.weight(1f).height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (shockPct == v) (if (v == 0) HighlighterGreen.copy(0.45f) else HighlighterPink.copy(0.45f)) else JournalPaper)
                    .border(1.dp, JournalMutedInk.copy(0.2f), RoundedCornerShape(8.dp))
                    .clickable { shockPct = v },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(label, fontFamily = TajawalFamily, fontSize = 12.sp,
                    fontWeight = if (shockPct == v) FontWeight.Bold else FontWeight.Normal,
                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    ResultCard {
        NotebookRuledSimRow(if (isRtl) "💰 الدخل في هاد الشهر" else "💰 Revenu ce mois", "${reducedIncome.toLong()} DH", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "🛡️ الأساسيات + الديون" else "🛡️ Essentiels + dettes", "${(essentials + d.metrics.debtPaymentsCentimes/100).toLong()} DH", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "💚 المبلغ المتاح للتوفير" else "💚 Disponible pour épargne", "${canPay.toLong()} DH",
            bold = true,
            color = if (planSurvives) HighlighterGreen else HighlighterPink,
            isRtl = isRtl
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background((if (planSurvives) HighlighterGreen else HighlighterPink).copy(alpha = 0.18f))
                .border(1.dp, (if (planSurvives) HighlighterGreen else HighlighterPink).copy(0.35f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            androidx.compose.material3.Text(
                text = if (planSurvives) {
                    if (isRtl) "✅ الخطة تصمد حتى في هاد الشهر الصعيب" else "✅ Le plan tient même dans ce mois difficile"
                } else {
                    if (isRtl) "⚠️ الشهر الصعيب ممكن يضرب الخطة — خاص احتياط طوارئ" else "⚠️ Ce mois difficile peut fragiliser le plan — prévoir un fonds d'urgence"
                },
                fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 6: Annual Expense Reserve
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeAnnualExpense(d: FullDiagnosticResult, goal: SavingsGoalEntity, isRtl: Boolean) {
    var expenseDh by remember { mutableStateOf("5000") }
    var monthsAway by remember { mutableIntStateOf(6) }
    SimTitle(if (isRtl) "📅 حساب الاحتياط للمصروف السنوي" else "📅 Provision pour une dépense annuelle")
    Spacer(Modifier.height(10.dp))

    val amt = expenseDh.toLongOrNull() ?: 0L
    val reserve = if (monthsAway > 0) amt.toDouble() / monthsAway else amt.toDouble()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Text(if (isRtl) "المبلغ الإجمالي للمصروف (DH):" else "Montant total de la dépense (DH):",
            fontFamily = TajawalFamily, fontSize = 13.sp, color = JournalMutedInk, style = TextStyle(platformStyle = NoFontPadding))
        androidx.compose.foundation.text.BasicTextField(
            value = expenseDh,
            onValueChange = { expenseDh = it.filter(Char::isDigit) },
            textStyle = TextStyle(fontFamily = TajawalFamily, fontSize = 15.sp, color = JournalWritingInk, platformStyle = NoFontPadding),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(JournalInk),
            modifier = Modifier.fillMaxWidth().height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(1.dp, JournalMutedInk.copy(0.30f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        androidx.compose.material3.Text(
            if (isRtl) "خلال $monthsAway ${if (monthsAway == 1) "شهر" else "أشهر"}:" else "Dans $monthsAway mois :",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Slider(
            value = monthsAway.toFloat(),
            onValueChange = { monthsAway = it.roundToInt().coerceAtLeast(1) },
            valueRange = 1f..12f,
            steps = 10,
            colors = SliderDefaults.colors(thumbColor = JournalInk, activeTrackColor = HighlighterBlue, inactiveTrackColor = JournalMutedInk.copy(0.3f))
        )
    }
    Spacer(Modifier.height(12.dp))

    ResultCard {
        NotebookRuledSimRow(if (isRtl) "📅 الاحتياط الشهري المطلوب" else "📅 Provision mensuelle requise", "${reserve.toLong()} DH", bold = true, color = HighlighterBlue, isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "💰 إجمالي عند الاستحقاق" else "💰 Total à l'échéance", "$amt DH", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "💡 عزل ${ reserve.toLong()} DH شهرياً في حساب منفصل" else "💡 Isolez ${reserve.toLong()} DH/mois sur un compte dédié", "", isRtl = isRtl)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Mode 7: Emergency Fund Coverage
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModeEmergencyFund(d: FullDiagnosticResult, goal: SavingsGoalEntity, isRtl: Boolean) {
    var targetMonths by remember { mutableIntStateOf(3) }
    SimTitle(if (isRtl) "🛡️ صندوق الطوارئ — شحال تغطي؟" else "🛡️ Fonds d'urgence — quelle couverture ?")
    Spacer(Modifier.height(10.dp))

    val essentials = d.metrics.protectedEssentialsCentimes / 100.0
    val current = d.metrics.emergencyFundCentimes / 100.0
    val currentCoverage = if (essentials > 0) current / essentials else 0.0
    val targetAmount = essentials * targetMonths
    val gap = (targetAmount - current).coerceAtLeast(0.0)
    val monthsToFill = if (d.metrics.realisticCapacityCentimes > 0) (gap / (d.metrics.realisticCapacityCentimes / 100.0)).toInt().coerceAtLeast(1) else 0

    Text(
        if (isRtl) "الهدف: $targetMonths ${if (isRtl) "أشهر من الأساسيات" else "mois de charges"}:" else "Cible : $targetMonths mois de charges :",
        fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1, 3, 6).forEach { v ->
            val label = "$v ${if (isRtl) "أشهر" else "mois"}"
            Box(
                modifier = Modifier.weight(1f).height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (targetMonths == v) HighlighterBlue.copy(0.40f) else JournalPaper)
                    .border(1.dp, JournalMutedInk.copy(0.2f), RoundedCornerShape(8.dp))
                    .clickable { targetMonths = v },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(label, fontFamily = TajawalFamily, fontSize = 12.sp,
                    fontWeight = if (targetMonths == v) FontWeight.Bold else FontWeight.Normal,
                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    ResultCard {
        NotebookRuledSimRow(if (isRtl) "💳 الأساسيات الشهرية" else "💳 Charges essentielles / mois", "${essentials.toLong()} DH", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "🛡️ الاحتياط الحالي" else "🛡️ Épargne sécurité actuelle", "${current.toLong()} DH (${String.format("%.1f", currentCoverage)} ${if (isRtl) "شهر" else "mois"})", isRtl = isRtl)
        NotebookRuledSimRow(if (isRtl) "🎯 الهدف الموصى به" else "🎯 Objectif recommandé", "${targetAmount.toLong()} DH ($targetMonths ${if (isRtl) "أشهر" else "mois"})", bold = true, color = HighlighterBlue, isRtl = isRtl)
        if (gap > 0) {
            NotebookRuledSimRow(if (isRtl) "📉 الفجوة" else "📉 Manque", "${gap.toLong()} DH", color = HighlighterPink, isRtl = isRtl)
            if (monthsToFill > 0) {
                NotebookRuledSimRow(if (isRtl) "⏱️ لتجميعه بالقدرة الحالية" else "⏱️ Pour y arriver à votre rythme", "$monthsToFill ${if (isRtl) "شهر" else "mois"}", isRtl = isRtl)
            }
        } else {
            NotebookRuledSimRow(if (isRtl) "✅ صندوق الطوارئ وافي!" else "✅ Fonds d'urgence suffisant !", "", bold = true, color = HighlighterGreen, isRtl = isRtl)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shared UI helpers for Simulator
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SimTitle(text: String) {
    androidx.compose.material3.Text(
        text = text,
        fontFamily = TajawalFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = JournalWritingInk,
        style = TextStyle(platformStyle = NoFontPadding)
    )
}

@Composable
private fun SimSubtitle(text: String) {
    androidx.compose.material3.Text(
        text = text,
        fontFamily = TajawalFamily,
        fontSize = 12.sp,
        color = JournalMutedInk,
        style = TextStyle(platformStyle = NoFontPadding)
    )
}

@Composable
private fun ResultCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(JournalPaper)
            .border(1.dp, JournalRule, RoundedCornerShape(10.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun NotebookRuledSimRow(
    label: String,
    value: String,
    bold: Boolean = false,
    color: Color = JournalWritingInk,
    isRtl: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(JournalRuleSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.Text(
            text = label,
            fontFamily = TajawalFamily,
            fontSize = 12.5.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            androidx.compose.material3.Text(
                text = value,
                fontFamily = TajawalFamily,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.5.sp,
                color = color,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(0.6.dp).background(JournalRule.copy(alpha = 0.4f)))
}
