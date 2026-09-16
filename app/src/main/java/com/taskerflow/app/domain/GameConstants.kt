package com.taskerflow.app.domain

object GameConstants {
    const val HEALTH_MAX = 100
    const val HEALTH_MIN = 0

    const val MISS_PENALTY_HEALTH = 5
    const val LATE_PENALTY_EP_STEP = 2   // per 10 min late
    const val LATE_PENALTY_EP_CAP = 6
    const val EARLY_BONUS_EP = 2         // completing 30+ min before deadline
    const val EARLY_THRESHOLD_MIN = 30

    const val EP_PER_HEALTH_TICK = 20
    const val HEALTH_GAIN_PER_TICK = 2

    const val RECOVERY_MODE_CLEAR_EP = 50
    const val FOCUS_LOCK_HEALTH_THRESHOLD = 50
    const val FOCUS_RELEASE_HEALTH_THRESHOLD = 55

    const val EP_PER_LEVEL = 80
    const val DAILY_HEALTH_DAMAGE_CAP = 25
}
