package com.neurasamu.build.solo_leveling_tasker.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    /** Task reminder (existing) — schedule before scheduledAt by offsetMinutes */
    fun scheduleReminder(
        ctx: Context,
        occurrenceId: Long,
        triggerAt: Long,
        title: String,
        epReward: Int,
        offsetMinutes: Int = 0
    ) {
        val fireAt = triggerAt - offsetMinutes * 60_000L
        if (fireAt <= System.currentTimeMillis()) return

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_EP, epReward)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            occurrenceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(ctx, am, fireAt, pi)
    }

    fun cancel(ctx: Context, occurrenceId: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx,
            occurrenceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    // ============== DEVICE ALARMS (user-created) ==============

    /**
     * Schedule a user alarm.
     * Computes the next fire time based on repeatRule + customDays.
     */
    fun scheduleAlarm(ctx: Context, alarmId: Long, hour: Int, minute: Int, repeatRule: String, customDays: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fireAt = computeNextFireTime(hour, minute, repeatRule, customDays) ?: return
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            ALARM_PI_REQUEST(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(ctx, am, fireAt, pi)
    }

    fun cancelAlarm(ctx: Context, alarmId: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx,
            ALARM_PI_REQUEST(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    /** Snooze: fire once at now + minutes (ignores repeat) */
    fun snoozeAlarm(ctx: Context, alarmId: Long, minutes: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fireAt = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            ALARM_PI_REQUEST(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExact(ctx, am, fireAt, pi)
    }

    private fun computeNextFireTime(hour: Int, minute: Int, repeatRule: String, customDays: String): Long? {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (repeatRule) {
            "NEVER" -> {
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }
            "DAILY" -> {
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }
            "WEEKDAYS" -> {
                do { target.add(Calendar.DAY_OF_YEAR, 1) }
                while (target.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY) ||
                    target.timeInMillis <= now.timeInMillis)
                return target.timeInMillis
            }
            "WEEKENDS" -> {
                do { target.add(Calendar.DAY_OF_YEAR, 1) }
                while (target.get(Calendar.DAY_OF_WEEK) !in listOf(Calendar.SATURDAY, Calendar.SUNDAY) ||
                    target.timeInMillis <= now.timeInMillis)
                return target.timeInMillis
            }
            "WEEKLY" -> {
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.WEEK_OF_YEAR, 1)
                }
                return target.timeInMillis
            }
            "MONTHLY" -> {
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.MONTH, 1)
                }
                return target.timeInMillis
            }
            "CUSTOM" -> {
                val days = customDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (days.isEmpty()) return null
                // days: 1=Mon..7=Sun ; Calendar: 1=Sun..7=Sat
                val calDays = days.map { if (it == 7) 1 else it + 1 }
                var guard = 0
                while (guard < 14) {
                    if (target.timeInMillis > now.timeInMillis &&
                        target.get(Calendar.DAY_OF_WEEK) in calDays) {
                        return target.timeInMillis
                    }
                    target.add(Calendar.DAY_OF_YEAR, 1)
                    guard++
                }
                return null
            }
        }
        return null
    }

    private fun setExact(ctx: Context, am: AlarmManager, fireAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    private fun ALARM_PI_REQUEST(id: Long): Int = (id + 900_000).toInt()
}
