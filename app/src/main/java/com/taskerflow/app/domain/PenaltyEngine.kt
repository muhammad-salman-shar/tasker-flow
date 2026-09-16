package com.taskerflow.app.domain

import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.data.model.PlayerStatsEntity

data class PenaltyDelta(
    val epDrained: Int,
    val hpDrained: Int,
    val minutesCharged: Int,
    val newEp: Int,
    val newHp: Int,
    val focusLockActive: Boolean,
    val totalMinutesLate: Int
)

object PenaltyEngine {

    const val MAX_MINUTES_CHARGED = 100
    const val EP_PER_MINUTE = 1
    const val HP_PER_MINUTE = 1
    const val FOCUS_LOCK_HP_THRESHOLD = 50

    /**
     * Compute how much penalty should be applied since last check.
     *
     * Rules:
     * - After deadline, each minute → -1 EP
     * - Cap at MAX_MINUTES_CHARGED minutes total
     * - Once EP reaches 0, HP starts draining at -1 per minute
     * - HP < FOCUS_LOCK_HP_THRESHOLD → Focus Lock active
     */
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
                ep = (ep - EP_PER_MINUTE).coerceAtLeast(0)
                epDrained++
            } else {
                hp = (hp - HP_PER_MINUTE).coerceAtLeast(0)
                hpDrained++
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
}
