package com.cash.guide.feature.checklist

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.domain.AndroidIcuGraphemeSegmenter
import com.cash.guide.domain.GraphemeSegmenter
import com.cash.guide.domain.JournalKeyboardController
import com.cash.guide.domain.JournalKeyboardLanguage
import com.cash.guide.domain.JournalShiftMode
import com.cash.guide.domain.JournalShiftState
import com.cash.guide.domain.ShiftAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChecklistInputTarget {
    NONE,
    ITEM_INPUT,
    TITLE
}

data class ChecklistUiState(
    val allChecklists: List<ChecklistWithItems> = emptyList(),
    val currentChecklist: ChecklistWithItems? = null,
    val inputText: TextFieldValue = TextFieldValue(""),
    val isTitleEditing: Boolean = false,
    val titleInput: TextFieldValue = TextFieldValue("Checklist"),
    val activeInputTarget: ChecklistInputTarget = ChecklistInputTarget.NONE,
    val keyboardLanguage: JournalKeyboardLanguage = JournalKeyboardLanguage.FRENCH,
    val shiftState: JournalShiftState = JournalShiftState(),
    val shiftMode: JournalShiftMode = JournalShiftMode.OFF,
    val keyboardExpanded: Boolean = true,
    val isLoading: Boolean = true,
    val showChecklistListDialog: Boolean = false,
    val showCreateNewDialog: Boolean = false,
    val newChecklistTitle: String = ""
)

