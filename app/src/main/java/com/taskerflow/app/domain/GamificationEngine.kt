package com.taskerflow.app.domain

import com.taskerflow.app.data.model.PlayerStatsEntity
import kotlin.math.max
import kotlin.math.min

data class EpResult(val epGained: Int, val healthGained: Int, val reason: String)

object GamificationEngine {

    /** Compute EP for completing a task. onTime=true → full base; late → degrade. */
    fun computeCompletionEp(baseEp: Int, minutesLate: Int): Int {
        if (minutesLate <= 0) return baseEp
        val penalty = min(
            (minutesLate / 10) * GameConstants.LATE_PENALTY_EP_STEP,
            GameConstants.LATE_PENALTY_EP_CAP
        )
        return max(0, baseEp - penalty)
    }

    /** Apply completion to stats. Returns updated stats. */
    fun applyCompletion(stats: PlayerStatsEntity, baseEp: Int, minutesLate: Int): Pair<PlayerStatsEntity, EpResult> {
        val gained = computeCompletionEp(baseEp, minutesLate)
        val newEp = stats.ep + gained

        // Every EP_PER_HEALTH_TICK EP crossing → +HEALTH_GAIN_PER_TICK health
        val healthTicksGained = newEp / GameConstants.EP_PER_HEALTH_TICK
        val oldHealthTicks = stats.ep / GameConstants.EP_PER_HEALTH_TICK
        val tickDelta = healthTicksGained - oldHealthTicks
        val healthGain = tickDelta * GameConstants.HEALTH_GAIN_PER_TICK
        val newHealth = min(GameConstants.HEALTH_MAX, stats.health + healthGain)

        val newLevel = computeLevel(newEp)

        val updated = stats.copy(
            ep = newEp,
            health = newHealth,
            level = newLevel,
            totalCompleted = stats.totalCompleted + 1,
            recoveryModeActive = newEp < GameConstants.RECOVERY_MODE_CLEAR_EP && stats.recoveryModeActive,
            focusLockActive = newHealth < GameConstants.FOCUS_RELEASE_HEALTH_THRESHOLD && stats.focusLockActive
        )
        return updated to EpResult(gained, healthGain, if (minutesLate > 0) "late" else "on_time")
    }

    /** Apply miss penalty. */
    fun applyMiss(stats: PlayerStatsEntity): PlayerStatsEntity {
        val newHealth = max(GameConstants.HEALTH_MIN, stats.health - GameConstants.MISS_PENALTY_HEALTH)
        val focusLock = newHealth < GameConstants.FOCUS_LOCK_HEALTH_THRESHOLD
        val recoveryMode = stats.ep < GameConstants.RECOVERY_MODE_CLEAR_EP
        return stats.copy(
            health = newHealth,
            totalMissed = stats.totalMissed + 1,
            recoveryModeActive = recoveryMode,
            focusLockActive = focusLock
        )
    }

    fun computeLevel(totalEp: Int): Int =
        max(1, totalEp / GameConstants.EP_PER_LEVEL + 1)

    fun levelProgress(totalEp: Int): Pair<Int, Int> {
        val level = computeLevel(totalEp)
        val floor = (level - 1) * GameConstants.EP_PER_LEVEL
        val next = level * GameConstants.EP_PER_LEVEL
        return (totalEp - floor) to (next - floor)
    }
}
