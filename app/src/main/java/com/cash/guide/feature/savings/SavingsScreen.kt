package com.cash.guide.feature.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.cash.guide.R
import com.cash.guide.data.db.SavingsDepositEntity
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.ui.notebook.DeleteConfirmationDialog
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledCard
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalSectionBadge
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalDotOnRule
import com.cash.guide.ui.notebook.journalTextOnRules
import com.cash.guide.ui.notebook.journalVisualOnRule
import com.cash.guide.ui.notebook.snapHeightToRule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a monetary amount safely in RTL and LTR without BiDi number scrambling.
 * Encloses the digits in Unicode Left-to-Right Marks (\u200E) and non-breaking spaces (\u00A0).
 */
fun formatSavingsMoney(amount: Long, isRtl: Boolean): String {
    val numStr = JournalLedgerManager.formatFrenchNumber(amount.toString())
    val unit = if (isRtl) "درهم" else "DH"
    return "\u200E$numStr\u200E $unit"
}

fun formatSavingsMoney(amount: Double, isRtl: Boolean): String {
    return formatSavingsMoney(amount.toLong(), isRtl)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var depositToDelete by remember { mutableStateOf<SavingsDepositEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
    ) {
        JournalRuledDocument(
            modifier = Modifier.fillMaxSize(),
            clearFocusOnTap = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Centered 3 Tabs (أهدافي | التحليل | المقالات) fitting between 2 lines (29dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .height(JournalRuleSpacing)
                            .clip(RoundedCornerShape(6.dp))
                            .background(JournalMutedInk.copy(alpha = 0.07f))
                            .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(6.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 1: أهدافي / Mes objectifs
                        val isTab1 = uiState.selectedTab == SavingsTab.PLAN
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                                .background(if (isTab1) HighlighterYellow.copy(alpha = 0.55f) else Color.Transparent)
                                .clickable { viewModel.selectTab(SavingsTab.PLAN) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.savings_tab_goals),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isTab1) JournalWritingInk else JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Divider line 1
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(JournalRule.copy(alpha = 0.60f))
                        )

                        // Tab 2: التحليل / Diagnostic
                        val isTab2 = uiState.selectedTab == SavingsTab.DIAGNOSTIC
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isTab2) HighlighterYellow.copy(alpha = 0.55f) else Color.Transparent)
                                .clickable { viewModel.selectTab(SavingsTab.DIAGNOSTIC) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.savings_tab_diagnostic),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = if (isTab2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isTab2) JournalWritingInk else JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Divider line 2
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(JournalRule.copy(alpha = 0.60f))
                        )

                        // Tab 3: المقالات / Conseils
                        val isTab3 = uiState.selectedTab == SavingsTab.TIPS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                                .background(if (isTab3) HighlighterYellow.copy(alpha = 0.55f) else Color.Transparent)
                                .clickable { viewModel.selectTab(SavingsTab.TIPS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.savings_tab_tips),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = if (isTab3) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isTab3) JournalWritingInk else JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Tab Content
                when (uiState.selectedTab) {
                    SavingsTab.PLAN -> {
                        val activeGoal = uiState.activeGoal ?: uiState.goals.firstOrNull()
                        if (activeGoal == null || uiState.goals.isEmpty()) {
                            SavingsEmptyState(
                                isRtl = isRtl,
                                onStartWizard = { viewModel.openQuestionnaire(null) }
                            )
                        } else {
                            if (uiState.goals.size > 1) {
                                GoalSwitcherChips(
                                    goals = uiState.goals,
                                    activeGoalId = activeGoal.id,
                                    onSelectGoal = { viewModel.selectGoal(it) },
                                    onNewPlan = { viewModel.openQuestionnaire(null) },
                                    isRtl = isRtl
                                )
                                Spacer(modifier = Modifier.height(JournalRuleSpacing))
                            }

                            HeroGoalSection(
                                goal = activeGoal,
                                diagnosis = uiState.diagnosis,
                                deposits = uiState.activeGoalDeposits,
                                isRtl = isRtl,
                                onAddDeposit = { viewModel.openDepositSheet(activeGoal) },
                                onEditGoal = { viewModel.openQuestionnaire(activeGoal) },
                                onDeleteGoal = { goalToDelete = activeGoal },
                                onNewPlan = { viewModel.openQuestionnaire(null) },
                                onDeleteDeposit = { depositToDelete = it },
                                onApplySuggestedDuration = { newMonths ->
                                    viewModel.applySuggestedDuration(activeGoal.id, newMonths)
                                }
                            )
                        }
                    }
                    SavingsTab.DIAGNOSTIC -> {
                        val activeGoal = uiState.activeGoal ?: uiState.goals.firstOrNull()
                        if (activeGoal == null || uiState.goals.isEmpty()) {
                            SavingsDiagnosticEmptyState(
                                isRtl = isRtl,
                                onStartWizard = { viewModel.openQuestionnaire(null) }
                            )
                        } else {
                            if (uiState.goals.size > 1) {
                                GoalSwitcherChips(
                                    goals = uiState.goals,
                                    activeGoalId = activeGoal.id,
                                    onSelectGoal = { viewModel.selectGoal(it) },
                                    onNewPlan = { viewModel.openQuestionnaire(null) },
                                    isRtl = isRtl
                                )
                                Spacer(modifier = Modifier.height(JournalRuleSpacing))
                            }

                            SavingsDiagnosticFullSection(
                                goal = activeGoal,
                                diagnosis = uiState.diagnosis,
                                fullDiagnostic = uiState.fullDiagnosticResult,
                                aiCoachAdvice = uiState.aiCoachAdvice,
                                isAiCoachLoading = uiState.isAiCoachLoading,
                                isRtl = isRtl,
                                onRequestAiCoachAdvice = { viewModel.requestAiCoachAdvice(activeGoal, isRtl) },
                                onClearAiCoachAdvice = { viewModel.clearAiCoachAdvice() },
                                onApplySuggestedDuration = { newMonths ->
                                    viewModel.applySuggestedDuration(activeGoal.id, newMonths)
                                },
                                onOpenQuestionnaire = { viewModel.openQuestionnaire(activeGoal) },
                                onOpenSimulator = { viewModel.openSimulator() },
                                onOpenCheckIn = { viewModel.openMonthlyCheckIn() }
                            )
                        }
                    }
                    SavingsTab.TIPS -> {
                        val activeGoal = uiState.activeGoal ?: uiState.goals.firstOrNull()
                        SavingsArticlesLibrarySection(
                            activeGoal = activeGoal,
                            recommendedArticleIds = uiState.fullDiagnosticResult?.recommendedContentIds ?: emptyList(),
                            isRtl = isRtl
                        )
                    }
                }

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }

    // Wizard BottomSheet
    if (uiState.isWizardOpen) {
        SavingsWizardSheet(
            formState = uiState.wizardForm,
            onPresetSelected = { preset, amt -> viewModel.setWizardGoalPreset(preset, amt) },
            onCustomTitleChanged = { viewModel.setWizardCustomTitle(it) },
            onTargetAmountChanged = { viewModel.setWizardTargetAmount(it) },
            onDurationChanged = { viewModel.setWizardDuration(it) },
            onSalarySelected = { bracket, amt -> viewModel.setWizardSalary(bracket, amt) },
            onCustomSalaryChanged = { viewModel.setWizardCustomSalary(it) },
            onEssentialsSelected = { viewModel.setWizardEssentials(it) },
            onLeisureSelected = { viewModel.setWizardLeisure(it) },
            onLeakDailyCostChanged = { viewModel.setWizardLeakDailyCost(it) },
            onLeakDaysPerWeekChanged = { viewModel.setWizardLeakDaysPerWeek(it) },
            onSavingsStyleSelected = { viewModel.setWizardSavingsStyle(it) },
            onInitialAmountChanged = { viewModel.setWizardInitialAmount(it) },
            onNextStep = { viewModel.nextWizardStep() },
            onPrevStep = { viewModel.prevWizardStep() },
            onClose = { viewModel.closeWizard() }
        )
    }

    // Add Deposit Sheet
    uiState.depositTargetGoal?.let { targetGoal ->
        AddDepositSheet(
            goal = targetGoal,
            onDismiss = { viewModel.closeDepositSheet() },
            onConfirmDeposit = { amtCentimes, note ->
                viewModel.addDeposit(targetGoal.id, amtCentimes, note)
            }
        )
    }

    // Delete Goal Confirmation Dialog
    goalToDelete?.let { goal ->
        DeleteConfirmationDialog(
            title = if (isRtl) "مسح هاد الهدف؟" else "Supprimer cet objectif ?",
            body = if (isRtl) {
                "واش متأكد باغي تمسح هدف \"${goal.title}\" وجميع الدفعات ديالو؟"
            } else {
                "Voulez-vous vraiment supprimer l'objectif \"${goal.title}\" et tous ses versements associés ?"
            },
            onConfirmDelete = {
                viewModel.deleteGoal(goal.id)
                goalToDelete = null
            },
            onDismiss = { goalToDelete = null }
        )
    }

    // Delete Deposit Confirmation Dialog
    depositToDelete?.let { dep ->
        DeleteConfirmationDialog(
            title = if (isRtl) "مسح هاد الدفعة؟" else "Supprimer ce versement ?",
            body = if (isRtl) {
                "واش متأكد باغي تمسح هاد الدفعة من سجل التوفير؟"
            } else {
                "Voulez-vous vraiment supprimer ce versement de l'historique ?"
            },
            onConfirmDelete = {
                viewModel.deleteDeposit(dep.id)
                depositToDelete = null
            },
            onDismiss = { depositToDelete = null }
        )
    }

    // Full Financial Interview Questionnaire Sheet
    val activeGoalForSheets = uiState.activeGoal ?: uiState.goals.firstOrNull()
    if (uiState.isQuestionnaireOpen) {
        FinancialQuestionnaireSheet(
            answers = uiState.questionnaireState,
            goal = activeGoalForSheets,
            onUpdateAnswers = { viewModel.updateQuestionnaireAnswers(it) },
            onSubmit = { viewModel.submitQuestionnaire() },
            onClose = { viewModel.closeQuestionnaire() }
        )
    }

    // 7-Mode Interactive Diagnostic Simulator Sheet
    if (uiState.isSimulatorOpen && uiState.fullDiagnosticResult != null && activeGoalForSheets != null) {
        DiagnosticSimulatorSheet(
            diagnostic = uiState.fullDiagnosticResult!!,
            goal = activeGoalForSheets,
            onClose = { viewModel.closeSimulator() }
        )
    }

    // Monthly Check-in Sheet
    if (uiState.isCheckInOpen && activeGoalForSheets != null) {
        MonthlyCheckInSheet(
            goal = activeGoalForSheets,
            onSubmit = { answers -> viewModel.submitCheckIn(activeGoalForSheets, answers) },
            onClose = { viewModel.closeCheckIn() }
        )
    }
}


