package com.cash.guide.domain.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cash.guide.MainActivity
import com.cash.guide.R
import com.cash.guide.data.db.HssabiDatabase
import com.cash.guide.data.db.ReminderRecurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeneralReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "general_reminders_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Rappels & alertes"
                val descriptionText = "Notifications pour les rappels programmés du carnet"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableLights(true)
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(GeneralReminderScheduler.EXTRA_REMINDER_ID) ?: return

        // 1. Action: "Marquer comme fait"
        if (intent.action == GeneralReminderScheduler.ACTION_COMPLETE_REMINDER) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HssabiDatabase.getInstance(context)
                    val now = System.currentTimeMillis()
                    db.reminderDao().updateCompleted(reminderId, true, now)
                    GeneralReminderScheduler.cancelReminder(context, reminderId)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // 2. Alarm Trigger: Post Notification
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HssabiDatabase.getInstance(context)
                val reminder = db.reminderDao().getReminderById(reminderId)

                if (reminder != null && reminder.isEnabled && !reminder.isCompleted) {
                    createNotificationChannel(context)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return@launch
                        }
                    }

                    // Intent to open MainActivity
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "reminders")
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        reminderId.hashCode(),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Action: Marquer comme fait
                    val completeIntent = Intent(context, GeneralReminderReceiver::class.java).apply {
                        action = GeneralReminderScheduler.ACTION_COMPLETE_REMINDER
                        putExtra(GeneralReminderScheduler.EXTRA_REMINDER_ID, reminderId)
                    }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminderId.hashCode() + 1,
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val recurrenceLabel = when (reminder.recurrenceType) {
                        ReminderRecurrence.DAILY.name -> "Quotidien"
                        ReminderRecurrence.WEEKLY.name -> "Hebdomadaire"
                        ReminderRecurrence.MONTHLY.name -> "Mensuel"
                        ReminderRecurrence.EVERY_3_MONTHS.name -> "Tous les 3 mois"
                        ReminderRecurrence.EVERY_6_MONTHS.name -> "Tous les 6 mois"
                        ReminderRecurrence.YEARLY.name -> "Annuel"
                        else -> "Rappel du carnet"
                    }

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_sarf_notification)
                        .setContentTitle(reminder.title)
                        .setContentText(if (reminder.description.isNotBlank()) reminder.description else recurrenceLabel)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(openPendingIntent)
                        .addAction(0, "✓ Marquer comme fait", completePendingIntent)
                        .build()

                    NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)

                    // Reschedule if recurring
                    val now = System.currentTimeMillis()
                    if (reminder.recurrenceType != ReminderRecurrence.ONCE.name) {
                        val nextOccurrence = GeneralReminderScheduler.calculateNextOccurrence(
                            currentTime = now,
                            targetEpochMs = reminder.targetEpochMs,
                            recurrenceType = reminder.recurrenceType,
                            repeatDays = reminder.repeatDays,
                            timeHour = reminder.timeHour,
                            timeMinute = reminder.timeMinute
                        )
                        val updated = reminder.copy(targetEpochMs = nextOccurrence, updatedAtEpochMs = now)
                        db.reminderDao().updateNextOccurrence(reminderId, nextOccurrence, now)
                        GeneralReminderScheduler.scheduleReminder(context, updated)
                    } else {
                        // Mark once reminder as completed after notification is posted
                        db.reminderDao().updateCompleted(reminderId, true, now)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
