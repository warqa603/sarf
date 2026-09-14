package com.cash.guide.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyPiece
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.HisabiMetrics
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.Ink
import com.cash.guide.ui.notebook.InkTone
import com.cash.guide.ui.notebook.ManropeFamily
import com.cash.guide.ui.notebook.MutedInk
import com.cash.guide.ui.notebook.Paper
import com.cash.guide.ui.notebook.PaperWarm
import com.cash.guide.ui.notebook.Rule
import com.cash.guide.ui.notebook.RuledDocument
import com.cash.guide.ui.notebook.RuledPaperBackground
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.WritingInk
import com.cash.guide.ui.notebook.arabicWritingStyle
import com.cash.guide.ui.notebook.baselineOnPaperRule
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.JournalKeyboardController
import com.cash.guide.domain.JournalKeyboardMode
import com.cash.guide.domain.JournalKeyboardLanguage
import com.cash.guide.domain.JournalShiftMode
import com.cash.guide.domain.JournalShiftState
import com.cash.guide.domain.ShiftAction
import com.cash.guide.domain.AndroidIcuGraphemeSegmenter
import android.os.SystemClock
import com.cash.guide.ui.notebook.JournalCategoryHeader
import com.cash.guide.ui.notebook.JournalCompactNumericDock
import com.cash.guide.ui.notebook.JournalTextKeyboardDock
import com.cash.guide.ui.notebook.JournalCalculatorPopup
import com.cash.guide.ui.notebook.JournalEntryRow
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalTotalResultBand
import androidx.compose.ui.platform.LocalTextInputService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cash.guide.ui.notebook.JournalAddRowButton
import com.cash.guide.ui.notebook.JournalRuleSpacing

enum class AppScreen {
    HOME,
    CALCULATOR
}

enum class ActiveField {
    NONE,
    TITLE,
    AMOUNT,
    HEADER_TITLE
}

data class EntryRow(
    val id: Long,
    val title: String = "",
    val expression: String = ""
)

private val EntryRowListSaver = listSaver<SnapshotStateList<EntryRow>, Any>(
    save = { stateList ->
        stateList.flatMap { listOf<Any>(it.id, it.title, it.expression) }
    },
    restore = { flatList ->
        val list = mutableStateListOf<EntryRow>()
        for (i in flatList.indices step 3) {
            val id = (flatList[i] as? Long) ?: (flatList[i] as? Number)?.toLong() ?: 1L
            val title = (flatList.getOrNull(i + 1) as? String).orEmpty()
            val expression = (flatList.getOrNull(i + 2) as? String).orEmpty()
            list.add(EntryRow(id, title, expression))
        }
        list
    }
)

private object BanknoteImageCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(path: String): ImageBitmap? = cache[path]

    fun decode(context: Context, path: String): ImageBitmap? {
        val existing = cache[path]
        if (existing != null) return existing
        return runCatching {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }.getOrNull()?.also { cache[path] = it }
    }
}

@Composable
private fun rememberBanknoteImage(path: String?): ImageBitmap? {
    val context = LocalContext.current
    if (path == null) return null
    val bitmapState = produceState<ImageBitmap?>(initialValue = BanknoteImageCache.get(path), key1 = path) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                BanknoteImageCache.decode(context, path)
            }
            value = loaded
        }
    }
    return bitmapState.value
}

