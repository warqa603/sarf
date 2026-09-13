package com.cash.guide.feature.groups

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.SettingsRepository
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.data.db.NoteEntity
import com.cash.guide.domain.CalculationImageShareHelper
import com.cash.guide.domain.GroupCategory
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.domain.export.ExcelExportHelper
import com.cash.guide.domain.export.FileExportManager
import com.cash.guide.domain.export.PdfExportHelper
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.JournalActionDelete
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.NotebookCalculationRow
import com.cash.guide.ui.notebook.NotebookPrimaryActionButton
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalVisualOnRule
import com.cash.guide.ui.notebook.resolveJournalFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ActionSheetRuleSpacing: Dp = 42.dp

@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onOpenCalculation: (String) -> Unit,
    onNewCalculationInGroup: (String) -> Unit,
    onOpenNote: (String) -> Unit = {},
    onNewNoteInGroup: () -> Unit = {},
    onOpenChecklist: (String) -> Unit = {},
    onNewChecklistInGroup: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val group = state.group
    val category = state.category

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultCurrency by settingsRepository.defaultCurrency.collectAsState(initial = MoneyUnit.DIRHAM)
    val defaultCurrencySuffix = if (defaultCurrency == MoneyUnit.DIRHAM) {
        stringResource(R.string.currency_dirham)
    } else {
        stringResource(R.string.currency_rial)
    }

    val groupColor = remember(group?.colorHex) {
        group?.let { parseGroupColor(it.colorHex) } ?: HighlighterYellow
    }

    val totalFormatted = JournalLedgerManager.formatTotal(state.totalCentimes, defaultCurrency)

    JournalRuledDocument(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        clearFocusOnTap = true
    ) {
        // Line 1: Back arrow + Group Title with wash on Start, (Total or Count) on End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Back button + Title
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = JournalRuleSpacing)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.cd_back),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Back,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = JournalInk,
                        size = 18.dp
                    )
                }

                // Title with highlighter wash
                val groupTitle = group?.name ?: ""
                Text(
                    text = groupTitle,
                    fontFamily = resolveJournalFont(groupTitle, isRtl),
                    fontSize = if (isArabicScript(groupTitle) || isRtl) 16.5.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    maxLines = 1,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .drawBehind {
                            val washHeight = 22.dp.toPx()
                            val washY = size.height - washHeight + 1.dp.toPx()
                            drawRoundRect(
                                color = groupColor.copy(alpha = 0.50f),
                                topLeft = Offset(-4.dp.toPx(), washY),
                                size = Size(size.width + 8.dp.toPx(), washHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                        .journalBaselineOnRule()
                )
            }

            // End: Total for Calculations, or Count for Notes / Checklists
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (category) {
                    GroupCategory.CALCULATIONS -> {
                        Text(
                            text = totalFormatted,
                            fontFamily = PatrickHandFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )

                        Text(
                            text = defaultCurrencySuffix,
                            fontFamily = if (defaultCurrencySuffix.contains(Regex("[a-zA-Z]"))) PatrickHandFamily else TajawalFamily,
                            fontSize = if (defaultCurrencySuffix.contains(Regex("[a-zA-Z]"))) 13.5.sp else 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                    GroupCategory.NOTES -> {
                        val count = state.notes.size
                        val countText = when (count) {
                            0 -> stringResource(R.string.group_notes_count_zero)
                            1 -> stringResource(R.string.group_notes_count_single)
                            else -> stringResource(R.string.group_notes_count, count)
                        }
                        Text(
                            text = countText,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 13.sp else 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                    GroupCategory.CHECKLISTS -> {
                        val count = state.checklists.size
                        val countText = when (count) {
                            0 -> stringResource(R.string.group_checklists_count_zero)
                            1 -> stringResource(R.string.group_checklists_count_single)
                            else -> stringResource(R.string.group_checklists_count, count)
                        }
                        Text(
                            text = countText,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 13.sp else 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                }
            }
        }

        // Line 2: 1-rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 3: Action button adapted to category
        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
            when (category) {
                GroupCategory.CALCULATIONS -> {
                    NotebookPrimaryActionButton(
                        text = stringResource(R.string.group_detail_new_calc),
                        onClick = { onNewCalculationInGroup(viewModel.groupId) }
                    )
                }
                GroupCategory.NOTES -> {
                    NotebookPrimaryActionButton(
                        text = stringResource(R.string.group_detail_new_note),
                        onClick = onNewNoteInGroup
                    )
                }
                GroupCategory.CHECKLISTS -> {
                    NotebookPrimaryActionButton(
                        text = stringResource(R.string.group_detail_new_checklist),
                        onClick = { viewModel.openCreateChecklistDialog() }
                    )
                }
            }
        }

        // Line 4: 1-rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 5+: Items list based on Category
        when (category) {
            GroupCategory.CALCULATIONS -> {
                val calculations = state.calculations
                if (calculations.isEmpty() && !state.isLoading) {
                    EmptyCategoryRow(stringResource(R.string.group_detail_empty), isRtl)
                } else {
                    calculations.forEachIndexed { index, calc ->
                        val calcCurrency = runCatching { MoneyUnit.valueOf(calc.calculation.currency) }.getOrDefault(MoneyUnit.DIRHAM)
                        val calcTotalFormatted = JournalLedgerManager.formatTotal(calc.totalCentimes, calcCurrency)
                        val calcCurrencySuffix = if (calcCurrency == MoneyUnit.DIRHAM) {
                            stringResource(R.string.currency_dirham)
                        } else {
                            stringResource(R.string.currency_rial)
                        }
                        val emptyItemsStr = stringResource(R.string.share_empty_items)
                        val articlePrefixStr = stringResource(R.string.share_article_prefix)

                        val subtitle = remember(calc, isRtl, emptyItemsStr, articlePrefixStr) {
                            val itemLabels = calc.items
                                .map { it.label.trim() }
                                .filter { it.isNotBlank() }

                            if (itemLabels.isNotEmpty()) {
                                itemLabels.joinToString(if (isRtl) "، " else ", ")
                            } else if (calc.items.isNotEmpty()) {
                                calc.items.indices.map { idx ->
                                    if (isRtl) "$articlePrefixStr \u200E${idx + 1}\u200F" else "$articlePrefixStr ${idx + 1}"
                                }.joinToString(if (isRtl) "، " else ", ")
                            } else {
                                emptyItemsStr
                            }
                        }

                        NotebookCalculationRow(
                            index = index,
                            title = calc.calculation.title,
                            totalAmount = calcTotalFormatted,
                            currencySuffix = calcCurrencySuffix,
                            subtitle = subtitle,
                            isPinned = false,
                            dotColorOverride = groupColor,
                            paymentStatus = calc.calculation.paymentStatus,
                            calcType = calc.calculation.calcType,
                            dueDateEpochMs = calc.calculation.dueDateEpochMs,
                            reminderEnabled = calc.calculation.reminderEnabled,
                            onClick = { onOpenCalculation(calc.calculation.id) },
                            onMoreClick = { viewModel.selectCalculationForAction(calc) }
                        )
                    }
                }
            }
            GroupCategory.NOTES -> {
                val notes = state.notes
                if (notes.isEmpty() && !state.isLoading) {
                    EmptyCategoryRow(stringResource(R.string.group_detail_empty_notes), isRtl)
                } else {
                    val dateFormatter = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
                    notes.forEach { note ->
                        val shortDateStr = dateFormatter.format(Date(note.updatedAtEpochMs))
                        GroupNoteRowItem(
                            note = note,
                            groupColor = groupColor,
                            shortDateStr = shortDateStr,
                            isRtl = isRtl,
                            onClick = { onOpenNote(note.id) },
                            onMoreClick = { viewModel.selectNoteForAction(note) }
                        )
                    }
                }
            }
            GroupCategory.CHECKLISTS -> {
                val checklists = state.checklists
                if (checklists.isEmpty() && !state.isLoading) {
                    EmptyCategoryRow(stringResource(R.string.group_detail_empty_checklists), isRtl)
                } else {
                    val dateFormatter = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
                    checklists.forEach { item ->
                        val shortDateStr = dateFormatter.format(Date(item.checklist.updatedAtEpochMs))
                        GroupChecklistRowItem(
                            item = item,
                            groupColor = groupColor,
                            shortDateStr = shortDateStr,
                            isRtl = isRtl,
                            onClick = { onOpenChecklist(item.checklist.id) },
                            onMoreClick = { viewModel.selectChecklistForAction(item) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing * 5))
    }

    // Create Checklist Dialog
    if (state.showCreateChecklistDialog) {
        var checklistTitle by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateChecklistDialog() },
            containerColor = JournalPaper,
            title = {
                Text(
                    text = stringResource(R.string.group_checklist_dialog_title),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = checklistTitle,
                        onValueChange = { checklistTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .drawBehind {
                                val strokeW = 1.2.dp.toPx()
                                val y = size.height
                                drawLine(
                                    color = JournalInk.copy(alpha = 0.6f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeW,
                                    cap = StrokeCap.Round
                                )
                            }
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        cursorBrush = SolidColor(JournalInk),
                        textStyle = TextStyle(
                            fontFamily = resolveJournalFont(checklistTitle, isRtl),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalInk
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (checklistTitle.isNotBlank()) {
                                coroutineScope.launch {
                                    val newId = viewModel.createChecklistInGroup(checklistTitle)
                                    onNewChecklistInGroup(newId)
                                }
                            }
                        })
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (checklistTitle.isNotBlank()) {
                            coroutineScope.launch {
                                val newId = viewModel.createChecklistInGroup(checklistTitle)
                                onNewChecklistInGroup(newId)
                            }
                        }
                    },
                    enabled = checklistTitle.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.group_dialog_save),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (checklistTitle.isNotBlank()) JournalInk else JournalMutedInk.copy(alpha = 0.4f)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateChecklistDialog() }) {
                    Text(
                        text = stringResource(R.string.group_dialog_cancel),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }

    // Calculation Actions Bottom Sheet
    state.selectedCalculationForAction?.let { calc ->
        GroupCalculationActionsSheet(
            calculationTitle = calc.calculation.title,
            onEdit = {
                viewModel.selectCalculationForAction(null)
                onOpenCalculation(calc.calculation.id)
            },
            onDuplicate = {
                viewModel.duplicateCalculation(calc.calculation.id)
            },
            onShareImage = {
                val groupTitle = group?.name
                CalculationImageShareHelper.shareCalculation(
                    context = context,
                    calculationWithItems = calc,
                    groupName = groupTitle,
                    isRtl = isRtl
                )
                viewModel.selectCalculationForAction(null)
            },
            onExportPdf = {
                val groupTitle = group?.name
                coroutineScope.launch {
                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfExportHelper.exportSingleCalculationPdf(
                            context = context,
                            calculationWithItems = calc,
                            groupName = groupTitle,
                            isRtl = isRtl
                        )
                    }
                    FileExportManager.shareFile(
                        context = context,
                        file = pdfFile,
                        mimeType = FileExportManager.MIME_PDF,
                        subject = calc.calculation.title
                    )
                }
                viewModel.selectCalculationForAction(null)
            },
            onExportExcel = {
                val groupTitle = group?.name
                coroutineScope.launch {
                    val csvFile = withContext(Dispatchers.IO) {
                        ExcelExportHelper.exportSingleCalculation(
                            context = context,
                            calculationWithItems = calc,
                            groupName = groupTitle
                        )
                    }
                    FileExportManager.shareFile(
                        context = context,
                        file = csvFile,
                        mimeType = FileExportManager.MIME_CSV,
                        subject = calc.calculation.title
                    )
                }
                viewModel.selectCalculationForAction(null)
            },
            onRemoveFromGroup = {
                viewModel.removeCalculationFromGroup(calc.calculation.id)
            },
            onDelete = {
                viewModel.promptDeleteCalculation(calc)
            },
            onDismiss = { viewModel.selectCalculationForAction(null) }
        )
    }

    // Note Actions Bottom Sheet
    state.selectedNoteForAction?.let { note ->
        GroupItemActionsSheet(
            title = note.title.ifBlank { stringResource(R.string.app_name) },
            onOpen = {
                viewModel.selectNoteForAction(null)
                onOpenNote(note.id)
            },
            onRemoveFromGroup = {
                viewModel.removeNoteFromGroup(note.id)
            },
            onDelete = {
                viewModel.promptDeleteNote(note)
            },
            onDismiss = { viewModel.selectNoteForAction(null) }
        )
    }

    // Checklist Actions Bottom Sheet
    state.selectedChecklistForAction?.let { checklist ->
        GroupItemActionsSheet(
            title = checklist.checklist.title.ifBlank { stringResource(R.string.app_name) },
            onOpen = {
                viewModel.selectChecklistForAction(null)
                onOpenChecklist(checklist.checklist.id)
            },
            onRemoveFromGroup = {
                viewModel.removeChecklistFromGroup(checklist.checklist.id)
            },
            onDelete = {
                viewModel.promptDeleteChecklist(checklist)
            },
            onDismiss = { viewModel.selectChecklistForAction(null) }
        )
    }

    // Delete Confirmation Dialog (Unified for Calculation, Note, or Checklist)
    val hasItemToDelete = state.calculationToDelete != null || state.noteToDelete != null || state.checklistToDelete != null
    if (hasItemToDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            containerColor = JournalPaper,
            title = {
                Text(
                    text = stringResource(R.string.delete_title),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_body),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 15.sp,
                    color = JournalWritingInk
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        state.calculationToDelete != null -> viewModel.confirmDeleteCalculation()
                        state.noteToDelete != null -> viewModel.confirmDeleteNote()
                        state.checklistToDelete != null -> viewModel.confirmDeleteChecklist()
                    }
                }) {
                    Text(
                        text = stringResource(R.string.delete_confirm),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        color = JournalActionDelete
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(
                        text = stringResource(R.string.delete_cancel),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }
}

@Composable
private fun EmptyCategoryRow(text: String, isRtl: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontFamily = resolveJournalFont(text, isRtl),
            fontSize = if (isRtl) 14.sp else 14.5.sp,
            fontWeight = FontWeight.Normal,
            color = JournalMutedInk.copy(alpha = 0.85f),
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.journalBaselineOnRule()
        )
    }
}

@Composable
private fun GroupNoteRowItem(
    note: NoteEntity,
    groupColor: Color,
    shortDateStr: String,
    isRtl: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val displayTitle = note.title.ifBlank { if (isRtl) "ملاحظة بدون عنوان" else "Note sans titre" }
    val previewContent = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        // Line 1: Dot + Title + (Pin) + Dashed line + Date + More
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
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(y = (-2).dp)
                ) {
                    drawCircle(color = groupColor)
                }

                Text(
                    text = displayTitle,
                    fontFamily = resolveJournalFont(displayTitle, isRtl),
                    fontSize = if (isArabicScript(displayTitle) || isRtl) 15.sp else 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalWritingInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                )

                if (note.isPinned) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.PinFilled,
                        contentDescription = null,
                        tint = JournalInk.copy(alpha = 0.85f),
                        size = 12.dp,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }

            // Connecting dashed line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 6.dp)
                    .drawBehind {
                        val strokeW = 0.85.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalWritingInk.copy(alpha = 0.28f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                        )
                    }
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = shortDateStr,
                    fontFamily = PatrickHandFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = JournalRuleSpacing)
                        .clickable(role = Role.Button, onClick = onMoreClick),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }
        }

        // Line 2: Preview Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = previewContent.ifBlank { "..." },
                fontFamily = resolveJournalFont(previewContent, isRtl),
                fontSize = if (isRtl) 12.5.sp else 13.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    platformStyle = NoFontPadding,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .journalBaselineOnRule()
            )
        }
    }
}

