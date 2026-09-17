package com.neurasamu.build.solo_leveling_tasker.data.model

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

/**
 * Deadline rendering scale. Derived from day count.
 * - WEEKLY: <= 7 days (amber)
 * - MONTHLY: <= 31 days (cyan)
 * - CUSTOM:  > 31 days (violet, no yearly concept)
 */
enum class DeadlineScale { WEEKLY, MONTHLY, CUSTOM }
