package com.cash.guide.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.domain.GroupCategory
import com.cash.guide.domain.UnifiedGroupItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<UnifiedGroupItem> = emptyList(),
    val filteredCategory: GroupCategory? = null,
    val isLoading: Boolean = true,
    val isCreateOrEditDialogOpen: Boolean = false,
    val editingGroup: CalculationGroupEntity? = null,
    val groupNameInput: String = "",
    val selectedColorHex: String = "#F4D66D",
    val selectedCategory: GroupCategory = GroupCategory.CALCULATIONS,
    val groupToDelete: CalculationGroupEntity? = null
) {
    val displayedGroups: List<UnifiedGroupItem>
        get() = if (filteredCategory == null) groups else groups.filter { it.category == filteredCategory }

    val calculationsGroupCount: Int
        get() = groups.count { it.category == GroupCategory.CALCULATIONS }

    val notesGroupCount: Int
        get() = groups.count { it.category == GroupCategory.NOTES }

    val checklistsGroupCount: Int
        get() = groups.count { it.category == GroupCategory.CHECKLISTS }

    val contactsGroupCount: Int
        get() = groups.count { it.category == GroupCategory.CONTACTS }
}

class GroupsViewModel(
    private val calculationRepository: CalculationRepository,
    private val noteRepository: NoteRepository,
    private val checklistRepository: ChecklistRepository,
    private val contactRepository: com.cash.guide.data.ContactRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val contactsFlow = contactRepository?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())
            combine(
                calculationRepository.observeAllGroups(),
                calculationRepository.observeAllSaved(),
                noteRepository.observeAll(),
                checklistRepository.observeAll(),
                contactsFlow
            ) { groups, calculations, notes, checklists, contacts ->
                groups.map { group ->
                    val cat = GroupCategory.fromStorage(group.category)
                    when (cat) {
                        GroupCategory.CALCULATIONS -> UnifiedGroupItem(
                            group = group,
                            calculations = calculations.filter { it.calculation.groupId == group.id }
                        )
                        GroupCategory.NOTES -> UnifiedGroupItem(
                            group = group,
                            notes = notes.filter { it.groupId == group.id }
                        )
                        GroupCategory.CHECKLISTS -> UnifiedGroupItem(
                            group = group,
                            checklists = checklists.filter { it.checklist.groupId == group.id }
                        )
                        GroupCategory.CONTACTS -> UnifiedGroupItem(
                            group = group,
                            contacts = contacts.filter { it.groupId == group.id }
                        )
                    }
                }
            }.collect { unifiedList ->
                _uiState.value = _uiState.value.copy(
                    groups = unifiedList,
                    isLoading = false
                )
            }
        }
    }

    fun setFilterCategory(category: GroupCategory?) {
        _uiState.value = _uiState.value.copy(filteredCategory = category)
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(
            isCreateOrEditDialogOpen = true,
            editingGroup = null,
            groupNameInput = "",
            selectedColorHex = "#F4D66D",
            selectedCategory = _uiState.value.filteredCategory ?: GroupCategory.CALCULATIONS
        )
    }

    fun openEditDialog(group: CalculationGroupEntity) {
        _uiState.value = _uiState.value.copy(
            isCreateOrEditDialogOpen = true,
            editingGroup = group,
            groupNameInput = group.name,
            selectedColorHex = group.colorHex,
            selectedCategory = GroupCategory.fromStorage(group.category)
        )
    }

    fun updateGroupNameInput(name: String) {
        _uiState.value = _uiState.value.copy(groupNameInput = name)
    }

    fun selectColorHex(hex: String) {
        _uiState.value = _uiState.value.copy(selectedColorHex = hex)
    }

    fun selectCategory(category: GroupCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun dismissCreateOrEditDialog() {
        _uiState.value = _uiState.value.copy(
            isCreateOrEditDialogOpen = false,
            editingGroup = null,
            groupNameInput = ""
        )
    }

    fun saveGroup() {
        val name = _uiState.value.groupNameInput.trim()
        if (name.isBlank()) return

        val editing = _uiState.value.editingGroup
        val colorHex = _uiState.value.selectedColorHex
        val category = _uiState.value.selectedCategory.storageKey

        viewModelScope.launch {
            if (editing != null) {
                calculationRepository.updateGroup(
                    editing.copy(
                        name = name,
                        colorHex = colorHex,
                        category = category
                    )
                )
            } else {
                calculationRepository.createGroup(name, colorHex, category)
            }
            dismissCreateOrEditDialog()
        }
    }

    fun promptDeleteGroup(group: CalculationGroupEntity) {
        _uiState.value = _uiState.value.copy(groupToDelete = group)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(groupToDelete = null)
    }

    fun confirmDeleteGroup() {
        val group = _uiState.value.groupToDelete ?: return
        viewModelScope.launch {
            calculationRepository.deleteGroup(group.id)
            dismissDeleteDialog()
        }
    }
}
