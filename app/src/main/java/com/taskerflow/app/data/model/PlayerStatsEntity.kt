package com.taskerflow.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val health: Int = 100,          // 0..100
    val ep: Int = 0,                // 0..(unbounded)
    val level: Int = 1,
    val streak: Int = 0,
    val lastStreakDayEpoch: Long = 0L,
    val recoveryModeActive: Boolean = false,
    val focusLockActive: Boolean = false,
    val cycleStartedAt: Long = 0L,
    val totalCompleted: Int = 0,
    val totalMissed: Int = 0,
    val totalRecoveries: Int = 0
)
