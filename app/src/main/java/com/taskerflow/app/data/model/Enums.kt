package com.taskerflow.app.data.model

enum class TaskType { DEADLINE, SCHEDULED, DURATION, FLEXIBLE }

enum class Priority { LOW, MEDIUM, HIGH, CRITICAL }

enum class Difficulty(val epReward: Int) {
    EASY(5), NORMAL(10), HARD(15), EXTREME(20)
}

enum class RepeatRule { NEVER, DAILY, WEEKDAYS, WEEKENDS, WEEKLY, MONTHLY, CUSTOM }

enum class OccurrenceStatus { PENDING, COMPLETED, MISSED, LATE, SKIPPED, RECOVERED }

enum class EventType { CREATED, REMINDED, SNOOZED, COMPLETED, MISSED, RESCHEDULED, RECOVERED, DELETED }

enum class RecoveryStatus { ACTIVE, COMPLETED, ABANDONED }

enum class Category { STUDY, CODING, FITNESS, WORK, PERSONAL, CREATIVE, LEARNING, OTHER }

/** How a deadline task should render (derived from date range). */
enum class DeadlineScale { SHORT, MEDIUM, LONG }
