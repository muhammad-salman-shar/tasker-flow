package com.taskerflow.app.domain

import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.data.model.PlayerStatsEntity
import kotlin.math.ceil
import kotlin.math.floor

data class PenaltyDelta(
    val epDrained: Int,
    val hpDrained: Int,
    val minutesCharged: Int,
    val newEp: Int,
    val newHp: Int,
    val focusLockActive: Boolean,
    val totalMinutesLate: Int
)

data class DrainRate(
    val overdueCount: Int,
    val epPerMinute: Int,
    val hpPerMinute: Int,
    val inHpPhase: Boolean
)

/**
 * Deterministic, percentage-based penalty.
 *
 * Rule:
 *   1 missed task  → 5% EP per minute
 *   2 missed tasks → 10% EP per minute
 *   5 missed tasks → 25% EP per minute
 *
 * When EP hits 0: HP drains with same % scaling.
 * Formula: ceil(currentEp * 0.05 * missedCount) per minute
 *
 * All calculations are derived from (occurrence.penaltyAppliedCount, now, deadlineAt)
 * so refresh is idempotent and app restarts don't reset anything.
 */
object PenaltyEngine {

    const val MAX_MINUTES_CHARGED = 1440   // 24h cap per task
    const val PCT_PER_TASK = 0.05          // 5% per missed task per minute
    const val FOCUS_LOCK_HP_THRESHOLD = 50

    fun computePenalty(
        stats: PlayerStatsEntity,
        occ: OccurrenceEntity,
        now: Long
    ): PenaltyDelta {
        val zero = PenaltyDelta(0, 0, 0, stats.ep, stats.health, stats.health < FOCUS_LOCK_HP_THRESHOLD, 0)
        if (occ.status != OccurrenceStatus.PENDING) return zero
        if (now <= occ.deadlineAt) return zero

        val totalMinutesLate = ((now - occ.deadlineAt) / 60_000L).toInt()
        val targetCharged = minOf(totalMinutesLate, MAX_MINUTES_CHARGED)
        val alreadyCharged = occ.penaltyAppliedCount
        val toCharge = (targetCharged - alreadyCharged).coerceAtLeast(0)
        if (toCharge == 0) return zero.copy(totalMinutesLate = totalMinutesLate)

        // This single-occurrence path is called per-occurrence by the tick loop,
        // but the tick loop applies them sequentially on the running totals.
        // Percentage is 5% of the *current* EP.
        var ep = stats.ep
        var hp = stats.health
        var epDrained = 0
        var hpDrained = 0

        repeat(toCharge) {
            if (ep > 0) {
                val drain = maxOf(1, ceil(ep * PCT_PER_TASK).toInt())
                val actual = minOf(drain, ep)
                ep -= actual
                epDrained += actual
            } else {
                // HP phase: 5% of current HP per minute (rounded up, min 1)
                if (hp > 0) {
                    val drain = maxOf(1, ceil(hp * PCT_PER_TASK).toInt())
                    val actual = minOf(drain, hp)
                    hp -= actual
                    hpDrained += actual
                }
            }
        }

        return PenaltyDelta(
            epDrained = epDrained,
            hpDrained = hpDrained,
            minutesCharged = toCharge,
            newEp = ep,
            newHp = hp,
            focusLockActive = hp < FOCUS_LOCK_HP_THRESHOLD,
            totalMinutesLate = totalMinutesLate
        )
    }

    fun currentDrainRate(
        stats: PlayerStatsEntity,
        overdue: List<OccurrenceEntity>
    ): DrainRate {
        val count = overdue.count { it.status == OccurrenceStatus.PENDING }
        if (count == 0) return DrainRate(0, 0, 0, false)
        val pct = count * PCT_PER_TASK
        val inHp = stats.ep <= 0
        val epRate = if (inHp) 0 else maxOf(1, ceil(stats.ep * pct).toInt())
        val hpRate = if (inHp) maxOf(1, ceil(stats.health * pct).toInt()) else 0
        return DrainRate(count, epRate, hpRate, inHp)
    }
}
