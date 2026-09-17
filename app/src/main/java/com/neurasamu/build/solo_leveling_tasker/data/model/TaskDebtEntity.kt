package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_debt",
    indices = [Index("taskId"), Index("occurrenceId")]
)
data class TaskDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val occurrenceId: Long,
    val originalDeadline: Long,
    val debtCreatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolved: Boolean = false
)
