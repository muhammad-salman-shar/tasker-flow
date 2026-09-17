package com.neurasamu.build.solo_leveling_tasker.domain

import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
import java.util.Calendar

object RecurrenceHelper {

    /** Given a scheduledAt millis and rule, return the next occurrence millis, or null if none. */
    fun nextOccurrence(from: Long, rule: RepeatRule, customDays: List<Int> = emptyList()): Long? {
        if (rule == RepeatRule.NEVER) return null
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        when (rule) {
            RepeatRule.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatRule.WEEKDAYS -> {
                do { cal.add(Calendar.DAY_OF_YEAR, 1) } while (cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY))
            }
            RepeatRule.WEEKENDS -> {
                do { cal.add(Calendar.DAY_OF_YEAR, 1) } while (cal.get(Calendar.DAY_OF_WEEK) !in listOf(Calendar.SATURDAY, Calendar.SUNDAY))
            }
            RepeatRule.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatRule.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RepeatRule.CUSTOM -> {
                if (customDays.isEmpty()) return null
                var guard = 0
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    guard++
                } while (cal.get(Calendar.DAY_OF_WEEK) !in customDays && guard < 14)
            }
            RepeatRule.NEVER -> return null
        }
        return cal.timeInMillis
    }
}
