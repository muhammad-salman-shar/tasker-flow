package com.neurasamu.build.solo_leveling_tasker.domain

import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import kotlin.math.max
import kotlin.math.min

data class EpResult(
    val epGained: Int,
    val healthGained: Int,
    val reason: String,
    val cycleProgress: Int,   // 0..89 (display)
    val totalCycles: Int      // lifetime cycles completed
)

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

    /**
     * Apply completion.
     *
     * Rules:
     * - ep field stores cycle progress (0..89)
     * - when ep + gained >= 90 → level++, ep = (ep+gained) % 90
     * - HP gain: 1 per 5 EP gained (normal completion)
     * - Recovery mode (health<50, missed>0) handles HP refill separately in repo
     */
    fun applyCompletion(
        stats: PlayerStatsEntity,
        baseEp: Int,
        minutesLate: Int,
        hpOverride: Int? = null,
        forceCycleFill: Boolean = false
    ): Pair<PlayerStatsEntity, EpResult> {
        val gained = computeCompletionEp(baseEp, minutesLate)
        val newEpRaw = if (forceCycleFill) {
            GameConstants.EP_PER_CYCLE
        } else {
            stats.ep + gained
        }

        val cyclesGained = newEpRaw / GameConstants.EP_PER_CYCLE
        val newEp = newEpRaw % GameConstants.EP_PER_CYCLE
        val newLevel = stats.level + cyclesGained

        val hpGain = hpOverride ?: (gained / GameConstants.EP_PER_HEALTH_TICK)
        val newHealth = min(GameConstants.HEALTH_MAX, stats.health + hpGain)

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
            recoveryModeActive = false,
            focusLockActive = newHealth < GameConstants.FOCUS_RELEASE_HEALTH_THRESHOLD && stats.focusLockActive
        )
        return updated to EpResult(gained, hpGain, reason, newEp, stats.level + cyclesGained - 1)
    }

    /** Award EP without touching HP/completion counters. Used for task creation reward. */
    fun awardEp(stats: PlayerStatsEntity, amount: Int): PlayerStatsEntity {
        if (amount <= 0) return stats
        val newEpRaw = stats.ep + amount
        val cyclesGained = newEpRaw / GameConstants.EP_PER_CYCLE
        val newEp = newEpRaw % GameConstants.EP_PER_CYCLE
        val newLevel = stats.level + cyclesGained
        return stats.copy(ep = newEp, level = newLevel)
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

    /** Level is stored directly — no derivation needed. */
    fun computeLevel(storedLevel: Int): Int = storedLevel.coerceAtLeast(1)

    /** Display progress within current cycle: (epInCycle, 90). */
    fun levelProgress(cycleEp: Int): Pair<Int, Int> =
        cycleEp.coerceIn(0, GameConstants.EP_PER_CYCLE) to GameConstants.EP_PER_CYCLE
}