@Composable
private fun GroupChecklistRowItem(
    item: ChecklistWithItems,
    groupColor: Color,
    shortDateStr: String,
    isRtl: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val displayTitle = item.checklist.title.ifBlank { if (isRtl) "قائمة بدون عنوان" else "Checklist sans titre" }
    val total = item.totalCount
    val completed = item.completedCount
    val statusText = if (total > 0 && completed == total) {
        if (isRtl) "مكتمل" else "Terminé"
    } else if (total > 0) {
        "$completed/$total"
    } else {
        shortDateStr
    }

    val previewText = item.sortedItems.take(3).joinToString(", ") { it.text }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        // Line 1: Dot + Title + Dashed line + Status + More
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
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(y = (-2).dp)
                ) {
                    drawCircle(color = groupColor)
                }

                Text(
                    text = displayTitle,
                    fontFamily = resolveJournalFont(displayTitle, isRtl),
                    fontSize = if (isArabicScript(displayTitle) || isRtl) 15.sp else 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalWritingInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                )
            }

            // Connecting dashed line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 6.dp)
                    .drawBehind {
                        val strokeW = 0.85.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalWritingInk.copy(alpha = 0.28f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                        )
                    }
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statusText,
                    fontFamily = PatrickHandFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (total > 0 && completed == total) HighlighterGreen else JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = JournalRuleSpacing)
                        .clickable(role = Role.Button, onClick = onMoreClick),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }
        }

        // Line 2: Items Preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = previewText.ifBlank { "..." },
                fontFamily = resolveJournalFont(previewText, isRtl),
                fontSize = if (isRtl) 12.5.sp else 13.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    platformStyle = NoFontPadding,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .journalBaselineOnRule()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupItemActionsSheet(
    title: String,
    onOpen: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.5.dp)
                        .background(JournalRule.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides layoutDirection
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ActionSheetRuleSpacing)
                        .drawBehind {
                            val strokeW = 0.6.dp.toPx()
                            val y = size.height
                            drawLine(
                                color = JournalRule.copy(alpha = 0.55f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeW
                            )
                        }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title.ifBlank { stringResource(R.string.app_name) },
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        maxLines = 1
                    )
                }

                // Open / Edit
                GroupActionSheetItem(
                    label = stringResource(R.string.action_edit),
                    symbol = HisabiSymbol.Pencil,
                    badgeColor = HighlighterPink.copy(alpha = 0.55f),
                    onClick = onOpen
                )

                // Remove from group
                GroupActionSheetItem(
                    label = stringResource(R.string.action_remove_from_group),
                    symbol = HisabiSymbol.Folder,
                    badgeColor = HighlighterBlue.copy(alpha = 0.55f),
                    onClick = onRemoveFromGroup
                )

                // Delete
                GroupActionSheetItem(
                    label = stringResource(R.string.action_delete),
                    symbol = HisabiSymbol.Trash,
                    badgeColor = JournalActionDelete.copy(alpha = 0.15f),
                    isDestructive = true,
                    onClick = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCalculationActionsSheet(
    calculationTitle: String,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onShareImage: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onExportExcel: (() -> Unit)? = null,
    onRemoveFromGroup: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.5.dp)
                        .background(JournalRule.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides layoutDirection
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ActionSheetRuleSpacing)
                        .drawBehind {
                            val strokeW = 0.6.dp.toPx()
                            val y = size.height
                            drawLine(
                                color = JournalRule.copy(alpha = 0.55f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeW
                            )
                        }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = calculationTitle.ifBlank { stringResource(R.string.app_name) },
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        maxLines = 1
                    )
                }

                // Edit
                GroupActionSheetItem(
                    label = stringResource(R.string.action_edit),
                    symbol = HisabiSymbol.Pencil,
                    badgeColor = HighlighterPink.copy(alpha = 0.55f),
                    onClick = onEdit
                )

                // Duplicate
                GroupActionSheetItem(
                    label = stringResource(R.string.action_duplicate),
                    symbol = HisabiSymbol.Copy,
                    badgeColor = HighlighterYellow.copy(alpha = 0.55f),
                    onClick = onDuplicate
                )

                // Share as Image
                if (onShareImage != null) {
                    GroupActionSheetItem(
                        label = stringResource(R.string.action_share_image),
                        symbol = HisabiSymbol.Share,
                        badgeColor = HighlighterGreen.copy(alpha = 0.55f),
                        onClick = onShareImage
                    )
                }

                // Export PDF
                if (onExportPdf != null) {
                    GroupActionSheetItem(
                        label = stringResource(R.string.action_export_pdf),
                        symbol = HisabiSymbol.Page,
                        badgeColor = HighlighterBlue.copy(alpha = 0.55f),
                        onClick = onExportPdf
                    )
                }

                // Export Excel
                if (onExportExcel != null) {
                    GroupActionSheetItem(
                        label = stringResource(R.string.export_as_excel),
                        symbol = HisabiSymbol.Table,
                        badgeColor = HighlighterYellow.copy(alpha = 0.55f),
                        onClick = onExportExcel
                    )
                }

                // Remove from group
                GroupActionSheetItem(
                    label = stringResource(R.string.action_remove_from_group),
                    symbol = HisabiSymbol.Folder,
                    badgeColor = HighlighterBlue.copy(alpha = 0.55f),
                    onClick = onRemoveFromGroup
                )

                // Delete
                GroupActionSheetItem(
                    label = stringResource(R.string.action_delete),
                    symbol = HisabiSymbol.Trash,
                    badgeColor = JournalActionDelete.copy(alpha = 0.15f),
                    isDestructive = true,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun GroupActionSheetItem(
    label: String,
    symbol: HisabiSymbol,
    badgeColor: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val tintColor = if (isDestructive) JournalActionDelete else JournalInk

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionSheetRuleSpacing)
            .drawBehind {
                val strokeW = 0.6.dp.toPx()
                val y = size.height
                drawLine(
                    color = JournalRule.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .journalVisualOnRule(gapAboveRule = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            HisabiSketchIcon(
                symbol = symbol,
                contentDescription = null,
                tint = tintColor,
                size = 16.dp
            )
        }

        Text(
            text = label,
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 17.5.sp else 19.sp,
            fontWeight = FontWeight.Normal,
            color = tintColor,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.journalBaselineOnRule()
        )
    }
}
