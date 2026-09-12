package com.cash.guide.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.ReminderRepository
import com.cash.guide.data.db.ReminderEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReminderFilterTab {
    ALL,
    ACTIVE,
    COMPLETED
}

data class RemindersUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val filteredReminders: List<ReminderEntity> = emptyList(),
    val searchQuery: String = "",
    val filterTab: ReminderFilterTab = ReminderFilterTab.ALL,
    val selectedMonthKey: String? = null,
    val availableMonths: List<Pair<String, String>> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val isCreateSheetOpen: Boolean = false,
    val editingReminder: ReminderEntity? = null,
    val isLoading: Boolean = false
)

class RemindersViewModel(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterTab = MutableStateFlow(ReminderFilterTab.ALL)
    private val _selectedMonthKey = MutableStateFlow<String?>(null)
    private val _isCreateSheetOpen = MutableStateFlow(false)
    private val _editingReminder = MutableStateFlow<ReminderEntity?>(null)

    private val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthDisplayFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    private data class FilterResult(
        val reminders: List<ReminderEntity>,
        val filteredReminders: List<ReminderEntity>,
        val searchQuery: String,
        val filterTab: ReminderFilterTab,
        val selectedMonthKey: String?,
        val availableMonths: List<Pair<String, String>>,
        val activeCount: Int,
        val completedCount: Int
    )

    private val remindersFilterData = combine(
        repository.allReminders,
        _searchQuery,
        _filterTab,
        _selectedMonthKey
    ) { reminders, query, filterTab, selectedMonth ->
        val activeCount = reminders.count { !it.isCompleted }
        val completedCount = reminders.count { it.isCompleted }

        // Compute available months from targetEpochMs
        val monthKeys = reminders.map {
            monthKeyFormatter.format(Date(it.targetEpochMs))
        }.distinct().sortedDescending()

        val availableMonths = monthKeys.map { key ->
            val parts = key.split("-")
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            key to monthDisplayFormatter.format(cal.time).replaceFirstChar { it.uppercase() }
        }

        val filtered = reminders.filter { reminder ->
            val matchesQuery = query.isBlank() ||
                reminder.title.contains(query, ignoreCase = true) ||
                reminder.description.contains(query, ignoreCase = true)

            val matchesTab = when (filterTab) {
                ReminderFilterTab.ALL -> true
                ReminderFilterTab.ACTIVE -> !reminder.isCompleted
                ReminderFilterTab.COMPLETED -> reminder.isCompleted
            }

            val matchesMonth = selectedMonth == null ||
                monthKeyFormatter.format(Date(reminder.targetEpochMs)) == selectedMonth

            matchesQuery && matchesTab && matchesMonth
        }

        FilterResult(
            reminders = reminders,
            filteredReminders = filtered,
            searchQuery = query,
            filterTab = filterTab,
            selectedMonthKey = selectedMonth,
            availableMonths = availableMonths,
            activeCount = activeCount,
            completedCount = completedCount
        )
    }

    val uiState: StateFlow<RemindersUiState> = combine(
        remindersFilterData,
        _isCreateSheetOpen,
        _editingReminder
    ) { filterResult, isCreateOpen, editingRem ->
        RemindersUiState(
            reminders = filterResult.reminders,
            filteredReminders = filterResult.filteredReminders,
            searchQuery = filterResult.searchQuery,
            filterTab = filterResult.filterTab,
            selectedMonthKey = filterResult.selectedMonthKey,
            availableMonths = filterResult.availableMonths,
            activeCount = filterResult.activeCount,
            completedCount = filterResult.completedCount,
            isCreateSheetOpen = isCreateOpen,
            editingReminder = editingRem,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState(isLoading = true)
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterTab(tab: ReminderFilterTab) {
        _filterTab.value = tab
    }

    fun selectMonth(monthKey: String?) {
        _selectedMonthKey.value = monthKey
    }

    fun openCreateDialog() {
        _editingReminder.value = null
        _isCreateSheetOpen.value = true
    }

    fun openEditDialog(reminder: ReminderEntity) {
        _editingReminder.value = reminder
        _isCreateSheetOpen.value = true
    }

    fun closeCreateDialog() {
        _isCreateSheetOpen.value = false
        _editingReminder.value = null
    }

    fun toggleEnabled(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleEnabled(id, isEnabled)
        }
    }

    fun toggleCompleted(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleCompleted(id, isCompleted)
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun createReminder(
        title: String,
        description: String = "",
        targetEpochMs: Long,
        recurrenceType: String,
        repeatDays: String = "",
        timeHour: Int = 9,
        timeMinute: Int = 0,
        colorTag: String = "BLUE"
    ) {
        viewModelScope.launch {
            repository.createReminder(
                title = title,
                description = description,
                targetEpochMs = targetEpochMs,
                recurrenceType = recurrenceType,
                repeatDays = repeatDays,
                timeHour = timeHour,
                timeMinute = timeMinute,
                colorTag = colorTag
            )
            _isCreateSheetOpen.value = false
        }
    }

    fun updateReminder(
        id: String,
        title: String,
        description: String = "",
        targetEpochMs: Long,
        recurrenceType: String,
        repeatDays: String = "",
        timeHour: Int = 9,
        timeMinute: Int = 0,
        colorTag: String = "BLUE"
    ) {
        viewModelScope.launch {
            val existing = _editingReminder.value ?: repository.getReminderById(id)
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    description = description,
                    targetEpochMs = targetEpochMs,
                    recurrenceType = recurrenceType,
                    repeatDays = repeatDays,
                    timeHour = timeHour,
                    timeMinute = timeMinute,
                    colorTag = colorTag
                )
                repository.updateReminder(updated)
            }
            _isCreateSheetOpen.value = false
            _editingReminder.value = null
        }
    }
}
