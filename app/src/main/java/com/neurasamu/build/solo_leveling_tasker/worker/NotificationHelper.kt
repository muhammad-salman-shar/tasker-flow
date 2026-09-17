package com.neurasamu.build.solo_leveling_tasker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neurasamu.build.solo_leveling_tasker.MainActivity

object NotificationHelper {

    const val CHANNEL_REMINDER = "task_reminder"
    const val CHANNEL_ALERT = "tasker_alert"

    private const val ID_FOCUS_LOCK = 99002
    private const val ID_HEALTH_WARNING = 99001
    private const val ID_CRITICAL = 99003

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(NotificationManager::class.java)

            val reminder = NotificationChannel(
                CHANNEL_REMINDER,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Reminders for upcoming tasks" }
            mgr.createNotificationChannel(reminder)

            val alert = NotificationChannel(
                CHANNEL_ALERT,
                "Health & Focus Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Warnings when health is critical" }
            mgr.createNotificationChannel(alert)
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
        val openPi = openAppPi(ctx, occurrenceId.toInt())

        val completePi = PendingIntent.getBroadcast(
            ctx, (occurrenceId + 100000).toInt(),
            Intent(ctx, TaskActionReceiver::class.java).apply {
                action = TaskActionReceiver.ACTION_COMPLETE
                putExtra(TaskActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePi = PendingIntent.getBroadcast(
            ctx, (occurrenceId + 200000).toInt(),
            Intent(ctx, TaskActionReceiver::class.java).apply {
                action = TaskActionReceiver.ACTION_SNOOZE
                putExtra(TaskActionReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            },
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
        } catch (_: SecurityException) {}
    }

    fun showHealthWarning(ctx: Context, health: Int) {
        ensureChannel(ctx)
        val pi = openAppPi(ctx, ID_HEALTH_WARNING)

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Health Critical: $health%")
            .setContentText("Your apps will be blocked soon. Recover by completing your tasks.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your apps will be blocked soon. Recover them by completing your task.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(ID_HEALTH_WARNING, notif)
        } catch (_: SecurityException) {}
    }

    fun showFocusLockActivated(ctx: Context, health: Int) {
        ensureChannel(ctx)
        val pi = openAppPi(ctx, ID_FOCUS_LOCK)

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🔒 Focus Lock Active")
            .setContentText("Apps blocked. Health $health%. Complete tasks to unlock.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(ID_FOCUS_LOCK, notif)
        } catch (_: SecurityException) {}
    }

    /** Call this when Health recovers above threshold — removes the persistent lock notification. */
    fun cancelFocusLock(ctx: Context) {
        try {
            NotificationManagerCompat.from(ctx).cancel(ID_FOCUS_LOCK)
        } catch (_: SecurityException) {}
    }

    fun cancelHealthWarning(ctx: Context) {
        try {
            NotificationManagerCompat.from(ctx).cancel(ID_HEALTH_WARNING)
        } catch (_: SecurityException) {}
    }

    fun showCriticalStarted(ctx: Context, title: String, endsAt: Long) {
        ensureChannel(ctx)
        val pi = openAppPi(ctx, ID_CRITICAL)
        val mins = ((endsAt - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0).toInt()
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🔴 Critical Task Active")
            .setContentText("$title — $mins min lock")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\nDevice locked for $mins minutes. Apps blocked until timer ends."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_CRITICAL, notif) } catch (_: SecurityException) {}
    }

    fun showCriticalFinished(ctx: Context, title: String) {
        try { NotificationManagerCompat.from(ctx).cancel(ID_CRITICAL) } catch (_: SecurityException) {}
        ensureChannel(ctx)
        val pi = openAppPi(ctx, ID_CRITICAL + 1)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("✅ Critical Task Complete")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_CRITICAL + 1, notif) } catch (_: SecurityException) {}
    }

    fun cancelCritical(ctx: Context) {
        try { NotificationManagerCompat.from(ctx).cancel(ID_CRITICAL) } catch (_: SecurityException) {}
    }

    private fun openAppPi(ctx: Context, id: Int): PendingIntent {
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            ctx, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
