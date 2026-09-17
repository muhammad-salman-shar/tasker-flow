package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: Category = Category.OTHER,
    val priority: Priority = Priority.MEDIUM,
    val taskType: TaskType = TaskType.SCHEDULED,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val repeatRule: RepeatRule = RepeatRule.NEVER,
    val customRepeatDays: String = "",         // CSV of day-of-week ints (1=Mon..7=Sun)
    val durationMinutes: Int = 0,
    val reminderOffsetMinutes: Int = 0,        // 0=exact time, 15=15 min before, etc.
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)
