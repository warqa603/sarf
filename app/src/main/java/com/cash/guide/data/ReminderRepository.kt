package com.cash.guide.data

import android.content.Context
import com.cash.guide.data.db.ReminderDao
import com.cash.guide.data.db.ReminderEntity
import com.cash.guide.domain.reminder.GeneralReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val context: Context
) {

    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun getReminderById(id: String): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun createReminder(
        title: String,
        description: String = "",
        targetEpochMs: Long,
        recurrenceType: String,
        repeatDays: String = "",
        timeHour: Int = 9,
        timeMinute: Int = 0,
        colorTag: String = "BLUE",
        calculationId: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val reminder = ReminderEntity(
            id = id,
            title = title,
            description = description,
            targetEpochMs = targetEpochMs,
            recurrenceType = recurrenceType,
            repeatDays = repeatDays,
            timeHour = timeHour,
            timeMinute = timeMinute,
            isEnabled = true,
            isCompleted = false,
            colorTag = colorTag,
            calculationId = calculationId,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        reminderDao.insert(reminder)
        GeneralReminderScheduler.scheduleReminder(context, reminder)
        return id
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        val now = System.currentTimeMillis()
        val updated = reminder.copy(updatedAtEpochMs = now)
        reminderDao.update(updated)
        if (updated.isEnabled && !updated.isCompleted) {
            GeneralReminderScheduler.scheduleReminder(context, updated)
        } else {
            GeneralReminderScheduler.cancelReminder(context, updated.id)
        }
    }

    suspend fun toggleEnabled(id: String, isEnabled: Boolean) {
        val now = System.currentTimeMillis()
        reminderDao.updateEnabled(id, isEnabled, now)
        val reminder = reminderDao.getReminderById(id) ?: return
        if (isEnabled && !reminder.isCompleted) {
            GeneralReminderScheduler.scheduleReminder(context, reminder.copy(isEnabled = true))
        } else {
            GeneralReminderScheduler.cancelReminder(context, id)
        }
    }

    suspend fun toggleCompleted(id: String, isCompleted: Boolean) {
        val now = System.currentTimeMillis()
        reminderDao.updateCompleted(id, isCompleted, now)
        val reminder = reminderDao.getReminderById(id) ?: return
        if (!isCompleted && reminder.isEnabled) {
            GeneralReminderScheduler.scheduleReminder(context, reminder.copy(isCompleted = false))
        } else {
            GeneralReminderScheduler.cancelReminder(context, id)
        }
    }

    suspend fun deleteReminder(id: String) {
        GeneralReminderScheduler.cancelReminder(context, id)
        reminderDao.delete(id)
    }

    suspend fun rescheduleAllActive() {
        val active = reminderDao.getAllActiveReminders()
        for (r in active) {
            GeneralReminderScheduler.scheduleReminder(context, r)
        }
    }
}
