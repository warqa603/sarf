package com.cash.guide.feature.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextOverflow
import com.cash.guide.ui.notebook.NotebookRemindersSheet
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import java.util.Locale
import com.cash.guide.domain.CalculationImageShareHelper
import com.cash.guide.domain.export.ExcelExportHelper
import com.cash.guide.domain.export.FileExportManager
import com.cash.guide.domain.export.PdfExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.DeleteConfirmationDialog
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.JournalBaselineHighlightedText
import com.cash.guide.ui.notebook.JournalCalculationRow
import com.cash.guide.ui.notebook.JournalDateRuleBand
import com.cash.guide.ui.notebook.getDateTimelineStyle
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.JournalNewCalculationButton
import com.cash.guide.ui.notebook.JournalRecentHeader
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.resolveJournalFont
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.SavedCalculationActionsSheet
import com.cash.guide.ui.notebook.CreditDueDateDialog
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.cash.guide.feature.groups.AssignToGroupDialog
import com.cash.guide.app.LocalizedContextWrapper
import com.cash.guide.domain.DateGroupHelper
import com.cash.guide.domain.RecentActivityItem
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.NotebookDateGroupBlock
import com.cash.guide.ui.notebook.NotebookActivityDateGroupBlock
import com.cash.guide.ui.notebook.NotebookActivityTimelineBlock
import com.cash.guide.ui.notebook.NotebookActivityActionsSheet
import com.cash.guide.ui.notebook.NotebookSpeedDialFab
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.NotebookSearchField
import com.cash.guide.ui.notebook.NotebookSectionBand
import com.cash.guide.domain.ChecklistShareHelper
import com.cash.guide.domain.NoteShareHelper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cash.guide.ui.notebook.MonthPickerDialog
import com.cash.guide.ui.notebook.NewCalculationSetupSheet
import java.text.SimpleDateFormat
import java.util.Date
import com.cash.guide.data.TemplateRepository
import com.cash.guide.data.db.CalculationWithItems

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is LocalizedContextWrapper -> originalActivity ?: baseContext.findActivity()
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewCalculation: () -> Unit = {},
    onNewCalculationWithParams: ((title: String, calcType: String, currency: MoneyUnit, templateId: String?) -> Unit)? = null,
    onOpenCalculation: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMonthCalculations: (year: Int, month: Int) -> Unit = { _, _ -> },
    onOpenStyleShowcase: () -> Unit = {},
    onOpenCalculs: () -> Unit = {},
    onOpenCashRegister: () -> Unit = {},
    onOpenGroups: () -> Unit = {},
    onOpenGroup: (String) -> Unit = {},
    onOpenChecklist: () -> Unit = {},
    onOpenChecklistWithId: (String) -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenNote: (String) -> Unit = {},
    onNewChecklist: () -> Unit = {},
    onNewNote: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onNewReminder: () -> Unit = {},
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
    var showRemindersSheet by remember { mutableStateOf(false) }
    var showNewCalcSetupSheet by remember { mutableStateOf(false) }
    var calcToAssignToGroup by remember { mutableStateOf<com.cash.guide.data.db.CalculationWithItems?>(null) }
    var creditDueDateCalc by remember { mutableStateOf<com.cash.guide.data.db.CalculationWithItems?>(null) }
    val currentMonthYear = remember(context) {
        val locale = context.resources.configuration.locales[0]
        SimpleDateFormat("MMMM yyyy", locale).format(Date()).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }

    LaunchedEffect(context) {
        viewModel.loadRecent(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        JournalRuledDocument(modifier = Modifier.fillMaxSize(), clearFocusOnTap = true) {
            // Line 1: Header Band (Compact "Hssabi" / "حسابي" + Month Year sitting directly on the ruled line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val greetingPrefix = stringResource(R.string.home_greeting_prefix)
                val greetingAnnotated = remember(greetingPrefix, state.userName) {
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Light,
                                color = JournalInk.copy(alpha = 0.72f)
                            )
                        ) {
                            append("$greetingPrefix ")
                        }
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = JournalInk
                            )
                        ) {
                            append(state.userName)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = greetingAnnotated,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 15.5.sp else 16.5.sp,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                    Text(
                        text = "😊",
                        fontSize = 14.sp,
                        modifier = Modifier.offset(y = (-3).dp)
                    )
                }

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

            // Line 2: 1 rule spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Line 3: Ruled-Line Search Row with Calendar icon popup
            NotebookSearchField(
                query = state.searchQuery,
                onQueryChange = { query -> viewModel.updateSearchQuery(query) },
                onOpenCalendar = { showMonthPicker = true },
                isDateFiltered = state.selectedDateEpoch != null
            )

            // Line 4: 1 rule spacer (the Espace requested by user)
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Line 5: 4 Category quick cards
            HomeCategoryCardsRow(
                isRtl = isRtl,
                onCalculs = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onOpenCalculs()
                },
                onNotes = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onOpenNotes()
                },
                onChecklists = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onOpenChecklist()
                },
                onRappels = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onOpenReminders()
                }
            )

            // Line 6: 1 rule spacer between category cards and week reminders card
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Line 7, 8 & 9: Auto-swiping Week Reminders Carousel Card (takes 3 spaces)
            HomeWeekRemindersCarousel(
                reminders = state.weekReminders,
                isRtl = isRtl,
                onOpenCalculation = onOpenCalculation
            )

            // 1 rule spacer before content
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Main Content: Filtered Search Results OR the 3 sections (Notes, Checklists, Calculs)
            if (state.isFiltering) {
                // When actively searching or filtering, show filtered groups
                if (state.displayActivityGroups.isNotEmpty()) {
                    state.displayActivityGroups.forEachIndexed { groupIndex, group ->
                        NotebookActivityDateGroupBlock(
                            header = group.header,
                            items = group.items,
                            onOpenCalculation = onOpenCalculation,
                            onOpenChecklist = onOpenChecklistWithId,
                            onOpenNote = onOpenNote,
                            onMoreClick = { item -> viewModel.selectActivityForAction(item) },
                            searchQuery = state.searchQuery
                        )
                        if (groupIndex < state.displayActivityGroups.lastIndex) {
                            Spacer(modifier = Modifier.height(JournalRuleSpacing))
                        }
                    }
                }
            } else if (!state.isActivityEmpty) {
                // === SECTION 1: Mes Notes ===
                HomeSectionHeaderRow(
                    title = stringResource(R.string.home_section_my_notes),
                    dotColor = Color(0xFFF59E0B),
                    highlighterColor = HighlighterYellow,
                    isRtl = isRtl,
                    onSeeAll = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onOpenNotes()
                    }
                )
                if (state.recentNotes.isNotEmpty()) {
                    NotebookActivityTimelineBlock(
                        items = state.recentNotes,
                        onOpenCalculation = onOpenCalculation,
                        onOpenChecklist = onOpenChecklistWithId,
                        onOpenNote = onOpenNote,
                        onMoreClick = { item -> viewModel.selectActivityForAction(item) },
                        searchQuery = state.searchQuery,
                        showIcon = false
                    )
                } else {
                    HomeSectionEmptyRow(
                        emptyText = stringResource(R.string.home_empty_notes),
                        isRtl = isRtl
                    )
                }

                // 1 rule spacer between sections
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // === SECTION 2: Mes Checklists ===
                HomeSectionHeaderRow(
                    title = stringResource(R.string.home_section_my_checklists),
                    dotColor = Color(0xFF10B981),
                    highlighterColor = HighlighterGreen,
                    isRtl = isRtl,
                    onSeeAll = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onOpenChecklist()
                    }
                )
                if (state.recentChecklists.isNotEmpty()) {
                    NotebookActivityTimelineBlock(
                        items = state.recentChecklists,
                        onOpenCalculation = onOpenCalculation,
                        onOpenChecklist = onOpenChecklistWithId,
                        onOpenNote = onOpenNote,
                        onMoreClick = { item -> viewModel.selectActivityForAction(item) },
                        searchQuery = state.searchQuery,
                        showIcon = false
                    )
                } else {
                    HomeSectionEmptyRow(
                        emptyText = stringResource(R.string.home_empty_checklists),
                        isRtl = isRtl
                    )
                }

                // 1 rule spacer between sections
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // === SECTION 3: Mes Calculs ===
                HomeSectionHeaderRow(
                    title = stringResource(R.string.home_section_my_calculs),
                    dotColor = Color(0xFFEF4444),
                    highlighterColor = HighlighterPink,
                    isRtl = isRtl,
                    onSeeAll = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onOpenCalculs()
                    }
                )
                if (state.recentCalculations.isNotEmpty()) {
                    NotebookActivityTimelineBlock(
                        items = state.recentCalculations,
                        onOpenCalculation = onOpenCalculation,
                        onOpenChecklist = onOpenChecklistWithId,
                        onOpenNote = onOpenNote,
                        onMoreClick = { item -> viewModel.selectActivityForAction(item) },
                        searchQuery = state.searchQuery,
                        showIcon = false
                    )
                } else {
                    HomeSectionEmptyRow(
                        emptyText = stringResource(R.string.home_empty_calculs),
                        isRtl = isRtl
                    )
                }
            } else if (!state.isLoading) {
                // Empty state sitting directly on the ruled line
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

            // Bottom Spacers: 5 notebook lines for full scrolling clearance above dock
            Spacer(modifier = Modifier.height(JournalRuleSpacing * 5))
        }

        // Scrim overlay when FAB speed dial is open
        if (state.isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.setFabExpanded(false) }
                    )
            )
        }

        // Notebook Speed Dial FAB (52dp pink circle expanding into 3 capsules)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 18.dp, bottom = 18.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            NotebookSpeedDialFab(
                isExpanded = state.isFabExpanded,
                onToggle = { viewModel.toggleFabExpanded() },
                onDismiss = { viewModel.setFabExpanded(false) },
                onNewCalcul = {
                    viewModel.setFabExpanded(false)
                    showNewCalcSetupSheet = true
                },
                onNewChecklist = {
                    viewModel.setFabExpanded(false)
                    onOpenChecklist()
                },
                onNewNote = {
                    viewModel.setFabExpanded(false)
                    onOpenNotes()
                },
                onNewReminder = {
                    viewModel.setFabExpanded(false)
                    onNewReminder()
                }
            )
        }

        // Action Sheet
        val actionCalc = state.selectedCalculationForAction
        if (actionCalc != null) {
            val isPinned = actionCalc.calculation.id in state.pinnedCalculationIds
            SavedCalculationActionsSheet(
                calculationTitle = actionCalc.calculation.title,
                isPinned = isPinned,
                onTogglePin = {
                    viewModel.togglePin(actionCalc.calculation.id)
                },
                paymentStatus = actionCalc.calculation.paymentStatus,
                onTogglePaymentStatus = {
                    viewModel.togglePaymentStatus(actionCalc.calculation.id, actionCalc.calculation.paymentStatus)
                },
                calcType = actionCalc.calculation.calcType,
                onToggleCalcType = {
                    viewModel.toggleCalcType(actionCalc.calculation.id, actionCalc.calculation.calcType)
                },
                onSetDueDate = {
                    creditDueDateCalc = actionCalc
                    viewModel.selectCalculationForAction(null)
                },
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

        // Action Sheet for Checklist and Note activities
        val selectedAct = state.selectedActivityForAction
        if (selectedAct != null && selectedAct !is RecentActivityItem.CalculationActivity) {
            val actTitle = when (selectedAct) {
                is RecentActivityItem.ChecklistActivity -> selectedAct.checklistWithItems.checklist.title
                is RecentActivityItem.NoteActivity -> selectedAct.note.title
                else -> ""
            }
            NotebookActivityActionsSheet(
                title = actTitle,
                onOpen = {
                    when (selectedAct) {
                        is RecentActivityItem.ChecklistActivity -> onOpenChecklistWithId(selectedAct.checklistWithItems.checklist.id)
                        is RecentActivityItem.NoteActivity -> onOpenNote(selectedAct.note.id)
                        else -> Unit
                    }
                    viewModel.selectActivityForAction(null)
                },
                onShare = {
                    coroutineScope.launch {
                        when (selectedAct) {
                            is RecentActivityItem.ChecklistActivity -> {
                                ChecklistShareHelper.shareAsImage(
                                    context = context,
                                    checklistId = selectedAct.checklistWithItems.checklist.id,
                                    title = selectedAct.checklistWithItems.checklist.title,
                                    items = selectedAct.checklistWithItems.items,
                                    isRtl = isRtl
                                )
                            }
                            is RecentActivityItem.NoteActivity -> {
                                NoteShareHelper.shareAsImage(context, selectedAct.note, isRtl)
                            }
                            else -> Unit
                        }
                    }
                    viewModel.selectActivityForAction(null)
                },
                onDelete = {
                    viewModel.promptDeleteActivity(selectedAct)
                },
                onDismiss = {
                    viewModel.selectActivityForAction(null)
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
                    viewModel.loadRecent(context)
                }
            )
        }

        // Credit Due Date & Reminder Dialog
        if (creditDueDateCalc != null) {
            val targetCalc = creditDueDateCalc!!
            CreditDueDateDialog(
                initialDueDateEpochMs = targetCalc.calculation.dueDateEpochMs,
                initialReminderEnabled = targetCalc.calculation.reminderEnabled,
                initialReminderTimeEpochMs = targetCalc.calculation.reminderTimeEpochMs,
                onSave = { dueDate, reminderEnabled, reminderTime ->
                    viewModel.updateCreditDueDate(
                        context = context,
                        calculation = targetCalc,
                        dueDateEpochMs = dueDate,
                        reminderEnabled = reminderEnabled,
                        reminderTimeEpochMs = reminderTime
                    )
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.due_date_saved_toast),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    creditDueDateCalc = null
                },
                onClear = {
                    viewModel.updateCreditDueDate(
                        context = context,
                        calculation = targetCalc,
                        dueDateEpochMs = null,
                        reminderEnabled = false,
                        reminderTimeEpochMs = null
                    )
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.due_date_cleared_toast),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    creditDueDateCalc = null
                },
                onDismiss = { creditDueDateCalc = null }
            )
        }

        // Delete Confirmation Dialog
        if (state.calculationToDelete != null || state.activityToDelete != null) {
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
                    onOpenMonthCalculations(year, month)
                }
            )
        }

        // New Calculation Setup Sheet (Personnel vs Credit, Title, Currency, Templates)
        if (showNewCalcSetupSheet && onNewCalculationWithParams != null) {
            NewCalculationSetupSheet(
                onDismiss = { showNewCalcSetupSheet = false },
                onConfirm = { title, calcType, currency, templateId ->
                    showNewCalcSetupSheet = false
                    onNewCalculationWithParams(title, calcType, currency, templateId)
                }
            )
        }

        // Reminders & Due Dates Bottom Sheet
        if (showRemindersSheet) {
            NotebookRemindersSheet(
                reminders = state.reminderCalculations,
                onOpenCalculation = { calcId ->
                    showRemindersSheet = false
                    onOpenCalculation(calcId)
                },
                onDismiss = { showRemindersSheet = false }
            )
        }
    }
}

