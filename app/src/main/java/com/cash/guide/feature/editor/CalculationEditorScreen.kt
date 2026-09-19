package com.cash.guide.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
import com.cash.guide.domain.ai.ExistingCalculationRowContext
import com.cash.guide.ui.components.AiVoiceDockedBottomButton
import com.cash.guide.ui.components.AiVoiceInputTarget
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.JournalKeyboardLanguage
import com.cash.guide.domain.JournalKeyboardMode
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.ActiveField
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.JournalActionConfirm
import com.cash.guide.ui.notebook.JournalActionDelete
import com.cash.guide.ui.notebook.JournalAddRowButton
import com.cash.guide.ui.notebook.JournalCalculatorPopup
import com.cash.guide.ui.notebook.JournalCompactNumericDock
import com.cash.guide.ui.notebook.JournalEntryRow
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalTextKeyboardDock
import com.cash.guide.ui.notebook.JournalTotalResultBand
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.ManropeFamily
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.ExportOptionsBottomSheet
import com.cash.guide.ui.notebook.CreditDueDateDialog
import com.cash.guide.ui.notebook.UnsavedChangesDialog
import com.cash.guide.ui.notebook.MoneyBreakdownSheet
import com.cash.guide.ui.notebook.resolveJournalFont
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.domain.reminder.CreditDueUrgency
import com.cash.guide.domain.reminder.CreditStatusHelper
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculationEditorScreen(
    viewModel: CalculationEditorViewModel,
    calculationId: String? = null,
    initialGroupId: String? = null,
    initialType: String? = null,
    initialCurrency: String? = null,
    initialTitle: String? = null,
    templateId: String? = null,
    onNavigateBack: () -> Unit,
    onOpenCalculation: ((String) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    var showExportSheet by remember { mutableStateOf(false) }
    var showBreakdownSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(calculationId, initialGroupId, initialType, initialCurrency, initialTitle, templateId) {
        val currencyEnum = initialCurrency?.let { runCatching { MoneyUnit.valueOf(it) }.getOrNull() }
        viewModel.loadCalculation(
            id = calculationId,
            initialGroupId = initialGroupId,
            initialType = initialType,
            initialCurrency = currencyEnum,
            initialTitle = initialTitle,
            templateId = templateId
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(enabled = !isImeVisible) {
        viewModel.handleBackPress(onNavigateBack)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JournalPaper)
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            EditorTopBar(
                titleValue = state.title,
                onTitleChange = { viewModel.updateTitle(it) },
                isTitleActive = state.activeField == ActiveField.HEADER_TITLE,
                onTitleClick = { viewModel.selectCalculationTitle() },
                keyboardLanguage = state.keyboardLanguage,
                currency = state.currency,
                onCurrencyToggle = {
                    val next = if (state.currency == MoneyUnit.DIRHAM) MoneyUnit.RIAL else MoneyUnit.DIRHAM
                    viewModel.selectUnit(next)
                },
                paymentStatus = state.paymentStatus,
                onPaymentStatusToggle = { viewModel.togglePaymentStatus() },
                calcType = state.calcType,
                canUndo = state.canUndo,
                onUndoClick = { viewModel.undoDelete() },
                onCalculatorClick = { viewModel.openCalculatorPopup(state.activeRowId) },
                onSaveClick = { 
                    com.cash.guide.domain.ads.AdMobManager.getInstance(context).reportModification()
                    viewModel.saveCalculation(onSuccess = onNavigateBack) 
                },
                onShareClick = { showExportSheet = true },
                onBackClick = { viewModel.handleBackPress(onNavigateBack) }
            )

            // Title validation error
            if (state.validationError == "TITLE_REQUIRED") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(JournalActionDelete.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val errorText = stringResource(R.string.editor_title_required)
                    Text(
                        text = errorText,
                        fontFamily = resolveJournalFont(errorText, isRtl),
                        fontSize = if (isRtl) 13.sp else 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalActionDelete
                    )
                }
            }

            // Credit Due Date & Reminder Bar (when calcType == CREDIT)
            if (state.calcType == "CREDIT") {
                val dueInfo = remember(state.paymentStatus, state.calcType, state.dueDateEpochMs, state.reminderEnabled) {
                    CreditStatusHelper.computeDueInfo(
                        context = context,
                        paymentStatus = state.paymentStatus,
                        calcType = state.calcType,
                        dueDateEpochMs = state.dueDateEpochMs,
                        reminderEnabled = state.reminderEnabled
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .background(JournalPaper)
                        .drawBehind {
                            val strokeW = 0.6.dp.toPx()
                            drawLine(
                                color = JournalRule.copy(alpha = 0.50f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeW
                            )
                        }
                        .clickable(role = Role.Button) {
                            viewModel.setDueDateDialogVisible(true)
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (state.reminderEnabled) "🔔" else "📅",
                            fontSize = 13.sp
                        )
                        val currentDueDate = state.dueDateEpochMs
                        val dateText = if (currentDueDate != null) {
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            stringResource(R.string.credit_banner_due, sdf.format(Date(currentDueDate)))
                        } else {
                            stringResource(R.string.action_set_due_date)
                        }
                        Text(
                            text = dateText,
                            fontFamily = resolveJournalFont(dateText, isRtl),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dueInfo != null) dueInfo.textColor else JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        if (dueInfo != null && dueInfo.urgency != CreditDueUrgency.SETTLED) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(dueInfo.badgeColor)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = dueInfo.displayText,
                                    fontFamily = resolveJournalFont(dueInfo.displayText, isRtl),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dueInfo.textColor,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }

                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Pencil,
                        contentDescription = stringResource(R.string.action_set_due_date),
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Ruled Paper Content
                JournalRuledDocument(
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                // Breathing space above the first row, perfectly aligned with the ruled notebook grid
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                state.rows.forEachIndexed { index, row ->
                    val rowNum = index + 1
                    val isTitleActive = state.activeRowId == row.id && state.activeField == ActiveField.TITLE
                    val isAmountActive = state.activeRowId == row.id && state.activeField == ActiveField.AMOUNT
                    val isValid = MoneyMath.isValidExpression(row.amount.text)

                    JournalEntryRow(
                        rowNumber = rowNum,
                        titleValue = row.title,
                        amountValue = row.amount,
                        currencySuffix = if (state.currency == MoneyUnit.DIRHAM) stringResource(R.string.currency_dirham) else stringResource(R.string.currency_rial),
                        isTitleActive = isTitleActive,
                        isAmountActive = isAmountActive,
                        isValid = isValid,
                        onTitleValueChange = { viewModel.updateRowTitle(row.id, it) },
                        onAmountValueChange = { viewModel.updateRowAmount(row.id, it) },
                        onTitleFocused = { viewModel.selectRowField(row.id, ActiveField.TITLE) },
                        onAmountFocused = { viewModel.selectRowField(row.id, ActiveField.AMOUNT) },
                        onDelete = { viewModel.removeRow(row.id) },
                        onConfirm = { viewModel.confirmRowEdit(row.id) }
                    )
                }

                // 1 empty notebook line before Add Row to prevent accidental taps (faux clic)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Add Row Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    JournalAddRowButton(
                        onAddRow = {
                            viewModel.addNewRow()
                        }
                    )
                }

                // Exactly 2 empty notebook lines between Add Row and Total
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 2))

                // Total Result Band
                val primaryFormatted = com.cash.guide.domain.JournalLedgerManager.formatTotal(state.totalCentimes, state.currency)
                val currencySuffix = if (state.currency == MoneyUnit.DIRHAM) stringResource(R.string.currency_dirham) else stringResource(R.string.currency_rial)
                JournalTotalResultBand(
                    amount = primaryFormatted,
                    suffix = currencySuffix,
                    hasInvalidRows = state.hasInvalidRows,
                    canBreakdown = state.totalCentimes > 0 && !state.hasInvalidRows,
                    onShowBreakdown = { showBreakdownSheet = true }
                )

                // Extra clearance so content can scroll completely above the bottom-right AI button
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 4))
            }

            // Pinned Bottom-Right AI Voice Assistant Button with Manga Speech Bubble
            if (!state.calculator.isVisible) {
                val existingRowContexts = remember(state.rows) {
                    state.rows.filter { it.isPopulated }.mapIndexed { index, row ->
                        ExistingCalculationRowContext(
                            id = row.id.toString(),
                            index = index + 1,
                            label = row.title.text.trim(),
                            currentAmount = row.amount.text.toDoubleOrNull() ?: 0.0
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    AiVoiceDockedBottomButton(
                        target = AiVoiceInputTarget.CALCULATION,
                        existingRows = existingRowContexts,
                        onCalculationResult = { result ->
                            if (result.entries.isNotEmpty()) {
                                viewModel.addAiEntries(result.entries, result.title)
                            }
                            val count = result.entries.size
                            Toast.makeText(
                                context,
                                context.getString(R.string.calculation_ai_items_processed, count),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        // Contextual Calculator Popup
        if (state.calculator.isVisible) {
            JournalCalculatorPopup(
                expression = state.calculator.expression,
                result = state.calculator.result,
                hasError = state.calculator.hasError,
                canConfirm = !state.calculator.hasError && (
                    (state.calculator.isEvaluated && state.calculator.result.isNotBlank()) ||
                    (state.calculator.result.isNotBlank()) ||
                    (state.calculator.expression.isNotBlank() && MoneyMath.isValidExpression(state.calculator.expression))
                ),
                onKey = { viewModel.applyPopupKey(it) },
                onConfirm = { viewModel.confirmPopupResult() },
                onDismiss = { viewModel.closeCalculatorPopup() }
            )
        }

        // Unsaved Changes Dialog
        if (state.showUnsavedDialog) {
            UnsavedChangesDialog(
                onSave = { 
                    com.cash.guide.domain.ads.AdMobManager.getInstance(context).reportModification()
                    viewModel.saveCalculation(onSuccess = onNavigateBack) 
                },
                onDiscard = { viewModel.discardChanges(onNavigateBack) },
                onContinue = { viewModel.dismissUnsavedDialog() }
            )
        }

        // Credit Due Date & Reminder Dialog
        if (state.showDueDateDialog) {
            CreditDueDateDialog(
                initialDueDateEpochMs = state.dueDateEpochMs,
                initialReminderEnabled = state.reminderEnabled,
                initialReminderTimeEpochMs = state.reminderTimeEpochMs,
                onSave = { dueDate, reminderEnabled, reminderTime ->
                    viewModel.updateDueDate(dueDate, reminderEnabled, reminderTime, context)
                    viewModel.setDueDateDialogVisible(false)
                },
                onClear = {
                    viewModel.updateDueDate(null, false, null, context)
                    viewModel.setDueDateDialogVisible(false)
                },
                onDismiss = { viewModel.setDueDateDialogVisible(false) }
            )
        }

        // Export Options Bottom Sheet
        if (showExportSheet) {
            ExportOptionsBottomSheet(
                title = state.title.text.ifBlank { stringResource(R.string.editor_options_title) },
                onSaveAsTemplate = { viewModel.saveAsTemplate(context) },
                onDuplicate = {
                    viewModel.duplicateCurrentCalculation { newId ->
                        onOpenCalculation?.invoke(newId)
                    }
                },
                onSetDueDate = if (state.calcType == "CREDIT") { { viewModel.setDueDateDialogVisible(true) } } else null,
                onExportPdf = { viewModel.exportAsPdf(context, isRtl) },
                onExportExcel = { viewModel.exportAsExcel(context) },
                onShareImage = { viewModel.shareAsImage(context, isRtl) },
                onDismiss = { showExportSheet = false }
            )
        }

        // Money Breakdown Sheet (L-Wra9 o S-Sarf)
        if (showBreakdownSheet && state.totalCentimes > 0) {
            MoneyBreakdownSheet(
                totalCentimes = state.totalCentimes,
                onDismiss = { showBreakdownSheet = false }
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    titleValue: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    isTitleActive: Boolean,
    onTitleClick: () -> Unit,
    keyboardLanguage: JournalKeyboardLanguage,
    currency: MoneyUnit,
    onCurrencyToggle: () -> Unit,
    paymentStatus: String = "PAID",
    onPaymentStatusToggle: () -> Unit = {},
    calcType: String = "PERSONNEL",
    canUndo: Boolean,
    onUndoClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = JournalPaper,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 6.dp)
            ) {
                // --- Line 1 (Navigation Back + Calculation Title + Undo Button + Share Button) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (42dp touch target)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Back,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = JournalInk,
                        size = 20.dp
                    )
                }

                // Title in Watercolor Pink Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isTitleActive) HighlighterPink.copy(alpha = 0.52f)
                            else HighlighterPink.copy(alpha = 0.35f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = titleValue,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onTitleClick()
                                }
                            },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = resolveJournalFont(titleValue.text, isRtl),
                            fontSize = if (isArabicScript(titleValue.text) || isRtl) 15.5.sp else 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalInk,
                            textAlign = TextAlign.Center,
                            platformStyle = NoFontPadding
                        ),
                        cursorBrush = SolidColor(JournalInk),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (titleValue.text.isEmpty()) {
                                    val placeholderText = stringResource(R.string.editor_title_placeholder)
                                    Text(
                                        text = placeholderText,
                                        fontFamily = resolveJournalFont(placeholderText, isRtl),
                                        fontSize = if (isArabicScript(placeholderText) || isRtl) 15.sp else 15.5.sp,
                                        color = JournalMutedInk.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = TextStyle(platformStyle = NoFontPadding)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Undo Button (42dp touch target)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            enabled = canUndo,
                            onClickLabel = stringResource(R.string.cd_undo),
                            onClick = onUndoClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Undo,
                        contentDescription = stringResource(R.string.cd_undo),
                        tint = if (canUndo) JournalInk else JournalInk.copy(alpha = 0.25f),
                        size = 20.dp
                    )
                }

                // More Options Button (42dp touch target)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onShareClick),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = JournalInk,
                        size = 20.dp
                    )
                }
            }

            // --- Line 2 (Editor Tools: Currency + Calculator + Save) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Currency Switcher Badge (Right in RTL, Left in LTR)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HighlighterYellow.copy(alpha = 0.50f))
                        .clickable(role = Role.Button, onClick = onCurrencyToggle)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val currencyText = if (currency == MoneyUnit.DIRHAM) stringResource(R.string.currency_dirham) else stringResource(R.string.currency_rial)
                        Text(
                            text = currencyText,
                            fontFamily = resolveJournalFont(currencyText, isRtl),
                            fontSize = if (isRtl) 13.sp else 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Page,
                            contentDescription = null,
                            tint = JournalInk.copy(alpha = 0.6f),
                            size = 12.dp
                        )
                    }
                }

                // 2. Payment Status Rubber Stamp (Kredi / Khlass) - Only if calcType == "CREDIT"
                if (calcType == "CREDIT") {
                    val isPaid = paymentStatus == "PAID"
                    val stampColor = if (isPaid) Color(0xFF15803D) else Color(0xFFC2410C)
                    val stampBg = if (isPaid) Color(0xFFDCFCE7).copy(alpha = 0.65f) else Color(0xFFFFEDD5).copy(alpha = 0.65f)
                    val stampText = if (isPaid) {
                        stringResource(R.string.payment_status_paid) + " ✓"
                    } else {
                        stringResource(R.string.payment_status_unpaid)
                    }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = if (isPaid) -2f else 2f
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.35.dp,
                                color = stampColor.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(stampBg)
                            .clickable(role = Role.Button, onClick = onPaymentStatusToggle)
                            .padding(horizontal = 9.dp, vertical = 3.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stampText,
                            fontFamily = resolveJournalFont(stampText, isRtl),
                            fontSize = if (isRtl) 12.5.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = stampColor,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                // 3. Calculator Button (Center)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onCalculatorClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Calculator,
                        contentDescription = stringResource(R.string.editor_open_calculator),
                        tint = JournalInk,
                        size = 18.dp
                    )
                    val calcText = stringResource(R.string.calculator_title)
                    Text(
                        text = calcText,
                        fontFamily = resolveJournalFont(calcText, isRtl),
                        fontSize = if (isRtl) 13.sp else 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                // 3. Save ("حفظ") Button with lowered pink underline (Left in RTL, Right in LTR)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onSaveClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .drawBehind {
                            val strokeW = 1.35.dp.toPx()
                            val y = size.height + 2.5.dp.toPx()
                            drawLine(
                                color = HighlighterPink,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val saveText = stringResource(R.string.editor_save)
                    Text(
                        text = saveText,
                        fontFamily = resolveJournalFont(saveText, isRtl),
                        fontSize = if (isRtl) 14.5.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }

        // Subtle edge-to-edge light black line beneath tools (Dirham, Calculator, Save)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(JournalInk.copy(alpha = 0.16f))
        )
    }
}
}
