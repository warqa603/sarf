package com.cash.guide.feature.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.ui.notebook.*

/**
 * Full Financial Interview Sheet — sections A through L + Review.
 * Adaptive branching: some steps are shown only based on previous answers.
 *
 * This is the "أكمل الاستبيان الكامل" deep-dive questionnaire.
 * The existing 7-step SavingsWizardSheet remains for quick goal creation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FinancialQuestionnaireSheet(
    answers: QuestionnaireAnswers,
    goal: SavingsGoalEntity? = null,
    onUpdateAnswers: (QuestionnaireAnswers) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Flatten all visible steps into a single ordered list based on branching
    val allSteps = remember(answers) { buildVisibleSteps(answers) }
    var currentStepIdx by remember { mutableIntStateOf(0) }
    val currentStep = allSteps.getOrNull(currentStepIdx) ?: QuestionnaireStepId.REVIEW

    val totalSteps = allSteps.size
    val progress = if (totalSteps > 0) (currentStepIdx + 1).toFloat() / totalSteps.toFloat() else 0f
    val scrollState = rememberScrollState()
    val stepValidation = remember(currentStep, answers, isRtl) { validateStep(currentStep, answers, isRtl) }

    LaunchedEffect(currentStepIdx) { scrollState.scrollTo(0) }

    BackHandler {
        // Keep the interview open on the first step as well: cancellation is explicit
        // through the visible "Annuler" action, so no answers are lost by accident.
        if (currentStepIdx > 0) currentStepIdx--
    }

    ModalBottomSheet(
        // The interview is deliberately modal: do not let an accidental tap outside
        // the sheet discard a partially completed questionnaire. "Annuler" is the
        // explicit way back to the underlying screen.
        onDismissRequest = { },
        sheetState = sheetState,
        containerColor = JournalPaper,
        // Keep the notebook visible while the user works through a long interview.
        // A dark scrim made the underlying typography look greyed out and disappear.
        scrimColor = Color.Transparent,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JournalMutedInk.copy(alpha = 0.35f))
            )
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            QuestionnaireHeader(
                currentStep = currentStepIdx + 1,
                totalSteps = totalSteps,
                progress = progress,
                onPrev = { if (currentStepIdx > 0) currentStepIdx-- else onClose() },
                onClose = onClose,
                isRtl = isRtl
            )

            QuestionnaireStageLabel(currentStep, isRtl)

            Spacer(Modifier.height(14.dp))

            // ── Step content ───────────────────────────────────────────────────
            when (currentStep) {
                QuestionnaireStepId.GOAL_SETUP -> StepGoalSetup(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.A1_OWNERSHIP -> StepA1Ownership(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.A2_DEPENDENTS -> StepA2Dependents(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.A3_FAMILY_COMMITMENT -> StepA3FamilyCommitment(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.B1_INCOME -> StepB1Income(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.B2_INCOME_TYPE -> StepB2IncomeType(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.B3_INCOME_RANGE -> StepB3IncomeRange(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.C_ESSENTIALS -> StepCEssentials(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.D1_DEBTS -> StepD1Debts(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.D2_DEBT_DETAIL -> StepD2DebtDetail(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.E_EMERGENCY -> StepEEmergency(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.F_CASHFLOW -> StepFCashflow(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.G1_LEAK_SELECT -> StepG1LeakSelect(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.G2_LEAK_DETAIL -> StepG2LeakDetail(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.H_SEASONAL -> StepHSeasonal(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.I_SAVINGS_BEHAVIOR -> StepISavingsBehavior(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.J_BUYING_BEHAVIOR -> StepJBuyingBehavior(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.K_USER_LIMITS -> StepKUserLimits(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.L_GOAL_FLEXIBILITY -> StepLGoalFlexibility(answers, isRtl, onUpdateAnswers)
                QuestionnaireStepId.REVIEW -> StepReview(answers, goal, isRtl)
            }

            Spacer(Modifier.height(20.dp))

            // ── Navigation button ──────────────────────────────────────────────
            val isLast = currentStep == QuestionnaireStepId.REVIEW
            if (!stepValidation.first) {
                Text(
                    text = stepValidation.second,
                    fontFamily = TajawalFamily,
                    fontSize = 12.sp,
                    color = Color(0xFFB23A48),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (!stepValidation.first) JournalMutedInk.copy(alpha = 0.10f)
                        else if (isLast) HighlighterGreen.copy(alpha = 0.45f)
                        else HighlighterYellow.copy(alpha = 0.50f)
                    )
                    .border(1.dp, JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable(enabled = stepValidation.first, role = Role.Button) {
                        if (isLast) {
                            onSubmit()
                        } else {
                            if (currentStepIdx < allSteps.size - 1) currentStepIdx++
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLast) {
                        if (isRtl) "احفظ الهدف والخطة 🚀" else "Enregistrer l'objectif et le plan 🚀"
                    } else {
                        if (isRtl) "التالي ←" else "Suivant →"
                    },
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (stepValidation.first) JournalWritingInk else JournalMutedInk.copy(alpha = 0.65f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Step IDs (flat list for branching)
// ══════════════════════════════════════════════════════════════════════════════

private enum class QuestionnaireStepId {
    GOAL_SETUP,
    A1_OWNERSHIP, A2_DEPENDENTS, A3_FAMILY_COMMITMENT,
    B1_INCOME, B2_INCOME_TYPE, B3_INCOME_RANGE,
    C_ESSENTIALS,
    D1_DEBTS, D2_DEBT_DETAIL,
    E_EMERGENCY,
    F_CASHFLOW,
    G1_LEAK_SELECT, G2_LEAK_DETAIL,
    H_SEASONAL,
    I_SAVINGS_BEHAVIOR,
    J_BUYING_BEHAVIOR,
    K_USER_LIMITS,
    L_GOAL_FLEXIBILITY,
    REVIEW
}

private fun buildVisibleSteps(answers: QuestionnaireAnswers): List<QuestionnaireStepId> {
    val steps = mutableListOf<QuestionnaireStepId>()
    steps += QuestionnaireStepId.GOAL_SETUP
    steps += QuestionnaireStepId.A1_OWNERSHIP
    steps += QuestionnaireStepId.A2_DEPENDENTS
    if (answers.dependentsCount > 0 || answers.hasFamilyCommitments) {
        steps += QuestionnaireStepId.A3_FAMILY_COMMITMENT
    }
    steps += QuestionnaireStepId.B1_INCOME
    steps += QuestionnaireStepId.B2_INCOME_TYPE
    if (answers.incomeType != "INCOME_STABLE") {
        steps += QuestionnaireStepId.B3_INCOME_RANGE
    }
    steps += QuestionnaireStepId.C_ESSENTIALS
    steps += QuestionnaireStepId.D1_DEBTS
    if (answers.debtType != "DEBT_NONE") {
        steps += QuestionnaireStepId.D2_DEBT_DETAIL
    }
    steps += QuestionnaireStepId.E_EMERGENCY
    steps += QuestionnaireStepId.F_CASHFLOW
    steps += QuestionnaireStepId.G1_LEAK_SELECT
    if (answers.selectedLeaks.isNotEmpty()) {
        steps += QuestionnaireStepId.G2_LEAK_DETAIL
    }
    steps += QuestionnaireStepId.H_SEASONAL
    steps += QuestionnaireStepId.I_SAVINGS_BEHAVIOR
    steps += QuestionnaireStepId.J_BUYING_BEHAVIOR
    steps += QuestionnaireStepId.K_USER_LIMITS
    steps += QuestionnaireStepId.L_GOAL_FLEXIBILITY
    steps += QuestionnaireStepId.REVIEW
    return steps
}

private fun validateStep(step: QuestionnaireStepId, a: QuestionnaireAnswers, isRtl: Boolean): Pair<Boolean, String> {
    val message = when (step) {
        QuestionnaireStepId.GOAL_SETUP -> when {
            a.goalTitle.isBlank() -> if (isRtl) "كتب اسم الهدف باش نعرفو على شنو غادي نبنيو الخطة." else "Donnez un nom à l'objectif pour personnaliser le plan."
            a.goalTargetCentimes <= a.goalInitialCentimes -> if (isRtl) "المبلغ المستهدف خاصو يكون أكبر من اللي مجموع دابا." else "Le montant visé doit être supérieur au montant déjà disponible."
            a.goalTargetMonths <= 0 -> if (isRtl) "اختار مدة صالحة للهدف." else "Choisissez une durée valide."
            else -> ""
        }
        QuestionnaireStepId.B1_INCOME -> if (a.netMonthlyIncomeCentimes <= 0)
            if (isRtl) "دخل المتوسط الشهري باش ما نعطيوكش تشخيص وهمي." else "Indiquez un revenu mensuel moyen pour éviter un diagnostic trompeur."
        else ""
        QuestionnaireStepId.B3_INCOME_RANGE -> if (a.incomeLowestCentimes <= 0 || a.incomeHighestCentimes < a.incomeLowestCentimes)
            if (isRtl) "دخل أضعف وأقوى شهر، وخلي المبلغ الأعلى أكبر من الأدنى." else "Indiquez un mois bas et un mois haut cohérents."
        else ""
        QuestionnaireStepId.D2_DEBT_DETAIL -> if (a.debtPaymentsCentimes <= 0)
            if (isRtl) "دخل مجموع أقساط الديون الشهرية." else "Indiquez le total mensuel des remboursements."
        else ""
        QuestionnaireStepId.G2_LEAK_DETAIL -> if (a.selectedLeaks.any { key ->
            val d = a.leakDetails[key]
            d == null || d.costPerUseDh <= 0 || d.weeklyFrequency <= 0
        }) if (isRtl) "كمل الثمن وعدد المرات لكل مصروف اخترتيه." else "Complétez le coût et la fréquence de chaque poste choisi."
        else ""
        else -> ""
    }
    return (message.isBlank()) to message
}

@Composable
private fun QuestionnaireStageLabel(step: QuestionnaireStepId, isRtl: Boolean) {
    val label = when (step) {
        QuestionnaireStepId.GOAL_SETUP -> if (isRtl) "الهدف" else "Objectif"
        QuestionnaireStepId.A1_OWNERSHIP, QuestionnaireStepId.A2_DEPENDENTS, QuestionnaireStepId.A3_FAMILY_COMMITMENT -> if (isRtl) "الوضع العائلي" else "Foyer"
        QuestionnaireStepId.B1_INCOME, QuestionnaireStepId.B2_INCOME_TYPE, QuestionnaireStepId.B3_INCOME_RANGE -> if (isRtl) "المدخول" else "Revenus"
        QuestionnaireStepId.C_ESSENTIALS, QuestionnaireStepId.D1_DEBTS, QuestionnaireStepId.D2_DEBT_DETAIL -> if (isRtl) "الالتزامات" else "Charges & dettes"
        QuestionnaireStepId.E_EMERGENCY, QuestionnaireStepId.F_CASHFLOW -> if (isRtl) "الأمان المالي" else "Sécurité financière"
        QuestionnaireStepId.G1_LEAK_SELECT, QuestionnaireStepId.G2_LEAK_DETAIL, QuestionnaireStepId.H_SEASONAL -> if (isRtl) "المصاريف" else "Dépenses"
        QuestionnaireStepId.I_SAVINGS_BEHAVIOR, QuestionnaireStepId.J_BUYING_BEHAVIOR, QuestionnaireStepId.K_USER_LIMITS -> if (isRtl) "العادات والحدود" else "Habitudes & limites"
        QuestionnaireStepId.L_GOAL_FLEXIBILITY -> if (isRtl) "مرونة الخطة" else "Souplesse du plan"
        QuestionnaireStepId.REVIEW -> if (isRtl) "مراجعة الأجوبة" else "Vérification"
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(HighlighterBlue))
        Spacer(Modifier.width(7.dp))
        Text(
            text = if (isRtl) "$label · المعلومات كتبقى فالتليفون" else "$label · données conservées sur cet appareil",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuestionnaireHeader(
    currentStep: Int,
    totalSteps: Int,
    progress: Float,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    isRtl: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(29.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isRtl) "إلغاء" else "Annuler",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .clickable(role = Role.Button, onClick = onClose)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier.width(150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isRtl) "المرحلة $currentStep من $totalSteps" else "Étape $currentStep sur $totalSteps",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JournalMutedInk.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(JournalInk)
                )
            }
        }

        if (currentStep > 1) {
            Text(
                text = if (isRtl) "السابق" else "Retour",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClick = onPrev)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Spacer(Modifier.width(36.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shared UI helpers
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        fontFamily = TajawalFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = JournalWritingInk,
        style = TextStyle(platformStyle = NoFontPadding)
    )
}

@Composable
private fun StepSubtitle(text: String) {
    Text(
        text = text,
        fontFamily = TajawalFamily,
        fontSize = 13.sp,
        color = JournalMutedInk,
        style = TextStyle(platformStyle = NoFontPadding)
    )
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String = "",
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) HighlighterYellow.copy(alpha = 0.45f) else JournalPaper)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) JournalWritingInk.copy(alpha = 0.60f) else JournalMutedInk.copy(alpha = 0.22f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = TajawalFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = TajawalFamily,
                    fontSize = 12.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

@Composable
private fun MultiChoiceCard(
    title: String,
    subtitle: String = "",
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) HighlighterBlue.copy(alpha = 0.30f) else JournalPaper)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) JournalWritingInk.copy(alpha = 0.50f) else JournalMutedInk.copy(alpha = 0.22f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape)
                    .background(if (isSelected) JournalInk else Color.Transparent)
                    .border(1.dp, JournalMutedInk, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", fontSize = 9.sp, color = JournalPaper, style = TextStyle(platformStyle = NoFontPadding))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = TajawalFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontFamily = TajawalFamily,
                        fontSize = 11.5.sp,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    centimes: Long,
    onCentimesChange: (Long) -> Unit,
    placeholder: String = "0"
) {
    var text by remember(centimes) {
        mutableStateOf(if (centimes > 0) (centimes / 100).toString() else "")
    }

    Column {
        Text(
            text = label,
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(1.dp, JournalMutedInk.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { v ->
                    text = v.filter { it.isDigit() }
                    val dh = text.toLongOrNull() ?: 0L
                    onCentimesChange(dh * 100)
                },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontFamily = TajawalFamily,
                    fontSize = 15.sp,
                    color = JournalWritingInk,
                    platformStyle = NoFontPadding
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(JournalInk),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = TajawalFamily,
                            fontSize = 14.sp,
                            color = JournalMutedInk.copy(alpha = 0.6f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    inner()
                }
            )
            Text(
                text = "DH",
                fontFamily = TajawalFamily,
                fontSize = 13.sp,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Step 0 — Goal Setup
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepGoalSetup(
    a: QuestionnaireAnswers,
    isRtl: Boolean,
    onUpdate: (QuestionnaireAnswers) -> Unit
) {
    val presets = listOf(
        Triple("CAR", if (isRtl) "🚗 سيارة" else "🚗 Voiture", if (isRtl) "شراء سيارة" else "Achat voiture"),
        Triple("HOUSE", if (isRtl) "🏠 سكن" else "🏠 Logement", if (isRtl) "دفعة السكن" else "Apport logement"),
        Triple("EMERGENCY", if (isRtl) "🛡️ طوارئ" else "🛡️ Urgence", if (isRtl) "صندوق الطوارئ" else "Fonds d'urgence"),
        Triple("EVENT", if (isRtl) "🎉 مناسبة" else "🎉 Événement", if (isRtl) "مناسبة عائلية" else "Événement familial"),
        Triple("PROJECT", if (isRtl) "💼 مشروع" else "💼 Projet", if (isRtl) "بداية مشروع" else "Projet perso"),
        Triple("OTHER", if (isRtl) "🎯 هدف خاص" else "🎯 Autre", if (isRtl) "هدف شخصي" else "Mon objectif")
    )

    val durationOptions = listOf(3, 6, 12, 18, 24, 36)
    val colorOptions = listOf(
        "BLUE" to Color(0xFF2A6F97),
        "GREEN" to Color(0xFF2E7D32),
        "AMBER" to Color(0xFFE65100),
        "PURPLE" to Color(0xFF6A1B9A)
    )

    // Start with a localized, editable title instead of leaking a title from a
    // different language into the questionnaire.
    LaunchedEffect(a.goalPreset, isRtl) {
        if (a.goalTitle.isBlank()) {
            val defaultTitle = presets.firstOrNull { it.first == a.goalPreset }?.third
                ?: if (isRtl) "هدف التوفير" else "Mon objectif"
            onUpdate(a.copy(goalTitle = defaultTitle))
        }
    }

    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "🎯 حدد هدفك المالي" else "🎯 Définissez votre objectif d'épargne")
        StepSubtitle(if (isRtl) "هاد الهدف هو البوصلة ديال خطتك والتحليل المالي ديالك" else "Cet objectif sera la boussole de votre plan et de votre diagnostic")
        Spacer(Modifier.height(14.dp))

        // Preset chips in 2 rows of 3
        Text(
            text = if (isRtl) "نوع الهدف" else "Type d'objectif",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.chunked(3).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowPresets.forEach { (key, label, defaultTitle) ->
                        val isSelected = a.goalPreset == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HighlighterYellow.copy(alpha = 0.50f) else JournalPaper)
                                .border(
                                    1.dp,
                                    if (isSelected) JournalWritingInk else JournalMutedInk.copy(alpha = 0.25f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onUpdate(
                                        a.copy(
                                            goalPreset = key,
                                            goalTitle = if (a.goalTitle.isBlank() || presets.any { it.third == a.goalTitle }) defaultTitle else a.goalTitle
                                        )
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = TajawalFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Title input field
        Text(
            text = if (isRtl) "عنوان الهدف" else "Nom de l'objectif",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(1.dp, JournalMutedInk.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = a.goalTitle,
                onValueChange = { onUpdate(a.copy(goalTitle = it)) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontFamily = TajawalFamily,
                    fontSize = 14.sp,
                    color = JournalWritingInk,
                    platformStyle = NoFontPadding
                ),
                singleLine = true,
                cursorBrush = SolidColor(JournalInk),
                decorationBox = { inner ->
                    if (a.goalTitle.isEmpty()) {
                        Text(
                            text = if (isRtl) "مثلاً: دفعة الشقة، سيارة مستعملة..." else "Ex: Apport appartement...",
                            fontFamily = TajawalFamily,
                            fontSize = 13.sp,
                            color = JournalMutedInk.copy(alpha = 0.6f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Target Amount
        AmountField(
            label = if (isRtl) "المبلغ المستهدف الإجمالي" else "Montant total visé",
            centimes = a.goalTargetCentimes,
            onCentimesChange = { onUpdate(a.copy(goalTargetCentimes = it)) },
            placeholder = "10000"
        )

        Spacer(Modifier.height(10.dp))

        // Initial Amount
        AmountField(
            label = if (isRtl) "المبلغ المتوفر حالياً (إن وجد)" else "Montant déjà de côté (si existant)",
            centimes = a.goalInitialCentimes,
            onCentimesChange = { onUpdate(a.copy(goalInitialCentimes = it)) },
            placeholder = "0"
        )

        Spacer(Modifier.height(12.dp))

        // Target Duration
        Text(
            text = if (isRtl) "المدة المستهدفة (بالأشهر)" else "Durée visée (en mois)",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            durationOptions.forEach { months ->
                val isSelected = a.goalTargetMonths == months
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) HighlighterYellow.copy(alpha = 0.50f) else JournalPaper)
                        .border(
                            1.dp,
                            if (isSelected) JournalWritingInk else JournalMutedInk.copy(alpha = 0.25f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onUpdate(a.copy(goalTargetMonths = months)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$months ${if (isRtl) "ش" else "m"}",
                        fontFamily = TajawalFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Color tag selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isRtl) "لون التمييز" else "Couleur du carnet",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorOptions.forEach { (tag, color) ->
                    val isSelected = a.goalColorTag == tag
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) JournalWritingInk else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onUpdate(a.copy(goalColorTag = tag)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Live calculation preview box
        val remaining = maxOf(0L, a.goalTargetCentimes - a.goalInitialCentimes)
        val perMonth = if (a.goalTargetMonths > 0) remaining / a.goalTargetMonths else 0L
        val perDay = perMonth / 30

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(JournalPaper)
                .border(1.dp, JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isRtl) "المطلوب شهرياً:" else "Requis par mois :",
                        fontFamily = TajawalFamily,
                        fontSize = 13.sp,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                    Text(
                        text = "${perMonth / 100} DH",
                        fontFamily = TajawalFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isRtl) "المطلوب يومياً:" else "Requis par jour :",
                        fontFamily = TajawalFamily,
                        fontSize = 12.sp,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                    Text(
                        text = "~${perDay / 100} DH / ${if (isRtl) "يوم" else "j"}",
                        fontFamily = TajawalFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section A — Personal / Family
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepA1Ownership(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "كتدبر هاد الهدف بوحدك ولا مع شخص آخر؟" else "Gérez-vous cet objectif seul ou à plusieurs ?")
        StepSubtitle(if (isRtl) "هاد المعلومة كتساعدنا نحسبو القدرة الحقيقية صح" else "Cela nous aide à calculer la capacité réelle")
        Spacer(Modifier.height(12.dp))
        listOf(
            "GOAL_SOLO" to (if (isRtl) "بوحدي" else "Seul(e)"),
            "GOAL_SHARED" to (if (isRtl) "مع الزوج/الزوجة أو شريك" else "Avec conjoint(e) ou partenaire"),
            "GOAL_FAMILY" to (if (isRtl) "مساهمة عائلية" else "Contribution familiale")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.goalOwnership == key) { onUpdate(a.copy(goalOwnership = key)) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepA2Dependents(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "شحال من شخص كتساهم فمصاريفهم بشكل منتظم؟" else "Combien de personnes dépendent financièrement de vous ?")
        StepSubtitle(if (isRtl) "الكراء، التقضية، الدراسة... كتحتسبو فهاد الحساب" else "Loyer, alimentation, scolarité... entrent dans ce calcul")
        Spacer(Modifier.height(12.dp))
        listOf(
            0 to (if (isRtl) "غير راسي" else "Personne (juste moi)"),
            1 to (if (isRtl) "شخص واحد" else "1 personne"),
            2 to (if (isRtl) "شخصان (2)" else "2 personnes"),
            3 to (if (isRtl) "3 أو أكثر" else "3 ou plus")
        ).forEach { (count, label) ->
            ChoiceCard(
                title = label,
                subtitle = if (count > 0 && isRtl) "كيزيد وزن الاستقرار فالتوصيات" else if (count > 0) "Renforce l'importance de la stabilité" else "",
                isSelected = a.dependentsCount == count
            ) {
                onUpdate(a.copy(dependentsCount = count, hasFamilyCommitments = count > 0 || a.hasFamilyCommitments))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepA3FamilyCommitment(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "شحال تقريباً التزاماتك العائلية الثابتة كل شهر؟" else "Montant mensuel de vos obligations familiales fixes ?")
        StepSubtitle(if (isRtl) "مساعدة الوالدين، نفقة، دعم الأخوة... هذا ما يُحمى ولا يُقطع" else "Aide parents, pension, soutien fratrie... à protéger")
        Spacer(Modifier.height(12.dp))
        AmountField(
            label = if (isRtl) "المبلغ الشهري (درهم):" else "Montant mensuel (DH):",
            centimes = a.familyCommitmentCentimes,
            onCentimesChange = { onUpdate(a.copy(familyCommitmentCentimes = it)) },
            placeholder = "0"
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section B — Income
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepB1Income(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    val brackets = listOf(
        300000L to (if (isRtl) "أقل من 3 000 DH" else "Moins de 3 000 DH"),
        400000L to (if (isRtl) "3 000 – 5 000 DH" else "3 000 – 5 000 DH"),
        650000L to (if (isRtl) "5 000 – 8 000 DH" else "5 000 – 8 000 DH"),
        1000000L to (if (isRtl) "8 000 – 12 000 DH" else "8 000 – 12 000 DH"),
        1600000L to (if (isRtl) "12 000 – 20 000 DH" else "12 000 – 20 000 DH"),
        0L to (if (isRtl) "متغير بزاف" else "Très variable")
    )
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "شحال متوسط دخلك الشهري الصافي؟" else "Quel est votre revenu mensuel net moyen ?")
        StepSubtitle(if (isRtl) "بعد الضرائب وقبل الصرف. هاد المعلومة كتبقى فالهاتف فقط." else "Après impôts, avant dépenses. Stocké uniquement sur votre appareil.")
        Spacer(Modifier.height(12.dp))
        // Exact amount field first
        AmountField(
            label = if (isRtl) "الدخل الشهري الصافي (درهم) — ادخل الرقم مباشرة إذا أمكن:" else "Revenu net mensuel (DH) — saisir directement si possible :",
            centimes = a.netMonthlyIncomeCentimes,
            onCentimesChange = { onUpdate(a.copy(netMonthlyIncomeCentimes = it)) },
            placeholder = "6000"
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (isRtl) "أو اختر الشريحة التقريبية:" else "Ou choisissez une tranche approximative :",
            fontFamily = TajawalFamily,
            fontSize = 12.5.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(Modifier.height(6.dp))
        brackets.forEach { (centimes, label) ->
            val selected = if (centimes == 0L) a.netMonthlyIncomeCentimes == 0L
            else a.netMonthlyIncomeCentimes in ((centimes - 100000L)..(centimes + 100000L))
            ChoiceCard(
                title = label,
                isSelected = selected
            ) {
                if (centimes > 0) onUpdate(a.copy(netMonthlyIncomeCentimes = centimes))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun StepB2IncomeType(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "الدخل ديالك كيف كيكون غالباً؟" else "Votre revenu est généralement :")
        Spacer(Modifier.height(12.dp))
        listOf(
            "INCOME_STABLE" to Triple(
                if (isRtl) "قار تقريباً كل شهر" else "Fixe ou quasi-fixe",
                if (isRtl) "موظف، عقد دائم، معاش..." else "Salarié, CDI, retraite...",
                HighlighterGreen
            ),
            "INCOME_VARIABLE" to Triple(
                if (isRtl) "كيتبدل شوية من شهر لآخر" else "Variable d'un mois à l'autre",
                if (isRtl) "كوميسيون، بريستاسيون، نشاط مستقل..." else "Commission, prestations, activité indépendante...",
                HighlighterYellow
            ),
            "INCOME_HIGH_VARIANCE" to Triple(
                if (isRtl) "متغير بزاف — صعيب نحدد رقم ثابت" else "Très variable — difficile à fixer",
                if (isRtl) "مرات كيبقى شهر كبير ومرات صغير جداً" else "Grands écarts entre bons et mauvais mois",
                HighlighterPink
            ),
            "INCOME_SEASONAL" to Triple(
                if (isRtl) "موسمي — بعض الأشهر قوي وبعضها خفيف" else "Saisonnier — forte variation selon la saison",
                if (isRtl) "صيف/رمضان قوي مثلاً..." else "Ex: fort en été ou pendant Ramadan...",
                HighlighterBlue
            )
        ).forEach { (key, triple) ->
            val (title, sub, _) = triple
            ChoiceCard(title = title, subtitle = sub, isSelected = a.incomeType == key) {
                onUpdate(a.copy(incomeType = key))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepB3IncomeRange(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "تقريباً شحال أقل شهر وأحسن شهر؟" else "Approximativement, vos mois les plus faibles et forts ?")
        StepSubtitle(if (isRtl) "نستعمل الأقل باش نبنيو خطة آمنة" else "Nous utilisons le minimum pour construire un plan sécurisé")
        Spacer(Modifier.height(12.dp))
        AmountField(
            label = if (isRtl) "أقل شهر (درهم):" else "Mois le plus faible (DH):",
            centimes = a.incomeLowestCentimes,
            onCentimesChange = { onUpdate(a.copy(incomeLowestCentimes = it)) }
        )
        Spacer(Modifier.height(10.dp))
        AmountField(
            label = if (isRtl) "أحسن شهر (درهم):" else "Meilleur mois (DH):",
            centimes = a.incomeHighestCentimes,
            onCentimesChange = { onUpdate(a.copy(incomeHighestCentimes = it)) }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section C — Protected Essentials
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepCEssentials(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "المصاريف الأساسية المحمية الشهرية؟" else "Vos charges essentielles mensuelles protégées ?")
        StepSubtitle(if (isRtl) "هذه ما نمسوهاش أبداً بالتقشف. ضع 0 إذا ما عندكش هاد النوع." else "Ces postes sont intouchables. Mettez 0 si vous n'en avez pas.")
        Spacer(Modifier.height(12.dp))
        val fields = listOf(
            Triple(if (isRtl) "🏠 الكراء أو قرض السكن (DH/شهر):" else "🏠 Loyer ou crédit logement (DH/mois):", a.housingCentimes) { v: Long -> a.copy(housingCentimes = v) },
            Triple(if (isRtl) "⚡ الماء + الكهرباء (متوسط شهري):" else "⚡ Eau + Électricité (moyenne mensuelle):", a.utilitiesCentimes) { v: Long -> a.copy(utilitiesCentimes = v) },
            Triple(if (isRtl) "📱 الأنترنت + الهاتف الضروري:" else "📱 Internet + téléphone nécessaire:", a.internetPhoneCentimes) { v: Long -> a.copy(internetPhoneCentimes = v) },
            Triple(if (isRtl) "🥗 التقضية المنزلية والماكلة الأساسية:" else "🥗 Courses alimentaires maison:", a.groceriesCentimes) { v: Long -> a.copy(groceriesCentimes = v) },
            Triple(if (isRtl) "🚌 التنقل الضروري للخدمة (نقل عمومي، بنزين...):" else "🚌 Transport travail/études:", a.workTransportCentimes) { v: Long -> a.copy(workTransportCentimes = v) },
            Triple(if (isRtl) "🏥 الصحة والدواء (متوسط):" else "🏥 Santé et pharmacie (moyenne):", a.healthCentimes) { v: Long -> a.copy(healthCentimes = v) },
            Triple(if (isRtl) "📚 الدراسة والأطفال والحضانة:" else "📚 Scolarité, enfants, crèche:", a.educationCentimes) { v: Long -> a.copy(educationCentimes = v) },
            Triple(if (isRtl) "🛡️ تأمينات ضرورية:" else "🛡️ Assurances nécessaires:", a.insuranceCentimes) { v: Long -> a.copy(insuranceCentimes = v) }
        )
        fields.forEach { (label, value, update) ->
            AmountField(label = label, centimes = value, onCentimesChange = { onUpdate(update(it)) })
            Spacer(Modifier.height(8.dp))
        }
        // Summary
        val total = a.housingCentimes + a.utilitiesCentimes + a.internetPhoneCentimes + a.groceriesCentimes + a.workTransportCentimes + a.healthCentimes + a.educationCentimes + a.insuranceCentimes
        if (total > 0) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(HighlighterGreen.copy(alpha = 0.20f))
                    .border(1.dp, HighlighterGreen.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isRtl) "✅ إجمالي الأساسيات المحمية: ${total / 100} DH/شهر" else "✅ Total charges protégées : ${total / 100} DH/mois",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section D — Debts
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepD1Debts(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "واش عندك ديون أو أقساط شهرية؟" else "Avez-vous des dettes ou mensualités ?")
        Spacer(Modifier.height(12.dp))
        listOf(
            "DEBT_NONE" to (if (isRtl) "لا، بدون ديون" else "Non, aucune dette"),
            "DEBT_MORTGAGE" to (if (isRtl) "قرض سكن" else "Crédit immobilier"),
            "DEBT_CAR" to (if (isRtl) "قرض سيارة" else "Crédit auto"),
            "DEBT_CONSUMER" to (if (isRtl) "قرض استهلاكي" else "Crédit à la consommation"),
            "DEBT_CARD" to (if (isRtl) "بطاقة/تسهيلات" else "Carte / facilité de caisse"),
            "DEBT_PERSONAL" to (if (isRtl) "دين لشخص" else "Dette envers une personne"),
            "DEBT_MULTIPLE" to (if (isRtl) "أكثر من واحد" else "Plusieurs à la fois")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.debtType == key) { onUpdate(a.copy(debtType = key)) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun StepD2DebtDetail(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "تفاصيل الأداءات والضغط المالي" else "Détails des paiements et pression financière")
        Spacer(Modifier.height(12.dp))
        AmountField(
            label = if (isRtl) "إجمالي الأقساط الشهرية (درهم):" else "Total des mensualités (DH):",
            centimes = a.debtPaymentsCentimes,
            onCentimesChange = { onUpdate(a.copy(debtPaymentsCentimes = it)) }
        )
        Spacer(Modifier.height(12.dp))
        Text(if (isRtl) "واش كتأخر فشي أداء أحياناً؟" else "Vous arrive-t-il de retarder un paiement ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(0 to (if (isRtl) "أبداً" else "Jamais"), 1 to (if (isRtl) "نادراً" else "Rarement"), 2 to (if (isRtl) "مرات" else "Parfois"), 3 to (if (isRtl) "غالباً" else "Souvent")).forEach { (v, label) ->
            ChoiceCard(label, isSelected = a.paymentDelayFrequency == v) { onUpdate(a.copy(paymentDelayFrequency = v)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(if (isRtl) "واش كتسلف باش تكمل الشهر؟" else "Vous arrive-t-il d'emprunter pour finir le mois ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(0 to (if (isRtl) "أبداً" else "Jamais"), 1 to (if (isRtl) "نادراً" else "Rarement"), 2 to (if (isRtl) "بعض الشهور" else "Certains mois"), 3 to (if (isRtl) "غالباً" else "Souvent")).forEach { (v, label) ->
            ChoiceCard(label, isSelected = a.endOfMonthBorrowFrequency == v) { onUpdate(a.copy(endOfMonthBorrowFrequency = v)) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section E — Emergency Fund
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepEEmergency(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "صندوق الطوارئ والحماية المالية" else "Fonds d'urgence et protection financière")
        StepSubtitle(if (isRtl) "فلوس منفصلة على الهدف، للمصاريف الطارئة" else "Argent séparé de l'objectif, pour imprévus")
        Spacer(Modifier.height(12.dp))
        AmountField(
            label = if (isRtl) "شحال عندك كاحتياطي للطوارئ (درهم)؟" else "Montant de votre épargne de sécurité (DH) ?",
            centimes = a.emergencyFundCentimes,
            onCentimesChange = { onUpdate(a.copy(emergencyFundCentimes = it)) },
            placeholder = "0"
        )
        Spacer(Modifier.height(12.dp))
        Text(if (isRtl) "إلا وقع مصروف طارئ 3000 DH، غالباً غادي:" else "Face à une dépense urgente de 3000 DH, vous :",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(
            "READY_CASH" to (if (isRtl) "نخلصو من فلوس جاهزة" else "Je paie depuis mon épargne disponible"),
            "FROM_SAVINGS" to (if (isRtl) "ناخد من فلوس الهدف" else "Je prends dans l'objectif"),
            "CREDIT" to (if (isRtl) "نستعمل الكريدي أو الكارط" else "Je recours au crédit / carte"),
            "BORROW" to (if (isRtl) "نسلف من شخص" else "J'emprunte à quelqu'un"),
            "UNKNOWN" to (if (isRtl) "ما عارفش" else "Je ne sais pas")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.emergencyResponse == key) { onUpdate(a.copy(emergencyResponse = key)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(if (isRtl) "فين كتكون فلوس الطوارئ؟" else "Où sont placés vos fonds d'urgence ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(
            "SAME_ACCOUNT" to (if (isRtl) "نفس الحساب اليومي" else "Même compte courant"),
            "SEPARATE" to (if (isRtl) "حساب منفصل" else "Compte séparé"),
            "CASH" to (if (isRtl) "كاش في البيت" else "Espèces à la maison"),
            "NONE" to (if (isRtl) "ما عنديش" else "Je n'en ai pas")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.emergencyFundLocation == key) { onUpdate(a.copy(emergencyFundLocation = key)) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section F — Cash Flow
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepFCashflow(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "آخر الشهر غالباً كيف كيكون الحال؟" else "En fin de mois, en général :")
        Spacer(Modifier.height(12.dp))
        listOf(
            "END_SURPLUS" to (if (isRtl) "كيبقى مبلغ مزيان" else "Il reste une belle somme"),
            "END_LITTLE" to (if (isRtl) "كيبقى شوية" else "Il reste un peu"),
            "END_ZERO" to (if (isRtl) "قريب من الصفر" else "Proche de zéro"),
            "END_DEFICIT" to (if (isRtl) "كيسالي قبل نهاية الشهر" else "L'argent manque avant la fin du mois"),
            "END_BORROW" to (if (isRtl) "كنحتاج تسلاف أو كريدي" else "Je dois emprunter ou utiliser le crédit")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.monthEndSituation == key) { onUpdate(a.copy(monthEndSituation = key)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(if (isRtl) "واش عارف فين كيمشي الصرف غالباً؟" else "Savez-vous généralement où va votre argent ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(
            "KNOWS_WELL" to (if (isRtl) "نعم مزيان" else "Oui, clairement"),
            "APPROXIMATELY" to (if (isRtl) "تقريباً" else "Approximativement"),
            "NO" to (if (isRtl) "لا، ما كنعرفش" else "Non, pas vraiment")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.spendingAwareness == key) { onUpdate(a.copy(spendingAwareness = key)) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section G — Leaks (multi-select + detail)
// ══════════════════════════════════════════════════════════════════════════════

private val LEAK_OPTIONS = listOf(
    "CAFE" to Pair("☕ القهاوي والفطور والماكلة برا", "☕ Cafés, petits-dej, repas dehors"),
    "SHOPPING" to Pair("🛍️ الشوبينغ والملابس والكماليات", "🛍️ Shopping vêtements & gadgets"),
    "OUTINGS" to Pair("🚗 الخرجات والويكاند والكازوال", "🚗 Sorties, week-ends & loisirs"),
    "SUBSCRIPTIONS" to Pair("📱 اشتراكات رقمية وفورفيات زايدة", "📱 Abonnements & forfaits superflus"),
    "TAXIS" to Pair("🚕 طاكسي وسيارة مشترك بدون حاجة", "🚕 Taxis non nécessaires"),
    "GAMBLING" to Pair("🎰 باريات أو ألعاب بالفلوس", "🎰 Paris / jeux d'argent"),
    "NONE" to Pair("✅ ما عنديش مشكل واضح", "✅ Pas de problème identifié")
)

@Composable
private fun StepG1LeakSelect(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "شنو الحوايج اللي كتحس أنها كتجر منك الفلوس أكثر من اللازم؟" else "Quels postes vous semblent absorber trop d'argent ?")
        StepSubtitle(if (isRtl) "اختار كل اللي ينطبق عليك (اختيار متعدد)" else "Sélectionnez tout ce qui s'applique (choix multiple)")
        Spacer(Modifier.height(12.dp))
        LEAK_OPTIONS.forEach { (key, labels) ->
            val label = if (isRtl) labels.first else labels.second
            val isSelected = key in a.selectedLeaks
            MultiChoiceCard(label, isSelected = isSelected) {
                val newList = if (isSelected) {
                    a.selectedLeaks.filter { it != key }
                } else {
                    if (key == "NONE") listOf("NONE")
                    else (a.selectedLeaks.filter { it != "NONE" } + key)
                }
                onUpdate(a.copy(selectedLeaks = newList))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun StepG2LeakDetail(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    val relevantLeaks = a.selectedLeaks.filter { it != "NONE" && it != "GAMBLING" }
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "نحسبو لك المبلغ الحقيقي لكل تسرب" else "Calculons le vrai coût de chaque fuite")
        StepSubtitle(if (isRtl) "هاد الأرقام هي اللي تفرق فالتشخيص الحقيقي" else "Ces chiffres font toute la différence dans le diagnostic")
        Spacer(Modifier.height(12.dp))

        relevantLeaks.forEach { leakKey ->
            val leakInfo = SavingsKnowledgeBase.getLeak(leakKey)
            val detail = a.leakDetails[leakKey] ?: LeakAnswerDetail()
            val monthly = detail.costPerUseDh * detail.weeklyFrequency * 52.0 / 12.0

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(HighlighterYellow.copy(alpha = 0.18f))
                    .border(1.dp, JournalMutedInk.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = if (isRtl) leakInfo.titleAr else leakInfo.titleFr,
                        fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
                        color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Cost per use chips
                    Text(if (isRtl) "شحال تقريباً كل مرة؟" else "Combien par usage ?",
                        fontFamily = TajawalFamily, fontSize = 12.sp, color = JournalMutedInk, style = TextStyle(platformStyle = NoFontPadding))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        leakInfo.quickCostOptionsDh.forEachIndexed { idx, cost ->
                            val lbl = if (isRtl) leakInfo.quickCostLabelsAr.getOrElse(idx) { "${cost.toInt()} DH" }
                                      else leakInfo.quickCostLabelsFr.getOrElse(idx) { "${cost.toInt()} DH" }
                            val isCostSel = detail.costPerUseDh == cost
                            Box(
                                modifier = Modifier.weight(1f).height(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCostSel) HighlighterYellow.copy(alpha = 0.55f) else JournalPaper)
                                    .border(1.dp, if (isCostSel) JournalWritingInk.copy(0.6f) else JournalMutedInk.copy(0.25f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdate(a.copy(leakDetails = a.leakDetails + (leakKey to detail.copy(costPerUseDh = cost))))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lbl, fontFamily = TajawalFamily, fontSize = 9.5.sp,
                                    fontWeight = if (isCostSel) FontWeight.Bold else FontWeight.Normal,
                                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding),
                                    modifier = Modifier.padding(horizontal = 1.dp), maxLines = 1, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Frequency chips
                    Text(if (isRtl) "شحال من مرة فالسيمانة؟" else "Combien de fois par semaine ?",
                        fontFamily = TajawalFamily, fontSize = 12.sp, color = JournalMutedInk, style = TextStyle(platformStyle = NoFontPadding))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(2.0 to "2×", 3.0 to "3×", 5.0 to "5×", 7.0 to "7×").forEach { (freq, label) ->
                            val isFreqSel = detail.weeklyFrequency == freq
                            Box(
                                modifier = Modifier.weight(1f).height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isFreqSel) HighlighterGreen.copy(alpha = 0.45f) else JournalPaper)
                                    .border(1.dp, if (isFreqSel) JournalWritingInk.copy(0.6f) else JournalMutedInk.copy(0.25f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdate(a.copy(leakDetails = a.leakDetails + (leakKey to detail.copy(weeklyFrequency = freq))))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontFamily = TajawalFamily, fontSize = 11.sp,
                                    fontWeight = if (isFreqSel) FontWeight.Bold else FontWeight.Normal,
                                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
                            }
                        }
                    }
                    // Live shock line
                    if (monthly > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isRtl) "💥 ${monthly.toLong()} DH/شهر ← ${(monthly * 12).toLong()} DH/عام" else "💥 ${monthly.toLong()} DH/mois ← ${(monthly * 12).toLong()} DH/an",
                            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = Color(0xFFD32F2F), style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Gambling special handling
        if ("GAMBLING" in a.selectedLeaks) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(HighlighterPink.copy(alpha = 0.18f))
                    .border(1.dp, HighlighterPink.copy(0.35f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(if (isRtl) "🎰 باريات / ألعاب بالفلوس" else "🎰 Paris / Jeux d'argent",
                        fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isRtl)
                            "التكلفة الحقيقية: الخسارة ما خاصهاش تولي سبب باش تزيد مبلغ آخر. الأفضل تحط سقفاً صارماً."
                        else
                            "Règle clé : une perte ne doit jamais déclencher une mise supplémentaire. Fixez un plafond strict.",
                        fontFamily = TajawalFamily, fontSize = 12.sp, color = JournalWritingInk, lineHeight = 18.sp,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section H — Seasonal Expenses
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepHSeasonal(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    val seasonalOptions = listOf(
        "رمضان" to "Ramadan",
        "عيد الأضحى" to "Aïd Al-Adha",
        "عيد الفطر" to "Aïd Al-Fitr",
        "الدخول المدرسي" to "Rentrée scolaire",
        "سفر الصيف" to "Voyage d'été",
        "مناسبات/عرس" to "Fêtes / mariages",
        "التأمين" to "Assurance annuelle",
        "vignette / صيانة سيارة" to "Vignette / entretien auto",
        "علاج أسنان" to "Soins dentaires",
        "صيانة المنزل" to "Entretien logement",
        "ضرائب/رسوم" to "Taxes / frais annuels",
        "هدايا" to "Cadeaux annuels"
    )

    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "المصاريف الموسمية اللي كتفاجئك؟" else "Quelles dépenses saisonnières vous surprennent ?")
        StepSubtitle(if (isRtl) "هاد المصاريف خاصها احتياط شهري — هنا نحسبوه" else "Ces dépenses nécessitent une provision mensuelle — on la calcule ici")
        Spacer(Modifier.height(12.dp))

        seasonalOptions.forEach { (labelAr, labelFr) ->
            val label = if (isRtl) labelAr else labelFr
            val existing = a.seasonalExpenses.find { it.label == labelAr }
            val isSelected = existing != null
            MultiChoiceCard(label, isSelected = isSelected) {
                val newList = if (isSelected) {
                    a.seasonalExpenses.filter { it.label != labelAr }
                } else {
                    a.seasonalExpenses + SeasonalExpenseAnswer(label = labelAr, annualCentimes = 0L, monthDue = 0)
                }
                onUpdate(a.copy(seasonalExpenses = newList))
            }
            if (existing != null) {
                Spacer(Modifier.height(4.dp))
                AmountField(
                    label = if (isRtl) "المبلغ السنوي التقريبي (DH):" else "Montant annuel estimé (DH):",
                    centimes = existing.annualCentimes,
                    onCentimesChange = { amt ->
                        val updated = a.seasonalExpenses.map { se ->
                            if (se.label == labelAr) se.copy(annualCentimes = amt) else se
                        }
                        onUpdate(a.copy(seasonalExpenses = updated))
                    }
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        // Summary reserve
        val totalAnnual = a.seasonalExpenses.sumOf { it.annualCentimes }
        if (totalAnnual > 0) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(HighlighterBlue.copy(alpha = 0.20f))
                    .border(1.dp, HighlighterBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isRtl)
                        "📅 الاحتياط الشهري المطلوب للمصاريف الموسمية: ${totalAnnual / 100 / 12} DH/شهر"
                    else
                        "📅 Provision mensuelle nécessaire : ${totalAnnual / 100 / 12} DH/mois",
                    fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section I — Savings Behavior
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepISavingsBehavior(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "كيف كتدخر عادةً؟" else "Comment épargnez-vous habituellement ?")
        Spacer(Modifier.height(12.dp))
        listOf(
            "SAVE_FIRST" to (if (isRtl) "أول الشهر — قبل أي صرف (الأمثل!)" else "En début de mois — avant toute dépense (idéal !)"),
            "SAVE_MID" to (if (isRtl) "وسط الشهر" else "En milieu de mois"),
            "SAVE_END" to (if (isRtl) "آخر الشهر إذا بقى شي" else "En fin de mois si quelque chose reste"),
            "SAVE_IRREGULAR" to (if (isRtl) "بشكل غير منتظم" else "De façon irrégulière"),
            "SAVE_NEVER" to (if (isRtl) "ما كندخرش حالياً" else "Je n'épargne pas actuellement")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.savingTiming == key) { onUpdate(a.copy(savingTiming = key)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(if (isRtl) "شحال تقدر توفر بارتياح بلا ضغط؟" else "Combien pouvez-vous épargner confortablement sans pression ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        AmountField(
            label = if (isRtl) "التوفير المريح الشهري (DH):" else "Épargne mensuelle confortable (DH):",
            centimes = a.comfortSavingCentimes,
            onCentimesChange = { onUpdate(a.copy(comfortSavingCentimes = it)) }
        )
        Spacer(Modifier.height(8.dp))
        AmountField(
            label = if (isRtl) "الحد الأدنى حتى في شهر صعيب (DH):" else "Minimum absolu même en mois difficile (DH):",
            centimes = a.minimumSavingCentimes,
            onCentimesChange = { onUpdate(a.copy(minimumSavingCentimes = it)) }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section J — Buying Behavior
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepJBuyingBehavior(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "السلوك الشرائي" else "Votre comportement d'achat")
        StepSubtitle(if (isRtl) "ماشي للحكم — لاختيار النصيحة الأنسب ليك" else "Pas pour juger — pour choisir le meilleur conseil adapté")
        Spacer(Modifier.height(12.dp))
        Text(if (isRtl) "قبل حاجة ماشي ضرورية:" else "Pour un achat non essentiel :",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(
            "QUICK" to (if (isRtl) "كنقرر بسرعة" else "Je décide rapidement"),
            "WAIT_A_BIT" to (if (isRtl) "كنستنى شوية" else "J'attends un peu"),
            "COMPARE" to (if (isRtl) "كنقارن ونفكر بتأني" else "Je compare et réfléchis longuement")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.purchaseDecisionStyle == key) { onUpdate(a.copy(purchaseDecisionStyle = key)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(if (isRtl) "التخفيض كيدفعك تشري حاجة ما كنتيش ناوي عليها؟" else "Les soldes vous poussent-ils à acheter ce que vous ne prévoyiez pas ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(0 to (if (isRtl) "نادراً" else "Rarement"), 1 to (if (isRtl) "مرات" else "Parfois"), 2 to (if (isRtl) "بزاف" else "Souvent")).forEach { (v, label) ->
            ChoiceCard(label, isSelected = a.discountTriggerBuying == v) { onUpdate(a.copy(discountTriggerBuying = v)) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section K — User-Protected Preferences
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepKUserLimits(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "شنو ما بغيتيش الخطة تطلب منك تنقصه؟" else "Qu'est-ce que vous ne souhaitez pas que le plan vous demande de réduire ?")
        StepSubtitle(if (isRtl) "هاد الأشياء كتبقى محمية فالتوصيات — كنلقاو بدائل أخرى" else "Ces éléments resteront protégés dans les recommandations")
        Spacer(Modifier.height(12.dp))
        listOf(
            "CAFE" to Pair("القهوة / الفطور" , "Café / petit-déjeuner"),
            "OUTINGS" to Pair("الخروجات مع العائلة والأصدقاء", "Sorties famille/amis"),
            "SPORT" to Pair("الرياضة والصحة", "Sport et santé"),
            "FAMILY_HELP" to Pair("مساعدة العائلة", "Aide à la famille"),
            "TRAVEL" to Pair("السفر (ولو مرة في السنة)", "Voyages (même annuels)"),
            "SHOPPING" to Pair("شوية شوبينغ مريح", "Un peu de shopping"),
            "FOOD_OUT" to Pair("الماكلة برا للاسترخاء", "Manger dehors pour se détendre")
        ).forEach { (key, labels) ->
            val label = if (isRtl) labels.first else labels.second
            val isSelected = key in a.userProtectedPreferences
            MultiChoiceCard(label, isSelected = isSelected) {
                val newList = if (isSelected) a.userProtectedPreferences.filter { it != key }
                              else a.userProtectedPreferences + key
                onUpdate(a.copy(userProtectedPreferences = newList))
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section L — Goal Flexibility
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepLGoalFlexibility(a: QuestionnaireAnswers, isRtl: Boolean, onUpdate: (QuestionnaireAnswers) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "المرونة في الهدف" else "Flexibilité de votre objectif")
        Spacer(Modifier.height(12.dp))
        Text(if (isRtl) "الهدف بالنسبة ليك:" else "Cet objectif est pour vous :",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(
            "ESSENTIAL" to (if (isRtl) "ضروري جداً — لا مجال للتأخير" else "Indispensable — aucun délai acceptable"),
            "IMPORTANT" to (if (isRtl) "مهم — ولكن مرن شوية" else "Important mais un peu flexible"),
            "NICE_TO_HAVE" to (if (isRtl) "nice-to-have — إذا مشى عادي" else "Nice-to-have — si ça se fait naturellement")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.goalImportance == key) { onUpdate(a.copy(goalImportance = key)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(if (isRtl) "التاريخ المستهدف:" else "La date cible :",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(
            "FIXED" to (if (isRtl) "ثابت لا يتغير" else "Fixe, non négociable"),
            "FLEXIBLE_3M" to (if (isRtl) "ممكن نزيد 3 أشهر" else "Peut s'allonger de 3 mois"),
            "FLEXIBLE_6M" to (if (isRtl) "ممكن نزيد 6 أشهر" else "Peut s'allonger de 6 mois"),
            "VERY_FLEXIBLE" to (if (isRtl) "مرن جداً — المهم نوصل" else "Très flexible — l'essentiel est d'y arriver")
        ).forEach { (key, label) ->
            ChoiceCard(label, isSelected = a.deadlineFlexibility == key) { onUpdate(a.copy(deadlineFlexibility = key)) }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(if (isRtl) "واش مستعد تزيد الدخل إذا لزم؟" else "Êtes-vous prêt(e) à augmenter vos revenus si nécessaire ?",
            fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
        Spacer(Modifier.height(6.dp))
        listOf(true to (if (isRtl) "نعم، ممكن نفكر فيه" else "Oui, c'est envisageable"), false to (if (isRtl) "لا، مش الوقت" else "Non, pas pour l'instant")).forEach { (v, label) ->
            ChoiceCard(label, isSelected = a.willingToIncreaseIncome == v) { onUpdate(a.copy(willingToIncreaseIncome = v)) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Review Step — Profile Summary before confirming
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepReview(a: QuestionnaireAnswers, goal: SavingsGoalEntity?, isRtl: Boolean) {
    val income = a.netMonthlyIncomeCentimes / 100
    val essentials = (a.housingCentimes + a.utilitiesCentimes + a.internetPhoneCentimes + a.groceriesCentimes + a.workTransportCentimes + a.healthCentimes + a.educationCentimes + a.insuranceCentimes) / 100
    val debts = a.debtPaymentsCentimes / 100
    val leakCount = a.selectedLeaks.filter { it != "NONE" }.size

    val goalTitle = goal?.title ?: a.goalTitle.ifBlank { if (isRtl) "الهدف المالي" else "Objectif financier" }
    val goalTarget = (goal?.targetAmountCentimes ?: a.goalTargetCentimes) / 100
    val goalMonths = goal?.targetMonths ?: a.goalTargetMonths

    Column(Modifier.fillMaxWidth()) {
        StepTitle(if (isRtl) "🎯 ملخص البروفايل والهدف" else "🎯 Résumé du profil et de l'objectif")
        StepSubtitle(if (isRtl) "هاد المعلومات غادي تضبط الهدف والتحليل ديالك على المقاس" else "Ces informations calibreront votre objectif et votre analyse personnalisée")
        Spacer(Modifier.height(14.dp))

        listOf(
            "🎯 " + (if (isRtl) "الهدف" else "Objectif") to "$goalTitle — $goalTarget DH",
            "📅 " + (if (isRtl) "المدة" else "Durée") to "$goalMonths ${if (isRtl) "شهر" else "mois"}",
            "💰 " + (if (isRtl) "الدخل الشهري" else "Revenu mensuel") to (if (income > 0) "$income DH" else if (isRtl) "غير محدد" else "Non précisé"),
            "🏠 " + (if (isRtl) "الأساسيات المحمية" else "Charges protégées") to (if (essentials > 0) "$essentials DH" else if (isRtl) "0 DH" else "0 DH"),
            "📋 " + (if (isRtl) "الأداءات والديون" else "Mensualités dettes") to (if (debts > 0) "$debts DH" else if (isRtl) "بدون ديون" else "Sans dettes"),
            "💸 " + (if (isRtl) "نقاط التسرب المحددة" else "Fuites identifiées") to (if (leakCount > 0) "$leakCount ${if (isRtl) "باب" else "postes"}" else if (isRtl) "بدون تسرب" else "Aucune fuite"),
            "🔧 " + (if (isRtl) "المرونة" else "Flexibilité") to when (a.deadlineFlexibility) {
                "FIXED" -> if (isRtl) "ثابت" else "Fixe"
                "VERY_FLEXIBLE" -> if (isRtl) "مرن جداً" else "Très flexible"
                else -> if (isRtl) "مرن" else "Flexible"
            }
        ).forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().height(JournalRuleSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontFamily = TajawalFamily, fontSize = 13.sp, color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding))
                Text(value, fontFamily = TajawalFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding))
            }
            Box(Modifier.fillMaxWidth().height(0.8.dp).background(JournalRule.copy(alpha = 0.5f)))
        }

        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(HighlighterGreen.copy(alpha = 0.22f))
                .border(1.dp, HighlighterGreen.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(
                text = if (isRtl)
                    "✅ الضغط على زر الحفظ غادي يولد تشخيصك الكامل على المقاس بدون إنترنت."
                else
                    "✅ En appuyant sur Enregistrer, votre diagnostic complet sera généré en local, sans internet.",
                fontFamily = TajawalFamily, fontSize = 12.5.sp, color = JournalWritingInk,
                lineHeight = 18.sp, style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}
