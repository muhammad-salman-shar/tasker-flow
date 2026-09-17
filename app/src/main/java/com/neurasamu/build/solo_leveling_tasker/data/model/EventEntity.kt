package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [Index("occurrenceId"), Index("type"), Index("timestamp")]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val occurrenceId: Long,
    val taskId: Long,
    val type: EventType,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val epDelta: Int = 0,
    val healthDelta: Int = 0
)
