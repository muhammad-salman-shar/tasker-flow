package com.taskerflow.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "occurrences",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId"), Index("status"), Index("scheduledAt")]
)
data class OccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val scheduledAt: Long,       // when to start / be reminded
    val deadlineAt: Long,        // when it becomes MISSED
    val durationMinutes: Int = 0,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
