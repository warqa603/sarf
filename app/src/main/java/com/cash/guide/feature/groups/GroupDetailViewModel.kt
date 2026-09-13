package com.cash.guide.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.data.db.NoteEntity
import com.cash.guide.domain.GroupCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val group: CalculationGroupEntity? = null,
    val calculations: List<CalculationWithItems> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val checklists: List<ChecklistWithItems> = emptyList(),
    val isLoading: Boolean = true,
    // Calculation action states
    val selectedCalculationForAction: CalculationWithItems? = null,
    val calculationToDelete: CalculationWithItems? = null,
    // Note action states
    val selectedNoteForAction: NoteEntity? = null,
    val noteToDelete: NoteEntity? = null,
    // Checklist action states
    val selectedChecklistForAction: ChecklistWithItems? = null,
    val checklistToDelete: ChecklistWithItems? = null,
    val showCreateChecklistDialog: Boolean = false
) {
    val category: GroupCategory
        get() = GroupCategory.fromStorage(group?.category)

    val totalCentimes: Long
        get() = calculations.sumOf { it.totalCentimes }

    val itemCount: Int
        get() = when (category) {
            GroupCategory.CALCULATIONS -> calculations.size
            GroupCategory.NOTES -> notes.size
            GroupCategory.CHECKLISTS -> checklists.size
        }
}

class GroupDetailViewModel(
    val groupId: String,
    private val calculationRepository: CalculationRepository,
    private val noteRepository: NoteRepository,
    private val checklistRepository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                calculationRepository.observeGroup(groupId),
                calculationRepository.observeAllSaved(),
                noteRepository.observeByGroup(groupId),
                checklistRepository.observeByGroup(groupId)
            ) { group, allSaved, groupNotes, groupChecklists ->
                val groupCalculations = allSaved.filter { it.calculation.groupId == groupId }
                _uiState.value = _uiState.value.copy(
                    group = group,
                    calculations = groupCalculations,
                    notes = groupNotes,
                    checklists = groupChecklists,
                    isLoading = false
                )
            }.collect { }
        }
    }

    // Calculations
    fun selectCalculationForAction(calc: CalculationWithItems?) {
        _uiState.value = _uiState.value.copy(selectedCalculationForAction = calc)
    }

    fun promptDeleteCalculation(calc: CalculationWithItems) {
        _uiState.value = _uiState.value.copy(
            selectedCalculationForAction = null,
            calculationToDelete = calc
        )
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            calculationToDelete = null,
            noteToDelete = null,
            checklistToDelete = null
        )
    }

    fun confirmDeleteCalculation() {
        val calc = _uiState.value.calculationToDelete ?: return
        viewModelScope.launch {
            calculationRepository.deleteCalculation(calc.calculation.id)
            dismissDeleteDialog()
        }
    }

    fun removeCalculationFromGroup(calcId: String) {
        viewModelScope.launch {
            calculationRepository.assignCalculationToGroup(calcId, null)
            _uiState.value = _uiState.value.copy(selectedCalculationForAction = null)
        }
    }

    fun duplicateCalculation(calcId: String) {
        viewModelScope.launch {
            calculationRepository.duplicateCalculation(calcId)
            _uiState.value = _uiState.value.copy(selectedCalculationForAction = null)
        }
    }

    // Notes
    fun selectNoteForAction(note: NoteEntity?) {
        _uiState.value = _uiState.value.copy(selectedNoteForAction = note)
    }

    fun promptDeleteNote(note: NoteEntity) {
        _uiState.value = _uiState.value.copy(
            selectedNoteForAction = null,
            noteToDelete = note
        )
    }

    fun confirmDeleteNote() {
        val note = _uiState.value.noteToDelete ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(note.id)
            dismissDeleteDialog()
        }
    }

    fun removeNoteFromGroup(noteId: String) {
        viewModelScope.launch {
            noteRepository.assignNoteToGroup(noteId, null)
            _uiState.value = _uiState.value.copy(selectedNoteForAction = null)
        }
    }

    suspend fun createNoteInGroup(): String {
        return noteRepository.createNote(groupId = groupId)
    }

    // Checklists
    fun selectChecklistForAction(item: ChecklistWithItems?) {
        _uiState.value = _uiState.value.copy(selectedChecklistForAction = item)
    }

    fun promptDeleteChecklist(item: ChecklistWithItems) {
        _uiState.value = _uiState.value.copy(
            selectedChecklistForAction = null,
            checklistToDelete = item
        )
    }

    fun confirmDeleteChecklist() {
        val item = _uiState.value.checklistToDelete ?: return
        viewModelScope.launch {
            checklistRepository.deleteChecklist(item.checklist.id)
            dismissDeleteDialog()
        }
    }

    fun removeChecklistFromGroup(checklistId: String) {
        viewModelScope.launch {
            checklistRepository.assignChecklistToGroup(checklistId, null)
            _uiState.value = _uiState.value.copy(selectedChecklistForAction = null)
        }
    }

    fun openCreateChecklistDialog() {
        _uiState.value = _uiState.value.copy(showCreateChecklistDialog = true)
    }

    fun dismissCreateChecklistDialog() {
        _uiState.value = _uiState.value.copy(showCreateChecklistDialog = false)
    }

    suspend fun createChecklistInGroup(title: String): String {
        dismissCreateChecklistDialog()
        return checklistRepository.createChecklist(title = title, groupId = groupId)
    }
}
