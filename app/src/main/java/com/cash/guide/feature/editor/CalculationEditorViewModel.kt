package com.cash.guide.feature.editor

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.R
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.SettingsRepository
import com.cash.guide.data.TemplateRepository
import com.cash.guide.data.db.CalculationEntity
import com.cash.guide.data.db.CalculationItemEntity
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.domain.AndroidIcuGraphemeSegmenter
import com.cash.guide.domain.CalculationImageShareHelper
import com.cash.guide.domain.export.ExcelExportHelper
import com.cash.guide.domain.export.FileExportManager
import com.cash.guide.domain.export.PdfExportHelper
import com.cash.guide.domain.reminder.CreditReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cash.guide.domain.GraphemeSegmenter
import com.cash.guide.domain.JournalKeyboardController
import com.cash.guide.domain.JournalKeyboardLanguage
import com.cash.guide.domain.JournalKeyboardMode
import com.cash.guide.domain.JournalShiftMode
import com.cash.guide.domain.JournalShiftState
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.domain.ShiftAction
import com.cash.guide.ui.ActiveField
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DeletedRowAction(
    val row: EditorRowUiState,
    val originalIndex: Int
)

class CalculationEditorViewModel(
    private val calculationRepository: CalculationRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val templateRepository: TemplateRepository? = null,
    private val graphemeSegmenter: GraphemeSegmenter = AndroidIcuGraphemeSegmenter(),
    private val monotonicClock: () -> Long = { SystemClock.uptimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var nextRowId = 2L
    private var lastLatinShiftMode = JournalShiftMode.OFF
    private var draftSaveJob: Job? = null
    private val undoStack = mutableListOf<DeletedRowAction>()

    fun loadCalculation(
        id: String?,
        initialGroupId: String? = null,
        initialType: String? = null,
        initialCurrency: MoneyUnit? = null,
        initialTitle: String? = null,
        templateId: String? = null
    ) {
        undoStack.clear()
        viewModelScope.launch {
            val defaultCurrency = settingsRepository?.defaultCurrency?.first() ?: MoneyUnit.DIRHAM
            val defaultLanguage = when (settingsRepository?.appLanguage?.first()) {
                "ar", "dar" -> JournalKeyboardLanguage.ARABIC
                "en" -> JournalKeyboardLanguage.ENGLISH
                else -> JournalKeyboardLanguage.FRENCH
            }

            if (id == null) {
                val hasExplicitParams = initialType != null || !initialTitle.isNullOrBlank() || initialCurrency != null || !templateId.isNullOrBlank()
                // Check if an uncommitted new draft exists
                val draft = if (hasExplicitParams) null else calculationRepository.getRecoverableDraft(null)
                val effectiveGroupId = initialGroupId ?: draft?.calculation?.groupId
                if (draft != null && (draft.calculation.title.isNotBlank() || draft.items.isNotEmpty())) {
                    val draftCurrency = runCatching { MoneyUnit.valueOf(draft.calculation.currency) }.getOrDefault(defaultCurrency)
                    val restoredRows = if (draft.items.isEmpty()) {
                        listOf(EditorRowUiState(id = 1L))
                    } else {
                        draft.items.sortedBy { it.position }.mapIndexed { idx, item ->
                            val rawText = item.rawExpression?.takeIf { it.isNotBlank() }
                                ?: if (item.amountCentimes > 0) MoneyMath.fromCentimes(item.amountCentimes, draftCurrency) else ""
                            EditorRowUiState(
                                id = (idx + 1).toLong(),
                                title = TextFieldValue(item.label, TextRange(item.label.length)),
                                amount = TextFieldValue(rawText, TextRange(rawText.length)),
                                rawExpression = rawText
                            )
                        }
                    }
                    nextRowId = (restoredRows.maxOfOrNull { it.id } ?: 1L) + 1
                    _uiState.update {
                        it.copy(
                            calculationId = draft.calculation.id,
                            editingSavedId = null,
                            mode = EditorMode.NEW,
                            title = TextFieldValue(draft.calculation.title, TextRange(draft.calculation.title.length)),
                            currency = draftCurrency,
                            rows = restoredRows,
                            activeRowId = restoredRows.firstOrNull()?.id,
                            activeField = ActiveField.TITLE,
                            keyboardMode = JournalKeyboardMode.TEXT,
                            keyboardLanguage = defaultLanguage,
                            keyboardExpanded = true,
                            isDirty = true,
                            recoveredDraft = true,
                            createdAtEpochMs = draft.calculation.createdAtEpochMs,
                            groupId = effectiveGroupId,
                            paymentStatus = draft.calculation.paymentStatus,
                            calcType = draft.calculation.calcType,
                            dueDateEpochMs = draft.calculation.dueDateEpochMs,
                            reminderEnabled = draft.calculation.reminderEnabled,
                            reminderTimeEpochMs = draft.calculation.reminderTimeEpochMs
                        )
                    }
                } else {
                    val initialId = draft?.calculation?.id ?: UUID.randomUUID().toString()
                    val targetType = initialType ?: "PERSONNEL"
                    val targetCurrency = initialCurrency ?: defaultCurrency
                    val targetTitle = initialTitle ?: ""

                    val isArabic = defaultLanguage == JournalKeyboardLanguage.ARABIC
                    val template = if (!templateId.isNullOrBlank()) templateRepository?.getTemplate(templateId, isArabic) else null

                    val effectiveTitle = if (targetTitle.isNotBlank()) targetTitle else (template?.title ?: "")
                    val effectiveType = if (initialType != null) initialType else (template?.calcType ?: targetType)
                    val effectivePaymentStatus = if (effectiveType == "CREDIT") "UNPAID" else "PAID"
                    val effectiveCurrency = if (initialCurrency != null) initialCurrency else {
                        template?.let { runCatching { MoneyUnit.valueOf(it.currency) }.getOrNull() } ?: targetCurrency
                    }

                    val initialRows = if (template != null && template.itemLabels.isNotEmpty()) {
                        template.itemLabels.mapIndexed { idx, label ->
                            EditorRowUiState(
                                id = (idx + 1).toLong(),
                                title = TextFieldValue(label, TextRange(label.length)),
                                amount = TextFieldValue("", TextRange.Zero),
                                rawExpression = ""
                            )
                        }
                    } else {
                        listOf(EditorRowUiState(id = 1L))
                    }
                    nextRowId = (initialRows.maxOfOrNull { it.id } ?: 1L) + 1

                    _uiState.update {
                        it.copy(
                            calculationId = initialId,
                            editingSavedId = null,
                            mode = EditorMode.NEW,
                            title = TextFieldValue(effectiveTitle, TextRange(effectiveTitle.length)),
                            currency = effectiveCurrency,
                            rows = initialRows,
                            activeRowId = initialRows.firstOrNull()?.id ?: 1L,
                            activeField = if (template != null) ActiveField.AMOUNT else if (effectiveTitle.isNotBlank()) ActiveField.AMOUNT else ActiveField.TITLE,
                            keyboardMode = if (template != null || effectiveTitle.isNotBlank()) JournalKeyboardMode.NUMBER else JournalKeyboardMode.TEXT,
                            keyboardLanguage = defaultLanguage,
                            keyboardExpanded = true,
                            isDirty = effectiveTitle.isNotBlank() || template != null,
                            groupId = effectiveGroupId,
                            paymentStatus = effectivePaymentStatus,
                            calcType = effectiveType
                        )
                    }
                }
            } else {
                // Check if draft for this saved calculation exists
                val draft = calculationRepository.getRecoverableDraft(id)
                val saved = calculationRepository.getCalculation(id)
                val originalCreatedAt = saved?.calculation?.createdAtEpochMs
                val effectiveGroupId = initialGroupId ?: draft?.calculation?.groupId ?: saved?.calculation?.groupId
                if (draft != null) {
                    val draftCurrency = runCatching { MoneyUnit.valueOf(draft.calculation.currency) }.getOrDefault(defaultCurrency)
                    val restoredRows = draft.items.sortedBy { it.position }.mapIndexed { idx, item ->
                        val rawText = item.rawExpression?.takeIf { it.isNotBlank() }
                            ?: if (item.amountCentimes > 0) MoneyMath.fromCentimes(item.amountCentimes, draftCurrency) else ""
                        EditorRowUiState(
                            id = (idx + 1).toLong(),
                            title = TextFieldValue(item.label, TextRange(item.label.length)),
                            amount = TextFieldValue(rawText, TextRange(rawText.length)),
                            rawExpression = rawText
                        )
                    }
                    nextRowId = (restoredRows.maxOfOrNull { it.id } ?: 1L) + 1
                    _uiState.update {
                        it.copy(
                            calculationId = draft.calculation.id,
                            editingSavedId = id,
                            mode = EditorMode.EXISTING,
                            title = TextFieldValue(draft.calculation.title, TextRange(draft.calculation.title.length)),
                            currency = draftCurrency,
                            rows = restoredRows.ifEmpty { listOf(EditorRowUiState(id = 1L)) },
                            activeRowId = null,
                            activeField = ActiveField.NONE,
                            keyboardMode = JournalKeyboardMode.NUMBER,
                            keyboardExpanded = false,
                            isDirty = true,
                            recoveredDraft = true,
                            createdAtEpochMs = originalCreatedAt ?: draft.calculation.createdAtEpochMs,
                            groupId = effectiveGroupId,
                            paymentStatus = draft.calculation.paymentStatus,
                            calcType = draft.calculation.calcType,
                            dueDateEpochMs = draft.calculation.dueDateEpochMs,
                            reminderEnabled = draft.calculation.reminderEnabled,
                            reminderTimeEpochMs = draft.calculation.reminderTimeEpochMs
                        )
                    }
                } else if (saved != null) {
                    val loadedCurrency = runCatching { MoneyUnit.valueOf(saved.calculation.currency) }.getOrDefault(defaultCurrency)
                    val loadedRows = saved.items.sortedBy { it.position }.mapIndexed { idx, item ->
                        val amountStr = if (!item.rawExpression.isNullOrBlank()) {
                            item.rawExpression
                        } else {
                            MoneyMath.fromCentimes(item.amountCentimes, loadedCurrency)
                        }
                        EditorRowUiState(
                            id = (idx + 1).toLong(),
                            title = TextFieldValue(item.label, TextRange(item.label.length)),
                            amount = TextFieldValue(amountStr, TextRange(amountStr.length)),
                            rawExpression = item.rawExpression ?: ""
                        )
                    }
                    val rows = loadedRows.ifEmpty { listOf(EditorRowUiState(id = 1L)) }
                    nextRowId = (rows.maxOfOrNull { it.id } ?: 1L) + 1
                    _uiState.update {
                        it.copy(
                            calculationId = saved.calculation.id,
                            editingSavedId = id,
                            mode = EditorMode.EXISTING,
                            title = TextFieldValue(saved.calculation.title, TextRange(saved.calculation.title.length)),
                            currency = loadedCurrency,
                            rows = rows,
                            activeRowId = null,
                            activeField = ActiveField.NONE,
                            keyboardMode = JournalKeyboardMode.NUMBER,
                            keyboardExpanded = false,
                            isDirty = false,
                            createdAtEpochMs = saved.calculation.createdAtEpochMs,
                            groupId = effectiveGroupId,
                            paymentStatus = saved.calculation.paymentStatus,
                            calcType = saved.calculation.calcType,
                            dueDateEpochMs = saved.calculation.dueDateEpochMs,
                            reminderEnabled = saved.calculation.reminderEnabled,
                            reminderTimeEpochMs = saved.calculation.reminderTimeEpochMs
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(value: TextFieldValue) {
        _uiState.update { state ->
            state.copy(
                title = value,
                isDirty = true,
                validationError = if (value.text.isNotBlank() && state.validationError == "TITLE_REQUIRED") null else state.validationError
            )
        }
        scheduleDraftSave()
    }

    fun addAiEntries(entries: List<com.cash.guide.domain.ai.CalculationAiEntry>, suggestedTitle: String = "") {
        if (entries.isEmpty()) return
        _uiState.update { current ->
            val updatedRows = current.rows.toMutableList()
            val brandNewRows = mutableListOf<EditorRowUiState>()

            for (entry in entries) {
                val amountStr = if (entry.amount <= 0.0) {
                    ""
                } else if (entry.amount % 1.0 == 0.0) {
                    entry.amount.toLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", entry.amount)
                }

                val targetIdLong = entry.existingRowId?.toLongOrNull()
                val existingIdx = if (targetIdLong != null) updatedRows.indexOfFirst { it.id == targetIdLong } else -1

                if (existingIdx != -1) {
                    // Update existing row in place
                    val old = updatedRows[existingIdx]
                    val newTitle = if (entry.label.isNotBlank()) entry.label else old.title.text
                    updatedRows[existingIdx] = old.copy(
                        title = TextFieldValue(newTitle, TextRange(newTitle.length)),
                        amount = TextFieldValue(amountStr, TextRange(amountStr.length)),
                        rawExpression = amountStr
                    )
                } else {
                    // New row to append
                    brandNewRows.add(
                        EditorRowUiState(
                            id = nextRowId++,
                            title = TextFieldValue(entry.label, TextRange(entry.label.length)),
                            amount = TextFieldValue(amountStr, TextRange(amountStr.length)),
                            rawExpression = amountStr
                        )
                    )
                }
            }

            val combined = (updatedRows.filter { it.isPopulated } + brandNewRows).ifEmpty {
                listOf(EditorRowUiState(id = nextRowId++))
            }
            val updatedTitle = if (current.title.text.isBlank() && suggestedTitle.isNotBlank()) {
                TextFieldValue(suggestedTitle, TextRange(suggestedTitle.length))
            } else {
                current.title
            }
            current.copy(
                title = updatedTitle,
                rows = combined,
                activeRowId = combined.lastOrNull()?.id,
                activeField = ActiveField.NONE,
                isDirty = true
            )
        }
        scheduleDraftSave()
    }

    fun updateRowTitle(id: Long, value: TextFieldValue) {
        _uiState.update { state ->
            val updated = state.rows.map { row ->
                if (row.id == id) row.copy(title = value) else row
            }
            state.copy(rows = updated, isDirty = true)
        }
        scheduleDraftSave()
    }

    fun updateRowAmount(id: Long, value: TextFieldValue) {
        val filteredText = value.text.filter { it.isDigit() || it == '.' }
        val sanitizedText = if (filteredText.count { it == '.' } > 1) {
            val firstDot = filteredText.indexOf('.')
            filteredText.filterIndexed { idx, ch -> ch != '.' || idx == firstDot }
        } else {
            filteredText
        }
        val sanitizedValue = if (sanitizedText != value.text) {
            TextFieldValue(text = sanitizedText, selection = TextRange(sanitizedText.length.coerceAtMost(value.selection.start)))
        } else {
            value
        }

        _uiState.update { state ->
            val updated = state.rows.map { row ->
                if (row.id == id) row.copy(amount = sanitizedValue) else row
            }
            state.copy(rows = updated, isDirty = true)
        }
        scheduleDraftSave()
    }

    fun addNewRow() {
        val newId = nextRowId++
        val newRow = EditorRowUiState(id = newId)
        _uiState.update { state ->
            val updatedRows = state.rows + newRow
            var newShiftState = state.shiftState
            var newShiftMode = state.shiftMode
            if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
                newShiftState = JournalKeyboardController.reduceShift(
                    state = state.shiftState,
                    action = ShiftAction.AutoSetOneShot,
                    monotonicNow = monotonicClock
                )
                newShiftMode = newShiftState.mode
                lastLatinShiftMode = newShiftState.mode
            }
            state.copy(
                rows = updatedRows,
                activeRowId = newId,
                activeField = ActiveField.TITLE,
                keyboardMode = JournalKeyboardMode.TEXT,
                keyboardExpanded = true,
                shiftState = newShiftState,
                shiftMode = newShiftMode,
                pendingFocusRowId = newId,
                isDirty = true
            )
        }
        scheduleDraftSave()
    }

    fun removeRow(id: Long) {
        val currentRows = _uiState.value.rows
        val index = currentRows.indexOfFirst { it.id == id }
        if (index >= 0) {
            val rowToDelete = currentRows[index]
            undoStack.add(DeletedRowAction(row = rowToDelete, originalIndex = index))
        }

        _uiState.update { state ->
            val filtered = state.rows.filter { it.id != id }
            val updatedRows = filtered.ifEmpty {
                val freshId = nextRowId++
                listOf(EditorRowUiState(id = freshId))
            }
            val newActiveRowId = if (state.activeRowId == id) null else state.activeRowId
            val newActiveField = if (state.activeRowId == id) ActiveField.NONE else state.activeField
            val newKeyboardMode = if (state.activeRowId == id) JournalKeyboardMode.NONE else state.keyboardMode
            val newKeyboardExpanded = if (state.activeRowId == id) false else state.keyboardExpanded
            state.copy(
                rows = updatedRows,
                activeRowId = newActiveRowId,
                activeField = newActiveField,
                keyboardMode = newKeyboardMode,
                keyboardExpanded = newKeyboardExpanded,
                canUndo = undoStack.isNotEmpty(),
                isDirty = true
            )
        }
        scheduleDraftSave()
    }

    fun undoDelete() {
        val last = undoStack.removeLastOrNull() ?: return
        _uiState.update { state ->
            val mutableRows = state.rows.toMutableList()
            val finalRows = if (mutableRows.size == 1 && mutableRows[0].isEmpty) {
                listOf(last.row)
            } else {
                val insertIdx = last.originalIndex.coerceIn(0, mutableRows.size)
                mutableRows.add(insertIdx, last.row)
                mutableRows
            }
            state.copy(
                rows = finalRows,
                canUndo = undoStack.isNotEmpty(),
                isDirty = true
            )
        }
        scheduleDraftSave()
    }

    fun selectCalculationTitle() {
        _uiState.update { state ->
            var newShiftState = state.shiftState
            var newShiftMode = state.shiftMode
            if (state.title.text.isEmpty() && state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
                newShiftState = JournalKeyboardController.reduceShift(
                    state = state.shiftState,
                    action = ShiftAction.AutoSetOneShot,
                    monotonicNow = monotonicClock
                )
                newShiftMode = newShiftState.mode
                lastLatinShiftMode = newShiftState.mode
            }
            state.copy(
                activeRowId = null,
                activeField = ActiveField.HEADER_TITLE,
                keyboardMode = JournalKeyboardMode.TEXT,
                keyboardExpanded = true,
                shiftState = newShiftState,
                shiftMode = newShiftMode
            )
        }
    }

    fun confirmCalculationTitle() {
        _uiState.update { state ->
            state.copy(
                activeField = ActiveField.NONE,
                keyboardMode = JournalKeyboardMode.NONE,
                keyboardExpanded = false,
                shiftState = JournalShiftState(mode = JournalShiftMode.OFF),
                shiftMode = JournalShiftMode.OFF
            )
        }
    }

    fun confirmRowEdit(id: Long) {
        _uiState.update { state ->
            state.copy(
                activeRowId = null,
                activeField = ActiveField.NONE,
                keyboardMode = state.keyboardMode.takeIf { it != JournalKeyboardMode.NONE } ?: JournalKeyboardMode.NUMBER,
                keyboardExpanded = false,
                shiftState = JournalShiftState(mode = JournalShiftMode.OFF),
                shiftMode = JournalShiftMode.OFF
            )
        }
    }

    fun selectRowField(rowId: Long, field: ActiveField) {
        _uiState.update { state ->
            when (field) {
                ActiveField.HEADER_TITLE -> {
                    var newShiftState = state.shiftState
                    var newShiftMode = state.shiftMode
                    if (state.title.text.isEmpty() && state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
                        newShiftState = JournalKeyboardController.reduceShift(
                            state = state.shiftState,
                            action = ShiftAction.AutoSetOneShot,
                            monotonicNow = monotonicClock
                        )
                        newShiftMode = newShiftState.mode
                        lastLatinShiftMode = newShiftState.mode
                    }
                    state.copy(
                        activeRowId = null,
                        activeField = ActiveField.HEADER_TITLE,
                        keyboardMode = JournalKeyboardMode.TEXT,
                        keyboardExpanded = true,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
                ActiveField.TITLE -> {
                    var newShiftState = state.shiftState
                    var newShiftMode = state.shiftMode
                    val row = state.rows.firstOrNull { it.id == rowId }
                    if (row != null && row.title.text.isEmpty() && state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
                        newShiftState = JournalKeyboardController.reduceShift(
                            state = state.shiftState,
                            action = ShiftAction.AutoSetOneShot,
                            monotonicNow = monotonicClock
                        )
                        newShiftMode = newShiftState.mode
                        lastLatinShiftMode = newShiftState.mode
                    }
                    state.copy(
                        activeRowId = rowId,
                        activeField = ActiveField.TITLE,
                        keyboardMode = JournalKeyboardMode.TEXT,
                        keyboardExpanded = true,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode,
                        pendingFocusRowId = rowId
                    )
                }
                ActiveField.AMOUNT -> {
                    state.copy(
                        activeRowId = rowId,
                        activeField = ActiveField.AMOUNT,
                        keyboardMode = JournalKeyboardMode.NUMBER,
                        keyboardExpanded = true,
                        pendingFocusRowId = rowId
                    )
                }
                ActiveField.NONE -> {
                    state.copy(
                        activeRowId = null,
                        activeField = ActiveField.NONE,
                        keyboardMode = JournalKeyboardMode.NONE,
                        keyboardExpanded = false
                    )
                }
            }
        }
    }

    fun toggleKeyboardExpanded() {
        _uiState.update { state ->
            val nextExpanded = !state.keyboardExpanded
            if (nextExpanded && state.activeRowId == null && state.activeField != ActiveField.HEADER_TITLE) {
                val targetRow = state.rows.lastOrNull() ?: EditorRowUiState(id = 1L)
                state.copy(
                    keyboardExpanded = true,
                    activeRowId = targetRow.id,
                    activeField = if (state.keyboardMode == JournalKeyboardMode.TEXT) ActiveField.TITLE else ActiveField.AMOUNT
                )
            } else {
                state.copy(keyboardExpanded = nextExpanded)
            }
        }
    }

    fun selectUnit(unit: MoneyUnit) {
        _uiState.update { state ->
            if (unit == state.currency) return@update state
            val convertedRows = state.rows.map { row ->
                val converted = MoneyMath.convertExpression(row.amount.text, state.currency, unit)
                row.copy(
                    amount = TextFieldValue(text = converted, selection = TextRange(converted.length))
                )
            }
            state.copy(
                currency = unit,
                rows = convertedRows,
                isDirty = true
            )
        }
        scheduleDraftSave()
    }

    fun applyTextKey(text: String) {
        val state = _uiState.value
        if (state.activeField == ActiveField.HEADER_TITLE) {
            val newVal = JournalKeyboardController.insertText(state.title, text)
            var newShiftState = state.shiftState
            var newShiftMode = state.shiftMode
            if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
                newShiftState = JournalKeyboardController.reduceShift(
                    state = state.shiftState,
                    action = ShiftAction.UserTypedText(text),
                    monotonicNow = monotonicClock
                )
                newShiftMode = newShiftState.mode
                lastLatinShiftMode = newShiftState.mode
            }
            _uiState.update { s ->
                s.copy(
                    title = newVal,
                    shiftState = newShiftState,
                    shiftMode = newShiftMode,
                    isDirty = true,
                    validationError = if (newVal.text.isNotBlank() && s.validationError == "TITLE_REQUIRED") null else s.validationError
                )
            }
            scheduleDraftSave()
            return
        }

        val targetId = state.activeRowId ?: return
        val row = state.rows.firstOrNull { it.id == targetId } ?: return
        val newVal = JournalKeyboardController.insertText(row.title, text)

        var newShiftState = state.shiftState
        var newShiftMode = state.shiftMode
        if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
            newShiftState = JournalKeyboardController.reduceShift(
                state = state.shiftState,
                action = ShiftAction.UserTypedText(text),
                monotonicNow = monotonicClock
            )
            newShiftMode = newShiftState.mode
            lastLatinShiftMode = newShiftState.mode
        }

        _uiState.update { s ->
            val updated = s.rows.map { if (it.id == targetId) it.copy(title = newVal) else it }
            s.copy(rows = updated, shiftState = newShiftState, shiftMode = newShiftMode, isDirty = true)
        }
        scheduleDraftSave()
    }

    fun applyTextBackspace() {
        val state = _uiState.value
        if (state.activeField == ActiveField.HEADER_TITLE) {
            val newVal = JournalKeyboardController.deleteBackward(state.title, graphemeSegmenter)
            _uiState.update { s ->
                s.copy(
                    title = newVal,
                    isDirty = true,
                    validationError = if (newVal.text.isNotBlank() && s.validationError == "TITLE_REQUIRED") null else s.validationError
                )
            }
            scheduleDraftSave()
            return
        }

        val targetId = state.activeRowId ?: return
        val row = state.rows.firstOrNull { it.id == targetId } ?: return
        val newVal = JournalKeyboardController.deleteBackward(row.title, graphemeSegmenter)
        _uiState.update { s ->
            val updated = s.rows.map { if (it.id == targetId) it.copy(title = newVal) else it }
            s.copy(rows = updated, isDirty = true)
        }
        scheduleDraftSave()
    }

    fun applyCompactKey(key: String) {
        val state = _uiState.value
        if (state.activeField == ActiveField.HEADER_TITLE) {
            val curVal = state.title
            val newVal = if (key == "⌫") {
                JournalKeyboardController.deleteBackward(curVal, graphemeSegmenter)
            } else {
                JournalKeyboardController.insertText(curVal, key)
            }
            _uiState.update { s ->
                s.copy(
                    title = newVal,
                    isDirty = true,
                    validationError = if (newVal.text.isNotBlank() && s.validationError == "TITLE_REQUIRED") null else s.validationError
                )
            }
            scheduleDraftSave()
            return
        }

        val targetId = state.activeRowId ?: return
        val row = state.rows.firstOrNull { it.id == targetId } ?: return
        val curVal = row.amount
        val newVal = if (key == "⌫") {
            JournalKeyboardController.deleteBackward(curVal, graphemeSegmenter)
        } else {
            val curText = curVal.text
            if (key == "." && curText.contains('.')) {
                curVal
            } else {
                JournalKeyboardController.insertText(curVal, key)
            }
        }
        _uiState.update { s ->
            val updated = s.rows.map { if (it.id == targetId) it.copy(amount = newVal) else it }
            s.copy(rows = updated, isDirty = true)
        }
        scheduleDraftSave()
    }

    fun cycleLanguage() {
        _uiState.update { state ->
            val nextLang = JournalKeyboardController.nextLanguage(state.keyboardLanguage)
            var newShiftState = state.shiftState
            var newShiftMode = state.shiftMode
            if (state.keyboardLanguage == JournalKeyboardLanguage.ARABIC && nextLang != JournalKeyboardLanguage.ARABIC) {
                newShiftState = newShiftState.copy(mode = lastLatinShiftMode)
                newShiftMode = lastLatinShiftMode
            } else if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC && nextLang == JournalKeyboardLanguage.ARABIC) {
                lastLatinShiftMode = newShiftState.mode
                newShiftMode = JournalShiftMode.OFF
            }
            state.copy(keyboardLanguage = nextLang, shiftState = newShiftState, shiftMode = newShiftMode)
        }
    }

    fun selectLanguage(lang: JournalKeyboardLanguage) {
        _uiState.update { state ->
            var newShiftState = state.shiftState
            var newShiftMode = state.shiftMode
            if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC && lang == JournalKeyboardLanguage.ARABIC) {
                lastLatinShiftMode = newShiftState.mode
                newShiftMode = JournalShiftMode.OFF
            } else if (state.keyboardLanguage == JournalKeyboardLanguage.ARABIC && lang != JournalKeyboardLanguage.ARABIC) {
                newShiftState = newShiftState.copy(mode = lastLatinShiftMode)
                newShiftMode = lastLatinShiftMode
            }
            state.copy(keyboardLanguage = lang, shiftState = newShiftState, shiftMode = newShiftMode)
        }
    }

    fun toggleShift() {
        _uiState.update { state ->
            if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
                val newShiftState = JournalKeyboardController.reduceShift(
                    state = state.shiftState,
                    action = ShiftAction.UserTapShift(monotonicClock()),
                    monotonicNow = monotonicClock
                )
                lastLatinShiftMode = newShiftState.mode
                state.copy(shiftState = newShiftState, shiftMode = newShiftState.mode)
            } else {
                state
            }
        }
    }

    fun switchToTextMode() {
        val state = _uiState.value
        if (state.activeField == ActiveField.HEADER_TITLE) {
            _uiState.update { it.copy(keyboardMode = JournalKeyboardMode.TEXT) }
            return
        }
        val targetId = state.activeRowId ?: state.rows.firstOrNull()?.id ?: return
        selectRowField(targetId, ActiveField.TITLE)
    }

    fun switchToNumericMode() {
        val state = _uiState.value
        if (state.activeField == ActiveField.HEADER_TITLE) {
            _uiState.update { it.copy(keyboardMode = JournalKeyboardMode.NUMBER) }
            return
        }
        val targetId = state.activeRowId ?: state.rows.firstOrNull()?.id ?: return
        selectRowField(targetId, ActiveField.AMOUNT)
    }

    fun openCalculatorPopup(rowId: Long? = null) {
        _uiState.update { state ->
            val target = rowId ?: state.activeRowId ?: state.rows.firstOrNull()?.id
            state.copy(
                calculator = CalculatorPopupState(
                    isVisible = true,
                    targetRowId = target,
                    expression = "",
                    result = "",
                    hasError = false,
                    isEvaluated = false
                )
            )
        }
    }

    fun closeCalculatorPopup() {
        _uiState.update { state ->
            state.copy(calculator = CalculatorPopupState(isVisible = false))
        }
    }

    fun applyPopupKey(key: String) {
        _uiState.update { state ->
            val calc = state.calculator
            val operators = setOf('+', '−', '×', '÷')
            var expr = calc.expression
            var res = calc.result
            var err = false
            var evaluated = false

            when (key) {
                "C" -> {
                    expr = ""
                    res = ""
                    err = false
                    evaluated = false
                }
                "⌫" -> {
                    if (expr.isNotEmpty()) expr = expr.dropLast(1)
                    res = ""
                    err = false
                    evaluated = false
                }
                "=" -> {
                    if (expr.isNotBlank()) {
                        val eval = MoneyMath.evaluate(expr)?.stripTrailingZeros()?.toPlainString()
                        if (eval != null && !eval.contains("NaN") && !eval.contains("Infinity")) {
                            res = eval
                            evaluated = true
                        } else {
                            err = true
                        }
                    }
                }
                "+", "−", "×", "÷" -> {
                    if (calc.isEvaluated && calc.result.isNotBlank()) {
                        expr = calc.result + key
                        res = ""
                        evaluated = false
                    } else {
                        expr = when {
                            expr.isBlank() -> expr
                            expr.last() == '(' -> expr
                            expr.last() in operators -> expr.dropLast(1) + key
                            else -> expr + key
                        }
                        res = ""
                        evaluated = false
                    }
                }
                "(" -> {
                    if (calc.isEvaluated) {
                        expr = "("
                        res = ""
                        evaluated = false
                    } else {
                        expr = if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')')) {
                            expr + "×("
                        } else {
                            expr + "("
                        }
                        res = ""
                        evaluated = false
                    }
                }
                ")" -> {
                    val openCount = expr.count { it == '(' }
                    val closeCount = expr.count { it == ')' }
                    if (!calc.isEvaluated && openCount > closeCount && expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')')) {
                        expr += ")"
                        res = ""
                        evaluated = false
                    }
                }
                "." -> {
                    if (calc.isEvaluated) {
                        expr = "0."
                        res = ""
                        evaluated = false
                    } else {
                        res = ""
                        evaluated = false
                        val lastPart = expr.split('+', '−', '×', '÷', '(', ')').lastOrNull().orEmpty()
                        expr = when {
                            lastPart.contains('.') -> expr
                            lastPart.isEmpty() -> expr + "0."
                            else -> expr + "."
                        }
                    }
                }
                else -> { // Digits 0-9
                    if (calc.isEvaluated) {
                        expr = key
                        res = ""
                        evaluated = false
                    } else {
                        res = ""
                        evaluated = false
                        if (expr.isNotEmpty() && expr.last() == ')') {
                            expr += "×" + key
                        } else {
                            val lastPart = expr.split('+', '−', '×', '÷', '(', ')').lastOrNull().orEmpty()
                            if (lastPart == "0") {
                                if (key != "0") expr = expr.dropLast(1) + key
                            } else if (expr.length < 32) {
                                expr += key
                            }
                        }
                    }
                }
            }

            state.copy(
                calculator = calc.copy(
                    expression = expr,
                    result = res,
                    hasError = err,
                    isEvaluated = evaluated
                )
            )
        }
    }

    fun confirmPopupResult() {
        val state = _uiState.value
        val calc = state.calculator
        val targetResult = if (calc.isEvaluated && calc.result.isNotBlank() && !calc.hasError) {
            calc.result
        } else if (!calc.hasError && calc.result.isNotBlank()) {
            calc.result
        } else if (!calc.hasError && calc.expression.isNotBlank()) {
            MoneyMath.evaluate(calc.expression)?.stripTrailingZeros()?.toPlainString()
        } else null

        if (targetResult.isNullOrBlank() || targetResult.contains("NaN") || targetResult.contains("Infinity")) return
        val targetId = calc.targetRowId ?: return
        updateRowAmount(targetId, TextFieldValue(text = targetResult, selection = TextRange(targetResult.length)))
        _uiState.update { s ->
            s.copy(
                activeRowId = targetId,
                activeField = ActiveField.AMOUNT,
                calculator = CalculatorPopupState(isVisible = false)
            )
        }
    }

    fun toggleBreakdownSheet(show: Boolean) {
        _uiState.update { it.copy(showBreakdownSheet = show) }
    }

    fun dismissUnsavedDialog() {
        _uiState.update { it.copy(showUnsavedDialog = false) }
    }

    fun handleBackPress(onNavigateBack: () -> Unit) {
        val state = _uiState.value
        if (state.calculator.isVisible) {
            closeCalculatorPopup()
            return
        }
        if (state.showBreakdownSheet) {
            toggleBreakdownSheet(false)
            return
        }
        if (state.isDirty) {
            _uiState.update { it.copy(showUnsavedDialog = true) }
        } else {
            viewModelScope.launch {
                // Untouched new draft can be deleted safely
                if (state.mode == EditorMode.NEW && state.title.text.isBlank() && state.rows.all { it.isEmpty }) {
                    state.calculationId?.let { calculationRepository.deleteDraft(it) }
                }
                onNavigateBack()
            }
        }
    }

    fun discardChanges(onNavigateBack: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(showUnsavedDialog = false, isDirty = false) }
            val draftId = if (state.mode == EditorMode.EXISTING) {
                "draft_" + (state.editingSavedId ?: "")
            } else {
                state.calculationId ?: ""
            }
            if (draftId.isNotBlank()) {
                calculationRepository.deleteDraft(draftId)
            }
            onNavigateBack()
        }
    }

    fun saveCalculation(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        val titleText = state.title.text.trim()
        if (titleText.isEmpty()) {
            _uiState.update {
                it.copy(
                    validationError = "TITLE_REQUIRED",
                    activeRowId = null,
                    activeField = ActiveField.TITLE,
                    keyboardMode = JournalKeyboardMode.TEXT,
                    keyboardExpanded = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null) }

            val targetId = state.editingSavedId ?: state.calculationId ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val populatedRows = state.rows.filter { it.isPopulated }
            val itemEntities = populatedRows.mapIndexed { idx, row ->
                val centimes = MoneyMath.toCentimes(row.amount.text, state.currency) ?: 0L
                CalculationItemEntity(
                    id = UUID.randomUUID().toString(),
                    calculationId = targetId,
                    label = row.title.text.trim(),
                    amountCentimes = centimes,
                    rawExpression = row.amount.text.trim(),
                    position = idx,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            }

            val calculationEntity = CalculationEntity(
                id = targetId,
                title = titleText,
                currency = state.currency.name,
                createdAtEpochMs = state.createdAtEpochMs ?: now,
                updatedAtEpochMs = now,
                status = "SAVED",
                editingCalculationId = null,
                groupId = state.groupId,
                paymentStatus = state.paymentStatus,
                calcType = state.calcType,
                dueDateEpochMs = state.dueDateEpochMs,
                reminderEnabled = state.reminderEnabled,
                reminderTimeEpochMs = state.reminderTimeEpochMs
            )

            calculationRepository.saveCalculation(calculationEntity, itemEntities)

            // Cancel any pending draft saves
            draftSaveJob?.cancel()

            _uiState.update {
                it.copy(
                    calculationId = targetId,
                    editingSavedId = targetId,
                    mode = EditorMode.EXISTING,
                    isSaving = false,
                    isDirty = false,
                    isSaved = true
                )
            }
            onSuccess?.invoke()
        }
    }

    fun togglePaymentStatus() {
        _uiState.update { state ->
            val nextStatus = if (state.paymentStatus == "PAID") "UNPAID" else "PAID"
            state.copy(paymentStatus = nextStatus, isDirty = true)
        }
        val current = _uiState.value
        if (current.mode == EditorMode.EXISTING && current.editingSavedId != null) {
            viewModelScope.launch {
                calculationRepository.updatePaymentStatus(current.editingSavedId, current.paymentStatus)
            }
        }
        scheduleDraftSave()
    }

    fun toggleCalcType() {
        _uiState.update { state ->
            val nextType = if (state.calcType == "CREDIT") "PERSONNEL" else "CREDIT"
            val nextStatus = if (nextType == "CREDIT") "UNPAID" else "PAID"
            state.copy(calcType = nextType, paymentStatus = nextStatus, isDirty = true)
        }
        val current = _uiState.value
        if (current.mode == EditorMode.EXISTING && current.editingSavedId != null) {
            viewModelScope.launch {
                calculationRepository.updateCalcType(current.editingSavedId, current.calcType)
                calculationRepository.updatePaymentStatus(current.editingSavedId, current.paymentStatus)
            }
        }
        scheduleDraftSave()
    }

    private fun scheduleDraftSave() {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(600)
            val state = _uiState.value
            if (!state.isDirty) return@launch

            val now = System.currentTimeMillis()
            val draftId: String
            val editingSavedId: String?

            if (state.mode == EditorMode.EXISTING && state.editingSavedId != null) {
                draftId = "draft_" + state.editingSavedId
                editingSavedId = state.editingSavedId
            } else {
                draftId = state.calculationId ?: UUID.randomUUID().toString()
                editingSavedId = null
            }

            val itemEntities = state.rows.mapIndexed { idx, row ->
                val centimes = MoneyMath.toCentimes(row.amount.text, state.currency) ?: 0L
                CalculationItemEntity(
                    id = UUID.randomUUID().toString(),
                    calculationId = draftId,
                    label = row.title.text,
                    amountCentimes = centimes,
                    rawExpression = row.amount.text,
                    position = idx,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            }

            val draftEntity = CalculationEntity(
                id = draftId,
                title = state.title.text,
                currency = state.currency.name,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                status = "DRAFT",
                editingCalculationId = editingSavedId,
                groupId = state.groupId,
                paymentStatus = state.paymentStatus,
                calcType = state.calcType,
                dueDateEpochMs = state.dueDateEpochMs,
                reminderEnabled = state.reminderEnabled,
                reminderTimeEpochMs = state.reminderTimeEpochMs
            )

            calculationRepository.saveDraft(draftEntity, itemEntities)
        }
    }

    fun setDueDateDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDueDateDialog = visible) }
    }

    fun updateDueDate(
        dueDateEpochMs: Long?,
        reminderEnabled: Boolean,
        reminderTimeEpochMs: Long?,
        context: Context? = null
    ) {
        _uiState.update {
            it.copy(
                dueDateEpochMs = dueDateEpochMs,
                reminderEnabled = reminderEnabled,
                reminderTimeEpochMs = reminderTimeEpochMs,
                isDirty = true
            )
        }
        val targetId = _uiState.value.editingSavedId ?: _uiState.value.calculationId
        if (targetId != null) {
            viewModelScope.launch {
                calculationRepository.updateCreditDueDate(
                    id = targetId,
                    dueDateEpochMs = dueDateEpochMs,
                    reminderEnabled = reminderEnabled,
                    reminderTimeEpochMs = reminderTimeEpochMs
                )
                if (context != null) {
                    if (reminderEnabled && reminderTimeEpochMs != null && reminderTimeEpochMs > System.currentTimeMillis()) {
                        val totalCentimes = _uiState.value.totalCentimes
                        val totalFormatted = String.format(java.util.Locale.US, "%.2f %s", totalCentimes / 100.0, _uiState.value.currency.name)
                        CreditReminderScheduler.scheduleReminder(
                            context = context,
                            calculationId = targetId,
                            title = _uiState.value.title.text.ifBlank { "Crédit" },
                            amountFormatted = totalFormatted,
                            reminderTimeEpochMs = reminderTimeEpochMs
                        )
                    } else {
                        CreditReminderScheduler.cancelReminder(context, targetId)
                    }
                }
            }
        }
        scheduleDraftSave()
    }

    fun shareAsImage(context: Context, isRtl: Boolean) {
        val state = _uiState.value
        val titleText = state.title.text.trim().ifBlank { context.getString(R.string.editor_new_title) }
        val targetId = state.editingSavedId ?: state.calculationId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val populatedRows = state.rows.filter { it.isPopulated }
        val itemEntities = populatedRows.mapIndexed { idx, row ->
            val centimes = MoneyMath.toCentimes(row.amount.text, state.currency) ?: 0L
            CalculationItemEntity(
                id = UUID.randomUUID().toString(),
                calculationId = targetId,
                label = row.title.text.trim(),
                amountCentimes = centimes,
                rawExpression = row.amount.text.trim(),
                position = idx,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        }
        val totalCentimes = itemEntities.sumOf { it.amountCentimes }

        viewModelScope.launch {
            val existing = targetId.let { calculationRepository.getCalculation(it) }
            val groupName = existing?.calculation?.groupId?.let { gid ->
                calculationRepository.getGroup(gid)?.name
            }
            CalculationImageShareHelper.shareCalculation(
                context = context,
                calculationId = targetId,
                title = titleText,
                currency = state.currency,
                items = itemEntities,
                totalCentimes = totalCentimes,
                groupName = groupName,
                createdAtEpochMs = state.createdAtEpochMs ?: now,
                isRtl = isRtl
            )
        }
    }

    fun exportAsPdf(context: Context, isRtl: Boolean) {
        val state = _uiState.value
        val titleText = state.title.text.trim().ifBlank { context.getString(R.string.editor_new_title) }
        val targetId = state.editingSavedId ?: state.calculationId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val populatedRows = state.rows.filter { it.isPopulated }
        val itemEntities = populatedRows.mapIndexed { idx, row ->
            val centimes = MoneyMath.toCentimes(row.amount.text, state.currency) ?: 0L
            CalculationItemEntity(
                id = UUID.randomUUID().toString(),
                calculationId = targetId,
                label = row.title.text.trim(),
                amountCentimes = centimes,
                rawExpression = row.amount.text.trim(),
                position = idx,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        }
        val calcEntity = CalculationEntity(
            id = targetId,
            title = titleText,
            currency = state.currency.name,
            createdAtEpochMs = state.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
            status = "SAVED",
            groupId = state.groupId
        )
        val calcWithItems = CalculationWithItems(calcEntity, itemEntities)

        viewModelScope.launch {
            val groupName = state.groupId?.let { gid ->
                calculationRepository.getGroup(gid)?.name
            }
            val pdfFile = withContext(Dispatchers.IO) {
                PdfExportHelper.exportSingleCalculationPdf(
                    context = context,
                    calculationWithItems = calcWithItems,
                    groupName = groupName,
                    isRtl = isRtl
                )
            }
            FileExportManager.shareFile(
                context = context,
                file = pdfFile,
                mimeType = FileExportManager.MIME_PDF,
                subject = titleText
            )
        }
    }

    fun exportAsExcel(context: Context) {
        val state = _uiState.value
        val titleText = state.title.text.trim().ifBlank { context.getString(R.string.editor_new_title) }
        val targetId = state.editingSavedId ?: state.calculationId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val populatedRows = state.rows.filter { it.isPopulated }
        val itemEntities = populatedRows.mapIndexed { idx, row ->
            val centimes = MoneyMath.toCentimes(row.amount.text, state.currency) ?: 0L
            CalculationItemEntity(
                id = UUID.randomUUID().toString(),
                calculationId = targetId,
                label = row.title.text.trim(),
                amountCentimes = centimes,
                rawExpression = row.amount.text.trim(),
                position = idx,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        }
        val calcEntity = CalculationEntity(
            id = targetId,
            title = titleText,
            currency = state.currency.name,
            createdAtEpochMs = state.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
            status = "SAVED",
            groupId = state.groupId
        )
        val calcWithItems = CalculationWithItems(calcEntity, itemEntities)

        viewModelScope.launch {
            val groupName = state.groupId?.let { gid ->
                calculationRepository.getGroup(gid)?.name
            }
            val csvFile = withContext(Dispatchers.IO) {
                ExcelExportHelper.exportSingleCalculation(
                    context = context,
                    calculationWithItems = calcWithItems,
                    groupName = groupName
                )
            }
            FileExportManager.shareFile(
                context = context,
                file = csvFile,
                mimeType = FileExportManager.MIME_CSV,
                subject = titleText
            )
        }
    }

    fun saveAsTemplate(context: Context) {
        val currentState = _uiState.value
        val title = currentState.title.text.trim().ifBlank {
            context.getString(R.string.editor_new_title)
        }
        val items = currentState.rows.map { it.title.text.trim() }.filter { it.isNotBlank() }
        val repo = templateRepository ?: TemplateRepository.getInstance(context)
        viewModelScope.launch {
            repo.saveCustomTemplate(
                title = title,
                calcType = currentState.calcType,
                currency = currentState.currency.name,
                itemLabels = items
            )
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.template_saved_success),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun duplicateCurrentCalculation(onDuplicated: (String) -> Unit) {
        val state = _uiState.value
        val titleText = state.title.text.trim()
        val targetId = state.editingSavedId ?: state.calculationId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            if (titleText.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val populatedRows = state.rows.filter { it.isPopulated }
                val itemEntities = populatedRows.mapIndexed { idx, row ->
                    val centimes = MoneyMath.toCentimes(row.amount.text, state.currency) ?: 0L
                    CalculationItemEntity(
                        id = UUID.randomUUID().toString(),
                        calculationId = targetId,
                        label = row.title.text.trim(),
                        amountCentimes = centimes,
                        rawExpression = row.amount.text.trim(),
                        position = idx,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now
                    )
                }
                val calcEntity = CalculationEntity(
                    id = targetId,
                    title = titleText,
                    currency = state.currency.name,
                    createdAtEpochMs = state.createdAtEpochMs ?: now,
                    updatedAtEpochMs = now,
                    status = "SAVED",
                    groupId = state.groupId,
                    paymentStatus = state.paymentStatus,
                    calcType = state.calcType
                )
                calculationRepository.saveCalculation(calcEntity, itemEntities)
            }
            val duplicated = calculationRepository.duplicateCalculation(targetId)
            if (duplicated != null) {
                withContext(Dispatchers.Main) {
                    onDuplicated(duplicated.calculation.id)
                }
            }
        }
    }
}
