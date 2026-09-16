package com.taskerflow.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskerflow.app.MainActivity
import com.taskerflow.app.R

object NotificationHelper {

    const val CHANNEL_REMINDER = "task_reminder"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_REMINDER,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Reminders for upcoming tasks" }
            mgr.createNotificationChannel(ch)
        }
    }

    fun showReminder(
        ctx: Context,
        occurrenceId: Long,
        title: String,
        epReward: Int,
        dueAt: Long
    ) {
        ensureChannel(ctx)

        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            ctx, occurrenceId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(ctx, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_COMPLETE
            putExtra(TaskActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        val completePi = PendingIntent.getBroadcast(
            ctx, (occurrenceId + 100000).toInt(), completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(ctx, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_SNOOZE
            putExtra(TaskActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        val snoozePi = PendingIntent.getBroadcast(
            ctx, (occurrenceId + 200000).toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(ctx, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 $title")
            .setContentText("Due now • +$epReward EP")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePi)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePi)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(occurrenceId.toInt(), notif)
        } catch (_: SecurityException) { /* user denied POST_NOTIFICATIONS */ }
    }
}
