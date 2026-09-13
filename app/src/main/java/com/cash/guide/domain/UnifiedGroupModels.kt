package com.cash.guide.domain

import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.data.db.NoteEntity

enum class GroupCategory(val storageKey: String) {
    CALCULATIONS("CALCULATIONS"),
    NOTES("NOTES"),
    CHECKLISTS("CHECKLISTS");

    companion object {
        fun fromStorage(key: String?): GroupCategory {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: CALCULATIONS
        }
    }
}

data class UnifiedGroupItem(
    val group: CalculationGroupEntity,
    val calculations: List<CalculationWithItems> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val checklists: List<ChecklistWithItems> = emptyList()
) {
    val category: GroupCategory
        get() = GroupCategory.fromStorage(group.category)

    val itemCount: Int
        get() = when (category) {
            GroupCategory.CALCULATIONS -> calculations.size
            GroupCategory.NOTES -> notes.size
            GroupCategory.CHECKLISTS -> checklists.size
        }

    val totalCentimes: Long
        get() = when (category) {
            GroupCategory.CALCULATIONS -> calculations.sumOf { it.totalCentimes }
            else -> 0L
        }

    val previewTitles: List<String>
        get() = when (category) {
            GroupCategory.CALCULATIONS -> calculations.map { it.calculation.title.trim() }.filter { it.isNotBlank() }.take(3)
            GroupCategory.NOTES -> notes.map { it.title.trim() }.filter { it.isNotBlank() }.take(3)
            GroupCategory.CHECKLISTS -> checklists.map { it.checklist.title.trim() }.filter { it.isNotBlank() }.take(3)
        }
}
