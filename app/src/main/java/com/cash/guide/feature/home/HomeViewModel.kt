package com.cash.guide.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.R
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.ReminderRepository
import com.cash.guide.data.SettingsRepository
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.data.db.NoteEntity
import com.cash.guide.data.db.ReminderEntity
import com.cash.guide.data.db.ReminderRecurrence
import com.cash.guide.domain.ActivityDateGroupHelper
import com.cash.guide.domain.DateGroupHelper
import com.cash.guide.domain.RecentActivityItem
import com.cash.guide.domain.reminder.CreditReminderScheduler
import com.cash.guide.domain.reminder.GeneralReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    val repository: CalculationRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val checklistRepository: ChecklistRepository? = null,
    private val noteRepository: NoteRepository? = null,
    private val reminderRepository: ReminderRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allCalculations: List<CalculationWithItems> = emptyList()
    private var allChecklists: List<ChecklistWithItems> = emptyList()
    private var allNotes: List<NoteEntity> = emptyList()
    private var allReminders: List<ReminderEntity> = emptyList()
    private var lastContext: Context? = null

    init {
        if (settingsRepository != null) {
            viewModelScope.launch {
                settingsRepository.pinnedCalculationIds.collect { pinned ->
                    _uiState.update { it.copy(pinnedCalculationIds = pinned) }
                    lastContext?.let { applyFilters(it) }
                }
            }
            viewModelScope.launch {
                settingsRepository.userName.collect { name ->
                    _uiState.update { it.copy(userName = name) }
                }
            }
        }
    }

    fun togglePin(calculationId: String) {
        viewModelScope.launch {
            settingsRepository?.togglePinCalculation(calculationId)
        }
    }

    fun loadRecent(context: Context) {
        lastContext = context
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val calcsFlow = repository.observeAllSaved()
            val checklistsFlow = checklistRepository?.observeAll() ?: flowOf(emptyList())
            val notesFlow = noteRepository?.observeAll() ?: flowOf(emptyList())
            val remindersFlow = reminderRepository?.allReminders ?: flowOf(emptyList())
            val groupsFlow = repository.observeAllGroupsWithCalculations()

            combine(calcsFlow, checklistsFlow, notesFlow, remindersFlow, groupsFlow) { calcs, checklists, notes, reminders, groups ->
                HomeSourceData(calcs, checklists, notes, reminders, groups)
            }.collect { source ->
                val (calcs, checklists, notes, reminders, groups) = source
                allCalculations = calcs
                allChecklists = checklists
                allNotes = notes
                allReminders = reminders
                _uiState.update {
                    it.copy(
                        favoriteGroups = groups
                            .sortedByDescending { group -> group.group.updatedAtEpochMs }
                            .take(2)
                    )
                }
                applyFilters(context)
            }
        }
    }

    fun setFabExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isFabExpanded = expanded) }
    }

    fun toggleFabExpanded() {
        _uiState.update { it.copy(isFabExpanded = !it.isFabExpanded) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        lastContext?.let { applyFilters(it) }
    }

    fun filterByDate(epochMs: Long?) {
        _uiState.update { it.copy(selectedDateEpoch = epochMs) }
        lastContext?.let { applyFilters(it) }
    }

    fun clearDateFilter() {
        filterByDate(null)
    }

    fun setPaymentFilter(filter: PaymentFilter) {
        _uiState.update { it.copy(selectedPaymentFilter = filter) }
        lastContext?.let { applyFilters(it) }
    }

    fun togglePaymentStatus(calculationId: String, currentStatus: String) {
        val nextStatus = if (currentStatus == "PAID") "UNPAID" else "PAID"
        viewModelScope.launch {
            repository.updatePaymentStatus(calculationId, nextStatus)
            if (nextStatus == "PAID") {
                lastContext?.let { CreditReminderScheduler.cancelReminder(it, calculationId) }
            }
        }
    }

    fun toggleCalcType(calculationId: String, currentType: String) {
        val nextType = if (currentType == "CREDIT") "PERSONNEL" else "CREDIT"
        val nextStatus = if (nextType == "CREDIT") "UNPAID" else "PAID"
        viewModelScope.launch {
            repository.updateCalcType(calculationId, nextType)
            repository.updatePaymentStatus(calculationId, nextStatus)
            if (nextType == "PERSONNEL") {
                lastContext?.let { CreditReminderScheduler.cancelReminder(it, calculationId) }
            }
        }
    }

    fun updateCreditDueDate(
        context: Context,
        calculation: CalculationWithItems,
        dueDateEpochMs: Long?,
        reminderEnabled: Boolean,
        reminderTimeEpochMs: Long?
    ) {
        viewModelScope.launch {
            repository.updateCreditDueDate(
                id = calculation.calculation.id,
                dueDateEpochMs = dueDateEpochMs,
                reminderEnabled = reminderEnabled,
                reminderTimeEpochMs = reminderTimeEpochMs
            )
            if (reminderEnabled && reminderTimeEpochMs != null && reminderTimeEpochMs > System.currentTimeMillis()) {
                val totalCentimes = calculation.totalCentimes
                val totalFormatted = String.format(java.util.Locale.US, "%.2f %s", totalCentimes / 100.0, calculation.calculation.currency)
                CreditReminderScheduler.scheduleReminder(
                    context = context,
                    calculationId = calculation.calculation.id,
                    title = calculation.calculation.title,
                    amountFormatted = totalFormatted,
                    reminderTimeEpochMs = reminderTimeEpochMs
                )
            } else {
                CreditReminderScheduler.cancelReminder(context, calculation.calculation.id)
            }
        }
    }

    private fun applyFilters(context: Context) {
        val query = _uiState.value.searchQuery.trim()
        val selectedDate = _uiState.value.selectedDateEpoch
        val paymentFilter = _uiState.value.selectedPaymentFilter
        val pinnedIds = _uiState.value.pinnedCalculationIds
        val favorites = allCalculations.filter { it.calculation.id in pinnedIds }

        val unpaidTotal = allCalculations
            .filter { (it.calculation.calcType == "CREDIT" || it.calculation.paymentStatus == "UNPAID") && it.calculation.paymentStatus == "UNPAID" }
            .sumOf { it.totalCentimes }

        val now = LocalDate.now(ZoneId.systemDefault())
        val monthTotal = allCalculations.filter {
            val date = Instant.ofEpochMilli(it.calculation.updatedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
            date.year == now.year && date.monthValue == now.monthValue
        }.sumOf { it.totalCentimes }

        // Filter Calculations
        val filteredCalcs = allCalculations.filter { calc ->
            val matchesQuery = if (query.isBlank()) true else {
                calc.calculation.title.contains(query, ignoreCase = true) ||
                calc.items.any { it.label.contains(query, ignoreCase = true) }
            }
            val matchesDate = if (selectedDate == null) true else {
                isSameDay(calc.calculation.updatedAtEpochMs, selectedDate)
            }
            val matchesPayment = when (paymentFilter) {
                PaymentFilter.ALL -> true
                PaymentFilter.UNPAID -> calc.calculation.calcType == "CREDIT" || calc.calculation.paymentStatus == "UNPAID"
                PaymentFilter.PAID -> calc.calculation.paymentStatus == "PAID"
            }
            matchesQuery && matchesDate && matchesPayment
        }

        // Filter Checklists (only show when PaymentFilter == ALL)
        val filteredChecklists = if (paymentFilter == PaymentFilter.ALL) {
            allChecklists.filter { chk ->
                val matchesQuery = if (query.isBlank()) true else {
                    chk.checklist.title.contains(query, ignoreCase = true) ||
                    chk.items.any { it.text.contains(query, ignoreCase = true) }
                }
                val matchesDate = if (selectedDate == null) true else {
                    isSameDay(chk.checklist.updatedAtEpochMs, selectedDate)
                }
                matchesQuery && matchesDate
            }
        } else emptyList()

        // Filter Notes (only show when PaymentFilter == ALL)
        val filteredNotes = if (paymentFilter == PaymentFilter.ALL) {
            allNotes.filter { n ->
                val matchesQuery = if (query.isBlank()) true else {
                    n.title.contains(query, ignoreCase = true) ||
                    n.content.contains(query, ignoreCase = true)
                }
                val matchesDate = if (selectedDate == null) true else {
                    isSameDay(n.updatedAtEpochMs, selectedDate)
                }
                matchesQuery && matchesDate
            }
        } else emptyList()

        // 1. Calculations only groups (legacy compatibility)
        val recentCalcGroups = DateGroupHelper.groupByDate(
            items = allCalculations,
            todayString = context.getString(R.string.date_today),
            yesterdayString = context.getString(R.string.date_yesterday),
            thisWeekString = context.getString(R.string.date_this_week),
            locale = context.resources.configuration.locales[0]
        )

        val filteredCalcGroups = DateGroupHelper.groupByDate(
            items = filteredCalcs,
            todayString = context.getString(R.string.date_today),
            yesterdayString = context.getString(R.string.date_yesterday),
            thisWeekString = context.getString(R.string.date_this_week),
            locale = context.resources.configuration.locales[0]
        )

        // 2. Unified Activity Groups (Calculations + Checklists + Notes)
        val allActivityList = mutableListOf<RecentActivityItem>()
        allCalculations.forEach { allActivityList.add(RecentActivityItem.CalculationActivity(it)) }
        allChecklists.forEach { allActivityList.add(RecentActivityItem.ChecklistActivity(it)) }
        allNotes.forEach { allActivityList.add(RecentActivityItem.NoteActivity(it)) }

        val filteredActivityList = mutableListOf<RecentActivityItem>()
        filteredCalcs.forEach { filteredActivityList.add(RecentActivityItem.CalculationActivity(it)) }
        filteredChecklists.forEach { filteredActivityList.add(RecentActivityItem.ChecklistActivity(it)) }
        filteredNotes.forEach { filteredActivityList.add(RecentActivityItem.NoteActivity(it)) }

        val recentActivityGroups = ActivityDateGroupHelper.groupByDate(
            items = allActivityList,
            todayString = context.getString(R.string.date_today),
            yesterdayString = context.getString(R.string.date_yesterday),
            thisWeekString = context.getString(R.string.date_this_week),
            locale = context.resources.configuration.locales[0]
        )

        val filteredActivityGroups = ActivityDateGroupHelper.groupByDate(
            items = filteredActivityList,
            todayString = context.getString(R.string.date_today),
            yesterdayString = context.getString(R.string.date_yesterday),
            thisWeekString = context.getString(R.string.date_this_week),
            locale = context.resources.configuration.locales[0]
        )

        // 2-2-2 stream for Activité récente (up to 2 calcs, 2 checklists, 2 notes = 6 max)
        val top2Calcs: List<RecentActivityItem> = filteredCalcs
            .sortedByDescending { it.calculation.updatedAtEpochMs }
            .take(2)
            .map { RecentActivityItem.CalculationActivity(it) }

        val top2Checklists: List<RecentActivityItem> = filteredChecklists
            .sortedWith(
                compareByDescending<com.cash.guide.data.db.ChecklistWithItems> {
                    it.items.isNotEmpty() && it.items.any { item -> !item.isChecked }
                }.thenByDescending { it.checklist.updatedAtEpochMs }
            )
            .take(2)
            .map { RecentActivityItem.ChecklistActivity(it) }

        val top2Notes: List<RecentActivityItem> = filteredNotes
            .sortedByDescending { it.updatedAtEpochMs }
            .take(2)
            .map { RecentActivityItem.NoteActivity(it) }

        val top6RecentActivity: List<RecentActivityItem> = (top2Calcs + top2Checklists + top2Notes)
            .sortedByDescending { it.updatedAtEpochMs }

        // Items for Aujourd'hui not already in the balanced recent activity stream
        val top6Ids = top6RecentActivity.map { it.id }.toSet()
        val nowMs = System.currentTimeMillis()

        val todayRemainingCalcs: List<RecentActivityItem> = filteredCalcs
            .filter { it.calculation.id !in top6Ids && isSameDay(it.calculation.updatedAtEpochMs, nowMs) }
            .sortedByDescending { it.calculation.updatedAtEpochMs }
            .take(2)
            .map { RecentActivityItem.CalculationActivity(it) }

        val todayRemainingChecklists: List<RecentActivityItem> = filteredChecklists
            .filter { it.checklist.id !in top6Ids && isSameDay(it.checklist.updatedAtEpochMs, nowMs) }
            .sortedWith(
                compareByDescending<com.cash.guide.data.db.ChecklistWithItems> {
                    it.items.isNotEmpty() && it.items.any { item -> !item.isChecked }
                }.thenByDescending { it.checklist.updatedAtEpochMs }
            )
            .take(2)
            .map { RecentActivityItem.ChecklistActivity(it) }

        val todayRemainingNotes: List<RecentActivityItem> = filteredNotes
            .filter { it.id !in top6Ids && isSameDay(it.updatedAtEpochMs, nowMs) }
            .sortedByDescending { it.updatedAtEpochMs }
            .take(2)
            .map { RecentActivityItem.NoteActivity(it) }

        val todayRemainingItems: List<RecentActivityItem> = (todayRemainingCalcs + todayRemainingChecklists + todayRemainingNotes)
            .sortedByDescending { it.updatedAtEpochMs }

        // Home is a "page of the day", not a second archive. One item is offered
        // as the continuation point and the following four become today's concise list.
        val homeFlow = (top6RecentActivity + todayRemainingItems)
            .distinctBy { "${it::class.simpleName}:${it.id}" }
            .sortedByDescending { it.updatedAtEpochMs }
        val continueItems = homeFlow.take(1)
        val todayActivityItems = homeFlow.drop(1).take(3)

        val reminders = allCalculations.filter {
            it.calculation.reminderEnabled || it.calculation.dueDateEpochMs != null ||
            (it.calculation.calcType == "CREDIT" && it.calculation.paymentStatus == "UNPAID")
        }.sortedWith(
            compareBy<CalculationWithItems> { it.calculation.dueDateEpochMs ?: Long.MAX_VALUE }
                .thenByDescending { it.calculation.updatedAtEpochMs }
        )

        val nowCal = java.util.Calendar.getInstance()
        val currentWeek = nowCal.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentYear = nowCal.get(java.util.Calendar.YEAR)

        // 1. Calculation reminders for the week
        val weekCalcItems = allCalculations.filter { calc ->
            val due = calc.calculation.dueDateEpochMs
            val isUnpaid = (calc.calculation.calcType == "CREDIT" || calc.calculation.paymentStatus == "UNPAID") && calc.calculation.paymentStatus != "PAID"
            val hasReminder = calc.calculation.reminderEnabled
            if (due != null) {
                val dueCal = java.util.Calendar.getInstance().apply { timeInMillis = due }
                (dueCal.get(java.util.Calendar.YEAR) == currentYear && dueCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek) ||
                (due <= nowMs && isUnpaid)
            } else {
                hasReminder || isUnpaid
            }
        }.map { HomeWeekReminderItem.Calculation(it) }

        // 2. General reminders (from Reminders activity) for the week
        val weekGeneralItems = allReminders.filter { reminder ->
            if (!reminder.isEnabled || reminder.isCompleted) return@filter false

            val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = reminder.targetEpochMs }
            val inCurrentWeek = targetCal.get(java.util.Calendar.YEAR) == currentYear &&
                targetCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek
            val isOverdue = reminder.targetEpochMs <= nowMs

            when (reminder.recurrenceType) {
                ReminderRecurrence.ONCE.name -> inCurrentWeek || isOverdue
                ReminderRecurrence.DAILY.name -> true
                ReminderRecurrence.WEEKLY.name -> true
                else -> {
                    val nextEpoch = GeneralReminderScheduler.calculateNextOccurrence(
                        currentTime = nowMs,
                        targetEpochMs = reminder.targetEpochMs,
                        recurrenceType = reminder.recurrenceType,
                        repeatDays = reminder.repeatDays,
                        timeHour = reminder.timeHour,
                        timeMinute = reminder.timeMinute
                    )
                    val nextCal = java.util.Calendar.getInstance().apply { timeInMillis = nextEpoch }
                    (nextCal.get(java.util.Calendar.YEAR) == currentYear && nextCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek) ||
                        inCurrentWeek || isOverdue
                }
            }
        }.map { HomeWeekReminderItem.General(it) }

        val weekItems: List<HomeWeekReminderItem> = (weekGeneralItems + weekCalcItems).sortedWith(
            compareBy<HomeWeekReminderItem> { it.targetEpochMs }
        )

        // Fallback if no items strictly this week: all active reminders
        val allActiveGeneral = allReminders.filter { it.isEnabled && !it.isCompleted }
            .map { HomeWeekReminderItem.General(it) }
        val allActiveCalcs = reminders.map { HomeWeekReminderItem.Calculation(it) }
        val allActiveReminders = (allActiveGeneral + allActiveCalcs).sortedBy { it.targetEpochMs }

        val activeWeekReminders = if (weekItems.isNotEmpty()) weekItems else allActiveReminders

        // Top 5 items for the home sections (Notes, Checklists, Calculations)
        val top5Notes: List<RecentActivityItem> = filteredNotes
            .sortedByDescending { it.updatedAtEpochMs }
            .take(5)
            .map { RecentActivityItem.NoteActivity(it) }

        val top5Checklists: List<RecentActivityItem> = filteredChecklists
            .sortedByDescending { it.checklist.updatedAtEpochMs }
            .take(5)
            .map { RecentActivityItem.ChecklistActivity(it) }

        val top5Calcs: List<RecentActivityItem> = filteredCalcs
            .sortedByDescending { it.calculation.updatedAtEpochMs }
            .take(5)
            .map { RecentActivityItem.CalculationActivity(it) }

        _uiState.update {
            it.copy(
                recentDateGroups = recentCalcGroups,
                filteredDateGroups = filteredCalcGroups,
                recentActivityGroups = recentActivityGroups,
                filteredActivityGroups = filteredActivityGroups,
                recentActivityItems = continueItems,
                todayActivityItems = todayActivityItems,
                favoriteCalculations = favorites,
                reminderCalculations = reminders,
                weekReminders = activeWeekReminders,
                recentNotes = top5Notes,
                recentChecklists = top5Checklists,
                recentCalculations = top5Calcs,
                unpaidTotalCentimes = unpaidTotal,
                monthTotalCentimes = monthTotal,
                isLoading = false
            )
        }
    }

    private fun isSameDay(epoch1: Long, epoch2: Long): Boolean {
        val c1 = java.util.Calendar.getInstance().apply { timeInMillis = epoch1 }
        val c2 = java.util.Calendar.getInstance().apply { timeInMillis = epoch2 }
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR) &&
               c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun selectCalculationForAction(calc: CalculationWithItems?) {
        _uiState.update { it.copy(selectedCalculationForAction = calc) }
    }

    fun selectActivityForAction(activity: RecentActivityItem?) {
        _uiState.update { it.copy(selectedActivityForAction = activity) }
        if (activity is RecentActivityItem.CalculationActivity) {
            _uiState.update { it.copy(selectedCalculationForAction = activity.calculationWithItems) }
        }
    }

    fun promptDeleteActivity(activity: RecentActivityItem) {
        _uiState.update {
            it.copy(
                selectedCalculationForAction = null,
                selectedActivityForAction = null,
                activityToDelete = activity
            )
        }
    }

    fun requestDelete(calc: CalculationWithItems) {
        _uiState.update {
            it.copy(
                selectedCalculationForAction = null,
                selectedActivityForAction = null,
                calculationToDelete = calc,
                activityToDelete = RecentActivityItem.CalculationActivity(calc)
            )
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(
                calculationToDelete = null,
                activityToDelete = null
            )
        }
    }

    fun confirmDelete() {
        confirmDeleteActivity()
    }

    fun confirmDeleteActivity() {
        val activity = _uiState.value.activityToDelete
        val calc = _uiState.value.calculationToDelete
        viewModelScope.launch {
            if (activity != null) {
                when (activity) {
                    is RecentActivityItem.CalculationActivity -> {
                        repository.deleteCalculation(activity.id)
                        lastContext?.let { CreditReminderScheduler.cancelReminder(it, activity.id) }
                    }
                    is RecentActivityItem.ChecklistActivity -> {
                        checklistRepository?.deleteChecklist(activity.id)
                    }
                    is RecentActivityItem.NoteActivity -> {
                        noteRepository?.deleteNote(activity.id)
                    }
                }
            } else if (calc != null) {
                repository.deleteCalculation(calc.calculation.id)
                lastContext?.let { CreditReminderScheduler.cancelReminder(it, calc.calculation.id) }
            }
            _uiState.update {
                it.copy(
                    calculationToDelete = null,
                    activityToDelete = null,
                    selectedActivityForAction = null,
                    selectedCalculationForAction = null
                )
            }
        }
    }

    fun duplicateCalculation(calc: CalculationWithItems, onDuplicated: (String) -> Unit) {
        viewModelScope.launch {
            val duplicated = repository.duplicateCalculation(calc.calculation.id)
            if (duplicated != null) {
                onDuplicated(duplicated.calculation.id)
            }
        }
    }
}

private data class HomeSourceData(
    val calculations: List<CalculationWithItems>,
    val checklists: List<ChecklistWithItems>,
    val notes: List<NoteEntity>,
    val reminders: List<ReminderEntity>,
    val groups: List<com.cash.guide.data.db.CalculationGroupWithCalculations>
)
