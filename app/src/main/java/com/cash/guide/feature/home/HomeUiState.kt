package com.cash.guide.feature.home

import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.CalculationGroupWithCalculations
import com.cash.guide.data.db.ReminderEntity
import com.cash.guide.domain.ActivityDateGroup
import com.cash.guide.domain.CalculationDateGroup
import com.cash.guide.domain.RecentActivityItem

enum class PaymentFilter {
    ALL,
    UNPAID,
    PAID
}

sealed interface HomeWeekReminderItem {
    val id: String
    val title: String
    val targetEpochMs: Long

    data class General(
        val reminder: ReminderEntity
    ) : HomeWeekReminderItem {
        override val id: String get() = reminder.id
        override val title: String get() = reminder.title
        override val targetEpochMs: Long get() = reminder.targetEpochMs
    }

    data class Calculation(
        val calculationWithItems: CalculationWithItems
    ) : HomeWeekReminderItem {
        override val id: String get() = calculationWithItems.calculation.id
        override val title: String get() = calculationWithItems.calculation.title
        override val targetEpochMs: Long
            get() = calculationWithItems.calculation.dueDateEpochMs
                ?: calculationWithItems.calculation.reminderTimeEpochMs
                ?: calculationWithItems.calculation.updatedAtEpochMs
    }
}

data class HomeUiState(
    val recentDateGroups: List<CalculationDateGroup> = emptyList(),
    val filteredDateGroups: List<CalculationDateGroup> = emptyList(),
    val recentActivityGroups: List<ActivityDateGroup> = emptyList(),
    val filteredActivityGroups: List<ActivityDateGroup> = emptyList(),
    val recentActivityItems: List<RecentActivityItem> = emptyList(),
    val todayActivityItems: List<RecentActivityItem> = emptyList(),
    val favoriteCalculations: List<CalculationWithItems> = emptyList(),
    val reminderCalculations: List<CalculationWithItems> = emptyList(),
    val weekReminders: List<HomeWeekReminderItem> = emptyList(),
    val recentNotes: List<RecentActivityItem> = emptyList(),
    val recentChecklists: List<RecentActivityItem> = emptyList(),
    val recentCalculations: List<RecentActivityItem> = emptyList(),
    val favoriteGroups: List<CalculationGroupWithCalculations> = emptyList(),
    val pinnedCalculationIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedDateEpoch: Long? = null,
    val selectedPaymentFilter: PaymentFilter = PaymentFilter.ALL,
    val unpaidTotalCentimes: Long = 0L,
    val monthTotalCentimes: Long = 0L,
    val isLoading: Boolean = true,
    val selectedCalculationForAction: CalculationWithItems? = null,
    val selectedActivityForAction: RecentActivityItem? = null,
    val calculationToDelete: CalculationWithItems? = null,
    val activityToDelete: RecentActivityItem? = null,
    val isFabExpanded: Boolean = false,
    val userName: String = ""
) {
    val isFiltering: Boolean
        get() = searchQuery.isNotBlank() || selectedDateEpoch != null || selectedPaymentFilter != PaymentFilter.ALL

    val displayDateGroups: List<CalculationDateGroup>
        get() = if (isFiltering) filteredDateGroups else recentDateGroups

    val displayActivityGroups: List<ActivityDateGroup>
        get() = if (isFiltering) filteredActivityGroups else recentActivityGroups

    val isEmpty: Boolean
        get() = !isLoading && recentNotes.isEmpty() && recentChecklists.isEmpty() && recentCalculations.isEmpty() && displayActivityGroups.isEmpty()

    val isActivityEmpty: Boolean
        get() = !isLoading && recentNotes.isEmpty() && recentChecklists.isEmpty() && recentCalculations.isEmpty() && displayActivityGroups.isEmpty()
}
