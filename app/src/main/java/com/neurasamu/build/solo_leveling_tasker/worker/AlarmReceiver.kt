package com.neurasamu.build.solo_leveling_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.neurasamu.build.solo_leveling_tasker.ui.alarm.AlarmRingActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0) return

        val ringIntent = Intent(ctx, AlarmRingActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        ctx.startActivity(ringIntent)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