/**
 * Clean, lightweight Empty State sitting directly on blue notebook lines without heavy box.
 */
@Composable
private fun SavingsEmptyState(
    isRtl: Boolean,
    onStartWizard: () -> Unit
) {
    Spacer(modifier = Modifier.height(JournalRuleSpacing))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(HighlighterYellow.copy(alpha = 0.30f)),
            contentAlignment = Alignment.Center
        ) {
            HisabiSketchIcon(
                symbol = HisabiSymbol.Coin,
                contentDescription = null,
                tint = JournalWritingInk,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isRtl) "خطة التوفير الذكية" else "Plan d'Épargne Intelligent",
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRtl) {
                "ماعندك حتى هدف توفير دابا.. حدد هدفك وجاوب على استبيان بسيط والتطبيق غادي يقاد ليك خطة مالية مخصصة على قياسك."
            } else {
                "Vous n'avez pas encore d'objectif actif. Définissez votre objectif et vos finances pour bâtir un plan sur-mesure !"
            },
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = 14.sp,
            color = JournalMutedInk,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Clean action button: Soft pastel green, NO outline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HighlighterGreen.copy(alpha = 0.38f))
                .clickable(role = Role.Button, onClick = onStartWizard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRtl) "+ ابدأ تحديد الهدف والخطة 🚀" else "+ Définir un objectif et un plan 🚀",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

/**
 * Chip row to switch between multiple goals if the user created more than one.
 */
@Composable
private fun GoalSwitcherChips(
    goals: List<SavingsGoalEntity>,
    activeGoalId: String,
    onSelectGoal: (String) -> Unit,
    onNewPlan: () -> Unit,
    isRtl: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        goals.forEach { g ->
            val isSelected = g.id == activeGoalId
            val categoryColor = getGoalCategoryColor(g.colorTag)
            Box(
                modifier = Modifier
                    .height(JournalRuleSpacing)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) categoryColor.copy(alpha = 0.16f) else JournalMutedInk.copy(alpha = 0.08f))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) categoryColor else JournalRule.copy(alpha = 0.50f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectGoal(g.id) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = g.title,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isSelected) JournalWritingInk else JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        // Quick add new goal chip [+]
        Box(
            modifier = Modifier
                .height(JournalRuleSpacing)
                .clip(RoundedCornerShape(6.dp))
                .background(HighlighterYellow.copy(alpha = 0.35f))
                .border(
                    width = 1.dp,
                    color = JournalRule.copy(alpha = 0.50f),
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onNewPlan() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontFamily = PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

/**
 * Pure Notebook Blue-Ruled Goal Section:
 * All elements sit directly on the 29dp journal rules with zero enclosing card boxes.
 *
 * 1. Goal title line with margin dot (Rule 1)
 * 2. na9ez star (Skip 1 blue line)
 * 3. Target Amount in bold category color (Rule 2)
 * 4. na9ez star (Skip 1 blue line)
 * 5. Progress bar: 0% on LEFT, 100% on RIGHT, current % shown (Rule 3)
 * 6. na9ez star (Skip 1 blue line)
 * 7. Saved vs Remaining (Rule 4)
 * 8. na9ez star (Skip 1 blue line)
 * 9. Monthly requirement & optional smart pacing advice (Rule 5)
 * 10. na9ez star (Skip 1 blue line)
 * 11. Centered action button (+ زيد مبلغ توفير) & delete icon (Rule 6)
 * 12. na9ez star (Skip 1 blue line)
 * 13. Centered button (+ إضافة هدف جديد) (Rule 7)
 * 14. na9ez star (Skip 1 blue line)
 * 15. Living Deposit History (سجل التوفير) on notebook lines
 */
@Composable
private fun HeroGoalSection(
    goal: SavingsGoalEntity,
    diagnosis: PlanDiagnosis?,
    deposits: List<SavingsDepositEntity>,
    isRtl: Boolean,
    onAddDeposit: () -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onNewPlan: () -> Unit,
    onDeleteDeposit: (SavingsDepositEntity) -> Unit,
    onApplySuggestedDuration: (Int) -> Unit
) {
    val targetDh = goal.targetAmountCentimes / 100.0
    val currentDh = goal.currentAmountCentimes / 100.0
    val remainingDh = (targetDh - currentDh).coerceAtLeast(0.0)
    val progressRatio = if (targetDh > 0) (currentDh / targetDh).coerceIn(0.0, 1.0).toFloat() else 0f
    val progressPercent = (progressRatio * 100).toInt()
    val months = goal.targetMonths.coerceAtLeast(1)
    val monthlyReq = if (months > 0) remainingDh / months else remainingDh
    val dailyReq = monthlyReq / 30.0

    val categoryColor = getGoalCategoryColor(goal.colorTag)

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- 1. TARGET AMOUNT (Rule 1) ---
        // Centered number (40sp bold) with smaller currency unit (18sp bold) sitting directly on the blue line at the top
        val formattedTarget = remember(targetDh, isRtl, categoryColor) {
            val numStr = JournalLedgerManager.formatFrenchNumber(targetDh.toLong().toString())
            val unitStr = if (isRtl) "درهم" else "DH"
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, color = categoryColor)) {
                    append("\u200E$numStr\u200E")
                }
                append(" ")
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = categoryColor)) {
                    append(unitStr)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formattedTarget,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 2. PROGRESS BAR (Rule 2) ---
        // Progress bar sits directly on the blue line: dynamic % on start, 100% on end, bar in middle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dynamic percentage (starts at 0%, updates to 5%, 10%, 15%, etc.)
            Text(
                text = "$progressPercent%",
                fontFamily = PatrickHandFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (progressPercent > 0) categoryColor else JournalWritingInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )

            // Middle: Progress bar track resting on the blue line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .offset(y = (-1).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(JournalRule.copy(alpha = 0.35f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width * progressRatio
                    if (barWidth > 0) {
                        val startX = if (isRtl) size.width - barWidth else 0f
                        drawRoundRect(
                            color = categoryColor,
                            topLeft = Offset(startX, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // 100%
            Text(
                text = "100%",
                fontFamily = PatrickHandFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = JournalWritingInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 3. STATS: OBJECTIF, ÉPARGNÉ, RESTE, REQUIS PAR MOIS, REQUIS PAR JOUR (5 rows with connected notebook dots) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeW = 1.4.dp.toPx()
                    val dotRadiusPx = 3.5.dp.toPx()
                    val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                    val rowHeightPx = JournalRuleSpacing.toPx()
                    val startY = rowHeightPx - dotRadiusPx
                    val endY = 4 * rowHeightPx + (rowHeightPx - dotRadiusPx)

                    drawLine(
                        color = JournalRule.copy(alpha = 0.60f),
                        start = Offset(guideX, startY),
                        end = Offset(guideX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
        ) {
            // Line 1: Objectif
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = categoryColor)
                }
                Text(
                    text = if (isRtl) "الهدف: ${goal.title}" else "Objectif : ${goal.title}",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Line 2: Épargné
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = Color(0xFF00796B))
                }
                Text(
                    text = if (isRtl) "وفّرتي: ${formatSavingsMoney(currentDh, true)}" else "Épargné : ${formatSavingsMoney(currentDh, false)}",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color(0xFF00796B),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Line 3: Reste
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = Color(0xFFE65100))
                }
                Text(
                    text = if (isRtl) "باقي ليك: ${formatSavingsMoney(remainingDh, true)}" else "Reste : ${formatSavingsMoney(remainingDh, false)}",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Line 4: Requis par mois
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = categoryColor)
                }
                Text(
                    text = if (isRtl) {
                        "المطلوب في الشهر: ${formatSavingsMoney(monthlyReq, true)}"
                    } else {
                        "Requis par mois : ${formatSavingsMoney(monthlyReq, false)}"
                    },
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Line 5: Requis par jour
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = categoryColor)
                }
                Text(
                    text = if (isRtl) {
                        "المطلوب في اليوم: ${formatSavingsMoney(dailyReq, true)}"
                    } else {
                        "Requis par jour : ${formatSavingsMoney(dailyReq, false)}"
                    },
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        // Smart Pacing Advisory (1 rule line if active)
        if (diagnosis != null && diagnosis.suggestedMonths > 0) {
            Spacer(modifier = Modifier.height(JournalRuleSpacing))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HighlighterYellow.copy(alpha = 0.25f))
                    .border(1.dp, JournalRule.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .clickable { onApplySuggestedDuration(diagnosis.suggestedMonths) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val newEst = targetDh / diagnosis.suggestedMonths
                Text(
                    text = if (isRtl) {
                        "💡 نصيحة: مدد لـ ${diagnosis.suggestedMonths} شهر (${formatSavingsMoney(newEst, true)}/ش)"
                    } else {
                        "💡 Conseil : Passer à ${diagnosis.suggestedMonths} mois (${formatSavingsMoney(newEst, false)}/m)"
                    },
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                Text(
                    text = if (isRtl) "تطبيق ✨" else "Appliquer ✨",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF00796B),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 5. ACTION BUTTONS ROW: + Ajouter épargne & Red Delete icon ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button: + Ajouter épargne 💰
            Box(
                modifier = Modifier
                    .height(JournalRuleSpacing)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HighlighterGreen.copy(alpha = 0.40f))
                    .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onAddDeposit)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) "+ زيد مبلغ توفير 💰" else "+ Ajouter épargne 💰",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Edit Goal / Plan Icon button
            Box(
                modifier = Modifier
                    .size(JournalRuleSpacing)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HighlighterYellow.copy(alpha = 0.35f))
                    .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onEditGoal),
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Pencil,
                    contentDescription = "Edit Goal",
                    tint = JournalWritingInk,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Goal Icon button (RED icon & red tint)
            Box(
                modifier = Modifier
                    .size(JournalRuleSpacing)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE53935).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onDeleteGoal),
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Trash,
                    contentDescription = "Delete Goal",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 6. LIVING DEPOSIT HISTORY (سجل التوفير) ---
        // Placed directly below the goal's action buttons
        LivingDepositHistory(
            deposits = deposits,
            isRtl = isRtl,
            onDeleteDeposit = onDeleteDeposit
        )

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 7. BUTTON: + Ajouter un nouvel objectif (At the very bottom!) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(JournalRuleSpacing)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HighlighterYellow.copy(alpha = 0.35f))
                    .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onNewPlan)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) "+ إضافة هدف جديد" else "+ Ajouter un nouvel objectif",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Living Deposit History: Displays real deposits on clean notebook rules with dots sitting on the blue line.
 */
