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

data class NoteEditorUiState(
    val id: String = "",
    val title: TextFieldValue = TextFieldValue(""),
    val content: TextFieldValue = TextFieldValue(""),
    val colorTag: String = "DEFAULT",
    val isPinned: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isListening: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val activeInputTarget: NoteInputTarget = NoteInputTarget.NONE,
    val groupId: String? = null,
    val keyboardLanguage: JournalKeyboardLanguage = JournalKeyboardLanguage.FRENCH,
    val keyboardExpanded: Boolean = true,
    val shiftMode: JournalShiftMode = JournalShiftMode.OFF,
    val shiftState: JournalShiftState = JournalShiftState()
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
        _uiState.update { it.copy(title = newTitle) }
        saveChanges()
    }

    fun updateContent(newContent: TextFieldValue) {
        _uiState.update { it.copy(content = newContent) }
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
        speechHelper.stopListening()
    }

    private fun appendDictatedText(text: String) {
        val current = _uiState.value.content.text
        val updated = if (current.isBlank()) {
            text.trim()
        } else {
            current.trimEnd() + " " + text.trim()
        }
        _uiState.update {
            it.copy(
                content = TextFieldValue(updated, TextRange(updated.length))
            )
        }
        saveChanges()
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