/**
 * 4 Category Quick Cards placed directly below the Search Bar.
 * Fits neatly between two ruled lines (height = JournalRuleSpacing = 29dp).
 * Features a minimalist ink outline, black text, and permanent category indicator dots on the left:
 * - Calculs: Red / Rose
 * - Notes: Amber / Yellow
 * - Checklists: Emerald Green
 * - Rappels: Soft Blue
 */
@Composable
private fun HomeCategoryCardsRow(
    isRtl: Boolean,
    onCalculs: () -> Unit,
    onNotes: () -> Unit,
    onChecklists: () -> Unit,
    onRappels: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CategoryQuickCard(
            title = stringResource(R.string.home_category_calculs),
            dotColor = Color(0xFFEF4444),
            isRtl = isRtl,
            onClick = onCalculs,
            modifier = Modifier.weight(1f)
        )
        CategoryQuickCard(
            title = stringResource(R.string.home_category_notes),
            dotColor = Color(0xFFF59E0B),
            isRtl = isRtl,
            onClick = onNotes,
            modifier = Modifier.weight(1f)
        )
        CategoryQuickCard(
            title = stringResource(R.string.home_category_checklists),
            dotColor = Color(0xFF10B981),
            isRtl = isRtl,
            onClick = onChecklists,
            modifier = Modifier.weight(1f)
        )
        CategoryQuickCard(
            title = stringResource(R.string.home_category_rappels),
            dotColor = Color(0xFF3B82F6),
            isRtl = isRtl,
            onClick = onRappels,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryQuickCard(
    title: String,
    dotColor: Color,
    isRtl: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(
                width = 0.95.dp,
                color = JournalWritingInk.copy(alpha = 0.85f),
                shape = RoundedCornerShape(7.dp)
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Colored dot permanently representing the category on the left
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color = dotColor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontFamily = resolveJournalFont(title, isRtl),
                    fontSize = if (isArabicScript(title) || isRtl) 11.5.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = JournalWritingInk,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Auto-swiping horizontal carousel card showing reminders of the week.
 * Placed neatly on the notebook rules below the 4 category cards.
 * Height: 58.dp (2 ruled lines), black ink outline (0.95dp), clean paper background.
 * Cycles every 3.5s when there are multiple reminders.
 */
@Composable
private fun HomeWeekRemindersCarousel(
    reminders: List<CalculationWithItems>,
    isRtl: Boolean,
    onOpenCalculation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentReminders by rememberUpdatedState(reminders)
    val pagerState = rememberPagerState(pageCount = { currentReminders.size.coerceAtLeast(1) })

    LaunchedEffect(pagerState, reminders.size) {
        if (reminders.size > 1) {
            while (true) {
                delay(3500)
                if (!pagerState.isScrollInProgress) {
                    val count = currentReminders.size
                    if (count > 1) {
                        val targetPage = (pagerState.currentPage + 1) % count
                        try {
                            pagerState.animateScrollToPage(targetPage)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 3)
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(JournalPaper)
            .border(
                width = 0.95.dp,
                color = JournalWritingInk.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (reminders.isEmpty()) {
            // Empty state slide
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                    )
                    Text(
                        text = stringResource(R.string.home_week_reminders_title),
                        fontFamily = resolveJournalFont(stringResource(R.string.home_week_reminders_title), isRtl),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_week_reminders_empty),
                    fontFamily = resolveJournalFont(stringResource(R.string.home_week_reminders_empty), isRtl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = reminders[page]
                val calcTitle = item.calculation.title.ifBlank {
                    stringResource(R.string.home_quick_calculation)
                }
                val reminderEpoch = item.calculation.dueDateEpochMs
                    ?: item.calculation.reminderTimeEpochMs
                    ?: item.calculation.updatedAtEpochMs
                val reminderDateStr = dateFormat.format(Date(reminderEpoch))

                val currencyUnit = runCatching { MoneyUnit.valueOf(item.calculation.currency) }.getOrDefault(MoneyUnit.DIRHAM)
                val totalNumber = JournalLedgerManager.formatTotal(item.totalCentimes, currencyUnit)
                val currencySuffix = when {
                    isRtl -> if (currencyUnit == MoneyUnit.DIRHAM) stringResource(R.string.currency_dirham) else stringResource(R.string.currency_rial)
                    currencyUnit == MoneyUnit.DIRHAM -> "DH"
                    else -> "Rial"
                }
                val totalFormatted = "$totalNumber $currencySuffix"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            role = Role.Button,
                            onClick = { onOpenCalculation(item.calculation.id) }
                        )
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row 1 (Top): Category dot + Tag ("Rappels de la semaine") + Dots indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF3B82F6), CircleShape)
                            )
                            Text(
                                text = stringResource(R.string.home_week_reminders_title),
                                fontFamily = resolveJournalFont(stringResource(R.string.home_week_reminders_title), isRtl),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        // Slide indicator dots
                        if (reminders.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                repeat(reminders.size.coerceAtMost(6)) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (index == page) 5.dp else 3.5.dp)
                                            .background(
                                                color = if (index == page) Color(0xFF3B82F6) else JournalMutedInk.copy(alpha = 0.35f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Row 2 (Middle): Calculation Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = calcTitle,
                            fontFamily = resolveJournalFont(calcTitle, isRtl),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JournalWritingInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    // Row 3 (Bottom): Reminder date with blue bell icon on start, Amount on end
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Bell,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                size = 13.dp
                            )
                            Text(
                                text = reminderDateStr,
                                fontFamily = PatrickHandFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (item.calculation.dueDateEpochMs != null && item.calculation.dueDateEpochMs <= System.currentTimeMillis() && item.calculation.paymentStatus == "UNPAID") {
                                    Color(0xFFDC2626)
                                } else {
                                    Color(0xFFC2410C)
                                },
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }

                        Text(
                            text = totalFormatted,
                            fontFamily = PatrickHandFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (item.calculation.paymentStatus == "UNPAID") Color(0xFFDC2626) else JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Section Header Row on a single notebook rule line.
 * Start: Colored category pill/dot + Section title (e.g. "Mes notes", "Mes checklists", "Mes calculs").
 * End: Compact "Voir tout" ("voir tout suira") with small handwritten arrow.
 */
@Composable
private fun HomeSectionHeaderRow(
    title: String,
    dotColor: Color,
    highlighterColor: Color,
    isRtl: Boolean,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Section title pill on the start
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(highlighterColor.copy(alpha = 0.40f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.5.dp)
                        .background(dotColor, CircleShape)
                )
                Text(
                    text = title,
                    fontFamily = resolveJournalFont(title, isRtl),
                    fontSize = if (isArabicScript(title) || isRtl) 14.sp else 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        // Compact "Voir tout" ("voir tout suira") on the end
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    role = Role.Button,
                    onClick = onSeeAll
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.home_see_all),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = JournalWritingInk.copy(alpha = 0.75f),
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Canvas(
                modifier = Modifier.size(10.dp, 8.dp)
            ) {
                val strokeW = 1.15.dp.toPx()
                val tint = JournalWritingInk.copy(alpha = 0.75f)
                val midY = size.height / 2f
                if (isRtl) {
                    drawLine(tint, Offset(size.width, midY), Offset(1f, midY), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(tint, Offset(3.5.dp.toPx(), 1f), Offset(1f, midY), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(tint, Offset(3.5.dp.toPx(), size.height - 1f), Offset(1f, midY), strokeWidth = strokeW, cap = StrokeCap.Round)
                } else {
                    drawLine(tint, Offset(0f, midY), Offset(size.width - 1f, midY), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(tint, Offset(size.width - 3.5.dp.toPx(), 1f), Offset(size.width - 1f, midY), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(tint, Offset(size.width - 3.5.dp.toPx(), size.height - 1f), Offset(size.width - 1f, midY), strokeWidth = strokeW, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * Placeholder row shown when a section is empty, sitting directly on a ruled line.
 */
@Composable
private fun HomeSectionEmptyRow(
    emptyText: String,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emptyText,
            fontFamily = resolveJournalFont(emptyText, isRtl),
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = JournalMutedInk.copy(alpha = 0.55f),
            style = TextStyle(platformStyle = NoFontPadding)
        )
    }
}