class ChecklistViewModel(
    private val checklistRepository: ChecklistRepository,
    initialChecklistId: String? = null,
    private val graphemeSegmenter: GraphemeSegmenter = AndroidIcuGraphemeSegmenter()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private var activeId: String? = initialChecklistId

    init {
        viewModelScope.launch {
            checklistRepository.observeAll().collectLatest { all ->
                val target = if (activeId != null) {
                    all.find { it.checklist.id == activeId } ?: all.firstOrNull()
                } else {
                    all.firstOrNull()
                }

                if (target == null && all.isEmpty()) {
                    // Automatically create default initial checklist if none exists
                    val newId = checklistRepository.createChecklist("Checklist")
                    activeId = newId
                } else {
                    activeId = target?.checklist?.id
                    _uiState.update { s ->
                        s.copy(
                            allChecklists = all,
                            currentChecklist = target,
                            titleInput = if (s.activeInputTarget != ChecklistInputTarget.TITLE) {
                                TextFieldValue(target?.checklist?.title ?: "Checklist")
                            } else {
                                s.titleInput
                            },
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun selectChecklist(id: String) {
        activeId = id
        val target = _uiState.value.allChecklists.find { it.checklist.id == id }
        _uiState.update { s ->
            s.copy(
                currentChecklist = target,
                titleInput = TextFieldValue(target?.checklist?.title ?: "Checklist"),
                activeInputTarget = ChecklistInputTarget.NONE,
                isTitleEditing = false,
                showChecklistListDialog = false
            )
        }
    }

    fun setInputText(value: TextFieldValue) {
        if (value.text.length <= 80) {
            _uiState.update { it.copy(inputText = value) }
        }
    }

    fun focusItemInput() {
        _uiState.update {
            it.copy(
                activeInputTarget = ChecklistInputTarget.ITEM_INPUT,
                isTitleEditing = false
            )
        }
    }

    fun focusTitle() {
        val curTitle = _uiState.value.currentChecklist?.checklist?.title ?: "Checklist"
        _uiState.update {
            it.copy(
                activeInputTarget = ChecklistInputTarget.TITLE,
                isTitleEditing = true,
                titleInput = TextFieldValue(curTitle, selection = androidx.compose.ui.text.TextRange(curTitle.length))
            )
        }
    }

    fun updateTitleInput(value: TextFieldValue) {
        if (value.text.length > 40) return
        _uiState.update { it.copy(titleInput = value) }
    }

    fun updateInputText(value: TextFieldValue) {
        if (value.text.length > 80) return
        _uiState.update { it.copy(inputText = value) }
    }

    fun hideKeyboard() {
        if (_uiState.value.activeInputTarget == ChecklistInputTarget.TITLE) {
            saveTitle()
        }
        _uiState.update {
            it.copy(
                activeInputTarget = ChecklistInputTarget.NONE,
                isTitleEditing = false
            )
        }
    }

    fun toggleKeyboardExpanded() {
        _uiState.update { it.copy(keyboardExpanded = !it.keyboardExpanded) }
    }

    fun cycleLanguage() {
        val next = when (_uiState.value.keyboardLanguage) {
            JournalKeyboardLanguage.FRENCH -> JournalKeyboardLanguage.ARABIC
            JournalKeyboardLanguage.ARABIC -> JournalKeyboardLanguage.ENGLISH
            JournalKeyboardLanguage.ENGLISH -> JournalKeyboardLanguage.FRENCH
        }
        _uiState.update { it.copy(keyboardLanguage = next) }
    }

    fun selectLanguage(lang: JournalKeyboardLanguage) {
        _uiState.update { it.copy(keyboardLanguage = lang) }
    }

    fun toggleShift() {
        val now = System.currentTimeMillis()
        val nextState = JournalKeyboardController.reduceShift(
            state = _uiState.value.shiftState,
            action = ShiftAction.UserTapShift(now),
            monotonicNow = { now }
        )
        _uiState.update { it.copy(shiftState = nextState, shiftMode = nextState.mode) }
    }

    fun applyTextKey(key: String) {
        val state = _uiState.value
        val now = System.currentTimeMillis()

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
            ChecklistInputTarget.ITEM_INPUT -> {
                if (state.inputText.text.length >= 80) return
                val newVal = JournalKeyboardController.insertText(state.inputText, key)
                _uiState.update {
                    it.copy(
                        inputText = newVal,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
            }
            ChecklistInputTarget.TITLE -> {
                if (state.titleInput.text.length >= 40) return
                val newVal = JournalKeyboardController.insertText(state.titleInput, key)
                _uiState.update {
                    it.copy(
                        titleInput = newVal,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
            }
            ChecklistInputTarget.NONE -> {
                if (state.inputText.text.length >= 80) return
                val newVal = JournalKeyboardController.insertText(state.inputText, key)
                _uiState.update {
                    it.copy(
                        activeInputTarget = ChecklistInputTarget.ITEM_INPUT,
                        inputText = newVal,
                        shiftState = newShiftState,
                        shiftMode = newShiftMode
                    )
                }
            }
        }
    }

    fun applyTextBackspace() {
        val state = _uiState.value
        when (state.activeInputTarget) {
            ChecklistInputTarget.ITEM_INPUT -> {
                val newVal = JournalKeyboardController.deleteBackward(state.inputText, graphemeSegmenter)
                _uiState.update { it.copy(inputText = newVal) }
            }
            ChecklistInputTarget.TITLE -> {
                val newVal = JournalKeyboardController.deleteBackward(state.titleInput, graphemeSegmenter)
                _uiState.update { it.copy(titleInput = newVal) }
            }
            ChecklistInputTarget.NONE -> {}
        }
    }

    fun confirmInput() {
        when (_uiState.value.activeInputTarget) {
            ChecklistInputTarget.ITEM_INPUT -> {
                addItem()
            }
            ChecklistInputTarget.TITLE -> {
                saveTitle()
                hideKeyboard()
            }
            ChecklistInputTarget.NONE -> {}
        }
    }

    fun addItem() {
        val text = _uiState.value.inputText.text.trim()
        val current = _uiState.value.currentChecklist ?: return
        if (text.isEmpty()) return
        viewModelScope.launch {
            checklistRepository.addItem(current.checklist.id, text)
            _uiState.update { it.copy(inputText = TextFieldValue("")) }
        }
    }

    fun addMultipleItems(items: List<String>) {
        val current = _uiState.value.currentChecklist ?: return
        viewModelScope.launch {
            items.forEach { text ->
                if (text.isNotBlank()) {
                    checklistRepository.addItem(current.checklist.id, text.trim())
                }
            }
        }
    }

    fun toggleItem(itemId: String, isChecked: Boolean) {
        viewModelScope.launch {
            checklistRepository.toggleItem(itemId, isChecked)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            checklistRepository.deleteItem(itemId)
        }
    }

    fun setAllItemsChecked(isChecked: Boolean) {
        val current = _uiState.value.currentChecklist ?: return
        viewModelScope.launch {
            checklistRepository.setAllItemsChecked(current.checklist.id, isChecked)
        }
    }

    fun deleteCompletedItems() {
        val current = _uiState.value.currentChecklist ?: return
        viewModelScope.launch {
            checklistRepository.deleteCompletedItems(current.checklist.id)
        }
    }

    fun saveTitle() {
        val current = _uiState.value.currentChecklist ?: return
        val newTitle = _uiState.value.titleInput.text.trim().ifBlank { "Checklist" }
        viewModelScope.launch {
            checklistRepository.updateTitle(current.checklist.id, newTitle)
            _uiState.update {
                it.copy(
                    isTitleEditing = false,
                    activeInputTarget = if (it.activeInputTarget == ChecklistInputTarget.TITLE) ChecklistInputTarget.NONE else it.activeInputTarget
                )
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(showCreateNewDialog = true, newChecklistTitle = "") }
    }

    fun setNewChecklistTitle(title: String) {
        _uiState.update { it.copy(newChecklistTitle = title) }
    }

    fun confirmCreateChecklist() {
        val title = _uiState.value.newChecklistTitle.trim().ifBlank { "Checklist" }
        viewModelScope.launch {
            val newId = checklistRepository.createChecklist(title)
            activeId = newId
            _uiState.update { it.copy(showCreateNewDialog = false) }
        }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateNewDialog = false) }
    }

    fun showListsDialog() {
        _uiState.update { it.copy(showChecklistListDialog = true) }
    }

    fun dismissListsDialog() {
        _uiState.update { it.copy(showChecklistListDialog = false) }
    }

    fun deleteCurrentChecklist() {
        val current = _uiState.value.currentChecklist ?: return
        viewModelScope.launch {
            checklistRepository.deleteChecklist(current.checklist.id)
            activeId = null
        }
    }

    fun importFromLink(title: String, items: List<Pair<String, Boolean>>) {
        viewModelScope.launch {
            val newId = checklistRepository.importChecklist(title, items)
            activeId = newId
        }
    }

    fun assignToGroup(groupId: String?) {
        val currentId = activeId ?: return
        viewModelScope.launch {
            checklistRepository.assignChecklistToGroup(currentId, groupId)
        }
    }
}
