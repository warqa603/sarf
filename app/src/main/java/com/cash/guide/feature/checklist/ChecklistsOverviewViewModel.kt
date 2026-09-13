package com.cash.guide.feature.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.db.ChecklistWithItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChecklistsOverviewUiState(
    val checklists: List<ChecklistWithItems> = emptyList(),
    val filteredChecklists: List<ChecklistWithItems> = emptyList(),
    val searchQuery: String = "",
    val selectedMonthKey: String? = null,
    val availableMonths: List<Pair<String, String>> = emptyList(), // Pair(key, displayTitle)
    val showCreateDialog: Boolean = false,
    val newChecklistTitle: String = "",
    val checklistToDelete: ChecklistWithItems? = null,
    val isLoading: Boolean = true
)

class ChecklistsOverviewViewModel(
    private val checklistRepository: ChecklistRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedMonthKey = MutableStateFlow<String?>(null)
    val selectedMonthKey: StateFlow<String?> = _selectedMonthKey

    private data class DialogState(
        val showCreateDialog: Boolean = false,
        val newChecklistTitle: String = "",
        val checklistToDelete: ChecklistWithItems? = null
    )

    private val _dialogState = MutableStateFlow(DialogState())

    val checklists: StateFlow<List<ChecklistWithItems>> = checklistRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<ChecklistsOverviewUiState> = combine(
        checklistRepository.observeAll(),
        _searchQuery,
        _selectedMonthKey,
        _dialogState
    ) { allChecklists, query, selectedMonth, dialogState ->
        val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthDisplayFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Compute available distinct months from all existing checklists
        val allMonthKeys = allChecklists.map { item ->
            monthKeyFormatter.format(Date(item.checklist.createdAtEpochMs))
        }.distinct().sortedDescending()

        val availableMonths = allMonthKeys.map { key ->
            val sampleItem = allChecklists.first { monthKeyFormatter.format(Date(it.checklist.createdAtEpochMs)) == key }
            val raw = monthDisplayFormatter.format(Date(sampleItem.checklist.createdAtEpochMs))
            val display = raw.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            Pair(key, display)
        }

        // Apply month filter
        val monthFiltered = if (selectedMonth.isNullOrBlank()) {
            allChecklists
        } else {
            allChecklists.filter { item ->
                monthKeyFormatter.format(Date(item.checklist.createdAtEpochMs)) == selectedMonth
            }
        }

        // Apply search query filter
        val searchFiltered = if (query.isBlank()) {
            monthFiltered
        } else {
            val q = query.trim()
            monthFiltered.filter { item ->
                item.checklist.title.contains(q, ignoreCase = true) ||
                    item.items.any { it.text.contains(q, ignoreCase = true) }
            }
        }

        ChecklistsOverviewUiState(
            checklists = allChecklists,
            filteredChecklists = searchFiltered,
            searchQuery = query,
            selectedMonthKey = selectedMonth,
            availableMonths = availableMonths,
            showCreateDialog = dialogState.showCreateDialog,
            newChecklistTitle = dialogState.newChecklistTitle,
            checklistToDelete = dialogState.checklistToDelete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChecklistsOverviewUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMonth(monthKey: String?) {
        _selectedMonthKey.value = monthKey
    }

    fun openCreateDialog() {
        _dialogState.update { it.copy(showCreateDialog = true, newChecklistTitle = "") }
    }

    fun dismissCreateDialog() {
        _dialogState.update { it.copy(showCreateDialog = false, newChecklistTitle = "") }
    }

    fun setNewChecklistTitle(title: String) {
        _dialogState.update { it.copy(newChecklistTitle = title) }
    }

    fun confirmCreateChecklist(onCreated: (String) -> Unit) {
        val title = _dialogState.value.newChecklistTitle.trim().ifBlank { "Checklist" }
        viewModelScope.launch {
            val newId = checklistRepository.createChecklist(title)
            _dialogState.update { it.copy(showCreateDialog = false, newChecklistTitle = "") }
            onCreated(newId)
        }
    }

    fun promptDeleteChecklist(checklist: ChecklistWithItems) {
        _dialogState.update { it.copy(checklistToDelete = checklist) }
    }

    fun dismissDeleteDialog() {
        _dialogState.update { it.copy(checklistToDelete = null) }
    }

    fun confirmDeleteChecklist() {
        val target = _dialogState.value.checklistToDelete ?: return
        viewModelScope.launch {
            checklistRepository.deleteChecklist(target.checklist.id)
            _dialogState.update { it.copy(checklistToDelete = null) }
        }
    }
}
