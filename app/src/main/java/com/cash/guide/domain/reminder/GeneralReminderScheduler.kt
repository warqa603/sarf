package com.cash.guide.domain.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.cash.guide.data.db.ReminderEntity
import com.cash.guide.data.db.ReminderRecurrence
import java.util.Calendar

object GeneralReminderScheduler {

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_RECURRENCE = "extra_recurrence"
    const val ACTION_COMPLETE_REMINDER = "com.cash.guide.ACTION_COMPLETE_REMINDER"

    fun calculateNextOccurrence(
        currentTime: Long,
        targetEpochMs: Long,
        recurrenceType: String,
        repeatDays: String,
        timeHour: Int,
        timeMinute: Int
    ): Long {
        if (recurrenceType == ReminderRecurrence.ONCE.name) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = targetEpochMs
                set(Calendar.HOUR_OF_DAY, timeHour)
                set(Calendar.MINUTE, timeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, timeHour)
            set(Calendar.MINUTE, timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (recurrenceType) {
            ReminderRecurrence.DAILY.name -> {
                if (cal.timeInMillis <= currentTime) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return cal.timeInMillis
            }
            ReminderRecurrence.WEEKLY.name -> {
                val dayList = if (repeatDays.isNotBlank()) {
                    repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                } else {
                    val baseCal = Calendar.getInstance().apply { timeInMillis = targetEpochMs }
                    setOf(baseCal.get(Calendar.DAY_OF_WEEK))
                }

                if (dayList.contains(cal.get(Calendar.DAY_OF_WEEK)) && cal.timeInMillis > currentTime) {
                    return cal.timeInMillis
                }

                for (i in 1..7) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (dayList.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                        return cal.timeInMillis
                    }
                }
                return cal.timeInMillis
            }
            ReminderRecurrence.MONTHLY.name -> {
                val baseCal = Calendar.getInstance().apply { timeInMillis = targetEpochMs }
                val dayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                while (cal.timeInMillis <= currentTime) {
                    cal.add(Calendar.MONTH, 1)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                return cal.timeInMillis
            }
            ReminderRecurrence.EVERY_3_MONTHS.name -> {
                val baseCal = Calendar.getInstance().apply { timeInMillis = targetEpochMs }
                val dayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                while (cal.timeInMillis <= currentTime) {
                    cal.add(Calendar.MONTH, 3)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                return cal.timeInMillis
            }
            ReminderRecurrence.EVERY_6_MONTHS.name -> {
                val baseCal = Calendar.getInstance().apply { timeInMillis = targetEpochMs }
                val dayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                while (cal.timeInMillis <= currentTime) {
                    cal.add(Calendar.MONTH, 6)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                return cal.timeInMillis
            }
            ReminderRecurrence.YEARLY.name -> {
                val baseCal = Calendar.getInstance().apply { timeInMillis = targetEpochMs }
                val dayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)
                val month = baseCal.get(Calendar.MONTH)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                while (cal.timeInMillis <= currentTime) {
                    cal.add(Calendar.YEAR, 1)
                }
                return cal.timeInMillis
            }
            else -> return targetEpochMs
        }
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        if (!reminder.isEnabled || reminder.isCompleted) return

        val now = System.currentTimeMillis()
        val triggerTime = calculateNextOccurrence(
            currentTime = now,
            targetEpochMs = reminder.targetEpochMs,
            recurrenceType = reminder.recurrenceType,
            repeatDays = reminder.repeatDays,
            timeHour = reminder.timeHour,
            timeMinute = reminder.timeMinute
        )

        if (triggerTime <= now && reminder.recurrenceType == ReminderRecurrence.ONCE.name) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GeneralReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_RECURRENCE, reminder.recurrenceType)
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GeneralReminderReceiver::class.java)
        val requestCode = reminderId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        NotificationManagerCompat.from(context).cancel(requestCode)
    }
}
