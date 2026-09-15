package com.cash.guide.feature.savings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalSectionBadge
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalVisualOnRule
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
                // 1. Centered 3 Tabs (Mes objectifs | Diagnostic | Conseils) fitting between 2 lines (29dp)
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
                                text = if (isRtl) "أهدافي" else "Mes objectifs",
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isTab1) JournalWritingInk else JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Divider line (شلطة 1)
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
                                text = if (isRtl) "التحليل" else "Diagnostic",
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = if (isTab2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isTab2) JournalWritingInk else JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Divider line (شلطة 2)
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(JournalRule.copy(alpha = 0.60f))
                        )

                        // Tab 3: مقالات / Conseils
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
                                text = if (isRtl) "مقالات" else "Conseils",
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
                                onStartWizard = { viewModel.startWizard() }
                            )
                        } else {
                            if (uiState.goals.size > 1) {
                                GoalSwitcherChips(
                                    goals = uiState.goals,
                                    activeGoalId = activeGoal.id,
                                    onSelectGoal = { viewModel.selectGoal(it) },
                                    onNewPlan = { viewModel.startWizard() },
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
                                onDeleteGoal = { goalToDelete = activeGoal },
                                onNewPlan = { viewModel.startWizard() },
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
                                onStartWizard = { viewModel.startWizard() }
                            )
                        } else {
                            if (uiState.goals.size > 1) {
                                GoalSwitcherChips(
                                    goals = uiState.goals,
                                    activeGoalId = activeGoal.id,
                                    onSelectGoal = { viewModel.selectGoal(it) },
                                    onNewPlan = { viewModel.startWizard() },
                                    isRtl = isRtl
                                )
                                Spacer(modifier = Modifier.height(JournalRuleSpacing))
                            }

                            SavingsDiagnosticFullSection(
                                goal = activeGoal,
                                diagnosis = uiState.diagnosis,
                                aiCoachAdvice = uiState.aiCoachAdvice,
                                isAiCoachLoading = uiState.isAiCoachLoading,
                                isRtl = isRtl,
                                onRequestAiCoachAdvice = { viewModel.requestAiCoachAdvice(activeGoal, isRtl) },
                                onClearAiCoachAdvice = { viewModel.clearAiCoachAdvice() },
                                onApplySuggestedDuration = { newMonths ->
                                    viewModel.applySuggestedDuration(activeGoal.id, newMonths)
                                }
                            )
                        }
                    }
                    SavingsTab.TIPS -> {
                        val activeGoal = uiState.activeGoal ?: uiState.goals.firstOrNull()
                        SavingsArticlesLibrarySection(
                            activeGoal = activeGoal,
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
                "ماعندك حتى هدف توفير دابا.. جاوب على 6 ديال الأسئلة بسيطة والتطبيق غادي يقاد ليك خطة مالية مخصصة على قياس صاليرك ومصاريفك."
            } else {
                "Vous n'avez pas encore d'objectif actif. Répondez à quelques questions pour bâtir un plan sur-mesure !"
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
                text = if (isRtl) "+ إضافة هدف جديد 🚀" else "+ Ajouter un nouvel objectif 🚀",
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
        // Progress bar sits directly on the blue line: dynamic % on left, 100% on right, bar in middle
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left: Dynamic percentage (starts at 0%, updates to 5%, 10%, 15%, etc.)
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
                            drawRoundRect(
                                color = categoryColor,
                                topLeft = Offset(0f, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // Right: 100%
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                        .padding(horizontal = 18.dp),
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

                Spacer(modifier = Modifier.width(10.dp))

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
        SimpleDateFormat("d MMMM yyyy", if (isRtl) Locale("ar") else Locale.FRENCH)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Badge (height = JournalRuleSpacing = 29.dp)
        JournalSectionBadge(
            title = if (isRtl) "سجل التوفير" else "Historique des versements",
            badgeColor = HighlighterGreen.copy(alpha = 0.30f),
            trailingContent = {
                val countText = if (isRtl) {
                    if (deposits.size == 1) "دفعة واحدة" else "${deposits.size} دفعات"
                } else {
                    "${deposits.size} versement(s)"
                }
                Box(
                    modifier = Modifier
                        .height(JournalRuleSpacing)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HighlighterGreen.copy(alpha = 0.20f))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countText,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF00796B),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        )

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
 * Full Diagnostic Section reflecting user choices, spending leaks (café, shopping, etc.),
 * budget feasibility, and a dedicated Austerity Mode (خطة التقشف).
 * 100% of text lines sit directly on the 29dp blue notebook ruled lines.
 */
@Composable
private fun SavingsDiagnosticFullSection(
    goal: SavingsGoalEntity,
    diagnosis: PlanDiagnosis?,
    aiCoachAdvice: String?,
    isAiCoachLoading: Boolean,
    isRtl: Boolean,
    onRequestAiCoachAdvice: () -> Unit,
    onClearAiCoachAdvice: () -> Unit,
    onApplySuggestedDuration: (Int) -> Unit
) {
    val diag = diagnosis ?: return
    val leak = diag.leakInfo
    val shock = diag.shockNumbers
    val trap = diag.goalTrap

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- 1. HEADER ROW sitting on Rule 1 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) "📊 تشخيص البروفايل وخطة العمل" else "📊 Diagnostic du profil & Plan d'action",
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

        // --- 3. SECTION 2: LE CHOC DES PETITS MONTANTS (4 connected notebook lines) ---
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
            // Line 1: Fuite quotidienne
            NotebookRuledRow(
                text = if (isRtl) {
                    "• الصرف اليومي المعتاد: ${shock.dailyCostDh.toInt()} درهم (${shock.daysPerWeek} أيام فالسيمانة)"
                } else {
                    "• Dépense quotidienne : ${shock.dailyCostDh.toInt()} DH (${shock.daysPerWeek} jours/semaine)"
                },
                dotColor = Color(0xFFE65100),
                isRtl = isRtl
            )

            // Line 2: Drain mensuel
            NotebookRuledRow(
                text = if (isRtl) {
                    "الاستنزاف الشهري: ~${shock.formatMonthlyDrain()} DH كتمشي غير فـ ${leak.subtitleAr}"
                } else {
                    "Drain mensuel : ~${shock.formatMonthlyDrain()} DH/mois absorbés en petits extras"
                },
                dotColor = Color(0xFFD32F2F),
                isRtl = isRtl
            )

            // Line 3: Choc annuel
            NotebookRuledRow(
                text = if (isRtl) {
                    "الصدمة السنوية: ${shock.formatYearlyDrain()} درهم فالعام كتسلت بالدرهم الصغير!"
                } else {
                    "Choc annuel : ${shock.formatYearlyDrain()} DH/an volatilisés en micro-dépenses !"
                },
                dotColor = Color(0xFFC2185B),
                isRtl = isRtl
            )

            // Line 4: Opportunité de gain
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

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 4. SECTION 3: THAWABIT / VITAL PILLARS (4 connected notebook lines) ---
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
            // Line 1: Logement
            NotebookRuledRow(
                text = if (isRtl) "🏠 الكراء وطريطمون السكن: أساس الاستقرار النفسي والعائلي" else "🏠 Logement : Pilier vital, aucune impasse possible",
                dotColor = Color(0xFF00796B),
                isRtl = isRtl
            )

            // Line 2: Factures
            NotebookRuledRow(
                text = if (isRtl) "⚡ فواتير الماء والضوء والأنترنت: واجبات ترشد بلا تضييق مفرط" else "⚡ Factures d'énergie : Optimiser avec bon sens",
                dotColor = HighlighterYellow,
                isRtl = isRtl
            )

            // Line 3: Repas maison
            NotebookRuledRow(
                text = if (isRtl) "🥗 التقضية الصحية للدار: كتوفر 60% مقارنة بماكلة الزنقة" else "🥗 Repas maison : Économie de 60% vs alimentation dehors",
                dotColor = HighlighterGreen,
                isRtl = isRtl
            )

            // Line 4: Santé
            NotebookRuledRow(
                text = if (isRtl) "🏥 صحة وتطبيب العائلة: أولوية قصوى غير قابلة لأي تقشف" else "🏥 Santé de la famille : Priorité absolue sans compromis",
                dotColor = Color(0xFF00897B),
                isRtl = isRtl
            )
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 5. SECTION 4: GOAL-SPECIFIC HIDDEN TRAP (2 connected notebook lines) ---
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
            // Line 1: Warning
            NotebookRuledRow(
                text = if (isRtl) "التحذير: ${trap.warningAr}" else "Attention : ${trap.warningFr}",
                dotColor = Color(0xFFD32F2F),
                isRtl = isRtl
            )

            // Line 2: Golden rule
            NotebookRuledRow(
                text = if (isRtl) "وصية الكوتش: ${trap.goldenRuleAr}" else "Règle d'or : ${trap.goldenRuleFr}",
                dotColor = HighlighterGreen,
                isRtl = isRtl
            )
        }

        // na9ez star (Skip 1 blue line)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // --- 6. SECTION 5: PLAN D'AUSTÉRITÉ CIBLÉ (5 connected notebook lines) ---
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

        // na9ez star (Skip 1 blue line)
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
            // Trigger Button sitting on a 29dp blue rule
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
            // Display AI Coach lines directly on blue notebook lines!
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

private data class SavingsArticleItem(
    val id: String,
    val category: String,
    val tagColor: Color,
    val readTime: String,
    val title: String,
    val summary: String,
    val points: List<String>,
    val takeaway: String
)

/**
 * Moroccan Personal Finance Articles Section:
 * Interactive library of practical financial management guides, expandable directly on notebook paper lines.
 * 100% of text lines sit directly on the 29dp blue notebook ruled lines.
 */
@Composable
private fun SavingsArticlesLibrarySection(
    activeGoal: SavingsGoalEntity? = null,
    isRtl: Boolean
) {
    val recommendedId = when (activeGoal?.leisureCategory) {
        "CAFE" -> "art_cafe"
        "SHOPPING" -> "art_24h"
        else -> when {
            activeGoal?.title?.contains("طوارئ", true) == true || activeGoal?.title?.contains("urgence", true) == true -> "art_emergency"
            else -> "art_budget"
        }
    }
    var expandedArticleId by remember(recommendedId) { mutableStateOf<String?>(recommendedId) }

    val rawArticles = remember(isRtl) {
        listOf(
            SavingsArticleItem(
                id = "art_budget",
                category = if (isRtl) "الميزانية" else "Budget",
                tagColor = HighlighterGreen,
                readTime = if (isRtl) "⏱️ 3 دقائق" else "⏱️ 3 min",
                title = if (isRtl) "كيفاش تبني أول ميزانية فـ 15 دقيقة؟" else "Créer son premier budget en 15 minutes",
                summary = if (isRtl) {
                    "قاعدة 50/30/20 وطريقة توزيع الصالير المغربي بذكاء بلا حرمان."
                } else {
                    "La méthode 50/30/20 adaptée au Maroc sans privation."
                },
                points = if (isRtl) listOf(
                    "• قاعدة 50/30/20: 50% للضروريات، 30% كماليات، و20% للتوفير",
                    "• طريقة الأظرفة: قسم كاش الأسبوع فـ أظرفة وتفادى أداء البطاقة",
                    "• مراجعة الأحد: 5 دقائق فـ نهاية الأسبوع لضبط المصاريف"
                ) else listOf(
                    "• Règle 50/30/20 : 50% vital, 30% loisirs, 20% épargne",
                    "• Méthode des enveloppes : Allouer le cash de la semaine en liquide",
                    "• Revue du dimanche : 5 minutes pour faire le point sur ses dépenses"
                ),
                takeaway = if (isRtl) {
                    "💡 الخلاصة: الميزانية ماشي سجن، بل خريطة فين كيمشيو فلوسك."
                } else {
                    "💡 En résumé : Le budget n'est pas une contrainte, c'est une carte."
                }
            ),
            SavingsArticleItem(
                id = "art_cafe",
                category = if (isRtl) "المصاريف" else "Dépenses",
                tagColor = HighlighterYellow,
                readTime = if (isRtl) "⏱️ 2 دقائق" else "⏱️ 2 min",
                title = if (isRtl) "القهاوي والماكلة برا: نقص بلا ما تحرم راسك" else "Café et Restos : Réduire sans se priver",
                summary = if (isRtl) {
                    "الدرهم الصغير فـ القهوة كيتجمع.. ها كيفاش توفر حتى لـ 800 DH."
                } else {
                    "Le piège des petites dépenses et comment garder jusqu'à 800 DH."
                },
                points = if (isRtl) listOf(
                    "• الحسبة الصادمة: 20 DH يومياً = 600 DH/شهر = 7 200 DH فالعام!",
                    "• قهوة واحدة برا: شرب وحيدة مع صحابك، والباقي فالمكتب أو الدار",
                    "• وجبات الدار: وجد غداك فالدار 2 أو 3 مرات فـ السيمانة"
                ) else listOf(
                    "• Le calcul réel : 20 DH/jour = 600 DH/mois = 7 200 DH/an !",
                    "• 1 café extérieur max : Le reste au bureau ou à la maison",
                    "• Déjeuners maison : Préparer ses repas 2 à 3 fois par semaine"
                ),
                takeaway = if (isRtl) {
                    "💡 الخلاصة: ما تقطعش القهوة نهائياً؛ نقص التردد واستمتع بيها."
                } else {
                    "💡 En résumé : Ne supprimez pas le café, réduisez la fréquence."
                }
            ),
            SavingsArticleItem(
                id = "art_emergency",
                category = if (isRtl) "الأمان المالي" else "Sécurité",
                tagColor = HighlighterBlue,
                readTime = if (isRtl) "⏱️ 3 دقائق" else "⏱️ 3 min",
                title = if (isRtl) "صندوق الطوارئ: الدرع الحقيقي قبل أي استثمار" else "Le fonds d'urgence : Votre premier bouclier",
                summary = if (isRtl) {
                    "علاش هو أول خطوة قبل الدار أو الطوموبيل وفين تخليه."
                } else {
                    "La priorité absolue avant tout achat important ou investissement."
                },
                points = if (isRtl) listOf(
                    "• شنو هو: فلوس محطوطة للطوارئ فقط (مرض، عطب، انقطاع)",
                    "• الخطوة الأولى: جمع 1 000 درهم كدرع أولي كيحميك من الكريدي",
                    "• الهدف الكامل: جمع مصاريف شهر إلى 3 أشهر أساسية فالأمان"
                ) else listOf(
                    "• Définition : Liquidités réservées aux seuls imprévus graves",
                    "• Premier palier : 1 000 DH pour bloquer les petites dettes",
                    "• Objectif complet : 1 à 3 mois de charges vitales au chaud"
                ),
                takeaway = if (isRtl) {
                    "💡 الخلاصة: صندوق الطوارئ هو بوليصة تأمين ضد فخ الكريدي."
                } else {
                    "💡 En résumé : Le fonds d'urgence est votre bouclier anti-dettes."
                }
            ),
            SavingsArticleItem(
                id = "art_24h",
                category = if (isRtl) "العادات" else "Habitudes",
                tagColor = HighlighterPink,
                readTime = if (isRtl) "⏱️ 2 دقائق" else "⏱️ 2 min",
                title = if (isRtl) "قاعدة 24 ساعة: السلاح ضد الشوبينغ ونزوات الشراء" else "La règle des 24h contre les achats impulsifs",
                summary = if (isRtl) {
                    "كيفاش تفرق بين الرغبة اللحظية والحاجة الحقيقية وتحمي جيبك."
                } else {
                    "Différencier une envie passagère d'un besoin réel et utile."
                },
                points = if (isRtl) listOf(
                    "• المبدأ: تسنى 24 ساعة قبل أي شراء كمالي فايت 200 درهم",
                    "• قائمة الرغبات: قيد الحاجة فورقة؛ 70% من النزوات كتنسى",
                    "• فخ التخفيضات: شراء كماليات بـ -50% ماشي ربح بل استنزاف"
                ) else listOf(
                    "• Le principe : Attendre 24h pour tout achat plaisir > 200 DH",
                    "• Wishlist : Noter l'article ; 70% des envies passent seules",
                    "• Piège des promos : Acheter à -50% un extra reste une dépense"
                ),
                takeaway = if (isRtl) {
                    "💡 الخلاصة: مهلة 24 ساعة أكبر حليف لجيبك ضد فخ التسويق."
                } else {
                    "💡 En résumé : La patience de 24h est le meilleur filtre d'achats."
                }
            ),
            SavingsArticleItem(
                id = "art_income",
                category = if (isRtl) "الدخل" else "Revenus",
                tagColor = HighlighterYellow,
                readTime = if (isRtl) "⏱️ 3 دقائق" else "⏱️ 3 min",
                title = if (isRtl) "كيفاش تدبر دخل متغير (تجارة، فريلانس)؟" else "Gérer un revenu irrégulier ou variable",
                summary = if (isRtl) {
                    "كيفاش تضبط ميزانيتك وتوفر واخا المدخول ماشي قار كل شهر."
                } else {
                    "Stabiliser son budget et épargner quand les revenus varient."
                },
                points = if (isRtl) listOf(
                    "• الصالير الأدنى: اعتمد معدل أضعف 3 أشهر كأساس لمصاريفك",
                    "• التوفير بالنسبة: 30% فالشهور القوية و10% فالشهور الضعيفة",
                    "• حساب المخزون: خبي الفائض فالشهور المزيانة لتعويض النقص"
                ) else listOf(
                    "• Salaire plancher : Se baser sur ses 3 mois les plus faibles",
                    "• Épargne en % : 30% les bons mois, 10% les mois calmes",
                    "• Compte tampon : Stocker les surplus pour lisser les creux"
                ),
                takeaway = if (isRtl) {
                    "💡 الخلاصة: المرونة والنسبة المئوية هما سر الاستمرار والنجاح."
                } else {
                    "💡 En résumé : Avec des revenus variables, la flexibilité gagne."
                }
            ),
            SavingsArticleItem(
                id = "art_annual",
                category = if (isRtl) "التخطيط" else "Anticipation",
                tagColor = HighlighterGreen,
                readTime = if (isRtl) "⏱️ 3 دقائق" else "⏱️ 3 min",
                title = if (isRtl) "المصاريف السنوية: العيد، الدخول المدرسي، والعطلة" else "Anticiper les dépenses annuelles",
                summary = if (isRtl) {
                    "كيفاش المناسبات السنوية ما تبقاش مفاجأة تضطرك للكريدي."
                } else {
                    "Provisionner chaque mois pour ne jamais subir de choc financier."
                },
                points = if (isRtl) listOf(
                    "• مواعيد قارة: العيد والدخول المدرسي عندهم تاريخ معروف",
                    "• الاقتطاع المسبق: قسم المبلغ على 12 شهر (300 DH/شهر)",
                    "• تفادى الكريدي: ما تمولش مناسبة بدين تبقى تخلص فيه شهور"
                ) else listOf(
                    "• Dates fixes : L'Aïd et la rentrée arrivent le même mois",
                    "• Sinking Funds : Diviser par 12 (ex: 300 DH/mois = 3 600 DH)",
                    "• Zéro crédit : Ne jamais financer un événement par une dette"
                ),
                takeaway = if (isRtl) {
                    "💡 الخلاصة: التخطيط كيحول المصاريف الكبيرة لمبالغ شهرية صغيرة."
                } else {
                    "💡 En résumé : Provisionner transforme les grosses dépenses en miettes."
                }
            )
        )
    }

    val articles = remember(rawArticles, recommendedId, activeGoal) {
        if (activeGoal != null) {
            rawArticles.sortedByDescending { it.id == recommendedId }.map { item ->
                if (item.id == recommendedId) {
                    item.copy(category = if (isRtl) "⭐ موصى به" else "⭐ Recommandé")
                } else item
            }
        } else {
            rawArticles
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- 1. SECTION TITLE on Rule 1 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isRtl) "📚 مكتبة المقالات المالية وتدبير الميزانية" else "📚 Guides pratiques & Gestion financière",
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

        // --- 2. ARTICLES LIST ---
        articles.forEach { article ->
            val isExpanded = expandedArticleId == article.id

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (isExpanded) {
                            val barW = 2.5.dp.toPx()
                            val barX = if (isRtl) size.width - barW / 2 else barW / 2
                            drawLine(
                                color = article.tagColor.copy(alpha = 0.65f),
                                start = Offset(barX, 0f),
                                end = Offset(barX, size.height),
                                strokeWidth = barW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
            ) {
                // Line 1: Header Row (Badge + Title + Time + Toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .clickable {
                            expandedArticleId = if (isExpanded) null else article.id
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(article.tagColor.copy(alpha = 0.45f))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = article.category,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        Text(
                            text = article.title,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = JournalWritingInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = article.readTime,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 11.sp,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                        Text(
                            text = if (isExpanded) "▲" else "▼",
                            fontFamily = PatrickHandFamily,
                            fontSize = 11.sp,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                }

                // Line 2: Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .clickable {
                            expandedArticleId = if (isExpanded) null else article.id
                        },
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = article.summary,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 12.5.sp,
                        color = JournalMutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                // Expanded full article content sitting on rules
                if (isExpanded) {
                    article.points.forEach { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = point,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 12.sp,
                                color = JournalWritingInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.journalBaselineOnRule()
                            )
                        }
                    }

                    // Takeaway row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing),
                            verticalAlignment = Alignment.Bottom
                        ) {
                        Text(
                            text = article.takeaway,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF00796B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                }
            }

            // na9ez star between articles (Skip 1 blue line)
            Spacer(modifier = Modifier.height(JournalRuleSpacing))
        }
    }
}