@Composable
fun MoneyListApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val focusManager = LocalFocusManager.current
        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        var currentScreen by rememberSaveable { mutableStateOf(AppScreen.CALCULATOR) }
        var selectedUnit by rememberSaveable { mutableStateOf(MoneyUnit.RIAL) }
        var categoryName by rememberSaveable { mutableStateOf("Flous l9or3a") }

        var showCalculatorPopup by rememberSaveable { mutableStateOf(false) }
        var popupExpression by rememberSaveable { mutableStateOf("") }
        var popupResult by rememberSaveable { mutableStateOf("") }
        var popupHasError by rememberSaveable { mutableStateOf(false) }
        var popupIsEvaluated by rememberSaveable { mutableStateOf(false) }

        var nextId by rememberSaveable { mutableLongStateOf(2L) }
        val rows = rememberSaveable(saver = EntryRowListSaver) {
            mutableStateListOf(
                EntryRow(id = 1L)
            )
        }
        var activeRowId by rememberSaveable { mutableStateOf<Long?>(1L) }
        var activeField by rememberSaveable { mutableStateOf(ActiveField.TITLE) }
        var keyboardMode by rememberSaveable { mutableStateOf(JournalKeyboardMode.TEXT) }
        var selectedLanguage by rememberSaveable { mutableStateOf(JournalKeyboardLanguage.FRENCH) }
        var shiftMode by rememberSaveable { mutableStateOf(JournalShiftMode.ONE_SHOT) }
        var lastLatinShiftMode by rememberSaveable { mutableStateOf(JournalShiftMode.ONE_SHOT) }
        var shiftState by remember {
            mutableStateOf(
                JournalShiftState(
                    mode = JournalShiftMode.ONE_SHOT,
                    isAutoOneShot = true
                )
            )
        }
        var keyboardExpanded by rememberSaveable { mutableStateOf(true) }
        var pendingFocusRowId by remember { mutableStateOf<Long?>(1L) }
        var showBreakdown by rememberSaveable { mutableStateOf(false) }
        var showResetConfirmDialog by remember { mutableStateOf(false) }

        val graphemeSegmenter = remember { AndroidIcuGraphemeSegmenter() }

        val titleValueMap = remember { mutableStateMapOf<Long, TextFieldValue>() }
        val amountValueMap = remember { mutableStateMapOf<Long, TextFieldValue>() }

        fun getTitleValue(row: EntryRow): TextFieldValue {
            val existing = titleValueMap[row.id]
            return if (existing != null && existing.text == row.title) {
                existing
            } else {
                TextFieldValue(text = row.title, selection = TextRange(row.title.length)).also {
                    titleValueMap[row.id] = it
                }
            }
        }

        fun getAmountValue(row: EntryRow): TextFieldValue {
            val existing = amountValueMap[row.id]
            return if (existing != null && existing.text == row.expression) {
                existing
            } else {
                TextFieldValue(text = row.expression, selection = TextRange(row.expression.length)).also {
                    amountValueMap[row.id] = it
                }
            }
        }

        fun updateTitle(id: Long, value: TextFieldValue) {
            titleValueMap[id] = value
            val index = rows.indexOfFirst { it.id == id }
            if (index >= 0) {
                rows[index] = rows[index].copy(title = value.text)
            }
        }

        fun updateAmount(id: Long, value: TextFieldValue) {
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
            amountValueMap[id] = sanitizedValue
            val index = rows.indexOfFirst { it.id == id }
            if (index >= 0) {
                rows[index] = rows[index].copy(expression = sanitizedValue.text)
            }
        }

        val hasData = rows.any { it.title.isNotBlank() || it.expression.isNotBlank() }

        fun resetNewList() {
            focusManager.clearFocus()
            rows.clear()
            titleValueMap.clear()
            amountValueMap.clear()
            val initialRow = EntryRow(id = 1L)
            rows.add(initialRow)
            activeRowId = 1L
            activeField = ActiveField.TITLE
            keyboardMode = JournalKeyboardMode.TEXT
            keyboardExpanded = true
            if (selectedLanguage != JournalKeyboardLanguage.ARABIC) {
                shiftState = JournalKeyboardController.reduceShift(
                    state = shiftState,
                    action = ShiftAction.AutoSetOneShot,
                    monotonicNow = { SystemClock.uptimeMillis() }
                )
                shiftMode = shiftState.mode
                lastLatinShiftMode = shiftState.mode
            }
            nextId = 2L
            showCalculatorPopup = false
            popupExpression = ""
            popupResult = ""
            popupHasError = false
            popupIsEvaluated = false
            pendingFocusRowId = 1L
        }

        fun handleBackPress() {
            if (showCalculatorPopup) {
                showCalculatorPopup = false
                popupExpression = ""
                popupResult = ""
                popupHasError = false
                popupIsEvaluated = false
            } else if (hasData) {
                showResetConfirmDialog = true
            } else {
                currentScreen = AppScreen.HOME
            }
        }

        fun addNewRow() {
            val newId = nextId++
            val newRow = EntryRow(id = newId)
            rows.add(newRow)
            activeRowId = newId
            activeField = ActiveField.TITLE
            keyboardMode = JournalKeyboardMode.TEXT
            keyboardExpanded = true
            if (selectedLanguage != JournalKeyboardLanguage.ARABIC) {
                shiftState = JournalKeyboardController.reduceShift(
                    state = shiftState,
                    action = ShiftAction.AutoSetOneShot,
                    monotonicNow = { SystemClock.uptimeMillis() }
                )
                shiftMode = shiftState.mode
                lastLatinShiftMode = shiftState.mode
            }
            pendingFocusRowId = newId
            coroutineScope.launch {
                listState.animateScrollToItem(rows.size)
            }
        }

        fun confirmRowEdit(id: Long) {
            focusManager.clearFocus()
            activeRowId = null
            activeField = ActiveField.NONE
            keyboardMode = JournalKeyboardMode.NONE
            keyboardExpanded = false
            shiftState = JournalShiftState(mode = JournalShiftMode.OFF)
            shiftMode = JournalShiftMode.OFF
        }

        fun removeRow(id: Long) {
            val indexToRemove = rows.indexOfFirst { it.id == id }
            if (indexToRemove >= 0) {
                rows.removeAt(indexToRemove)
                titleValueMap.remove(id)
                amountValueMap.remove(id)
                if (activeRowId == id) {
                    activeRowId = null
                    activeField = ActiveField.NONE
                    keyboardMode = JournalKeyboardMode.NONE
                    keyboardExpanded = false
                }
            }
        }

        fun selectUnit(unit: MoneyUnit) {
            if (unit == selectedUnit) return
            rows.indices.forEach { index ->
                val row = rows[index]
                val converted = MoneyMath.convertExpression(row.expression, selectedUnit, unit)
                rows[index] = row.copy(expression = converted)
                amountValueMap[row.id] = TextFieldValue(text = converted, selection = TextRange(converted.length))
            }
            selectedUnit = unit
        }

        fun openCalculatorPopup() {
            focusManager.clearFocus()
            showCalculatorPopup = true
            popupExpression = ""
            popupResult = ""
            popupHasError = false
            popupIsEvaluated = false
        }

        fun closeCalculatorPopup() {
            showCalculatorPopup = false
            popupExpression = ""
            popupResult = ""
            popupHasError = false
            popupIsEvaluated = false
        }

        fun applyTextKey(text: String) {
            val targetId = activeRowId.takeIf { id -> rows.any { it.id == id } } ?: return
            val row = rows.first { it.id == targetId }
            val currentVal = getTitleValue(row)
            val newVal = JournalKeyboardController.insertText(currentVal, text)
            updateTitle(targetId, newVal)
            if (selectedLanguage != JournalKeyboardLanguage.ARABIC) {
                shiftState = JournalKeyboardController.reduceShift(
                    state = shiftState,
                    action = ShiftAction.UserTypedText(text),
                    monotonicNow = { SystemClock.uptimeMillis() }
                )
                shiftMode = shiftState.mode
                lastLatinShiftMode = shiftState.mode
            }
        }

        fun applyTextBackspace() {
            val targetId = activeRowId.takeIf { id -> rows.any { it.id == id } } ?: return
            val row = rows.first { it.id == targetId }
            val currentVal = getTitleValue(row)
            val newVal = JournalKeyboardController.deleteBackward(currentVal, graphemeSegmenter)
            updateTitle(targetId, newVal)
        }

        fun applyCompactKey(key: String) {
            val targetId = activeRowId.takeIf { id -> rows.any { it.id == id } } ?: return
            val row = rows.first { it.id == targetId }
            val currentVal = getAmountValue(row)
            val newVal = if (key == "⌫") {
                JournalKeyboardController.deleteBackward(currentVal, graphemeSegmenter)
            } else {
                val curText = currentVal.text
                if (key == "." && curText.contains('.')) {
                    currentVal
                } else {
                    JournalKeyboardController.insertText(currentVal, key)
                }
            }
            updateAmount(targetId, newVal)
        }

        fun cycleLanguage() {
            val nextLang = JournalKeyboardController.nextLanguage(selectedLanguage)
            if (selectedLanguage == JournalKeyboardLanguage.ARABIC && nextLang != JournalKeyboardLanguage.ARABIC) {
                shiftState = shiftState.copy(mode = lastLatinShiftMode)
                shiftMode = lastLatinShiftMode
            } else if (selectedLanguage != JournalKeyboardLanguage.ARABIC && nextLang == JournalKeyboardLanguage.ARABIC) {
                lastLatinShiftMode = shiftState.mode
                shiftMode = JournalShiftMode.OFF
            }
            selectedLanguage = nextLang
        }

        fun selectLanguage(lang: JournalKeyboardLanguage) {
            if (selectedLanguage != JournalKeyboardLanguage.ARABIC && lang == JournalKeyboardLanguage.ARABIC) {
                lastLatinShiftMode = shiftState.mode
                shiftMode = JournalShiftMode.OFF
            } else if (selectedLanguage == JournalKeyboardLanguage.ARABIC && lang != JournalKeyboardLanguage.ARABIC) {
                shiftState = shiftState.copy(mode = lastLatinShiftMode)
                shiftMode = lastLatinShiftMode
            }
            selectedLanguage = lang
        }

        fun toggleShift() {
            if (selectedLanguage != JournalKeyboardLanguage.ARABIC) {
                shiftState = JournalKeyboardController.reduceShift(
                    state = shiftState,
                    action = ShiftAction.UserTapShift(SystemClock.uptimeMillis()),
                    monotonicNow = { SystemClock.uptimeMillis() }
                )
                shiftMode = shiftState.mode
                lastLatinShiftMode = shiftState.mode
            }
        }

        fun switchToTextMode() {
            val targetId = activeRowId.takeIf { id -> rows.any { it.id == id } } ?: rows.firstOrNull()?.id ?: return
            activeRowId = targetId
            activeField = ActiveField.TITLE
            keyboardMode = JournalKeyboardMode.TEXT
            keyboardExpanded = true
            val titleText = rows.firstOrNull { it.id == targetId }?.title.orEmpty()
            if (titleText.isEmpty() && selectedLanguage != JournalKeyboardLanguage.ARABIC) {
                shiftState = JournalKeyboardController.reduceShift(
                    state = shiftState,
                    action = ShiftAction.AutoSetOneShot,
                    monotonicNow = { SystemClock.uptimeMillis() }
                )
                shiftMode = shiftState.mode
                lastLatinShiftMode = shiftState.mode
            }
        }

        fun switchToNumericMode() {
            val targetId = activeRowId.takeIf { id -> rows.any { it.id == id } } ?: rows.firstOrNull()?.id ?: return
            activeRowId = targetId
            activeField = ActiveField.AMOUNT
            keyboardMode = JournalKeyboardMode.NUMBER
            keyboardExpanded = true
        }

        fun applyPopupKey(key: String) {
            val operators = setOf('+', '−', '×', '÷')
            when (key) {
                "C" -> {
                    popupHasError = false
                    popupIsEvaluated = false
                    popupResult = ""
                    popupExpression = ""
                }
                "⌫" -> {
                    popupHasError = false
                    popupIsEvaluated = false
                    popupResult = ""
                    if (popupExpression.isNotEmpty()) {
                        popupExpression = popupExpression.dropLast(1)
                    }
                }
                "=" -> {
                    if (popupExpression.isBlank()) return
                    val evaluated = MoneyMath.evaluate(popupExpression)?.stripTrailingZeros()?.toPlainString()
                    if (evaluated != null && !evaluated.contains("NaN") && !evaluated.contains("Infinity")) {
                        popupResult = evaluated
                        popupIsEvaluated = true
                        popupHasError = false
                    } else {
                        popupResult = ""
                        popupIsEvaluated = false
                        popupHasError = true
                    }
                }
                "+", "−", "×", "÷" -> {
                    popupHasError = false
                    if (popupIsEvaluated && popupResult.isNotBlank()) {
                        popupExpression = popupResult + key
                        popupResult = ""
                        popupIsEvaluated = false
                    } else {
                        popupIsEvaluated = false
                        popupResult = ""
                        popupExpression = when {
                            popupExpression.isBlank() -> popupExpression
                            popupExpression.last() == '(' -> popupExpression
                            popupExpression.last() in operators -> popupExpression.dropLast(1) + key
                            else -> popupExpression + key
                        }
                    }
                }
                "(" -> {
                    popupHasError = false
                    if (popupIsEvaluated) {
                        popupExpression = "("
                        popupResult = ""
                        popupIsEvaluated = false
                    } else {
                        popupExpression = if (popupExpression.isNotEmpty() && (popupExpression.last().isDigit() || popupExpression.last() == ')')) {
                            popupExpression + "×("
                        } else {
                            popupExpression + "("
                        }
                        popupResult = ""
                        popupIsEvaluated = false
                    }
                }
                ")" -> {
                    popupHasError = false
                    val openCount = popupExpression.count { it == '(' }
                    val closeCount = popupExpression.count { it == ')' }
                    if (!popupIsEvaluated && openCount > closeCount && popupExpression.isNotEmpty() && (popupExpression.last().isDigit() || popupExpression.last() == ')')) {
                        popupExpression += ")"
                        popupResult = ""
                        popupIsEvaluated = false
                    }
                }
                "." -> {
                    popupHasError = false
                    if (popupIsEvaluated) {
                        popupExpression = "0."
                        popupResult = ""
                        popupIsEvaluated = false
                    } else {
                        popupResult = ""
                        popupIsEvaluated = false
                        val lastPart = popupExpression.split('+', '−', '×', '÷', '(', ')').lastOrNull().orEmpty()
                        popupExpression = when {
                            lastPart.contains('.') -> popupExpression
                            lastPart.isEmpty() -> popupExpression + "0."
                            else -> popupExpression + "."
                        }
                    }
                }
                else -> { // Digits 0-9
                    popupHasError = false
                    if (popupIsEvaluated) {
                        popupExpression = key
                        popupResult = ""
                        popupIsEvaluated = false
                    } else {
                        popupResult = ""
                        popupIsEvaluated = false
                        if (popupExpression.isNotEmpty() && popupExpression.last() == ')') {
                            popupExpression += "×" + key
                        } else {
                            val lastPart = popupExpression.split('+', '−', '×', '÷', '(', ')').lastOrNull().orEmpty()
                            if (lastPart == "0") {
                                if (key != "0") {
                                    popupExpression = popupExpression.dropLast(1) + key
                                }
                            } else if (popupExpression.length < 32) {
                                popupExpression += key
                            }
                        }
                    }
                }
            }
        }

        fun confirmPopupResult() {
            val targetResult = if (popupIsEvaluated && popupResult.isNotBlank() && !popupHasError) {
                popupResult
            } else if (!popupHasError && popupResult.isNotBlank()) {
                popupResult
            } else if (!popupHasError && popupExpression.isNotBlank()) {
                MoneyMath.evaluate(popupExpression)?.stripTrailingZeros()?.toPlainString()
            } else null

            if (targetResult.isNullOrBlank() || targetResult.contains("NaN") || targetResult.contains("Infinity")) return
            val targetId = activeRowId.takeIf { id -> rows.any { it.id == id } } ?: return
            updateAmount(targetId, TextFieldValue(text = targetResult, selection = TextRange(targetResult.length)))
            activeField = ActiveField.AMOUNT
            showCalculatorPopup = false
            popupExpression = ""
            popupResult = ""
            popupIsEvaluated = false
            popupHasError = false
        }

        val hasInvalidRows = rows.any { it.expression.isNotBlank() && !MoneyMath.isValidExpression(it.expression) }

        val totalCentimes = if (hasInvalidRows) {
            0L
        } else {
            rows.sumOf { row ->
                MoneyMath.toCentimes(row.expression, selectedUnit) ?: 0L
            }
        }

        RuledPaperBackground {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HisabiHomeScreen(
                        selectedUnit = selectedUnit,
                        onUnitSelected = ::selectUnit,
                        onOpenCalculator = { currentScreen = AppScreen.CALCULATOR }
                    )
                }

                AppScreen.CALCULATOR -> {
                    HisabiCalculatorScreen(
                        categoryName = categoryName,
                        rows = rows,
                        activeRowId = activeRowId,
                        activeField = activeField,
                        keyboardMode = keyboardMode,
                        language = selectedLanguage,
                        shiftMode = shiftMode,
                        selectedUnit = selectedUnit,
                        hasInvalidRows = hasInvalidRows,
                        totalCentimes = totalCentimes,
                        keyboardExpanded = keyboardExpanded,
                        showCalculatorPopup = showCalculatorPopup,
                        popupExpression = popupExpression,
                        popupResult = popupResult,
                        popupHasError = popupHasError,
                        popupIsEvaluated = popupIsEvaluated,
                        pendingFocusRowId = pendingFocusRowId,
                        listState = listState,
                        getTitleValue = ::getTitleValue,
                        getAmountValue = ::getAmountValue,
                        onBackClick = ::handleBackPress,
                        onOpenCalculator = ::openCalculatorPopup,
                        onCloseCalculator = ::closeCalculatorPopup,
                        onPopupKey = ::applyPopupKey,
                        onConfirmPopup = ::confirmPopupResult,
                        onTitleChange = ::updateTitle,
                        onAmountChange = ::updateAmount,
                        onTitleFocused = { id ->
                            activeRowId = id
                            activeField = ActiveField.TITLE
                            keyboardMode = JournalKeyboardMode.TEXT
                            keyboardExpanded = true
                            val titleText = rows.firstOrNull { it.id == id }?.title.orEmpty()
                            if (titleText.isEmpty() && selectedLanguage != JournalKeyboardLanguage.ARABIC) {
                                shiftState = JournalKeyboardController.reduceShift(
                                    state = shiftState,
                                    action = ShiftAction.AutoSetOneShot,
                                    monotonicNow = { SystemClock.uptimeMillis() }
                                )
                                shiftMode = shiftState.mode
                                lastLatinShiftMode = shiftState.mode
                            }
                        },
                        onAmountFocused = { id ->
                            activeRowId = id
                            activeField = ActiveField.AMOUNT
                            keyboardMode = JournalKeyboardMode.NUMBER
                            keyboardExpanded = true
                        },
                        onAddRow = ::addNewRow,
                        onPendingFocusHandled = { pendingFocusRowId = null },
                        onRemoveRow = ::removeRow,
                        onConfirmRow = ::confirmRowEdit,
                        onCycleLanguage = ::cycleLanguage,
                        onSelectLanguage = ::selectLanguage,
                        onToggleShift = ::toggleShift,
                        onTextKey = ::applyTextKey,
                        onTextBackspace = ::applyTextBackspace,
                        onCompactKey = ::applyCompactKey,
                        onSwitchToTextMode = ::switchToTextMode,
                        onSwitchToNumericMode = ::switchToNumericMode,
                        onToggleKeyboard = { keyboardExpanded = !keyboardExpanded },
                        onShowBreakdown = {
                            if (!hasInvalidRows && totalCentimes > 0) showBreakdown = true
                        }
                    )
                }
            }
        }

        if (showResetConfirmDialog) {
            HisabiResetDialog(
                onConfirm = {
                    showResetConfirmDialog = false
                    resetNewList()
                    currentScreen = AppScreen.HOME
                },
                onDismiss = { showResetConfirmDialog = false }
            )
        }

        if (showBreakdown && !hasInvalidRows && totalCentimes > 0) {
            HisabiBreakdownSheet(
                totalCentimes = totalCentimes,
                onDismiss = { showBreakdown = false }
            )
        }
    }
}

