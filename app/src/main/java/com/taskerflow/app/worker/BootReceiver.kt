package com.taskerflow.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i("TaskerFlow", "Boot completed — rescheduling tasks")
        // TODO: reschedule all PENDING occurrences via AlarmManager/WorkManager
    }
}
