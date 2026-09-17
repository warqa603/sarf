package com.cash.guide.domain

import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.data.db.NoteEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ActivityType {
    CALCULATION,
    CHECKLIST,
    NOTE
}

sealed interface RecentActivityItem {
    val id: String
    val title: String
    val updatedAtEpochMs: Long
    val activityType: ActivityType

    data class CalculationActivity(
        val calculationWithItems: CalculationWithItems
    ) : RecentActivityItem {
        override val id: String get() = calculationWithItems.calculation.id
        override val title: String get() = calculationWithItems.calculation.title
        override val updatedAtEpochMs: Long get() = calculationWithItems.calculation.updatedAtEpochMs
        override val activityType: ActivityType get() = ActivityType.CALCULATION
    }

    data class ChecklistActivity(
        val checklistWithItems: ChecklistWithItems
    ) : RecentActivityItem {
        override val id: String get() = checklistWithItems.checklist.id
        override val title: String get() = checklistWithItems.checklist.title
        override val updatedAtEpochMs: Long get() = checklistWithItems.checklist.updatedAtEpochMs
        override val activityType: ActivityType get() = ActivityType.CHECKLIST
    }

    data class NoteActivity(
        val note: NoteEntity
    ) : RecentActivityItem {
        override val id: String get() = note.id
        override val title: String get() = note.title.ifBlank {
            note.content.replace(Regex("==([a-zA-Z]:)?(.*?)== *"), "$2 ")
                .replace(Regex("[#*_~`✓☐•]"), "")
                .trim()
                .take(30)
        }
        override val updatedAtEpochMs: Long get() = note.updatedAtEpochMs
        override val activityType: ActivityType get() = ActivityType.NOTE
    }
}

data class ActivityDateGroup(
    val header: String,
    val items: List<RecentActivityItem>
)

object ActivityDateGroupHelper {

    fun groupByDate(
        items: List<RecentActivityItem>,
        todayString: String,
        yesterdayString: String,
        thisWeekString: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): List<ActivityDateGroup> {
        if (items.isEmpty()) return emptyList()

        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

        val currentYear = today.year
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM", locale)
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)

        val grouped = LinkedHashMap<String, MutableList<RecentActivityItem>>()

        val sortedItems = items.sortedByDescending { it.updatedAtEpochMs }

        sortedItems.forEach { item ->
            val itemDate = Instant.ofEpochMilli(item.updatedAtEpochMs).atZone(zoneId).toLocalDate()
            val header = when {
                itemDate.isEqual(today) -> todayString
                itemDate.isEqual(yesterday) -> yesterdayString
                !itemDate.isBefore(startOfWeek) -> thisWeekString
                itemDate.year == currentYear -> itemDate.format(monthFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
                else -> itemDate.format(monthYearFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
            }
            grouped.getOrPut(header) { mutableListOf() }.add(item)
        }

        return grouped.map { (header, list) ->
            ActivityDateGroup(header = header, items = list)
        }
    }
}
