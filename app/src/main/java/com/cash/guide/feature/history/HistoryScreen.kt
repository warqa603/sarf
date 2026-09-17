package com.cash.guide.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cash.guide.data.TemplateRepository
import com.cash.guide.domain.CalculationImageShareHelper
import com.cash.guide.domain.export.ExcelExportHelper
import com.cash.guide.domain.export.FileExportManager
import com.cash.guide.domain.export.PdfExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.DateGroupHelper
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.DeleteConfirmationDialog
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalCalculationRow
import com.cash.guide.ui.notebook.JournalDateRuleBand
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalRuleSpacing
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.cash.guide.ui.notebook.JournalLazyRuledDocument
import com.cash.guide.ui.notebook.MonthPickerDialog
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.NotebookCalculationRow
import com.cash.guide.ui.notebook.NotebookDateGroupBlock
import com.cash.guide.ui.notebook.NotebookPrimaryActionButton
import com.cash.guide.ui.notebook.NotebookSearchField
import com.cash.guide.ui.notebook.NotebookSegmentedControl
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.SavedCalculationActionsSheet
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.NotebookMetrics
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.feature.groups.AssignToGroupDialog
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    initialQuery: String? = null,
    onOpenCalculation: (String) -> Unit,
    onNewCalculation: (() -> Unit)? = null,
    onOpenMonthCalculations: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    var showMonthPicker by remember { mutableStateOf(false) }
    var calcToAssignToGroup by remember { mutableStateOf<com.cash.guide.data.db.CalculationWithItems?>(null) }

    val currentMonthYear = remember(context) {
        val locale = context.resources.configuration.locales[0]
        val sdf = java.text.SimpleDateFormat("LLLL yyyy", locale)
        val raw = sdf.format(java.util.Date())
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    LaunchedEffect(context) {
        viewModel.loadAll(context)
        if (!initialQuery.isNullOrBlank()) {
            viewModel.updateSearchQuery(initialQuery)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        JournalLazyRuledDocument(modifier = Modifier.fillMaxSize(), clearFocusOnTap = true) {
            // Line 1: Header Band (Compact "Historique" / "السجل" + Month Year sitting directly on the ruled line)
            item(key = "history_header_band") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.history_title),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 16.5.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = currentMonthYear,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 13.5.sp else 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = JournalMutedInk.copy(alpha = 0.85f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
            }

            // Line 2: 1 rule spacer
            item(key = "history_spacer_1") {
                Spacer(modifier = Modifier.height(JournalRuleSpacing))
            }

            // Line 3: Ruled-Line Search Row with Calendar icon popup
            item(key = "history_search_field") {
                NotebookSearchField(
                    query = state.searchQuery,
                    onQueryChange = { query -> viewModel.updateSearchQuery(query) },
                    onOpenCalendar = { showMonthPicker = true }
                )
            }

            // Line 4: 1 rule spacer
            item(key = "history_spacer_2") {
                Spacer(modifier = Modifier.height(JournalRuleSpacing))
            }

            // Line 5: Section Header & Filter: "Tous les calculs" on Start, "Tout / Ce mois" on End (sitting directly on the blue line)
            item(key = "history_filter_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .drawBehind {
                                val h = size.height
                                val w = size.width
                                val washHeight = 21.dp.toPx()
                                val washCenterY = h - 6.5.dp.toPx()
                                val washY = washCenterY - (washHeight / 2f)
                                val padH = 8.dp.toPx()
                                drawRoundRect(
                                    color = HighlighterBlue.copy(alpha = 0.55f),
                                    topLeft = Offset(-padH, washY),
                                    size = Size(w + padH * 2, washHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.history_section_title),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 14.sp else 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }

                    NotebookSegmentedControl(
                        options = listOf(
                            HistoryFilter.ALL to stringResource(R.string.history_filter_all),
                            HistoryFilter.THIS_MONTH to stringResource(R.string.history_filter_month)
                        ),
                        selectedOption = state.selectedFilter,
                        onSelectOption = { viewModel.setFilter(it) }
                    )
                }
            }

            // Line 8+: Results or Date Groups
            if (state.isSearching) {
                if (state.searchResults.isNotEmpty()) {
                    itemsIndexed(
                        items = state.searchResults,
                        key = { _, calc -> calc.calculation.id }
                    ) { idx, calc ->
                        val currency = runCatching { MoneyUnit.valueOf(calc.calculation.currency) }.getOrDefault(MoneyUnit.DIRHAM)
                        val totalFormatted = JournalLedgerManager.formatTotal(calc.totalCentimes, currency)
                        val currencySuffix = if (currency == MoneyUnit.DIRHAM) {
                            stringResource(R.string.currency_dirham)
                        } else {
                            stringResource(R.string.currency_rial)
                        }

                        NotebookCalculationRow(
                            index = idx,
                            title = calc.calculation.title,
                            totalAmount = totalFormatted,
                            currencySuffix = currencySuffix,
                            paymentStatus = calc.calculation.paymentStatus,
                            calcType = calc.calculation.calcType,
                            dueDateEpochMs = calc.calculation.dueDateEpochMs,
                            reminderEnabled = calc.calculation.reminderEnabled,
                            onClick = { onOpenCalculation(calc.calculation.id) },
                            onMoreClick = { viewModel.selectCalculationForAction(calc) }
                        )
                    }
                } else {
                    // Empty search result on 1 notebook line
                    item(key = "history_empty_search") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.history_no_results_title),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 14.5.sp else 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.journalBaselineOnRule()
                            )
                        }
                    }
                }
            } else {
                if (state.allDateGroups.isNotEmpty()) {
                    itemsIndexed(
                        items = state.allDateGroups,
                        key = { _, group -> group.header }
                    ) { groupIndex, group ->
                        NotebookDateGroupBlock(
                            header = group.header,
                            calculations = group.calculations,
                            onOpenCalculation = onOpenCalculation,
                            onMoreClick = { viewModel.selectCalculationForAction(it) },
                            searchQuery = state.searchQuery
                        )

                        // 1 empty notebook line spacer after each date group
                        if (groupIndex < state.allDateGroups.lastIndex) {
                            Spacer(modifier = Modifier.height(JournalRuleSpacing))
                        }
                    }
                } else if (!state.isLoading) {
                    // Empty history on 1 notebook line
                    item(key = "history_empty_state") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.home_empty_title),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 14.5.sp else 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.journalBaselineOnRule()
                            )
                        }
                    }
                }
            }

            // Bottom Spacers: 5 notebook lines for full scrolling clearance above dock
            item(key = "history_bottom_spacer") {
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 5))
            }
        }

        // Action Sheet
        val actionCalc = state.selectedCalculationForAction
        if (actionCalc != null) {
            SavedCalculationActionsSheet(
                calculationTitle = actionCalc.calculation.title,
                onEdit = {
                    onOpenCalculation(actionCalc.calculation.id)
                    viewModel.selectCalculationForAction(null)
                },
                onDuplicate = {
                    viewModel.duplicateCalculation(actionCalc) { newId ->
                        onOpenCalculation(newId)
                    }
                    viewModel.selectCalculationForAction(null)
                },
                onSaveAsTemplate = {
                    coroutineScope.launch {
                        val nonBlankItems = actionCalc.items
                            .sortedBy { it.position }
                            .map { it.label.trim() }
                            .filter { it.isNotBlank() }
                        TemplateRepository.getInstance(context).saveCustomTemplate(
                            title = actionCalc.calculation.title,
                            calcType = actionCalc.calculation.calcType,
                            currency = actionCalc.calculation.currency,
                            itemLabels = nonBlankItems
                        )
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.template_saved_success),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    viewModel.selectCalculationForAction(null)
                },
                onShareImage = {
                    val calcToShare = actionCalc
                    coroutineScope.launch {
                        val groupName = calcToShare.calculation.groupId?.let { gid ->
                            viewModel.repository.getGroup(gid)?.name
                        }
                        CalculationImageShareHelper.shareCalculation(
                            context = context,
                            calculationWithItems = calcToShare,
                            groupName = groupName,
                            isRtl = isRtl
                        )
                    }
                    viewModel.selectCalculationForAction(null)
                },
                onExportPdf = {
                    val calcToExport = actionCalc
                    coroutineScope.launch {
                        val groupName = calcToExport.calculation.groupId?.let { gid ->
                            viewModel.repository.getGroup(gid)?.name
                        }
                        val pdfFile = withContext(Dispatchers.IO) {
                            PdfExportHelper.exportSingleCalculationPdf(
                                context = context,
                                calculationWithItems = calcToExport,
                                groupName = groupName,
                                isRtl = isRtl
                            )
                        }
                        FileExportManager.shareFile(
                            context = context,
                            file = pdfFile,
                            mimeType = FileExportManager.MIME_PDF,
                            subject = calcToExport.calculation.title
                        )
                    }
                    viewModel.selectCalculationForAction(null)
                },
                onExportExcel = {
                    val calcToExport = actionCalc
                    coroutineScope.launch {
                        val groupName = calcToExport.calculation.groupId?.let { gid ->
                            viewModel.repository.getGroup(gid)?.name
                        }
                        val csvFile = withContext(Dispatchers.IO) {
                            ExcelExportHelper.exportSingleCalculation(
                                context = context,
                                calculationWithItems = calcToExport,
                                groupName = groupName
                            )
                        }
                        FileExportManager.shareFile(
                            context = context,
                            file = csvFile,
                            mimeType = FileExportManager.MIME_CSV,
                            subject = calcToExport.calculation.title
                        )
                    }
                    viewModel.selectCalculationForAction(null)
                },
                onAssignToGroup = {
                    calcToAssignToGroup = actionCalc
                },
                onDelete = {
                    viewModel.requestDelete(actionCalc)
                },
                onDismiss = {
                    viewModel.selectCalculationForAction(null)
                }
            )
        }

        // Assign to Group Dialog
        calcToAssignToGroup?.let { calc ->
            AssignToGroupDialog(
                calculationId = calc.calculation.id,
                currentGroupId = calc.calculation.groupId,
                calculationRepository = viewModel.repository,
                onDismiss = { calcToAssignToGroup = null },
                onAssigned = {
                    calcToAssignToGroup = null
                    viewModel.loadAll(context)
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

        // Month Picker Dialog Popup when calendar icon is clicked
        if (showMonthPicker) {
            val cal = remember { java.util.Calendar.getInstance() }
            MonthPickerDialog(
                initialYear = cal.get(java.util.Calendar.YEAR),
                initialMonth = cal.get(java.util.Calendar.MONTH) + 1,
                onDismiss = { showMonthPicker = false },
                onSelectMonth = { year, month ->
                    showMonthPicker = false
                    onOpenMonthCalculations?.invoke(year, month)
                }
            )
        }
    }
}
