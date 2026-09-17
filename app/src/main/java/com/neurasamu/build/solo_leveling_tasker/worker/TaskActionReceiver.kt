package com.neurasamu.build.solo_leveling_tasker.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.neurasamu.build.solo_leveling_tasker.TaskerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val occId = intent.getLongExtra(EXTRA_OCCURRENCE_ID, -1L)
        if (occId <= 0) return
        val action = intent.action ?: return
        val repo = (context.applicationContext as TaskerApp).repository
        val scope = CoroutineScope(Dispatchers.IO)

        // dismiss notification
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(occId.toInt())

        when (action) {
            ACTION_COMPLETE -> scope.launch {
                repo.completeOccurrence(occId)
                AlarmScheduler.cancel(context, occId)
            }
            ACTION_SNOOZE -> scope.launch {
                val occ = repo.getOccurrence(occId) ?: return@launch
                val newSched = occ.scheduledAt + 10 * 60_000L
                val newDeadline = occ.deadlineAt + 10 * 60_000L
                repo.snoozeOccurrence(occId, newSched, newDeadline)
                val task = repo.getTask(occ.taskId)
                if (task != null) {
                    AlarmScheduler.scheduleReminder(
                        context, occId, newSched, task.title, task.difficulty.epReward
                    )
                }
            }
            else -> Log.w("TaskerFlow", "Unknown action: $action")
        }
    }

    companion object {
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
        const val ACTION_COMPLETE = "com.neurasamu.build.solo_leveling_tasker.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.neurasamu.build.solo_leveling_tasker.ACTION_SNOOZE"
    }
}