@Composable
private fun LivingDepositHistory(
    deposits: List<SavingsDepositEntity>,
    isRtl: Boolean,
    onDeleteDeposit: (SavingsDepositEntity) -> Unit
) {
    val dateFormatter = remember(isRtl) {
        SimpleDateFormat("d MMMM yyyy", if (isRtl) Locale.forLanguageTag("ar") else Locale.FRENCH)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Full-width Header Bar spanning edge-to-edge (chadda men jenb 7tal jenb) with green background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .clip(RoundedCornerShape(6.dp))
                .background(HighlighterGreen.copy(alpha = 0.40f))
                .border(1.dp, JournalRule.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title on start (right in RTL)
            Text(
                text = if (isRtl) "سجل التوفير" else "Historique des versements",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )

            // Trailing badge: count of deposits
            val countText = if (isRtl) {
                if (deposits.size == 1) "دفعة واحدة" else "${deposits.size} دفعات"
            } else {
                "${deposits.size} versement(s)"
            }
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(JournalPaper.copy(alpha = 0.75f))
                    .border(0.8.dp, JournalRule.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countText,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF00796B),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        if (deposits.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRtl) {
                        "مازال ما سجلتي حتى دفعة لهاد الهدف ✍️"
                    } else {
                        "Aucun versement enregistré pour le moment ✍️"
                    },
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 13.sp,
                    color = JournalMutedInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        } else {
            deposits.forEach { deposit ->
                val dateStr = try {
                    dateFormatter.format(Date(deposit.dateEpochMs))
                } catch (e: Exception) {
                    ""
                }
                val amountDh = deposit.amountCentimes / 100.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Margin dot sitting directly on the blue line + Date & optional Note
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(7.dp)) {
                            drawCircle(color = Color(0xFF00796B))
                        }

                        Text(
                            text = if (deposit.note.isNotBlank()) "$dateStr (${deposit.note})" else dateStr,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = JournalWritingInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.journalBaselineOnRule(),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }

                    // Amount + Delete icon
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "+${formatSavingsMoney(amountDh, isRtl)}",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF00796B),
                            modifier = Modifier.journalBaselineOnRule(),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )

                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(JournalRuleSpacing)
                                .clickable { onDeleteDeposit(deposit) },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Trash,
                                contentDescription = "Delete Deposit",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .size(14.dp)
                                    .offset(y = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a restrained, tasteful color based on the goal tag.
 */
private fun getGoalCategoryColor(colorTag: String): Color {
    return when (colorTag) {
        "BLUE" -> Color(0xFF1976D2)   // Car
        "GREEN" -> Color(0xFF2E7D32)  // House
        "AMBER" -> Color(0xFFE65100)  // Emergency
        "PURPLE" -> Color(0xFF6A1B9A) // Business / Project
        else -> Color(0xFF00796B)     // Default
    }
}

/**
 * Diagnostic Empty State: displayed when no goal exists yet.
 * Strictly aligned on the 29dp blue notebook lines.
 */
@Composable
private fun SavingsDiagnosticEmptyState(
    isRtl: Boolean,
    onStartWizard: () -> Unit
) {
    Spacer(modifier = Modifier.height(JournalRuleSpacing))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon (2 rules: 58dp)
        Box(
            modifier = Modifier
                .height(JournalRuleSpacing * 2)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(HighlighterYellow.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Lightbulb,
                    contentDescription = null,
                    tint = JournalWritingInk,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Title row (Rule 1: 29dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) "التحليل المالي والتشخيص" else "Diagnostic & Analyse Personnalisée",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = JournalWritingInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // Subtitle line 1 (Rule 2: 29dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) {
                    "باش نعطيوك تشخيص دقيق لمصاريفك ونحددو نقط الاستنزاف (القهاوي، الشوبينغ...)"
                } else {
                    "Pour obtenir un diagnostic précis de vos dépenses et identifier vos fuites"
                },
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                color = JournalMutedInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // Subtitle line 2 (Rule 3: 29dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) {
                    "ونقترحو عليك خطة التقشف المناسبة، خاصك تنشئ هدف مالي أولاً."
                } else {
                    "et recevoir un plan d'austérité adapté, créez d'abord un objectif."
                },
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                color = JournalMutedInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // Spacer (Rule 4: 29dp)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Action button row (Rule 5: 29dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HighlighterYellow.copy(alpha = 0.50f))
                    .border(1.dp, JournalWritingInk.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .clickable { onStartWizard() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) "+ إنشاء هدف وبدء التشخيص" else "+ Créer un objectif & démarrer",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Actionable Personalized Advice Card ("Ordonnance Financière")
 * Calm, elegant notebook note styling with natural paper background, subtle ruled border,
 * clear step numbering, and clean impact badge.
 * Snapped to notebook blue rules via snapHeightToRule.
 */
@Composable
private fun DiagnosticAdviceCardItem(
    stepIndex: Int,
    card: DiagnosticAdviceCard,
    isRtl: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .snapHeightToRule()
            .clip(RoundedCornerShape(8.dp))
            .background(JournalPaper)
            .border(1.dp, JournalRule.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Top header: Step number badge + Emoji + Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Step badge [ 1 ], [ 2 ], etc.
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(JournalMutedInk.copy(alpha = 0.08f))
                        .border(0.8.dp, JournalRule.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepIndex",
                        fontFamily = PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                Text(
                    text = "${card.iconEmoji} " + (if (isRtl) card.titleAr else card.titleFr),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Detailed action advice in clear, readable Darija/French
        Text(
            text = if (isRtl) card.detailedAdviceAr else card.detailedAdviceFr,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
            color = JournalWritingInk.copy(alpha = 0.88f),
            style = TextStyle(platformStyle = NoFontPadding)
        )

        // Concrete impact tag at bottom (quiet and clean)
        val impactText = if (isRtl) card.concreteImpactAr else card.concreteImpactFr
        if (impactText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(HighlighterGreen.copy(alpha = 0.15f))
                        .border(0.8.dp, HighlighterGreen.copy(alpha = 0.40f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🎯 " + (if (isRtl) "الأثر: " else "Impact : ") + impactText,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}

/**
 * Pillar 3: "Hdi Rassek" — Default Psychological Traps (حضي راسك)
 * 4 fundamental financial traps in Morocco: 24h impulse rule, micro-expenses, card vs cash pain, fake promos.
 */
@Composable
private fun HdiRassekSection(isRtl: Boolean) {
    val traps = listOf(
        Triple(
            "🪤 " + (if (isRtl) "فخ النزوة والشراء اللحظي (قاعدة 24 ساعة)" else "Piège de l'achat impulsif (Règle des 24h)"),
            if (isRtl) "أي رغبة شراء فايتة 150 DH وما كانتش مبرمجة: تسنى يوماً كاملاً (24 ساعة) قبل ما تخلص. 80% من النزوات كتبرد وتتلاشى ثاني يوم."
            else "Tout achat coup de cœur imprévu > 150 DH : imposez-vous 24h d'attente. 80% des envies disparaissent le lendemain.",
            HighlighterYellow
        ),
        Triple(
            "🪤 " + (if (isRtl) "فخ المصاريف المجهرية (\"راها غير 20 درهم\")" else "Piège des micro-dépenses (\"C'est juste 20 DH\")"),
            if (isRtl) "20 درهم كل نهار ف كماليات عشوائية كتعطي 600 DH فالشهر و 7 200 DH فالعام! تقطيرة ب تقطيرة كيحمل الواد أو كيتثقب الصندوق."
            else "20 DH par jour en extras anodins font 600 DH/mois et 7 200 DH/an ! Ce sont les petits trous qui coulent les grands navires.",
            HighlighterPink
        ),
        Triple(
            "🪤 " + (if (isRtl) "فخ الأداء بالبطاقة البنكية (غياب ألم الكاش)" else "Piège de la carte bancaire (Absence de douleur du cash)"),
            if (isRtl) "الدفع بالبطاقة كيخلي الدماغ ما كيحسش بخروج الفلوس. سحب ميزانية المصروف الأسبوعي نقداً (Cash) ف أظرفة وخلي الكارط ف الدار."
            else "Payer par carte masque la douleur de dépenser. Retirez votre argent de poche en espèces chaque semaine et laissez la carte chez vous.",
            HighlighterBlue
        ),
        Triple(
            "🪤 " + (if (isRtl) "فخ التخفيضات والصولد الوهمي (Promo Trap)" else "Piège des fausses promos (Promo Trap)"),
            if (isRtl) "إلى شريتي حاجة بـ 300 DH عوض 500 DH بلا ما تكون محتاجها، راك ما وفرتيش 200 DH.. راك ضيعتي 300 DH! شري بالورقة والستيلو فقط."
            else "Acheter à 300 DH un article remisé de 500 DH dont vous n'avez pas besoin, ce n'est pas 200 DH d'économisés, c'est 300 DH de perdus.",
            HighlighterGreen
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .snapHeightToRule()
            .clip(RoundedCornerShape(8.dp))
            .background(JournalPaper)
            .border(1.dp, Color(0xFFC62828).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        traps.forEach { (title, desc, _) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = desc,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = JournalWritingInk.copy(alpha = 0.85f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Pillar 4: Inline Interactive Diagnostic Calculator (حاسبة السيناريوهات التفاعلية)
 * Direct on-page scenario testing for leaks, timeline extension, and extra contributions.
 */
@Composable
private fun InlineDiagnosticCalculator(
    fullDiagnostic: FullDiagnosticResult,
    goal: SavingsGoalEntity,
    isRtl: Boolean,
    onOpenFullSimulator: () -> Unit
) {
    var selectedScenario by remember { mutableStateOf(0) }
    val m = fullDiagnostic.metrics
    val targetDh = goal.targetAmountCentimes / 100.0
    val currentDh = goal.currentAmountCentimes / 100.0
    val remainingDh = (targetDh - currentDh).coerceAtLeast(0.0)
    val currentMonths = goal.targetMonths.coerceAtLeast(1)
    val reqMonthlyDh = (m.requiredMonthlyCentimes / 100.0).coerceAtLeast(1.0)

    val totalLeaksMonthly = fullDiagnostic.topLeaks.sumOf { it.monthlyDrainDh }
    val leakSavingsMonthly = (totalLeaksMonthly * 0.50).toInt()
    val leakSavingsAnnual = leakSavingsMonthly * 12

    val extendedMonths = currentMonths + 3
    val extendedMonthlyReq = if (extendedMonths > 0) (remainingDh / extendedMonths).toLong() else 0L
    val extendedDrop = (reqMonthlyDh - extendedMonthlyReq).toInt().coerceAtLeast(0)

    val boostSavingsDh = 200
    val boostedMonthly = (reqMonthlyDh + boostSavingsDh).toInt()
    val boostedMonths = if (boostedMonthly > 0) Math.ceil(remainingDh / boostedMonthly).toInt().coerceAtLeast(1) else currentMonths
    val monthsGained = (currentMonths - boostedMonths).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .snapHeightToRule()
            .clip(RoundedCornerShape(8.dp))
            .background(JournalPaper)
            .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Scenario Switcher Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val scenarios = listOf(
                "✂️ " + (if (isRtl) "نقص التسربات" else "Moins de fuites"),
                "📅 " + (if (isRtl) "تمديد المدة" else "+3 mois"),
                "📈 " + (if (isRtl) "زيادة التوفير" else "+200 DH")
            )
            scenarios.forEachIndexed { idx, label ->
                val isSelected = selectedScenario == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) HighlighterYellow.copy(alpha = 0.55f) else JournalMutedInk.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (isSelected) JournalWritingInk.copy(alpha = 0.40f) else JournalRule.copy(alpha = 0.35f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedScenario = idx },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live calculation result box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (selectedScenario) {
                        0 -> HighlighterGreen.copy(alpha = 0.18f)
                        1 -> HighlighterYellow.copy(alpha = 0.22f)
                        else -> HighlighterBlue.copy(alpha = 0.18f)
                    }
                )
                .border(
                    0.8.dp,
                    when (selectedScenario) {
                        0 -> HighlighterGreen.copy(alpha = 0.45f)
                        1 -> HighlighterYellow.copy(alpha = 0.50f)
                        else -> HighlighterBlue.copy(alpha = 0.45f)
                    },
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (selectedScenario) {
                    0 -> {
                        Text(
                            text = if (isRtl) "✨ توفير فوري: +$leakSavingsMonthly DH/شهر (+$leakSavingsAnnual DH فالعام)"
                            else "✨ Économie : +$leakSavingsMonthly DH/mois (+$leakSavingsAnnual DH/an)",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) {
                                if (totalLeaksMonthly > 0) {
                                    val coverage = ((leakSavingsMonthly.toDouble() / reqMonthlyDh) * 100).toInt().coerceAtMost(100)
                                    "تقليص مصاريف القهوة والخرجات للنصف كيغطي $coverage% من القسط المطلوب لهدفك بلا ما تزيد سنتيم من مدخولك!"
                                } else {
                                    "تقليص المصاريف المرنة كيوفر هامش أمان إضافي كيحميك من أي طارئ."
                                }
                            } else {
                                "Réduire vos fuites de moitié couvre une grande partie de la mensualité sans effort supplémentaire."
                            },
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = JournalWritingInk.copy(alpha = 0.88f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    1 -> {
                        Text(
                            text = if (isRtl) "✨ القسط الجديد: $extendedMonthlyReq DH/شهر (-$extendedDrop DH/شهر)"
                            else "✨ Nouvelle mensualité : $extendedMonthlyReq DH/mois (-$extendedDrop DH/mois)",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) "تمديد الأجل من $currentMonths أشهر إلى $extendedMonths أشهر كيخفف الضغط الشهري ويخلي الخطة واقعية ومريحة 100% بدون أي تضييق على مصاريفك الأساسية."
                            else "Allonger de $currentMonths à $extendedMonths mois réduit la pression mensuelle et garantit un effort soutenable.",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = JournalWritingInk.copy(alpha = 0.88f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    else -> {
                        Text(
                            text = if (isRtl) "✨ ربح الوقت: غتوصل لهدفك فـ $boostedMonths أشهر (ربح $monthsGained أشهر!)"
                            else "✨ Gain de temps : objectif atteint en $boostedMonths mois (gain de $monthsGained mois !)",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) "بزيادة 200 درهم شهرياً من التسربات المسترجعة، كتسرّع وتيرة التوفير وكتوصل لـ ${formatSavingsMoney(goal.targetAmountCentimes / 100, isRtl)} قبل الوقت المحدد."
                            else "En ajoutant 200 DH/mois récupérés sur les dépenses superflues, vous atteignez votre objectif beaucoup plus vite.",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = JournalWritingInk.copy(alpha = 0.88f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Button to open full advanced sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(JournalPaper)
                .border(1.dp, JournalRule.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .clickable { onOpenFullSimulator() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRtl) "🔮 فتح الحاسبة المتقدمة بـ 7 أوضاع ↗" else "🔮 Ouvrir le simulateur complet 7 modes ↗",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

/**
 * Standardized 4-Pillar Diagnostic Section reflecting:
 * 1. Diagnostic & Declared Data (فين كيمشيو الفلوس؟) on 100% blue notebook lines.
 * 2. Practical Tailored Solutions (حلول عملية مخصصة).
 * 3. Hdi Rassek (حضي راسك - الفخاخ النفسية ومصائد النزوة).
 * 4. Inline Interactive Calculator (حاسبة السيناريوهات التفاعلية).
 */
@Composable
private fun ProfessionalDiagnosticSection(
    goal: SavingsGoalEntity,
    diagnostic: FullDiagnosticResult,
    isRtl: Boolean,
    onOpenQuestionnaire: () -> Unit,
    onOpenSimulator: () -> Unit,
    onOpenCheckIn: () -> Unit
) {
    val m = diagnostic.metrics
    val statusColor = when (m.feasibility) {
        GoalFeasibility.COMFORTABLE, GoalFeasibility.FEASIBLE -> HighlighterGreen
        GoalFeasibility.TIGHT -> HighlighterYellow
        GoalFeasibility.AGGRESSIVE, GoalFeasibility.UNSAFE_NOW -> HighlighterPink
    }
    val status = when (m.feasibility) {
        GoalFeasibility.COMFORTABLE -> if (isRtl) "مريح" else "Confortable"
        GoalFeasibility.FEASIBLE -> if (isRtl) "واقعي" else "Réaliste"
        GoalFeasibility.TIGHT -> if (isRtl) "خاصو تعديل صغير" else "À ajuster"
        GoalFeasibility.AGGRESSIVE -> if (isRtl) "الضغط عالي" else "Trop exigeant"
        GoalFeasibility.UNSAFE_NOW -> if (isRtl) "الأمان أولاً" else "Sécurité d'abord"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) "التشخيص ديالك" else "Votre diagnostic",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = JournalWritingInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(statusColor.copy(alpha = 0.42f))
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(status, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp, color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding))
            }
        }

        Spacer(Modifier.height(JournalRuleSpacing))

        // The verdict leads with one conclusion and its evidence, not a generic score.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .snapHeightToRule()
                .clip(RoundedCornerShape(10.dp))
                .background(statusColor.copy(alpha = 0.16f))
                .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp)
        ) {
            Column {
                Text(
                    text = if (isRtl) diagnostic.summaryAr else diagnostic.summaryFr,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = JournalRuleSpacing.value.sp,
                    color = JournalWritingInk,
                    maxLines = 2,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalTextOnRules()
                )
                Text(
                    text = if (isRtl) "مبني على ${diagnostic.dataQualityScore}% من المعطيات الأساسية" else "Fondé sur ${diagnostic.dataQualityScore}% des données essentielles",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 11.5.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
                // Keep the bottom outline on its own paper rule, never through text.
                Spacer(Modifier.height(JournalRuleSpacing))
            }
        }

        DiagnosticSectionTitle(if (isRtl) "شنو فهمنا من أجوبتك" else "Ce que nous avons compris", isRtl, HighlighterBlue)
        diagnostic.understoodFacts.forEach { insight ->
            DiagnosticInsightRow(insight, isRtl)
        }

        if (diagnostic.strengths.isNotEmpty()) {
            DiagnosticSectionTitle(if (isRtl) "نقط القوة اللي نبنيو عليها" else "Vos points d'appui", isRtl, HighlighterGreen)
            diagnostic.strengths.forEach { insight -> DiagnosticInsightRow(insight, isRtl) }
        }

        if (diagnostic.warnings.isNotEmpty()) {
            DiagnosticSectionTitle(if (isRtl) "الأولوية دابا" else "Votre priorité maintenant", isRtl, HighlighterPink)
            diagnostic.warnings.take(2).forEach { warning ->
                DiagnosticPlainRow(
                    title = if (isRtl) warning.messageAr else warning.messageFr,
                    detail = if (isRtl) "قبل ما نزيدو القسط، خاص هاد النقطة تتوازن." else "À stabiliser avant d'augmenter la mensualité.",
                    dotColor = HighlighterPink,
                    isRtl = isRtl
                )
            }
        }

        DiagnosticSectionTitle(if (isRtl) "القرار على كل نوع ديال مصروف" else "Nos recommandations par poste", isRtl, HighlighterYellow)
        val grouped = listOf(
            BudgetDecisionKind.PROTECT to (if (isRtl) "حافظ عليه" else "À préserver"),
            BudgetDecisionKind.REDUCE to (if (isRtl) "نقص منو" else "À réduire"),
            BudgetDecisionKind.STOP to (if (isRtl) "حبسو إلا ما محتاجوش" else "À arrêter si inutile"),
            BudgetDecisionKind.PREPARE to (if (isRtl) "وجد ليه من دابا" else "À provisionner")
        )
        grouped.forEach { (kind, label) ->
            val decisions = diagnostic.budgetDecisions.filter { it.kind == kind }
            if (decisions.isNotEmpty()) {
                val color = when (kind) {
                    BudgetDecisionKind.PROTECT -> HighlighterGreen
                    BudgetDecisionKind.REDUCE -> HighlighterYellow
                    BudgetDecisionKind.STOP -> HighlighterPink
                    BudgetDecisionKind.PREPARE -> HighlighterBlue
                }
                Text(
                    text = label,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.padding(top = 8.dp, bottom = 3.dp)
                )
                decisions.take(3).forEach { decision -> DiagnosticDecisionRow(decision, color, isRtl) }
            }
        }

        DiagnosticSectionTitle(if (isRtl) "الخطة المقترحة" else "Le plan recommandé", isRtl, HighlighterGreen)
        val recommended = diagnostic.planOptions.firstOrNull()
        if (recommended != null) {
            DiagnosticPlainRow(
                title = if (isRtl) "${recommended.monthlyStr} درهم فالشهر لمدة ${recommended.months} شهر" else "${recommended.monthlyStr} DH par mois pendant ${recommended.months} mois",
                detail = if (isRtl) recommended.descAr else recommended.descFr,
                dotColor = HighlighterGreen,
                isRtl = isRtl
            )
        } else {
            DiagnosticPlainRow(
                title = if (isRtl) "بدا بالأمان المالي" else "Commencez par sécuriser le mois",
                detail = if (isRtl) "ما غاديش نفرضو قسط ما دام الهامش مازال ما توازن." else "Aucune mensualité forcée tant que la marge n'est pas stabilisée.",
                dotColor = HighlighterPink,
                isRtl = isRtl
            )
        }

        DiagnosticSectionTitle(if (isRtl) "أول 3 خطوات" else "Vos 3 prochaines actions", isRtl, HighlighterBlue)
        diagnostic.topActions.take(3).forEachIndexed { index, action ->
            DiagnosticPlainRow(
                title = "${index + 1}. " + if (isRtl) action.textAr else action.textFr,
                detail = "",
                dotColor = HighlighterBlue,
                isRtl = isRtl
            )
        }

        Spacer(Modifier.height(JournalRuleSpacing))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagnosticToolButton(if (isRtl) "بدّل الأجوبة" else "Modifier les réponses", Modifier.weight(1f), onOpenQuestionnaire, isRtl)
            DiagnosticToolButton(if (isRtl) "جرّب سيناريو" else "Tester un scénario", Modifier.weight(1f), onOpenSimulator, isRtl)
        }
        Spacer(Modifier.height(8.dp))
        DiagnosticToolButton(if (isRtl) "تحديث شهري سريع" else "Faire le point ce mois-ci", Modifier.fillMaxWidth(), onOpenCheckIn, isRtl)
        Spacer(Modifier.height(JournalRuleSpacing * 3))
    }
}

@Composable
private fun DiagnosticSectionTitle(text: String, isRtl: Boolean, color: Color) {
    Spacer(Modifier.height(JournalRuleSpacing))
    Row(modifier = Modifier.fillMaxWidth().height(JournalRuleSpacing), verticalAlignment = Alignment.Bottom) {
        Canvas(Modifier.journalDotOnRule().size(8.dp)) { drawCircle(color = color) }
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = JournalWritingInk,
            modifier = Modifier.journalBaselineOnRule(), style = TextStyle(platformStyle = NoFontPadding))
    }
}

@Composable
private fun DiagnosticInsightRow(insight: DiagnosticInsight, isRtl: Boolean) {
    val color = when (insight.tone) { "POSITIVE" -> HighlighterGreen; "ATTENTION" -> HighlighterPink; else -> HighlighterBlue }
    DiagnosticPlainRow(if (isRtl) insight.titleAr else insight.titleFr,
        if (isRtl) insight.detailAr else insight.detailFr, color, isRtl)
}

@Composable
private fun DiagnosticPlainRow(title: String, detail: String, dotColor: Color, isRtl: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
        Canvas(Modifier.journalDotOnRule().size(7.dp)) { drawCircle(color = dotColor) }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
                lineHeight = JournalRuleSpacing.value.sp,
                color = JournalWritingInk,
                maxLines = 2, style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalTextOnRules())
            if (detail.isNotBlank()) Text(detail, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 12.5.sp, lineHeight = JournalRuleSpacing.value.sp, color = JournalMutedInk,
                maxLines = 2, style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalTextOnRules())
        }
    }
}

@Composable
private fun DiagnosticDecisionRow(decision: BudgetDecision, color: Color, isRtl: Boolean) {
    val label = if (isRtl) decision.labelAr else decision.labelFr
    val reason = if (isRtl) decision.reasonAr else decision.reasonFr
    val amount = when (decision.kind) {
        BudgetDecisionKind.PROTECT -> if (decision.currentMonthlyCentimes > 0) JournalLedgerManager.formatFrenchNumber((decision.currentMonthlyCentimes / 100).toString()) + " DH" else ""
        BudgetDecisionKind.PREPARE -> JournalLedgerManager.formatFrenchNumber((decision.suggestedMonthlyCentimes / 100).toString()) + if (isRtl) " DH/شهر" else " DH/mois"
        else -> if (decision.monthlyImpactCentimes > 0) "−" + JournalLedgerManager.formatFrenchNumber((decision.monthlyImpactCentimes / 100).toString()) + if (isRtl) " DH/شهر" else " DH/mois" else ""
    }
    Box(modifier = Modifier.fillMaxWidth().snapHeightToRule().clip(RoundedCornerShape(8.dp))
        .background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
        .padding(horizontal = 11.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().height(JournalRuleSpacing), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(label, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily, fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding), modifier = Modifier.weight(1f).journalBaselineOnRule())
                if (amount.isNotBlank()) Text(amount, fontFamily = PatrickHandFamily, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding), modifier = Modifier.journalBaselineOnRule())
            }
            Text(reason, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily, fontSize = 12.sp,
                lineHeight = JournalRuleSpacing.value.sp, color = JournalMutedInk, maxLines = 2,
                style = TextStyle(platformStyle = NoFontPadding), modifier = Modifier.journalTextOnRules())
            Spacer(Modifier.height(JournalRuleSpacing))
        }
    }
}

@Composable
private fun DiagnosticToolButton(text: String, modifier: Modifier, onClick: () -> Unit, isRtl: Boolean) {
    Box(modifier = modifier.height(JournalRuleSpacing * 2).clip(RoundedCornerShape(8.dp)).background(JournalPaper)
        .border(1.dp, JournalRule.copy(alpha = 0.75f), RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.TopCenter) {
        Text(text, fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily, fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp, color = JournalWritingInk, style = TextStyle(platformStyle = NoFontPadding), modifier = Modifier.journalBaselineOnRule())
    }
}

@Composable
private fun SavingsDiagnosticFullSection(
    goal: SavingsGoalEntity,
    diagnosis: PlanDiagnosis?,
    fullDiagnostic: FullDiagnosticResult?,
    aiCoachAdvice: String?,
    isAiCoachLoading: Boolean,
    isRtl: Boolean,
    onRequestAiCoachAdvice: () -> Unit,
    onClearAiCoachAdvice: () -> Unit,
    onApplySuggestedDuration: (Int) -> Unit,
    onOpenQuestionnaire: () -> Unit,
    onOpenSimulator: () -> Unit,
    onOpenCheckIn: () -> Unit
) {
    if (fullDiagnostic?.understoodFacts?.isNotEmpty() == true) {
        ProfessionalDiagnosticSection(
            goal = goal,
            diagnostic = fullDiagnostic,
            isRtl = isRtl,
            onOpenQuestionnaire = onOpenQuestionnaire,
            onOpenSimulator = onOpenSimulator,
            onOpenCheckIn = onOpenCheckIn
        )
        return
    }
    if (fullDiagnostic != null) {
        val m = fullDiagnostic.metrics
        Column(modifier = Modifier.fillMaxWidth()) {

            // ══════════════════════════════════════════════════════════════════
            // PILLAR 1: DIAGNOSTIC & DECLARED DATA (فين كيمشيو الفلوس؟)
            // ══════════════════════════════════════════════════════════════════

            // 1. Status & Feasibility line on blue rule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "📊 تشخيص وضعك المالي" else "📊 Diagnostic Financier",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )

                val (badgeText, badgeColor) = when (m.feasibility) {
                    GoalFeasibility.COMFORTABLE -> (if (isRtl) "خطة مريحة" else "Confortable") to HighlighterGreen
                    GoalFeasibility.FEASIBLE -> (if (isRtl) "خطة واقعية" else "Réaliste") to HighlighterGreen
                    GoalFeasibility.TIGHT -> (if (isRtl) "خطة ضاغطة" else "Serré") to HighlighterYellow
                    GoalFeasibility.AGGRESSIVE -> (if (isRtl) "خطة قاصحة" else "Exigeant") to HighlighterPink
                    GoalFeasibility.UNSAFE_NOW -> (if (isRtl) "أولوية الأمان" else "Sécurité d'abord") to HighlighterPink
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // 2. Profile and Verdict Summary (Lines sitting on blue rules)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "• بروفايلك: ${fullDiagnostic.profileLabelAr}" else "• Profil : ${fullDiagnostic.profileLabelFr}",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                val summaryText = if (isRtl) fullDiagnostic.summaryAr else fullDiagnostic.summaryFr
                Text(
                    text = "• $summaryText",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 13.sp,
                    color = JournalWritingInk.copy(alpha = 0.90f),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val criticalWarning = fullDiagnostic.warnings.firstOrNull()
            if (criticalWarning != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "⚠️ " + (if (isRtl) criticalWarning.messageAr else criticalWarning.messageFr),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = HighlighterPink.copy(alpha = 0.95f),
                        modifier = Modifier.journalBaselineOnRule(),
                        style = TextStyle(platformStyle = NoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // 3. Declared Figures: فين كيمشيو الفلوس والأولويات
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "📌 أهم المعطيات المصرح بها (فين كيمشيو الفلوس):" else "📌 Vos chiffres déclarés (Où va l'argent) :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Notebook Ledger Rows connected with margin vertical guide line (Identical to "أهدافي" tab)
            val declaredLeaks = fullDiagnostic.topLeaks
            val totalLedgerRows = 4 + declaredLeaks.size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeW = 1.4.dp.toPx()
                        val dotRadiusPx = 3.5.dp.toPx()
                        val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                        val rowHeightPx = JournalRuleSpacing.toPx()
                        val startY = rowHeightPx - dotRadiusPx
                        val endY = (totalLedgerRows - 1) * rowHeightPx + (rowHeightPx - dotRadiusPx)

                        drawLine(
                            color = JournalRule.copy(alpha = 0.60f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                // Row 1: Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.journalBaselineOnRule(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color = JournalWritingInk) }
                        Text(
                            text = if (isRtl) "الدخل الشهري الصافي:" else "Revenu net mensuel :",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    Text(
                        text = m.incomeStr,
                        fontFamily = PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = JournalWritingInk,
                        modifier = Modifier.journalBaselineOnRule(),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                // Row 2: Essentials (Red lines)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .journalBaselineOnRule(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color = Color(0xFF00796B)) }
                        Text(
                            text = if (isRtl) "المصاريف الأساسية والديون:" else "Charges fixes & dettes :",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    Text(
                        text = m.essentialsStr,
                        fontFamily = PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF00796B),
                        modifier = Modifier.journalBaselineOnRule(),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                // Breakdown of declared leaks (concise, 100% fits on single blue line)
                declaredLeaks.forEach { leak ->
                    val leakShortTitle = when {
                        leak.key.contains("CAFE", true) || leak.titleAr.contains("قهو") || leak.titleAr.contains("قهاو") -> if (isRtl) "تسرب القهوة والزنقة" else "Cafés & restos"
                        leak.key.contains("OUTING", true) || leak.titleAr.contains("خرجات") || leak.titleAr.contains("كازوال") -> if (isRtl) "تسرب الخرجات والكازوال" else "Sorties & essence"
                        leak.key.contains("SHOP", true) || leak.titleAr.contains("شوبينغ") || leak.titleAr.contains("تسوق") -> if (isRtl) "تسرب المشتريات والنزوات" else "Shopping imprévu"
                        leak.key.contains("SUB", true) || leak.titleAr.contains("اشتراك") || leak.titleAr.contains("فورفي") -> if (isRtl) "تسرب الاشتراكات والفورفيات" else "Abonnements inutiles"
                        else -> if (isRtl) "تسرب " + leak.titleAr.take(18) else "Fuite " + leak.titleFr.take(18)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .journalBaselineOnRule(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color = Color(0xFFC62828)) }
                            Text(
                                text = "$leakShortTitle:",
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                        Text(
                            text = "${leak.monthStr} DH/شهر",
                            fontFamily = PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color(0xFFC62828),
                            modifier = Modifier.journalBaselineOnRule(),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                // Row: Available Free Margin (Green)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.journalBaselineOnRule(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color = Color(0xFF1B5E20)) }
                        Text(
                            text = if (isRtl) "الهامش الحر المتوفر للتوفير:" else "Marge réelle disponible :",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF1B5E20),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    Text(
                        text = m.marginStr,
                        fontFamily = PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.journalBaselineOnRule(),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                // Row: Required Monthly Installment (Blue)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.journalBaselineOnRule(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color = Color(0xFF0D47A1)) }
                        Text(
                            text = if (isRtl) "القسط الشهري المطلوب للهدف:" else "Mensualité requise objectif :",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF0D47A1),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    Text(
                        text = "${m.requiredStr}/شهر",
                        fontFamily = PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0D47A1),
                        modifier = Modifier.journalBaselineOnRule(),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // ══════════════════════════════════════════════════════════════════
            // PILLAR 2: PRACTICAL TAILORED SOLUTIONS (حلول عملية مخصصة)
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "💡 حلول عملية ومخصصة لمشاكلك:" else "💡 Solutions pratiques adaptées :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            val cardsToShow = fullDiagnostic.adviceCards.take(4)
            cardsToShow.forEachIndexed { index, card ->
                DiagnosticAdviceCardItem(
                    stepIndex = index + 1,
                    card = card,
                    isRtl = isRtl
                )
                Spacer(modifier = Modifier.height(JournalRuleSpacing))
            }

            // ══════════════════════════════════════════════════════════════════
            // PILLAR 3: "Hdi Rassek" - DEFAULT PSYCHOLOGICAL TRAPS (حضي راسك)
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "⚠️ حضي راسك (الفخاخ النفسية ومصائد النزوة):" else "⚠️ Attention aux pièges financiers (Par défaut) :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFC62828),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            HdiRassekSection(isRtl = isRtl)

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // ══════════════════════════════════════════════════════════════════
            // PILLAR 4: INLINE INTERACTIVE CALCULATOR (الحاسبة التفاعلية)
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "🔮 حاسبة السيناريوهات التفاعلية:" else "🔮 Simulateur interactif de scénarios :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            InlineDiagnosticCalculator(
                fullDiagnostic = fullDiagnostic,
                goal = goal,
                isRtl = isRtl,
                onOpenFullSimulator = onOpenSimulator
            )

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // ══════════════════════════════════════════════════════════════════
            // FOOTER ACTIONS & AI COACH
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "🛠️ أدوات إضافية:" else "🛠️ Outils complémentaires :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk.copy(alpha = 0.85f),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Edit Questionnaire button (snapped to rule)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JournalPaper)
                    .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                    .clickable { onOpenQuestionnaire() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) "↻ تعديل بيانات الاستبيان المالي" else "↻ Modifier les réponses du questionnaire",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Optional AI Coach
            Spacer(modifier = Modifier.height(10.dp))
            if (aiCoachAdvice == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HighlighterBlue.copy(alpha = 0.20f))
                        .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                        .clickable(enabled = !isAiCoachLoading) { onRequestAiCoachAdvice() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAiCoachLoading) {
                            if (isRtl) "⏳ جاري إعداد التشخيص من الكوتش..." else "⏳ Analyse par le Coach IA..."
                        } else {
                            if (isRtl) "✨ استشارة إضافية من كوتش الذكاء الاصطناعي" else "✨ Demander conseil au Coach IA"
                        },
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .snapHeightToRule()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JournalPaper)
                        .border(1.dp, JournalRule.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRtl) "✨ نصيحة كوتش AI:" else "✨ Conseil du Coach IA :",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Text(
                            text = if (isRtl) "تحديث ↻" else "Actualiser ↻",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.sp,
                            color = JournalMutedInk,
                            modifier = Modifier.clickable { onRequestAiCoachAdvice() }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = aiCoachAdvice,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = JournalWritingInk.copy(alpha = 0.90f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing * 3))
        }
    } else {
        // ══════════════════════════════════════════════════════════════════════
        // BASELINE ESTIMATE VIEW (before full questionnaire is completed)
        // ══════════════════════════════════════════════════════════════════════
        val diag = diagnosis
        if (diag == null) {
            SavingsDiagnosticEmptyState(
                isRtl = isRtl,
                onStartWizard = onOpenQuestionnaire
            )
            return
        }
        val leak = diag.leakInfo
        val shock = diag.shockNumbers
        val trap = diag.goalTrap

        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Questionnaire Invitation Banner ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HighlighterYellow.copy(alpha = 0.50f))
                        .border(1.dp, JournalWritingInk.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .clickable { onOpenQuestionnaire() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "✨ ابدأ الاستبيان المالي الكامل لتشخيص دقيق 🚀" else "✨ Passer le questionnaire complet pour un diagnostic précis 🚀",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 1. HEADER ROW sitting on Rule 1 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "📊 تشخيص أولي (تقديري)" else "📊 Diagnostic initial (estimé)",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )

                val statusText = when (diag.pressureLevel) {
                    "EASY" -> if (isRtl) "خطة مريحة" else "Plan Confortable"
                    "MODERATE" -> if (isRtl) "خطة متوازنة" else "Plan Équilibré"
                    else -> if (isRtl) "هدف ضاغط" else "Plan Exigeant"
                }
                val statusColor = when (diag.pressureLevel) {
                    "EASY" -> HighlighterGreen
                    "MODERATE" -> HighlighterYellow
                    else -> HighlighterPink
                }
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.40f))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            // na9ez star (Skip 1 blue line)
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 2. SECTION 1: CHOIX DÉCLARÉS DU PROFIL (4 connected notebook lines) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "📋 اختيارات البروفايل المالي:" else "📋 Vos choix de profil déclarés :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            val essentialDescShort = when (goal.essentialBracket) {
                "LOW" -> if (isRtl) "منخفضة <40%" else "Faibles <40%"
                "HIGH" -> if (isRtl) "مرتفعة >60%" else "Élevées >60%"
                else -> if (isRtl) "متوسطة 40-60%" else "Moyennes 40-60%"
            }

            val styleTitle = if (goal.savingsStyle == "TURBO") {
                if (isRtl) "🔥 نمط التقشف السريع (تزيار قوي)" else "🔥 Mode Turbo (austérité rapide)"
            } else {
                if (isRtl) "🌿 نمط متوازن (توفير مستمر)" else "🌿 Mode Équilibré (progression régulière)"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeW = 1.4.dp.toPx()
                        val dotRadiusPx = 3.5.dp.toPx()
                        val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                        val rowHeightPx = JournalRuleSpacing.toPx()
                        val startY = rowHeightPx - dotRadiusPx
                        val endY = 3 * rowHeightPx + (rowHeightPx - dotRadiusPx)

                        drawLine(
                            color = JournalRule.copy(alpha = 0.60f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                // Line 1: Objectif & Durée
                NotebookRuledRow(
                    text = if (isRtl) {
                        "الهدف: ${goal.title} (${formatSavingsMoney(goal.targetAmountCentimes / 100, true)}) على ${goal.targetMonths} شهر"
                    } else {
                        "Objectif : ${goal.title} (${formatSavingsMoney(goal.targetAmountCentimes / 100, false)}) sur ${goal.targetMonths} mois"
                    },
                    dotColor = HighlighterYellow,
                    isRtl = isRtl
                )

                // Line 2: Salaire & Charges fixes
                NotebookRuledRow(
                    text = if (isRtl) {
                        "الصالير المصرح: ${formatSavingsMoney(diag.salary, true)} (المصاريف الأساسية: $essentialDescShort)"
                    } else {
                        "Salaire déclaré : ${formatSavingsMoney(diag.salary, false)} (Charges : $essentialDescShort)"
                    },
                    dotColor = HighlighterGreen,
                    isRtl = isRtl
                )

                // Line 3: Principale fuite
                NotebookRuledRow(
                    text = if (isRtl) {
                        "الباب المستنزف: ${leak.titleAr} (${shock.dailyCostDh.toInt()} DH/يوم، ${shock.daysPerWeek}j/س)"
                    } else {
                        "Fuite ciblée : ${leak.titleFr} (${shock.dailyCostDh.toInt()} DH/j, ${shock.daysPerWeek}j/sem)"
                    },
                    dotColor = Color(0xFFE65100),
                    isRtl = isRtl
                )

                // Line 4: Style choisi
                NotebookRuledRow(
                    text = if (isRtl) "النمط المعتمد: $styleTitle" else "Style choisi : $styleTitle",
                    dotColor = Color(0xFF00796B),
                    isRtl = isRtl
                )
            }

            // na9ez star (Skip 1 blue line)
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 3. SECTION 2: LE CHOC DES PETITS MONTANTS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "💥 صدمة الدرهم الصغير (حساب الاستنزاف):" else "💥 Le Choc des Petits Montants :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeW = 1.4.dp.toPx()
                        val dotRadiusPx = 3.5.dp.toPx()
                        val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                        val rowHeightPx = JournalRuleSpacing.toPx()
                        val startY = rowHeightPx - dotRadiusPx
                        val endY = 3 * rowHeightPx + (rowHeightPx - dotRadiusPx)

                        drawLine(
                            color = JournalRule.copy(alpha = 0.60f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                NotebookRuledRow(
                    text = if (isRtl) {
                        "• الصرف اليومي المعتاد: ${shock.dailyCostDh.toInt()} درهم (${shock.daysPerWeek} أيام فالسيمانة)"
                    } else {
                        "• Dépense quotidienne : ${shock.dailyCostDh.toInt()} DH (${shock.daysPerWeek} jours/semaine)"
                    },
                    dotColor = Color(0xFFE65100),
                    isRtl = isRtl
                )

                NotebookRuledRow(
                    text = if (isRtl) {
                        "الاستنزاف الشهري: ~${shock.formatMonthlyDrain()} DH كتمشي غير فـ ${leak.subtitleAr}"
                    } else {
                        "Drain mensuel : ~${shock.formatMonthlyDrain()} DH/mois absorbés en petits extras"
                    },
                    dotColor = Color(0xFFD32F2F),
                    isRtl = isRtl
                )

                NotebookRuledRow(
                    text = if (isRtl) {
                        "الصدمة السنوية: ${shock.formatYearlyDrain()} درهم فالعام كتسلت بالدرهم الصغير!"
                    } else {
                        "Choc annuel : ${shock.formatYearlyDrain()} DH/an volatilisés en micro-dépenses !"
                    },
                    dotColor = Color(0xFFC2185B),
                    isRtl = isRtl
                )

                NotebookRuledRow(
                    text = if (isRtl) {
                        "إلى نقصتي للنصف: غاتوفر +${shock.formatHalfCutYearly()} DH سنوياً لصالح الهدف!"
                    } else {
                        "Réduire de 50% = récupérer +${shock.formatHalfCutYearly()} DH/an pour l'objectif !"
                    },
                    dotColor = HighlighterGreen,
                    isRtl = isRtl
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 4. SECTION 3: THAWABIT / VITAL PILLARS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "🛡️ الثوابت الحيوية (خط أحمر لا يمس):" else "🛡️ Les Piliers vitaux à préserver :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF00796B),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeW = 1.4.dp.toPx()
                        val dotRadiusPx = 3.5.dp.toPx()
                        val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                        val rowHeightPx = JournalRuleSpacing.toPx()
                        val startY = rowHeightPx - dotRadiusPx
                        val endY = 3 * rowHeightPx + (rowHeightPx - dotRadiusPx)

                        drawLine(
                            color = JournalRule.copy(alpha = 0.60f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                NotebookRuledRow(
                    text = if (isRtl) "🏠 الكراء وطريطمون السكن: أساس الاستقرار النفسي والعائلي" else "🏠 Logement : Pilier vital, aucune impasse possible",
                    dotColor = Color(0xFF00796B),
                    isRtl = isRtl
                )
                NotebookRuledRow(
                    text = if (isRtl) "⚡ فواتير الماء والضوء والأنترنت: واجبات ترشد بلا تضييق مفرط" else "⚡ Factures d'énergie : Optimiser avec bon sens",
                    dotColor = HighlighterYellow,
                    isRtl = isRtl
                )
                NotebookRuledRow(
                    text = if (isRtl) "🥗 التقضية الصحية للدار: كتوفر 60% مقارنة بماكلة الزنقة" else "🥗 Repas maison : Économie de 60% vs alimentation dehors",
                    dotColor = HighlighterGreen,
                    isRtl = isRtl
                )
                NotebookRuledRow(
                    text = if (isRtl) "🏥 صحة وتطبيب العائلة: أولوية قصوى غير قابلة لأي تقشف" else "🏥 Santé de la famille : Priorité absolue sans compromis",
                    dotColor = Color(0xFF00897B),
                    isRtl = isRtl
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 5. SECTION 4: GOAL-SPECIFIC HIDDEN TRAP ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "⚠️ فخ الهدف: ${trap.titleAr}" else "⚠️ Piège de l'objectif : ${trap.titleFr}",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFE65100),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeW = 1.4.dp.toPx()
                        val dotRadiusPx = 3.5.dp.toPx()
                        val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                        val rowHeightPx = JournalRuleSpacing.toPx()
                        val startY = rowHeightPx - dotRadiusPx
                        val endY = 1 * rowHeightPx + (rowHeightPx - dotRadiusPx)

                        drawLine(
                            color = JournalRule.copy(alpha = 0.60f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                NotebookRuledRow(
                    text = if (isRtl) "التحذير: ${trap.warningAr}" else "Attention : ${trap.warningFr}",
                    dotColor = Color(0xFFD32F2F),
                    isRtl = isRtl
                )
                NotebookRuledRow(
                    text = if (isRtl) "وصية الكوتش: ${trap.goldenRuleAr}" else "Règle d'or : ${trap.goldenRuleFr}",
                    dotColor = HighlighterGreen,
                    isRtl = isRtl
                )
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 6. SECTION 5: PLAN D'AUSTÉRITÉ CIBLÉ ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "⚡ خطة التقشف الذكية (لتسريع الهدف):" else "⚡ Plan d'austérité ciblé (Mode Turbo) :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF00796B),
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HighlighterGreen.copy(alpha = 0.50f))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "3 إلى 6 أشهر" else "3 à 6 mois",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            val austerityList = if (isRtl) diag.austerityStepsAr else diag.austerityStepsFr
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeW = 1.4.dp.toPx()
                        val dotRadiusPx = 3.5.dp.toPx()
                        val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                        val rowHeightPx = JournalRuleSpacing.toPx()
                        val startY = rowHeightPx - dotRadiusPx
                        val endY = (austerityList.size - 1) * rowHeightPx + (rowHeightPx - dotRadiusPx)

                        drawLine(
                            color = JournalRule.copy(alpha = 0.60f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                austerityList.forEachIndexed { idx, stepText ->
                    val dotCol = when (idx) {
                        0 -> Color(0xFFD32F2F)
                        1 -> Color(0xFFE65100)
                        2 -> HighlighterYellow
                        3 -> Color(0xFF00796B)
                        else -> HighlighterGreen
                    }
                    NotebookRuledRow(
                        text = "${idx + 1}. $stepText",
                        dotColor = dotCol,
                        isRtl = isRtl
                    )
                }
            }

            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // --- 7. SECTION 6: ON-DEMAND AI COACH SECTION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isRtl) "✨ استشارة كوتش الذكاء الاصطناعي:" else "✨ Conseil du Coach Financier IA :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.journalBaselineOnRule(),
                    style = TextStyle(platformStyle = NoFontPadding)
                )

                if (aiCoachAdvice != null) {
                    Text(
                        text = if (isRtl) "إعادة التحليل ↻" else "Actualiser ↻",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = JournalMutedInk,
                        modifier = Modifier
                            .clickable { onRequestAiCoachAdvice() }
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            if (aiCoachAdvice == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAiCoachLoading) HighlighterBlue.copy(alpha = 0.35f) else HighlighterYellow.copy(alpha = 0.50f))
                            .border(1.dp, JournalWritingInk.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .clickable(enabled = !isAiCoachLoading) { onRequestAiCoachAdvice() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAiCoachLoading) {
                                if (isRtl) "⏳ جاري إعداد التشخيص من الكوتش..." else "⏳ Analyse par le Coach IA en cours..."
                            } else {
                                if (isRtl) "✨ طلب تشخيص مخصص من كوتش AI 🚀" else "✨ Demander diagnostic au Coach IA 🚀"
                            },
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            } else {
                val coachLines = aiCoachAdvice.lines().filter { it.isNotBlank() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val strokeW = 1.4.dp.toPx()
                            val dotRadiusPx = 3.5.dp.toPx()
                            val guideX = if (isRtl) size.width - dotRadiusPx else dotRadiusPx
                            val rowHeightPx = JournalRuleSpacing.toPx()
                            val startY = rowHeightPx - dotRadiusPx
                            val endY = (coachLines.size - 1) * rowHeightPx + (rowHeightPx - dotRadiusPx)

                            drawLine(
                                color = HighlighterYellow.copy(alpha = 0.85f),
                                start = Offset(guideX, startY),
                                end = Offset(guideX, endY),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                ) {
                    coachLines.forEach { lineText ->
                        NotebookRuledRow(
                            text = lineText,
                            dotColor = HighlighterYellow,
                            isRtl = isRtl
                        )
                    }
                }
            }

            // --- 8. ALTERNATIVE SUGGESTION (if objective is tight) ---
            if (diag.suggestedMonths > 0) {
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (isRtl) {
                            "💡 اقتراح: تمديد الهدف لـ ${diag.suggestedMonths} شهر لتخفيف القسط الشهري"
                        } else {
                            "💡 Conseil : Allonger à ${diag.suggestedMonths} mois pour une mensualité plus douce"
                        },
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        color = JournalWritingInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.journalBaselineOnRule(),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HighlighterYellow.copy(alpha = 0.55f))
                            .border(1.dp, JournalWritingInk.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .clickable { onApplySuggestedDuration(diag.suggestedMonths) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRtl) "تطبيق التمديد لـ ${diag.suggestedMonths} شهر" else "Appliquer l'allongement à ${diag.suggestedMonths} mois",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single ruled line row with an aligned colored notebook margin dot.
 * Both the dot and text sit exactly on the 29dp blue rule.
 */
@Composable
private fun NotebookRuledRow(
    text: String,
    dotColor: Color,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val cleanText = text.trimStart().removePrefix("•").removePrefix("-").removePrefix("*").trimStart()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .journalVisualOnRule(gapAboveRule = 2.dp)
                .size(7.dp)
        ) {
            drawCircle(color = dotColor)
        }
        Text(
            text = cleanText,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = 13.sp,
            color = JournalWritingInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.journalBaselineOnRule()
        )
    }
}

/**
 * Moroccan Personal Finance Knowledge & Tips Section:
 * Rich, structured library of Moroccan-focused personal finance guides and behavioral insights.
 * Categorized by: All, Psychology of Spending, Smart Savings, Financial Safety, Household Economics, Income & Growth.
 * Features Daily Golden Tip + Horizontal Filter Chips + Expandable Ruled Notebook Cards.
 * 100% of text lines sit directly on the 29dp blue notebook ruled lines (Star Zra9).
 */
@Composable
private fun SavingsArticlesLibrarySection(
    activeGoal: SavingsGoalEntity? = null,
    recommendedArticleIds: List<String> = emptyList(),
    isRtl: Boolean
) {
    var selectedCategory by remember { mutableStateOf(ArticleCategoryGroup.ALL) }
    var currentTipIndex by remember { mutableStateOf(0) }

    val fallbackRecommendedId = when (activeGoal?.leisureCategory) {
        "CAFE" -> "art_house_cafe_lunch"
        "SHOPPING" -> "art_psych_24h_promo"
        else -> when {
            activeGoal?.title?.contains("طوارئ", true) == true || activeGoal?.title?.contains("urgence", true) == true -> "art_sec_emergency_fund"
            else -> "art_strat_50_30_20"
        }
    }
    val primaryRecommendedId = recommendedArticleIds.firstOrNull() ?: fallbackRecommendedId
    var selectedArticleForDetail by remember { mutableStateOf<SavingsArticleFull?>(null) }

    val allArticles = remember(recommendedArticleIds, primaryRecommendedId) {
        SavingsKnowledgeBase.getRecommendedArticles(
            if (recommendedArticleIds.isNotEmpty()) recommendedArticleIds else listOf(primaryRecommendedId)
        )
    }

    val filteredArticles = remember(selectedCategory, allArticles) {
        if (selectedCategory == ArticleCategoryGroup.ALL) {
            allArticles
        } else {
            allArticles.filter { it.categoryGroup == selectedCategory }
        }
    }

    // If an article is selected, display the dedicated full reading page
    if (selectedArticleForDetail != null) {
        val currentArticle = selectedArticleForDetail!!
        val currentIndex = filteredArticles.indexOfFirst { it.id == currentArticle.id }
        val prevArticle = if (currentIndex > 0) filteredArticles.getOrNull(currentIndex - 1) else null
        val nextArticle = if (currentIndex in 0 until filteredArticles.size - 1) filteredArticles.getOrNull(currentIndex + 1) else null

        SavingsArticleDetailView(
            article = currentArticle,
            prevArticle = prevArticle,
            nextArticle = nextArticle,
            onBack = { selectedArticleForDetail = null },
            onSelectArticle = { selectedArticleForDetail = it },
            isRtl = isRtl
        )
        return
    }

    val dailyTips = SavingsKnowledgeBase.DAILY_TIPS
    val activeTip = dailyTips[currentTipIndex % dailyTips.size]

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- 1. SECTION TITLE on Rule 1 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) "📚 دليل المستشار المالي ومكتبة المقالات" else "📚 Guides Pratiques & Conseils Financiers",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = JournalWritingInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 2. DAILY GOLDEN TIP CARD (قاعدة اليوم الذهبية) ---
        JournalRuledCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = JournalPaper,
            borderColor = JournalRule.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
            contentPaddingHorizontal = 12.dp
        ) {
            // Line 1: Header Row (Badge + Rotate Button) centered vertically in Rule 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Golden Tip Badge
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HighlighterYellow.copy(alpha = 0.40f))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "✨ قاعدة اليوم" else "✨ Règle d'or du jour",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                // Right: Suivant ↻ Button (Centered vertically & horizontally!)
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HighlighterYellow.copy(alpha = 0.60f))
                        .border(0.8.dp, JournalWritingInk.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                        .clickable { currentTipIndex++ }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRtl) "فكرة أخرى ↻" else "Suivant ↻",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            // Line 2: Full Title without truncation (100% on blue line)
            Text(
                text = "${activeTip.iconEmoji}  " + (if (isRtl) activeTip.titleAr else activeTip.titleFr),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = JournalRuleSpacing.value.sp,
                color = JournalWritingInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier
                    .fillMaxWidth()
                    .journalTextOnRules()
            )

            // Line 3+: Body Text (100% on blue lines)
            Text(
                text = if (isRtl) activeTip.tipAr else activeTip.tipFr,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 12.5.sp,
                lineHeight = JournalRuleSpacing.value.sp,
                color = JournalWritingInk.copy(alpha = 0.90f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier
                    .fillMaxWidth()
                    .journalTextOnRules()
            )

            // Spacing before Action Box ("هابطة شوية")
            Spacer(modifier = Modifier.height(10.dp))

            // Action Box: Centered text, no hidden text, breathing room
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(HighlighterGreen.copy(alpha = 0.22f))
                    .border(0.8.dp, Color(0xFF00796B).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = (if (isRtl) "🎯 خطوة عملية: " else "🎯 Action : ") + (if (isRtl) activeTip.actionAr else activeTip.actionFr),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF00796B),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            // Bottom breathing room before bottom border
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 3. HORIZONTAL CATEGORY FILTER CHIPS ---
        val categories = ArticleCategoryGroup.values()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                val count = if (cat == ArticleCategoryGroup.ALL) allArticles.size else allArticles.count { it.categoryGroup == cat }
                val label = "${cat.iconEmoji} " + (if (isRtl) cat.titleAr else cat.titleFr) + " ($count)"

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) HighlighterYellow.copy(alpha = 0.60f) else JournalPaper)
                        .border(
                            1.dp,
                            if (isSelected) JournalWritingInk.copy(alpha = 0.50f) else JournalRule.copy(alpha = 0.40f),
                            RoundedCornerShape(5.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 4. NUMBERED ARTICLES INDEX (فهرس المقالات المرقم) ---
        filteredArticles.forEachIndexed { index, article ->
            val isRecommended = article.id == primaryRecommendedId
            val itemNumber = if (index < 9) "0${index + 1}" else "${index + 1}"

            JournalRuledCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedArticleForDetail = article
                    },
                shape = RoundedCornerShape(8.dp),
                backgroundColor = JournalPaper.copy(alpha = 0.48f),
                borderColor = if (isRecommended) HighlighterGreen.copy(alpha = 0.70f) else JournalRule.copy(alpha = 0.55f),
                borderWidth = if (isRecommended) 1.2.dp else 1.dp,
                contentPaddingHorizontal = 12.dp
            ) {
                // Line 1: Header Row: Number + Category Badge + Read Time + Arrow Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Order Number: 01, 02...
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(HighlighterYellow.copy(alpha = 0.45f))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = itemNumber,
                                fontFamily = PatrickHandFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Category label
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isRecommended) HighlighterGreen.copy(alpha = 0.40f) else article.tagColor.copy(alpha = 0.30f))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRecommended) (if (isRtl) "⭐ موصى به" else "⭐ Recommandé") else "${article.iconEmoji} " + (if (isRtl) article.categoryLabelAr else article.categoryLabelFr),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Read time
                        Text(
                            text = if (isRtl) article.readTimeAr else article.readTimeFr,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 11.sp,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }

                    // Arrow indicator: "Lire →" / "قراءة ←"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isRtl) "قراءة ←" else "Lire →",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color(0xFF00796B),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                // Line 2: Full Title sitting on blue rules
                Text(
                    text = if (isRtl) article.titleAr else article.titleFr,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    lineHeight = JournalRuleSpacing.value.sp,
                    color = JournalWritingInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .fillMaxWidth()
                        .journalTextOnRules()
                )

                // Breathing room before bottom border
                Spacer(modifier = Modifier.height(10.dp))
            }

            // na9ez star between articles (Skip 1 blue line)
            Spacer(modifier = Modifier.height(JournalRuleSpacing))
        }
    }
}

/**
 * Dedicated Full-Screen Reading View for a single financial advice article.
 * 100% of text and sections sit on the 29dp blue notebook rules.
 * Features back navigation via button and Android system BackHandler.
 */
@Composable
private fun SavingsArticleDetailView(
    article: SavingsArticleFull,
    prevArticle: SavingsArticleFull?,
    nextArticle: SavingsArticleFull?,
    onBack: () -> Unit,
    onSelectArticle: (SavingsArticleFull) -> Unit,
    isRtl: Boolean
) {
    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Line 1: Top Navigation Bar sitting on Rule 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(HighlighterYellow.copy(alpha = 0.55f))
                    .border(0.8.dp, JournalWritingInk.copy(alpha = 0.30f), RoundedCornerShape(5.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isRtl) "→" else "←",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                    Text(
                        text = if (isRtl) "رجوع للمقالات" else "Retour aux conseils",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            // Reading Duration
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(JournalMutedInk.copy(alpha = 0.08f))
                    .border(0.6.dp, JournalRule.copy(alpha = 0.40f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) article.readTimeAr else article.readTimeFr,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 2: Category Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(article.tagColor.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${article.iconEmoji}  " + (if (isRtl) article.categoryLabelAr else article.categoryLabelFr),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        // Line 3+: Major Article Title sitting on blue rules
        Text(
            text = if (isRtl) article.titleAr else article.titleFr,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.5.sp,
            lineHeight = JournalRuleSpacing.value.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier
                .fillMaxWidth()
                .journalTextOnRules()
        )

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Summary / Introduction sitting on blue rules
        Text(
            text = if (isRtl) article.summaryAr else article.summaryFr,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = JournalRuleSpacing.value.sp,
            color = JournalWritingInk.copy(alpha = 0.90f),
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier
                .fillMaxWidth()
                .journalTextOnRules()
        )

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Action Steps Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) "🎯 خطوات ونصائح عملية:" else "🎯 Étapes et conseils pratiques :",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalWritingInk,
                modifier = Modifier.journalBaselineOnRule(),
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // Action Points on Rules
        val points = if (isRtl) article.actionPointsAr else article.actionPointsFr
        points.forEach { point ->
            val cleanText = point.removePrefix("• ").removePrefix("- ").trim()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Canvas(
                    modifier = Modifier
                        .journalDotOnRule()
                        .size(6.dp)
                ) {
                    drawCircle(color = article.tagColor)
                }
                Text(
                    text = cleanText,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = JournalRuleSpacing.value.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .weight(1f)
                        .journalTextOnRules()
                )
            }
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Moroccan Case Study (if present)
        val caseStudy = if (isRtl) article.caseStudyAr else article.caseStudyFr
        if (caseStudy != null) {
            JournalRuledCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = article.tagColor.copy(alpha = 0.12f),
                borderColor = article.tagColor.copy(alpha = 0.40f),
                shape = RoundedCornerShape(8.dp),
                contentPaddingHorizontal = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "📊 حسبة واقعية بالأرقام (المغرب):" else "📊 Chiffres concrets au Maroc :",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                Text(
                    text = caseStudy,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 12.5.sp,
                    lineHeight = JournalRuleSpacing.value.sp,
                    color = JournalWritingInk.copy(alpha = 0.95f),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalTextOnRules()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(JournalRuleSpacing))
        }

        // Golden Takeaway Card
        JournalRuledCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = HighlighterYellow.copy(alpha = 0.25f),
            borderColor = JournalWritingInk.copy(alpha = 0.35f),
            shape = RoundedCornerShape(8.dp),
            contentPaddingHorizontal = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRtl) "💡 خلاصة الذهب:" else "💡 Règle d'or à retenir :",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
            Text(
                text = if (isRtl) article.takeawayAr else article.takeawayFr,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = JournalRuleSpacing.value.sp,
                color = Color(0xFF00796B),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalTextOnRules()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Previous / Next Article Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prevArticle != null) {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(JournalPaper)
                        .border(0.8.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(4.dp))
                        .clickable { onSelectArticle(prevArticle) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.savings_article_prev),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (nextArticle != null) {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HighlighterYellow.copy(alpha = 0.60f))
                        .border(0.8.dp, JournalWritingInk.copy(alpha = 0.30f), RoundedCornerShape(4.dp))
                        .clickable { onSelectArticle(nextArticle) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.savings_article_next),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }

        // Bottom space so scrolling is comfortable above the navigation bar
        Spacer(modifier = Modifier.height(60.dp))
    }
}
