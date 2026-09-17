package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val repeatRule: RepeatRule = RepeatRule.NEVER,
    val customDays: String = "",                 // CSV of day-of-week ints (1=Mon..7=Sun)
    val soundUri: String = "",                   // empty = default system alarm tone
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val dismissMethod: DismissMethod = DismissMethod.EASY,
    val pinCode: String = "",                    // 6+ digits, only if dismissMethod == PIN
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
