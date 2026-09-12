package com.cash.guide.domain.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cash.guide.data.db.HssabiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreditBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HssabiDatabase.getInstance(context)
                    val now = System.currentTimeMillis()
                    val pendingReminders = db.calculationDao().getPendingCreditReminders(now)

                    for (calc in pendingReminders) {
                        val reminderTime = calc.calculation.reminderTimeEpochMs ?: continue
                        if (reminderTime > now) {
                            val totalCentimes = calc.totalCentimes
                            val totalFormatted = String.format(java.util.Locale.US, "%.2f %s", totalCentimes / 100.0, calc.calculation.currency)
                            CreditReminderScheduler.scheduleReminder(
                                context = context,
                                calculationId = calc.calculation.id,
                                title = calc.calculation.title,
                                amountFormatted = totalFormatted,
                                reminderTimeEpochMs = reminderTime
                            )
                        }
                    }

                    // Also reschedule active general reminders
                    val activeGeneralReminders = db.reminderDao().getAllActiveReminders()
                    for (reminder in activeGeneralReminders) {
                        GeneralReminderScheduler.scheduleReminder(context, reminder)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
