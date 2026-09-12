package com.cash.guide.feature.calculs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.DeleteConfirmationDialog
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.MonthPickerDialog
import com.cash.guide.ui.notebook.NewCalculationSetupSheet
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.NotebookDateGroupBlock
import com.cash.guide.ui.notebook.NotebookHubActionCard
import com.cash.guide.ui.notebook.NotebookSearchField
import com.cash.guide.ui.notebook.NotebookSectionBand
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.SavedCalculationActionsSheet
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.resolveJournalFont
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun CalculsScreen(
    viewModel: CalculsViewModel,
    onNavigateBack: () -> Unit,
    onOpenCalculation: (String) -> Unit,
    onOpenCashRegister: () -> Unit,
    onNewCalculationWithParams: ((title: String, calcType: String, currency: MoneyUnit, templateId: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    var showMonthPicker by remember { mutableStateOf(false) }
    var showNewCalcSetupSheet by remember { mutableStateOf(false) }

    val currentMonthYear = remember(context) {
        val locale = context.resources.configuration.locales[0]
        SimpleDateFormat("MMMM yyyy", locale).format(Date()).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }

    LaunchedEffect(context) {
        viewModel.loadCalculations(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Back button + Pink Highlighter "Calculs" pill on start, Month/Year on end
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = JournalPaper,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Start: Back button + "Calculs" soft pink pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onNavigateBack()
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

                        // Pink Highlighter Pill for Title: "Calculs" / "الحسابات"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighlighterPink.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                                val titleText = stringResource(R.string.home_action_calculs)
                                Text(
                                    text = titleText,
                                    fontFamily = resolveJournalFont(titleText, isRtl),
                                    fontSize = if (isArabicScript(titleText) || isRtl) 15.sp else 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }

                    // End: Current Month/Year (e.g. "Septembre 2026")
                    Text(
                        text = currentMonthYear,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 14.5.sp else 15.sp,
                        color = JournalWritingInk.copy(alpha = 0.80f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Notebook Ruled Body
            JournalRuledDocument(modifier = Modifier.fillMaxSize(), clearFocusOnTap = true) {
                // Line 1: 1 rule spacer
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 2: Search field with calendar picker
                NotebookSearchField(
                    query = state.searchQuery,
                    onQueryChange = { query -> viewModel.updateSearchQuery(query) },
                    onOpenCalendar = { showMonthPicker = true },
                    isDateFiltered = state.selectedDateEpoch != null || state.selectedYear != null
                )

                // Line 3: 1 rule spacer (tna9ez star)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 4: Action Buttons: "Nouveau calcul" + "Rendu de monnaie" (1 rule = 29dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary: Nouveau calcul
                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HighlighterPink.copy(alpha = 0.35f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.home_new_calculation),
                                onClick = { showNewCalcSetupSheet = true }
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Plus,
                                contentDescription = null,
                                tint = JournalWritingInk,
                                size = 13.5.dp
                            )
                            val btnText = stringResource(R.string.home_new_calculation)
                            Text(
                                text = btnText,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 13.5.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Secondary: Rendu de monnaie
                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDBEAFE).copy(alpha = 0.65f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.calculs_action_change_title),
                                onClick = onOpenCashRegister
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Wallet,
                                contentDescription = null,
                                tint = Color(0xFF1E40AF),
                                size = 13.5.dp
                            )
                            val btnText = stringResource(R.string.calculs_action_change_title)
                            Text(
                                text = btnText,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 13.5.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A),
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }

                // Line 5: 1 rule spacer (tna9ez star)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 6: "Calculs du mois" badge (1 rule = 29dp, touching top & bottom lines)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8EDD5))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val calculsDuMoisText = if (isRtl) "حسابات الشهر" else "Calculs du mois"
                        Text(
                            text = calculsDuMoisText,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 13.5.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }

                    val activeFilterDisplay = remember(state.selectedYear, state.selectedMonth, state.selectedDateEpoch, context) {
                        val locale = context.resources.configuration.locales[0]
                        when {
                            state.selectedYear != null && state.selectedMonth != null -> {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, state.selectedYear!!)
                                    set(Calendar.MONTH, state.selectedMonth!! - 1)
                                }
                                SimpleDateFormat("MMMM yyyy", locale).format(cal.time).replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                                }
                            }
                            state.selectedDateEpoch != null -> {
                                SimpleDateFormat("d MMMM yyyy", locale).format(Date(state.selectedDateEpoch!!))
                            }
                            else -> null
                        }
                    }

                    if (activeFilterDisplay != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .height(JournalRuleSpacing)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighlighterYellow.copy(alpha = 0.50f))
                                .clickable { viewModel.clearDateFilter() }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$activeFilterDisplay ✕",
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }

                // Recent calculations list
                if (!state.isEmpty) {
                    state.displayDateGroups.forEachIndexed { groupIndex, group ->
                        NotebookDateGroupBlock(
                            header = group.header,
                            calculations = group.calculations,
                            onOpenCalculation = onOpenCalculation,
                            onMoreClick = { viewModel.selectCalculationForAction(it) },
                            searchQuery = state.searchQuery,
                            pinnedCalculationIds = state.pinnedCalculationIds
                        )

                        if (groupIndex < state.displayDateGroups.lastIndex) {
                            Spacer(modifier = Modifier.height(JournalRuleSpacing))
                        }
                    }
                } else if (!state.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (state.isFiltering) {
                                stringResource(R.string.home_no_results_for_date)
                            } else {
                                stringResource(R.string.home_empty_title)
                            },
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 14.5.sp else 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                }

                // Bottom spacing for full scroll clearance
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 4))
            }
        }

        // Action Sheet for selected calculation
        val actionCalc = state.selectedCalculationForAction
        if (actionCalc != null) {
            val isPinned = actionCalc.calculation.id in state.pinnedCalculationIds
            SavedCalculationActionsSheet(
                calculationTitle = actionCalc.calculation.title,
                isPinned = isPinned,
                onTogglePin = { viewModel.togglePin(actionCalc.calculation.id) },
                paymentStatus = actionCalc.calculation.paymentStatus,
                onTogglePaymentStatus = {
                    viewModel.togglePaymentStatus(actionCalc.calculation.id, actionCalc.calculation.paymentStatus)
                },
                calcType = actionCalc.calculation.calcType,
                onToggleCalcType = {
                    viewModel.toggleCalcType(actionCalc.calculation.id, actionCalc.calculation.calcType)
                },
                onEdit = {
                    viewModel.selectCalculationForAction(null)
                    onOpenCalculation(actionCalc.calculation.id)
                },
                onDuplicate = {
                    viewModel.duplicateCalculation(actionCalc) { newId ->
                        viewModel.selectCalculationForAction(null)
                        onOpenCalculation(newId)
                    }
                },
                onDelete = {
                    viewModel.requestDelete(actionCalc)
                },
                onDismiss = {
                    viewModel.selectCalculationForAction(null)
                }
            )
        }

        // Delete Confirmation Dialog
        if (state.calculationToDelete != null) {
            DeleteConfirmationDialog(
                onConfirmDelete = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }

        // Month Picker Dialog
        if (showMonthPicker) {
            val cal = remember { Calendar.getInstance() }
            MonthPickerDialog(
                initialYear = state.selectedYear ?: cal.get(Calendar.YEAR),
                initialMonth = state.selectedMonth ?: (cal.get(Calendar.MONTH) + 1),
                onDismiss = { showMonthPicker = false },
                onSelectMonth = { year, month ->
                    showMonthPicker = false
                    viewModel.filterByMonth(year, month)
                }
            )
        }

        // New Calculation Setup Sheet
        if (showNewCalcSetupSheet && onNewCalculationWithParams != null) {
            NewCalculationSetupSheet(
                onDismiss = { showNewCalcSetupSheet = false },
                onConfirm = { title, calcType, currency, templateId ->
                    showNewCalcSetupSheet = false
                    onNewCalculationWithParams(title, calcType, currency, templateId)
                }
            )
        }
    }
}
