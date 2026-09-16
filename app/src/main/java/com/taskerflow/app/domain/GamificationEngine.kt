package com.taskerflow.app.domain

import com.taskerflow.app.data.model.PlayerStatsEntity
import kotlin.math.max
import kotlin.math.min

data class EpResult(val epGained: Int, val healthGained: Int, val reason: String)

object GamificationEngine {

    /**
     * minutesLate > 0  → task completed after deadline (penalty)
     * minutesLate == 0 → on time (full EP)
     * minutesLate < 0  → completed early (bonus if 30+ min early)
     */
    fun computeCompletionEp(baseEp: Int, minutesLate: Int): Int {
        return when {
            minutesLate <= -GameConstants.EARLY_THRESHOLD_MIN ->
                baseEp + GameConstants.EARLY_BONUS_EP
            minutesLate <= 0 -> baseEp
            else -> {
                val penalty = min(
                    (minutesLate / 10) * GameConstants.LATE_PENALTY_EP_STEP,
                    GameConstants.LATE_PENALTY_EP_CAP
                )
                max(0, baseEp - penalty)
            }
        }
    }

    fun applyCompletion(
        stats: PlayerStatsEntity,
        baseEp: Int,
        minutesLate: Int
    ): Pair<PlayerStatsEntity, EpResult> {
        val gained = computeCompletionEp(baseEp, minutesLate)
        val newEp = stats.ep + gained

        val oldTicks = stats.ep / GameConstants.EP_PER_HEALTH_TICK
        val newTicks = newEp / GameConstants.EP_PER_HEALTH_TICK
        val tickDelta = newTicks - oldTicks
        val healthGain = tickDelta * GameConstants.HEALTH_GAIN_PER_TICK
        val newHealth = min(GameConstants.HEALTH_MAX, stats.health + healthGain)

        val newLevel = computeLevel(newEp)

        val reason = when {
            minutesLate <= -GameConstants.EARLY_THRESHOLD_MIN -> "early"
            minutesLate > 0 -> "late"
            else -> "on_time"
        }

        val updated = stats.copy(
            ep = newEp,
            health = newHealth,
            level = newLevel,
            totalCompleted = stats.totalCompleted + 1,
            recoveryModeActive = newEp < GameConstants.RECOVERY_MODE_CLEAR_EP && stats.recoveryModeActive,
            focusLockActive = newHealth < GameConstants.FOCUS_RELEASE_HEALTH_THRESHOLD && stats.focusLockActive
        )
        return updated to EpResult(gained, healthGain, reason)
    }

    fun applyMiss(stats: PlayerStatsEntity): PlayerStatsEntity {
        val newHealth = max(GameConstants.HEALTH_MIN, stats.health - GameConstants.MISS_PENALTY_HEALTH)
        return stats.copy(
            health = newHealth,
            totalMissed = stats.totalMissed + 1,
            recoveryModeActive = stats.ep < GameConstants.RECOVERY_MODE_CLEAR_EP,
            focusLockActive = newHealth < GameConstants.FOCUS_LOCK_HEALTH_THRESHOLD
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
