package com.taskerflow.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val occId = intent.getLongExtra(EXTRA_OCCURRENCE_ID, -1L)
        if (occId <= 0) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Task Reminder"
        val ep = intent.getIntExtra(EXTRA_EP, 10)

        NotificationHelper.showReminder(
            ctx = ctx,
            occurrenceId = occId,
            title = title,
            epReward = ep,
            dueAt = System.currentTimeMillis()
        )
    }

    companion object {
        const val EXTRA_OCCURRENCE_ID = "reminder_occ_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_EP = "reminder_ep"
    }
}