@Composable
private fun HisabiCalculatorScreen(
    categoryName: String,
    rows: List<EntryRow>,
    activeRowId: Long?,
    activeField: ActiveField,
    keyboardMode: JournalKeyboardMode,
    language: JournalKeyboardLanguage,
    shiftMode: JournalShiftMode,
    selectedUnit: MoneyUnit,
    hasInvalidRows: Boolean,
    totalCentimes: Long,
    keyboardExpanded: Boolean,
    showCalculatorPopup: Boolean,
    popupExpression: String,
    popupResult: String,
    popupHasError: Boolean,
    popupIsEvaluated: Boolean,
    pendingFocusRowId: Long?,
    listState: LazyListState,
    getTitleValue: (EntryRow) -> TextFieldValue,
    getAmountValue: (EntryRow) -> TextFieldValue,
    onBackClick: () -> Unit,
    onOpenCalculator: () -> Unit,
    onCloseCalculator: () -> Unit,
    onPopupKey: (String) -> Unit,
    onConfirmPopup: () -> Unit,
    onTitleChange: (Long, TextFieldValue) -> Unit,
    onAmountChange: (Long, TextFieldValue) -> Unit,
    onTitleFocused: (Long) -> Unit,
    onAmountFocused: (Long) -> Unit,
    onAddRow: () -> Unit,
    onPendingFocusHandled: () -> Unit,
    onRemoveRow: (Long) -> Unit,
    onConfirmRow: (Long) -> Unit,
    onCycleLanguage: () -> Unit,
    onSelectLanguage: (JournalKeyboardLanguage) -> Unit,
    onToggleShift: () -> Unit,
    onTextKey: (String) -> Unit,
    onTextBackspace: () -> Unit,
    onCompactKey: (String) -> Unit,
    onSwitchToTextMode: () -> Unit,
    onSwitchToNumericMode: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onShowBreakdown: () -> Unit
) {
    BackHandler(onBack = {
        if (showCalculatorPopup) {
            onCloseCalculator()
        } else if (keyboardExpanded) {
            onToggleKeyboard()
        } else {
            onBackClick()
        }
    })

    val currencySuffix = if (selectedUnit == MoneyUnit.DIRHAM) "DH" else "rial"
    val primaryFormatted = JournalLedgerManager.formatTotal(totalCentimes, selectedUnit)

    val canConfirm = !popupHasError && rows.any { it.id == activeRowId } && (
        (popupIsEvaluated && popupResult.isNotBlank()) ||
        (popupResult.isNotBlank()) ||
        (popupExpression.isNotBlank() && MoneyMath.isValidExpression(popupExpression))
    )

    var dockHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val dockClearanceDp = remember(dockHeightPx, density) {
        with(density) {
            if (dockHeightPx > 0) (dockHeightPx.toDp() + 12.dp) else 16.dp
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        @Suppress("DEPRECATION")
        LocalTextInputService provides null,
        androidx.compose.foundation.text.selection.LocalTextSelectionColors provides androidx.compose.foundation.text.selection.TextSelectionColors(
            handleColor = JournalRule.copy(alpha = 0.85f),
            backgroundColor = HighlighterPink.copy(alpha = 0.35f)
        )
    ) {
        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
        androidx.compose.ui.platform.InterceptPlatformTextInput(
            interceptor = { _, _ ->
                kotlinx.coroutines.awaitCancellation()
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JournalPaper)
                        .statusBarsPadding()
                ) {
                JournalCategoryHeader(
                    categoryName = categoryName,
                    onOpenCalculator = onOpenCalculator
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    JournalRuledDocument(listState = listState) {
                        rows.forEachIndexed { index, row ->
                            androidx.compose.runtime.key(row.id) {
                                val isRowValid = MoneyMath.isValidExpression(row.expression)
                                val titleVal = getTitleValue(row)
                                val amountVal = getAmountValue(row)
                                val titleRequester = remember(row.id) { FocusRequester() }
                                val amountRequester = remember(row.id) { FocusRequester() }

                                LaunchedEffect(pendingFocusRowId) {
                                    if (pendingFocusRowId == row.id) {
                                        titleRequester.requestFocus()
                                        onPendingFocusHandled()
                                    }
                                }

                                JournalEntryRow(
                                    rowNumber = index + 1,
                                    titleValue = titleVal,
                                    amountValue = amountVal,
                                    currencySuffix = currencySuffix,
                                    isTitleActive = (row.id == activeRowId && activeField == ActiveField.TITLE),
                                    isAmountActive = (row.id == activeRowId && activeField == ActiveField.AMOUNT),
                                    isValid = isRowValid,
                                    titleFocusRequester = titleRequester,
                                    amountFocusRequester = amountRequester,
                                    onTitleValueChange = { onTitleChange(row.id, it) },
                                    onAmountValueChange = { onAmountChange(row.id, it) },
                                    onTitleFocused = { onTitleFocused(row.id) },
                                    onAmountFocused = { onAmountFocused(row.id) },
                                    onDelete = { onRemoveRow(row.id) },
                                    onConfirm = { onConfirmRow(row.id) }
                                )
                            }
                        }

                        // 1 empty notebook line before Add Row
                        Spacer(modifier = Modifier.height(JournalRuleSpacing))

                        JournalAddRowButton(onAddRow = onAddRow)

                        val spacerRules = if (keyboardExpanded && rows.size >= 4) 1 else 2
                        Spacer(modifier = Modifier.height(JournalRuleSpacing * spacerRules))

                        JournalTotalResultBand(
                            amount = primaryFormatted,
                            suffix = currencySuffix,
                            hasInvalidRows = hasInvalidRows,
                            canBreakdown = !hasInvalidRows && totalCentimes > 0,
                            onShowBreakdown = onShowBreakdown
                        )

                        Spacer(modifier = Modifier.height(dockClearanceDp))
                    }
                }

                if (!showCalculatorPopup && keyboardMode != JournalKeyboardMode.NONE) {
                    if (keyboardMode == JournalKeyboardMode.TEXT) {
                        JournalTextKeyboardDock(
                            language = language,
                            shiftMode = shiftMode,
                            expanded = keyboardExpanded,
                            onToggleExpand = onToggleKeyboard,
                            onCycleLanguage = onCycleLanguage,
                            onSelectLanguage = onSelectLanguage,
                            onToggleShift = onToggleShift,
                            onInsertText = onTextKey,
                            onBackspace = onTextBackspace,
                            onSwitchToNumericMode = onSwitchToNumericMode,
                            onConfirm = { activeRowId?.let { onConfirmRow(it) } },
                            modifier = Modifier.onSizeChanged { dockHeightPx = it.height }
                        )
                    } else {
                        JournalCompactNumericDock(
                            expanded = keyboardExpanded,
                            onToggleExpand = onToggleKeyboard,
                            onKey = onCompactKey,
                            onSwitchToTextMode = onSwitchToTextMode,
                            modifier = Modifier.onSizeChanged { dockHeightPx = it.height }
                        )
                    }
                }
            }

            // 5. Large Full Calculator Popup Modal (Overlay)
            if (showCalculatorPopup) {
                JournalCalculatorPopup(
                    expression = popupExpression,
                    result = popupResult,
                    hasError = popupHasError,
                    canConfirm = canConfirm,
                    onKey = onPopupKey,
                    onConfirm = onConfirmPopup,
                    onDismiss = onCloseCalculator
                )
            }
        }
    }
}
}

