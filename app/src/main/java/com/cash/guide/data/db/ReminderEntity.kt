package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderRecurrence {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY,
    EVERY_3_MONTHS,
    EVERY_6_MONTHS,
    YEARLY
}

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["targetEpochMs"]),
        Index(value = ["isEnabled"]),
        Index(value = ["isCompleted"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val targetEpochMs: Long,
    val recurrenceType: String = ReminderRecurrence.ONCE.name,
    val repeatDays: String = "", // e.g. "2,3,4,5,6" for Mon-Fri
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val colorTag: String = "BLUE",
    val calculationId: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
