package com.taskerflow.app.domain

import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.data.model.PlayerStatsEntity
import kotlin.math.ceil

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
 * Fast, deterministic penalty:
 *   - Each missed task drains 5% of current EP per minute, minimum 3 EP
 *   - Stacking: 5 tasks = 25% per minute
 *   - Once EP = 0, HP drains 15% of current HP per minute, minimum 3 HP
 *   - After 24h per task, that task stops draining
 *   - Deterministic via occurrence.penaltyAppliedCount (refresh-safe)
 */
object PenaltyEngine {

    const val MAX_MINUTES_CHARGED = 1440
    const val PCT_PER_TASK = 0.05
    const val MIN_EP_DRAIN = 3
    const val HP_PCT = 0.15
    const val MIN_HP_DRAIN = 3
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

        var ep = stats.ep
        var hp = stats.health
        var epDrained = 0
        var hpDrained = 0

        repeat(toCharge) {
            if (ep > 0) {
                val drain = maxOf(MIN_EP_DRAIN, ceil(ep * PCT_PER_TASK).toInt())
                val actual = minOf(drain, ep)
                ep -= actual
                epDrained += actual
            } else if (hp > 0) {
                val drain = maxOf(MIN_HP_DRAIN, ceil(hp * HP_PCT).toInt())
                val actual = minOf(drain, hp)
                hp -= actual
                hpDrained += actual
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
        val epRate = if (inHp) 0 else maxOf(MIN_EP_DRAIN * count, ceil(stats.ep * pct).toInt())
        val hpRate = if (inHp) maxOf(MIN_HP_DRAIN * count, ceil(stats.health * HP_PCT * count).toInt()) else 0
        return DrainRate(count, epRate, hpRate, inHp)
    }
}