@Composable
private fun HisabiHomeScreen(
    selectedUnit: MoneyUnit,
    onUnitSelected: (MoneyUnit) -> Unit,
    onOpenCalculator: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        RuledDocument {
            // Header Band (58dp = 2 grid units)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HisabiMetrics.Grid * 2)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "حسابي",
                        style = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        ),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                    Text(
                        text = "دفتر الحساب والصرف المغربي",
                        style = arabicWritingStyle(color = MutedInk, sizeSp = 13.5f),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                }
            }

            // Section Label Band (29dp = 1 grid unit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HisabiMetrics.Grid)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "العملة الأساسية:",
                    style = arabicWritingStyle(color = Ink, sizeSp = 15f, weight = FontWeight.Medium),
                    modifier = Modifier.baselineOnPaperRule()
                )
            }

            // Rial Currency Selection Band (58dp = 2 grid units)
            val isRial = selectedUnit == MoneyUnit.RIAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HisabiMetrics.Grid * 2)
                    .clickable(role = Role.RadioButton, onClick = { onUnitSelected(MoneyUnit.RIAL) })
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ريال",
                        style = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 16.sp,
                            fontWeight = if (isRial) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRial) InkTone.Orange.color else Ink
                        ),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                    Text(
                        text = "1 درهم = 20 ريال",
                        style = arabicWritingStyle(color = MutedInk, sizeSp = 12.5f),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                }

                if (isRial) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Check,
                        contentDescription = "محدد",
                        tint = InkTone.Orange.color,
                        size = 18.dp,
                        modifier = Modifier.offset(y = 10.dp)
                    )
                }
            }

            // Dirham Currency Selection Band (58dp = 2 grid units)
            val isDirham = selectedUnit == MoneyUnit.DIRHAM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HisabiMetrics.Grid * 2)
                    .clickable(role = Role.RadioButton, onClick = { onUnitSelected(MoneyUnit.DIRHAM) })
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "درهم",
                        style = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 16.sp,
                            fontWeight = if (isDirham) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDirham) InkTone.Orange.color else Ink
                        ),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                    Text(
                        text = "1 درهم = 100 سنتيم",
                        style = arabicWritingStyle(color = MutedInk, sizeSp = 12.5f),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                }

                if (isDirham) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Check,
                        contentDescription = "محدد",
                        tint = InkTone.Orange.color,
                        size = 18.dp,
                        modifier = Modifier.offset(y = 10.dp)
                    )
                }
            }

            // Open Calculator Action Band (58dp = 2 grid units)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HisabiMetrics.Grid * 2)
                    .clickable(role = Role.Button, onClick = onOpenCalculator)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "فتح دفتر الحساب",
                        style = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkTone.Orange.color
                        ),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                    Text(
                        text = "تدوين وحساب بالـ (${selectedUnit.arabicName})",
                        style = arabicWritingStyle(color = MutedInk, sizeSp = 13f),
                        modifier = Modifier.baselineOnPaperRule()
                    )
                }

                HisabiSketchIcon(
                    symbol = HisabiSymbol.Back,
                    contentDescription = null,
                    tint = InkTone.Orange.color,
                    size = 18.dp,
                    modifier = Modifier
                        .scale(scaleX = -1f, scaleY = 1f)
                        .offset(y = 10.dp)
                )
            }

            // Empty spacer rule
            Spacer(modifier = Modifier.height(HisabiMetrics.Grid))

            // Footer Quote Band
            Text(
                text = "« دفتر حساباتك بالريال والدرهم »",
                style = arabicWritingStyle(color = MutedInk.copy(alpha = 0.72f), sizeSp = 13f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .baselineOnPaperRule()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HisabiBreakdownSheet(
    totalCentimes: Long,
    onDismiss: () -> Unit
) {
    val pieces = remember(totalCentimes) { MoneyMath.breakdown(totalCentimes) }
    val remainderCentimes = remember(totalCentimes, pieces) {
        val accounted = pieces.sumOf { it.denomination.valueCentimes * it.count }
        (totalCentimes - accounted).coerceAtLeast(0)
    }

    val dirhamFormatted = MoneyMath.fromCentimes(totalCentimes, MoneyUnit.DIRHAM)
    val rialFormatted = MoneyMath.fromCentimes(totalCentimes, MoneyUnit.RIAL)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.5.dp)
                        .background(Rule.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "قيمة المجموع",
                    style = arabicWritingStyle(color = Ink, sizeSp = 18.5f, weight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }

            // Ruled Summary Conversion Band (58dp)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(PaperWarm, RoundedCornerShape(6.dp))
                        .border(BorderStroke(0.65.dp, Rule.copy(alpha = 0.72f)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$dirhamFormatted درهم",
                        style = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkTone.Orange.color
                        )
                    )
                    Text(
                        text = "  =  ",
                        style = arabicWritingStyle(color = MutedInk, sizeSp = 15f)
                    )
                    Text(
                        text = "$rialFormatted ريال",
                        style = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkTone.Orange.color
                        )
                    )
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Rule.copy(alpha = 0.54f), thickness = 0.55.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "طريقة تقسيم الفلوس",
                    style = arabicWritingStyle(color = Ink, sizeSp = 14.5f, weight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
            }

            // Ruled Ledger Rows for Denominations (58dp each)
            pieces.forEach { piece ->
                item(key = piece.denomination.label) {
                    BreakdownItemRow(piece = piece)
                }
            }

            if (remainderCentimes > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .drawBehind {
                                drawLine(
                                    color = Rule.copy(alpha = 0.54f),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 0.55.dp.toPx()
                                )
                            }
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الباقي (أقل من 10 سنتيم):",
                            style = arabicWritingStyle(color = Ink, sizeSp = 13.5f)
                        )
                        Text(
                            text = "$remainderCentimes سنتيم",
                            style = TextStyle(
                                fontFamily = ManropeFamily,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkTone.Orange.color
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownItemRow(piece: MoneyPiece) {
    val bitmap = rememberBanknoteImage(piece.denomination.assetPath)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .drawBehind {
                drawLine(
                    color = Rule.copy(alpha = 0.54f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.55.dp.toPx()
                )
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${piece.count}",
                style = TextStyle(
                    fontFamily = ManropeFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkTone.Orange.color
                )
            )
            Text(
                text = "×",
                style = arabicWritingStyle(color = MutedInk, sizeSp = 14f)
            )
            Text(
                text = piece.denomination.label,
                style = arabicWritingStyle(color = Ink, sizeSp = 15f, weight = FontWeight.Medium)
            )
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = piece.denomination.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(42.dp)
                    .width(68.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(BorderStroke(0.6.dp, Rule.copy(alpha = 0.7f)), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun HisabiResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(8.dp),
            color = Paper,
            border = BorderStroke(0.65.dp, Rule.copy(alpha = 0.85f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الرجوع إلى الرئيسية",
                    style = TextStyle(
                        fontFamily = TajawalFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                )

                Text(
                    text = "واش باغي ترجع للصفحة الرئيسية؟ الحساب الحالي غادي يتمسح.",
                    style = arabicWritingStyle(color = WritingInk, sizeSp = 14.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "متابعة الحساب",
                            style = arabicWritingStyle(color = MutedInk, sizeSp = 14f),
                            maxLines = 1
                        )
                    }

                    TextButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = onConfirm
                    ) {
                        Text(
                            text = "نعم، مسح والرجوع",
                            style = arabicWritingStyle(color = InkTone.Coral.color, sizeSp = 14f, weight = FontWeight.Medium),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


