package com.cash.guide.feature.note

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.db.NoteEntity
import com.cash.guide.domain.AndroidIcuGraphemeSegmenter
import com.cash.guide.domain.GraphemeSegmenter
import com.cash.guide.domain.JournalKeyboardController
import com.cash.guide.domain.JournalKeyboardLanguage
import com.cash.guide.domain.JournalShiftMode
import com.cash.guide.domain.JournalShiftState
import com.cash.guide.domain.ShiftAction
import com.cash.guide.domain.speech.SpeechRecognitionState
import com.cash.guide.domain.speech.SpeechRecognizerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NoteInputTarget {
    NONE,
    TITLE,
    CONTENT
}

data class NoteSnapshot(
    val title: TextFieldValue,
    val content: TextFieldValue
)

data class NoteEditorUiState(
    val id: String = "",
    val title: TextFieldValue = TextFieldValue(""),
    val content: TextFieldValue = TextFieldValue(""),
    val colorTag: String = "DEFAULT",
    val isPinned: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isListening: Boolean = false,
    val partialDictation: String = "",
    val dictationError: String? = null,
    val activeHighlighterColor: String = "YELLOW",
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val activeInputTarget: NoteInputTarget = NoteInputTarget.NONE,
    val groupId: String? = null,
    val keyboardLanguage: JournalKeyboardLanguage = JournalKeyboardLanguage.FRENCH,
    val keyboardExpanded: Boolean = true,
    val shiftMode: JournalShiftMode = JournalShiftMode.OFF,
    val shiftState: JournalShiftState = JournalShiftState(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class NoteViewModel(
    private val noteRepository: NoteRepository,
    private val noteId: String?,
    context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val speechHelper = SpeechRecognizerHelper(context)
    private val graphemeSegmenter: GraphemeSegmenter = AndroidIcuGraphemeSegmenter()
    private var lastLatinShiftMode = JournalShiftMode.OFF

    private val undoStack = mutableListOf<NoteSnapshot>()
    private val redoStack = mutableListOf<NoteSnapshot>()
    private var lastSnapshotMs = 0L

    private fun pushUndoSnapshot(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val current = NoteSnapshot(_uiState.value.title, _uiState.value.content)
        if (force || (now - lastSnapshotMs > 750L)) {
            val last = undoStack.lastOrNull()
            if (last == null || last.title.text != current.title.text || last.content.text != current.content.text) {
                if (undoStack.size >= 50) {
                    undoStack.removeAt(0)
                }
                undoStack.add(current)
                lastSnapshotMs = now
                _uiState.update { it.copy(canUndo = true) }
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = NoteSnapshot(_uiState.value.title, _uiState.value.content)
        redoStack.add(current)
        val prev = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update {
            it.copy(
                title = prev.title,
                content = prev.content,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
        saveChanges()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = NoteSnapshot(_uiState.value.title, _uiState.value.content)
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.update {
            it.copy(
                title = next.title,
                content = next.content,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        saveChanges()
    }

    init {
        speechHelper.onSpeechResult = { text ->
            if (text.isNotBlank()) {
                appendDictatedText(text)
            }
        }

        viewModelScope.launch {
            speechHelper.state.collect { st ->
                _uiState.update {
                    it.copy(
                        isListening = (st == SpeechRecognitionState.LISTENING || st == SpeechRecognitionState.PROCESSING)
                    )
                }
            }
        }

        viewModelScope.launch {
            speechHelper.partialText.collect { partial ->
                _uiState.update {
                    it.copy(partialDictation = partial)
                }
            }
        }

        viewModelScope.launch {
            speechHelper.errorMessage.collect { err ->
                _uiState.update {
                    it.copy(dictationError = err)
                }
            }
        }

        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            if (!noteId.isNullOrBlank()) {
                val existing = noteRepository.getNote(noteId)
                if (existing != null) {
                    val initialLang = if (isArabicScript(existing.title) || isArabicScript(existing.content)) {
                        JournalKeyboardLanguage.ARABIC
                    } else {
                        JournalKeyboardLanguage.FRENCH
                    }
                    _uiState.update {
                        it.copy(
                            id = existing.id,
                            title = TextFieldValue(existing.title, TextRange(existing.title.length)),
                            content = TextFieldValue(existing.content, TextRange(existing.content.length)),
                            colorTag = existing.colorTag,
                            isPinned = existing.isPinned,
                            groupId = existing.groupId,
                            createdAtEpochMs = existing.createdAtEpochMs,
                            keyboardLanguage = initialLang,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }

            // If new note
            val newId = noteId ?: java.util.UUID.randomUUID().toString()
            _uiState.update {
                it.copy(
                    id = newId,
                    isLoading = false,
                    activeInputTarget = NoteInputTarget.TITLE
                )
            }
        }
    }

    fun focusTitle() {
        _uiState.update {
            it.copy(
                activeInputTarget = NoteInputTarget.TITLE,
                keyboardExpanded = true
            )
        }
    }

    fun focusContent(cursorPosition: Int? = null) {
        _uiState.update { state ->
            val curText = state.content.text
            val targetPos = (cursorPosition ?: curText.length).coerceIn(0, curText.length)
            state.copy(
                content = state.content.copy(selection = TextRange(targetPos)),
                activeInputTarget = NoteInputTarget.CONTENT,
                keyboardExpanded = true
            )
        }
    }

    fun closeKeyboard() {
        _uiState.update {
            it.copy(activeInputTarget = NoteInputTarget.NONE)
        }
    }

    fun updateTitle(newTitle: TextFieldValue) {
        val oldText = _uiState.value.title.text
        if (oldText != newTitle.text) {
            redoStack.clear()
            pushUndoSnapshot(force = Math.abs(oldText.length - newTitle.text.length) > 1)
        }
        _uiState.update { it.copy(title = newTitle, canRedo = false) }
        saveChanges()
    }

    fun updateContent(newContent: TextFieldValue) {
        val oldState = _uiState.value.content
        val oldText = oldState.text
        var finalContent = newContent

        // Smart List Continuation (Enter key interception)
        if (newContent.text.length > oldText.length 
            && oldState.selection.collapsed
            && newContent.selection.collapsed
            && newContent.selection.start == oldState.selection.start + 1
        ) {
            val newlyTyped = newContent.text.substring(oldState.selection.start, newContent.selection.start)
            if (newlyTyped == "\n") {
                val textBeforeEnter = oldText.substring(0, oldState.selection.start)
                val lineStart = textBeforeEnter.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
                val previousLine = textBeforeEnter.substring(lineStart)
                
                val match = Regex("^([☐✓☑•\\-] |\\d+\\. )(.*)").find(previousLine)
                if (match != null) {
                    val prefix = match.groupValues[1]
                    val content = match.groupValues[2]
                    
                    if (content.isBlank()) {
                        // Double enter on empty list item: exit list mode (remove the empty prefix and the newline)
                        val withoutPrefix = newContent.text.removeRange(lineStart, newContent.selection.start)
                        finalContent = TextFieldValue(withoutPrefix, TextRange(lineStart))
                    } else {
                        // Continue the list
                        var newPrefix = prefix
                        if (prefix.startsWith("✓ ") || prefix.startsWith("☑ ")) newPrefix = "☐ "
                        else if (prefix.matches(Regex("\\d+\\. "))) {
                            val num = prefix.substringBefore('.').toIntOrNull() ?: 0
                            newPrefix = "${num + 1}. "
                        }
                        
                        val newText = newContent.text.substring(0, newContent.selection.start) + newPrefix + newContent.text.substring(newContent.selection.start)
                        val newSelection = newContent.selection.start + newPrefix.length
                        finalContent = TextFieldValue(newText, TextRange(newSelection))
                    }
                }
            }
        }

        if (oldText != finalContent.text) {
            redoStack.clear()
            pushUndoSnapshot(force = Math.abs(oldText.length - finalContent.text.length) > 1)
        }
        _uiState.update { it.copy(content = finalContent, canRedo = false) }
        saveChanges()
    }

    fun setColorTag(colorTag: String) {
        _uiState.update { it.copy(colorTag = colorTag) }
        saveChanges()
    }

    fun togglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.update { it.copy(isPinned = newPinned) }
        saveChanges()
    }

    fun applyTextKey(key: String) {
        val state = _uiState.value
        val now = SystemClock.uptimeMillis()

        var newShiftState = state.shiftState
        var newShiftMode = state.shiftMode
        if (state.keyboardLanguage != JournalKeyboardLanguage.ARABIC) {
            newShiftState = JournalKeyboardController.reduceShift(
                state = state.shiftState,
                action = ShiftAction.UserTypedText(key),
                monotonicNow = { now }
            )
            newShiftMode = newShiftState.mode
        }

        when (state.activeInputTarget) {
            NoteInputTarget.TITLE -> {
                val newVal = JournalKeyboardController.insertText(state.title, key)
                _uiState.update {
                    it.copy(
                        title = newVal,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
                saveChanges()
            }
            NoteInputTarget.CONTENT -> {
                val newVal = JournalKeyboardController.insertText(state.content, key)
                _uiState.update {
                    it.copy(
                        content = newVal,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
                saveChanges()
            }
            NoteInputTarget.NONE -> {
                // If keyboard was closed but key received, focus content
                val newVal = JournalKeyboardController.insertText(state.content, key)
                _uiState.update {
                    it.copy(
                        content = newVal,
                        activeInputTarget = NoteInputTarget.CONTENT,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
                saveChanges()
            }
        }
    }


    fun applyTextBackspace() {
        val state = _uiState.value
        when (state.activeInputTarget) {
            NoteInputTarget.TITLE -> {
                val newVal = JournalKeyboardController.deleteBackward(state.title, graphemeSegmenter)
                _uiState.update { it.copy(title = newVal) }
                saveChanges()
            }
            NoteInputTarget.CONTENT -> {
                val newVal = JournalKeyboardController.deleteBackward(state.content, graphemeSegmenter)
                _uiState.update { it.copy(content = newVal) }
                saveChanges()
            }
            NoteInputTarget.NONE -> {}
        }
    }

    fun confirmInput() {
        val state = _uiState.value
        when (state.activeInputTarget) {
            NoteInputTarget.TITLE -> {
                // Confirming title transitions focus to content
                _uiState.update {
                    it.copy(activeInputTarget = NoteInputTarget.CONTENT)
                }
            }
            NoteInputTarget.CONTENT -> {
                // Confirming in content inserts a newline on ruled paper
                val newVal = JournalKeyboardController.insertText(state.content, "\n")
                _uiState.update { it.copy(content = newVal) }
                saveChanges()
            }
            NoteInputTarget.NONE -> {}
        }
    }

    fun toggleShift() {
        val state = _uiState.value
        val now = SystemClock.uptimeMillis()
        val newShiftState = JournalKeyboardController.reduceShift(
            state = state.shiftState,
            action = ShiftAction.UserTapShift(now),
            monotonicNow = { now }
        )
        lastLatinShiftMode = newShiftState.mode
        _uiState.update {
            it.copy(
                shiftState = newShiftState,
                shiftMode = newShiftState.mode
            )
        }
    }

    fun cycleLanguage() {
        val next = JournalKeyboardController.nextLanguage(_uiState.value.keyboardLanguage)
        selectLanguage(next)
    }

    fun selectLanguage(lang: JournalKeyboardLanguage) {
        val currentLang = _uiState.value.keyboardLanguage
        var newShiftState = _uiState.value.shiftState
        var newShiftMode = _uiState.value.shiftMode

        if (currentLang == JournalKeyboardLanguage.ARABIC && lang != JournalKeyboardLanguage.ARABIC) {
            newShiftState = newShiftState.copy(mode = lastLatinShiftMode)
            newShiftMode = lastLatinShiftMode
        } else if (currentLang != JournalKeyboardLanguage.ARABIC && lang == JournalKeyboardLanguage.ARABIC) {
            lastLatinShiftMode = newShiftMode
            newShiftMode = JournalShiftMode.OFF
        }

        _uiState.update {
            it.copy(
                keyboardLanguage = lang,
                shiftState = newShiftState,
                shiftMode = newShiftMode
            )
        }
    }

    fun toggleKeyboardExpanded() {
        _uiState.update {
            it.copy(keyboardExpanded = !it.keyboardExpanded)
        }
    }

    fun startListening() {
        speechHelper.startListening()
    }

    fun stopListening() {
        commitDictation()
    }

    fun commitDictation() {
        speechHelper.stopAndDeliver()
        _uiState.update { it.copy(partialDictation = "", dictationError = null) }
    }

    fun cancelDictation() {
        speechHelper.stopListening()
        _uiState.update { it.copy(partialDictation = "", dictationError = null) }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            commitDictation()
        } else {
            startListening()
        }
    }

    fun setActiveHighlighterColor(colorTag: String) {
        _uiState.update { it.copy(activeHighlighterColor = colorTag.uppercase()) }
    }

    fun toggleHighlight(colorTag: String = _uiState.value.activeHighlighterColor) {
        val prefix = when (colorTag.uppercase()) {
            "PINK", "P" -> "p:"
            "GREEN", "G" -> "g:"
            "BLUE", "B" -> "b:"
            "ORANGE", "O" -> "o:"
            else -> "" // Default Yellow
        }

        val state = _uiState.value
        val current = state.content
        val text = current.text
        val sel = current.selection

        pushUndoSnapshot(force = true)
        redoStack.clear()

        if (sel.length > 0) {
            val start = sel.min.coerceIn(0, text.length)
            val end = sel.max.coerceIn(0, text.length)
            val selected = text.substring(start, end)

            val isWrapped = selected.startsWith("==") && selected.endsWith("==") && selected.length >= 4
            val newText: String
            val newSelection: TextRange

            if (isWrapped) {
                val inner = selected.substring(2, selected.length - 2)
                val stripped = if (inner.length >= 2 && inner[1] == ':' && inner[0] in listOf('p', 'g', 'b', 'o', 'y')) {
                    inner.substring(2)
                } else {
                    inner
                }
                newText = text.replaceRange(start, end, stripped)
                newSelection = TextRange(start, start + stripped.length)
            } else {
                val wrapped = "==$prefix$selected=="
                newText = text.replaceRange(start, end, wrapped)
                newSelection = TextRange(start, start + wrapped.length)
            }

            _uiState.update {
                it.copy(
                    content = TextFieldValue(newText, newSelection),
                    activeInputTarget = NoteInputTarget.CONTENT,
                    canUndo = true,
                    canRedo = false
                )
            }
        } else {
            val cursor = sel.start.coerceIn(0, text.length)
            val insert = "==$prefix=="
            val newText = text.replaceRange(cursor, cursor, insert)
            val newCursor = cursor + 2 + prefix.length

            _uiState.update {
                it.copy(
                    content = TextFieldValue(newText, TextRange(newCursor)),
                    activeInputTarget = NoteInputTarget.CONTENT,
                    canUndo = true,
                    canRedo = false
                )
            }
        }
        saveChanges()
    }

    private fun appendDictatedText(text: String) {
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val current = _uiState.value.content
        val curText = current.text
        val cursor = current.selection.start.coerceIn(0, curText.length)
        val prefix = if (cursor > 0 && !curText[cursor - 1].isWhitespace()) " " else ""
        val suffix = " "
        val insertion = prefix + text.trim() + suffix
        val updated = curText.replaceRange(cursor, cursor, insertion)
        val newCursor = cursor + insertion.length
        _uiState.update {
            it.copy(
                content = TextFieldValue(updated, TextRange(newCursor)),
                activeInputTarget = NoteInputTarget.CONTENT,
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun paste(clipboardText: String) {
        if (clipboardText.isEmpty()) return
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val state = _uiState.value
        when (state.activeInputTarget) {
            NoteInputTarget.TITLE -> {
                val current = state.title
                val start = current.selection.min.coerceIn(0, current.text.length)
                val end = current.selection.max.coerceIn(0, current.text.length)
                val newText = current.text.replaceRange(start, end, clipboardText)
                val newCursor = start + clipboardText.length
                _uiState.update {
                    it.copy(
                        title = TextFieldValue(newText, TextRange(newCursor)),
                        canUndo = true,
                        canRedo = false
                    )
                }
            }
            else -> {
                val current = state.content
                val start = current.selection.min.coerceIn(0, current.text.length)
                val end = current.selection.max.coerceIn(0, current.text.length)
                val newText = current.text.replaceRange(start, end, clipboardText)
                val newCursor = start + clipboardText.length
                _uiState.update {
                    it.copy(
                        content = TextFieldValue(newText, TextRange(newCursor)),
                        activeInputTarget = NoteInputTarget.CONTENT,
                        canUndo = true,
                        canRedo = false
                    )
                }
            }
        }
        saveChanges()
    }

    fun cut(onCopied: (String) -> Unit) {
        val state = _uiState.value
        when (state.activeInputTarget) {
            NoteInputTarget.TITLE -> {
                val current = state.title
                val start = current.selection.min.coerceIn(0, current.text.length)
                val end = current.selection.max.coerceIn(0, current.text.length)
                if (start != end) {
                    pushUndoSnapshot(force = true)
                    redoStack.clear()
                    val selectedText = current.text.substring(start, end)
                    onCopied(selectedText)
                    val newText = current.text.removeRange(start, end)
                    _uiState.update {
                        it.copy(
                            title = TextFieldValue(newText, TextRange(start)),
                            canUndo = true,
                            canRedo = false
                        )
                    }
                    saveChanges()
                }
            }
            else -> {
                val current = state.content
                val start = current.selection.min.coerceIn(0, current.text.length)
                val end = current.selection.max.coerceIn(0, current.text.length)
                if (start != end) {
                    pushUndoSnapshot(force = true)
                    redoStack.clear()
                    val selectedText = current.text.substring(start, end)
                    onCopied(selectedText)
                    val newText = current.text.removeRange(start, end)
                    _uiState.update {
                        it.copy(
                            content = TextFieldValue(newText, TextRange(start)),
                            canUndo = true,
                            canRedo = false
                        )
                    }
                    saveChanges()
                }
            }
        }
    }

    fun copy(onCopied: (String) -> Unit) {
        val state = _uiState.value
        val target = state.activeInputTarget
        val sel = if (target == NoteInputTarget.TITLE) state.title.selection else state.content.selection
        val text = if (target == NoteInputTarget.TITLE) state.title.text else state.content.text

        if (sel.min != sel.max) {
            val start = sel.min.coerceIn(0, text.length)
            val end = sel.max.coerceIn(0, text.length)
            onCopied(text.substring(start, end))
        } else {
            val full = buildString {
                if (state.title.text.isNotBlank()) {
                    append(state.title.text).append("\n\n")
                }
                append(state.content.text)
            }.trim()
            if (full.isNotBlank()) {
                onCopied(full)
            }
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            if (state.activeInputTarget == NoteInputTarget.TITLE) {
                state.copy(title = state.title.copy(selection = TextRange(0, state.title.text.length)))
            } else {
                state.copy(
                    content = state.content.copy(selection = TextRange(0, state.content.text.length)),
                    activeInputTarget = NoteInputTarget.CONTENT
                )
            }
        }
    }

    fun insertCheckbox() {
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val current = _uiState.value.content
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newLine, cursorShift) = when {
            line.startsWith("☐ ") -> "✓ " + line.removePrefix("☐ ") to 0
            line.startsWith("✓ ") -> line.removePrefix("✓ ") to -2
            line.startsWith("• ") -> "☐ " + line.removePrefix("• ") to 0
            else -> "☐ $line" to 2
        }

        val newText = text.replaceRange(lineStart, lineEnd, newLine)
        val newCursor = (cursor + cursorShift).coerceIn(0, newText.length)
        _uiState.update {
            it.copy(
                content = TextFieldValue(newText, TextRange(newCursor)),
                activeInputTarget = NoteInputTarget.CONTENT,
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun insertBullet() {
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val current = _uiState.value.content
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newLine, cursorShift) = when {
            line.startsWith("• ") -> line.removePrefix("• ") to -2
            line.startsWith("☐ ") -> "• " + line.removePrefix("☐ ") to 0
            line.startsWith("✓ ") -> "• " + line.removePrefix("✓ ") to 0
            else -> "• $line" to 2
        }

        val newText = text.replaceRange(lineStart, lineEnd, newLine)
        val newCursor = (cursor + cursorShift).coerceIn(0, newText.length)
        _uiState.update {
            it.copy(
                content = TextFieldValue(newText, TextRange(newCursor)),
                activeInputTarget = NoteInputTarget.CONTENT,
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun insertNumberedItem() {
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val current = _uiState.value.content
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        var nextNum = 1
        if (lineStart > 1) {
            val prevLineStart = text.lastIndexOf('\n', lineStart - 2).let { if (it == -1) 0 else it + 1 }
            val prevLine = text.substring(prevLineStart, lineStart - 1).trimStart()
            val match = Regex("^(\\d+)\\.").find(prevLine)
            if (match != null) {
                nextNum = (match.groupValues[1].toIntOrNull() ?: 0) + 1
            }
        }

        val prefix = "$nextNum. "
        val (newLine, cursorShift) = if (Regex("^\\d+\\.\\s").containsMatchIn(line)) {
            val stripped = line.replaceFirst(Regex("^\\d+\\.\\s"), "")
            stripped to -(line.length - stripped.length)
        } else {
            prefix + line to prefix.length
        }

        val newText = text.replaceRange(lineStart, lineEnd, newLine)
        val newCursor = (cursor + cursorShift).coerceIn(0, newText.length)
        _uiState.update {
            it.copy(
                content = TextFieldValue(newText, TextRange(newCursor)),
                activeInputTarget = NoteInputTarget.CONTENT,
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun insertDivider() {
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val current = _uiState.value.content
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)
        val divider = "\n────────────────────\n"
        val newText = text.replaceRange(cursor, cursor, divider)
        val newCursor = cursor + divider.length
        _uiState.update {
            it.copy(
                content = TextFieldValue(newText, TextRange(newCursor)),
                activeInputTarget = NoteInputTarget.CONTENT,
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun insertTimestamp() {
        pushUndoSnapshot(force = true)
        redoStack.clear()
        val current = _uiState.value.content
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)
        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val stamp = "[$timeStr] "
        val newText = text.replaceRange(cursor, cursor, stamp)
        val newCursor = cursor + stamp.length
        _uiState.update {
            it.copy(
                content = TextFieldValue(newText, TextRange(newCursor)),
                activeInputTarget = NoteInputTarget.CONTENT,
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun clearContent() {
        if (_uiState.value.content.text.isEmpty()) return
        pushUndoSnapshot(force = true)
        redoStack.clear()
        _uiState.update {
            it.copy(
                content = TextFieldValue("", TextRange.Zero),
                canUndo = true,
                canRedo = false
            )
        }
        saveChanges()
    }

    fun duplicateNote(copySuffix: String = "(نسخة)", onComplete: (String) -> Unit) {
        val s = _uiState.value
        val newId = java.util.UUID.randomUUID().toString()
        val newTitle = if (s.title.text.isNotBlank()) "${s.title.text} $copySuffix" else ""
        val newEntity = NoteEntity(
            id = newId,
            title = newTitle,
            content = s.content.text,
            colorTag = s.colorTag,
            isPinned = false,
            groupId = s.groupId,
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            noteRepository.insertNote(newEntity)
            onComplete(newId)
        }
    }

    fun getStats(): Pair<Int, Int> {
        val text = _uiState.value.content.text
        val charCount = text.length
        val wordCount = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        return Pair(wordCount, charCount)
    }

    fun saveChanges() {
        val current = _uiState.value
        if (current.id.isBlank()) return
        viewModelScope.launch {
            val existing = noteRepository.getNote(current.id)
            if (existing != null) {
                noteRepository.updateNote(
                    id = current.id,
                    title = current.title.text,
                    content = current.content.text,
                    colorTag = current.colorTag,
                    isPinned = current.isPinned
                )
                noteRepository.assignNoteToGroup(current.id, current.groupId)
            } else {
                noteRepository.insertNote(
                    NoteEntity(
                        id = current.id,
                        title = current.title.text.trim(),
                        content = current.content.text.trim(),
                        colorTag = current.colorTag,
                        isPinned = current.isPinned,
                        groupId = current.groupId,
                        createdAtEpochMs = current.createdAtEpochMs,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun assignToGroup(groupId: String?) {
        _uiState.update { it.copy(groupId = groupId) }
        val id = _uiState.value.id
        if (id.isNotBlank()) {
            viewModelScope.launch {
                val existing = noteRepository.getNote(id)
                if (existing != null) {
                    noteRepository.assignNoteToGroup(id, groupId)
                } else {
                    saveChanges()
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val id = _uiState.value.id
        if (id.isNotBlank()) {
            viewModelScope.launch {
                noteRepository.deleteNote(id)
                onDeleted()
            }
        } else {
            onDeleted()
        }
    }

    fun getNoteEntity(): NoteEntity {
        val s = _uiState.value
        return NoteEntity(
            id = s.id,
            title = s.title.text,
            content = s.content.text,
            colorTag = s.colorTag,
            isPinned = s.isPinned,
            groupId = s.groupId,
            createdAtEpochMs = s.createdAtEpochMs,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun isArabicScript(text: String): Boolean {
        return text.any { c ->
            c in '\u0600'..'\u06FF' ||
            c in '\u0750'..'\u077F' ||
            c in '\u08A0'..'\u08FF' ||
            c in '\uFB50'..'\uFDFF' ||
            c in '\uFE70'..'\uFEFF'
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper.destroy()
    }
}
