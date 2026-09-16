package com.taskerflow.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recovery_quests",
    indices = [Index("taskId"), Index("status")]
)
data class RecoveryQuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val originOccurrenceId: Long,
    val status: RecoveryStatus = RecoveryStatus.ACTIVE,
    val requiredEp: Int = 10,
    val earnedEp: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
