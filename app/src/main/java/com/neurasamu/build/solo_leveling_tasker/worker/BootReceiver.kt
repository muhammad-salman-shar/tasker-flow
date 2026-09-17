package com.neurasamu.build.solo_leveling_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.neurasamu.build.solo_leveling_tasker.TaskerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i("TaskerFlow", "Boot completed — rescheduling pending task alarms")
        val app = context.applicationContext as TaskerApp
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val pending = app.repository.getAllPendingOccurrences()
                val now = System.currentTimeMillis()
                for (occ in pending) {
                    if (occ.scheduledAt <= now) continue
                    val task = app.repository.getTask(occ.taskId) ?: continue
                    AlarmScheduler.scheduleReminder(
                        ctx = app,
                        occurrenceId = occ.id,
                        triggerAt = occ.scheduledAt,
                        title = task.title,
                        epReward = task.difficulty.epReward
                    )
                }
                Log.i("TaskerFlow", "Rescheduled ${pending.size} pending alarms")
            } catch (e: Exception) {
                Log.e("TaskerFlow", "Boot reschedule failed", e)
            }
        }
    }
}
