package com.neurasamu.build.solo_leveling_tasker.domain

object GameConstants {
    const val HEALTH_MAX = 100
    const val HEALTH_MIN = 0

    const val MISS_PENALTY_HEALTH = 5
    const val LATE_PENALTY_EP_STEP = 2
    const val LATE_PENALTY_EP_CAP = 6
    const val EARLY_BONUS_EP = 2
    const val EARLY_THRESHOLD_MIN = 30

    // HP gain: every 5 EP earned = +1 HP
    const val EP_PER_HEALTH_TICK = 5
    const val HEALTH_GAIN_PER_TICK = 1

    const val RECOVERY_MODE_CLEAR_EP = 50
    const val FOCUS_LOCK_HEALTH_THRESHOLD = 50
    const val FOCUS_RELEASE_HEALTH_THRESHOLD = 55

    // Level system: each "cycle" = 90 EP.
    // Level 1 → 2 needs 1 cycle (90 EP)
    // Level 2 → 3 needs 2 cycles (180 EP)
    // Level N → N+1 needs N cycles.
    const val EP_PER_CYCLE = 90

    const val DAILY_HEALTH_DAMAGE_CAP = 25

    // Task rewards
    const val EP_DAY_TASK = 5
}
