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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreditReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "credit_reminders_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.notification_channel_credit_reminders)
                val descriptionText = context.getString(R.string.notification_channel_credit_reminders_desc)
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
        val calculationId = intent.getStringExtra(CreditReminderScheduler.EXTRA_CALCULATION_ID) ?: return

        // 1. Handle direct action from notification: "Marquer comme payé"
        if (intent.action == CreditReminderScheduler.ACTION_MARK_PAID) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HssabiDatabase.getInstance(context)
                    val now = System.currentTimeMillis()
                    db.calculationDao().updatePaymentStatus(calculationId, "PAID", now)
                    CreditReminderScheduler.cancelReminder(context, calculationId)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // 2. Handle Alarm Trigger -> Display Notification if credit is still unpaid
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HssabiDatabase.getInstance(context)
                val calc = db.calculationDao().getCalculation(calculationId)

                // Only show notification if the calculation is saved, is a credit, and still UNPAID
                if (calc != null && calc.calculation.calcType == "CREDIT" && calc.calculation.paymentStatus == "UNPAID") {
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

                    val titleText = calc.calculation.title.ifBlank { context.getString(R.string.calc_type_credit) }
                    val totalCentimes = calc.totalCentimes
                    val totalFormatted = String.format(java.util.Locale.US, "%.2f %s", totalCentimes / 100.0, calc.calculation.currency)

                    // Intent to open MainActivity and jump straight to this calculation
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(CreditReminderScheduler.EXTRA_CALCULATION_ID, calculationId)
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        calculationId.hashCode(),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Action intent to mark as paid directly from notification
                    val markPaidIntent = Intent(context, CreditReminderReceiver::class.java).apply {
                        action = CreditReminderScheduler.ACTION_MARK_PAID
                        putExtra(CreditReminderScheduler.EXTRA_CALCULATION_ID, calculationId)
                    }
                    val markPaidPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (calculationId + "_paid").hashCode(),
                        markPaidIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_sarf_notification)
                        .setContentTitle(context.getString(R.string.notification_credit_title, titleText))
                        .setContentText(context.getString(R.string.notification_credit_body, totalFormatted))
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(context.getString(R.string.notification_credit_body, totalFormatted))
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(openPendingIntent)
                        .addAction(
                            0,
                            context.getString(R.string.notification_action_mark_paid),
                            markPaidPendingIntent
                        )
                        .build()

                    NotificationManagerCompat.from(context).notify(calculationId.hashCode(), notification)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
