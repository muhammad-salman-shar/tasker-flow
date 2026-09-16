package com.taskerflow.app.domain

import com.taskerflow.app.data.model.PlayerStatsEntity
import kotlin.math.max
import kotlin.math.min

data class EpResult(val epGained: Int, val healthGained: Int, val reason: String)

object GamificationEngine {

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

        val currentMilestone = newEp / GameConstants.EP_PER_HEALTH_TICK
        val highest = stats.highestEpMilestone
        val newMilestones = (currentMilestone - highest).coerceAtLeast(0)
        val healthGain = newMilestones * GameConstants.HEALTH_GAIN_PER_TICK
        val newHighest = max(highest, currentMilestone)

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
            focusLockActive = newHealth < GameConstants.FOCUS_RELEASE_HEALTH_THRESHOLD && stats.focusLockActive,
            highestEpMilestone = newHighest
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

    /** Cumulative EP required to *enter* level N (Level 1 starts at 0). */
    fun thresholdForLevel(level: Int): Int {
        if (level <= 1) return 0
        // Level N starts at: 90 * (N-1) * N / 2
        val n = level
        return GameConstants.EP_PER_CYCLE * (n - 1) * n / 2
    }

    /** How many cycles are needed to complete current level. */
    fun cyclesForLevel(level: Int): Int = level

    fun computeLevel(totalEp: Int): Int {
        var level = 1
        while (totalEp >= thresholdForLevel(level + 1)) level++
        return level
    }

    /**
     * Returns (currentCycleEp, EP_PER_CYCLE) — the circle displays 0..90.
     */
    fun levelProgress(totalEp: Int): Pair<Int, Int> {
        val level = computeLevel(totalEp)
        val levelStart = thresholdForLevel(level)
        val epInLevel = (totalEp - levelStart).coerceAtLeast(0)
        return (epInLevel % GameConstants.EP_PER_CYCLE) to GameConstants.EP_PER_CYCLE
    }
}
