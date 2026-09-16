package com.taskerflow.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.taskerflow.app.TaskerApp
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
        when (action) {
            ACTION_COMPLETE -> scope.launch { repo.completeOccurrence(occId) }
            ACTION_SNOOZE -> scope.launch {
                val occ = repo.getOccurrence(occId) ?: return@launch
                repo.snoozeOccurrence(occId, occ.scheduledAt + 10 * 60_000L, occ.deadlineAt + 10 * 60_000L)
            }
            else -> Log.w("TaskerFlow", "Unknown action: $action")
        }
    }

    companion object {
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
        const val ACTION_COMPLETE = "com.taskerflow.app.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.taskerflow.app.ACTION_SNOOZE"
    }
}
