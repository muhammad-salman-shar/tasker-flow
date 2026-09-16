package com.taskerflow.app.data.repo

import android.content.Context
import com.taskerflow.app.data.db.AppDatabase
import com.taskerflow.app.data.model.*
import com.taskerflow.app.domain.GamificationEngine
import com.taskerflow.app.domain.GameConstants
import com.taskerflow.app.domain.PenaltyEngine
import com.taskerflow.app.worker.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean

class TaskRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {

    private val taskDao = db.taskDao()
    private val occDao = db.occurrenceDao()
    private val eventDao = db.eventDao()
    private val debtDao = db.taskDebtDao()
    private val recoveryDao = db.recoveryQuestDao()
    private val statsDao = db.playerStatsDao()

    private var lastFocusLockActive = false

    // Repository-level save guard (prevents double-tap duplicate insert)
    private val saveInFlight = AtomicBoolean(false)

    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeActive()
    fun observeOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeAllOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeActiveDebts() = debtDao.observeActive()
    fun observeActiveRecoveries() = recoveryDao.observeActive()
    fun observeStats() = statsDao.observe()
    fun observeOccurrencesForTask(taskId: Long) = occDao.observeForTask(taskId)

    suspend fun createTaskWithOccurrence(task: TaskEntity, scheduledAt: Long, deadlineAt: Long): Pair<Long, Long>? {
        if (!saveInFlight.compareAndSet(false, true)) return null
        try {
            val fixedTask = task.copy(difficulty = Difficulty.EASY) // Day task = 5 EP
            val taskId = taskDao.insert(fixedTask)
            val occId = occDao.insert(
                OccurrenceEntity(
                    taskId = taskId,
                    scheduledAt = scheduledAt,
                    deadlineAt = deadlineAt,
                    durationMinutes = task.durationMinutes,
                    status = OccurrenceStatus.PENDING
                )
            )
            eventDao.insert(EventEntity(occurrenceId = occId, taskId = taskId, type = EventType.CREATED))
            return taskId to occId
        } finally {
            saveInFlight.set(false)
        }
    }

    suspend fun createDeadlineTaskWithDays(
        task: TaskEntity,
        startMillis: Long,
        endMillis: Long
    ): Pair<Long, List<Long>>? {
        if (!saveInFlight.compareAndSet(false, true)) return null
        try {
            val zone = ZoneId.systemDefault()
            val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
            val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
            val (from, to) = if (startDate.isAfter(endDate)) endDate to startDate else startDate to endDate
            val totalDays = ChronoUnit.DAYS.between(from, to).toInt() + 1

            // EP reward per day: <=7d = 10, <=31d = 15, else 20
            val diff = when {
                totalDays <= 7 -> Difficulty.NORMAL      // 10
                totalDays <= 31 -> Difficulty.HARD       // 15
                else -> Difficulty.EXTREME               // 20
            }

            val taskId = taskDao.insert(task.copy(difficulty = diff))
            val occIds = mutableListOf<Long>()

            var current = from
            var dayIndex = 1
            while (!current.isAfter(to) && dayIndex <= 3650) {
                val dayStart = current.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = current.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                val id = occDao.insert(
                    OccurrenceEntity(
                        taskId = taskId,
                        scheduledAt = dayStart,
                        deadlineAt = dayEnd,
                        durationMinutes = 0,
                        status = OccurrenceStatus.PENDING
                    )
                )
                eventDao.insert(
                    EventEntity(occurrenceId = id, taskId = taskId, type = EventType.CREATED, note = "day_$dayIndex")
                )
                occIds.add(id)
                current = current.plusDays(1)
                dayIndex++
            }
            return taskId to occIds
        } finally {
            saveInFlight.set(false)
        }
    }

    suspend fun updateTask(task: TaskEntity) { taskDao.update(task) }

    suspend fun updateTaskWithOccurrence(task: TaskEntity, scheduledAt: Long, deadlineAt: Long): Long? {
        taskDao.update(task)
        val occ = occDao.getLatestPendingForTask(task.id)
        if (occ != null) {
            occDao.update(
                occ.copy(scheduledAt = scheduledAt, deadlineAt = deadlineAt, durationMinutes = task.durationMinutes)
            )
            eventDao.insert(EventEntity(occurrenceId = occ.id, taskId = task.id, type = EventType.RESCHEDULED))
            return occ.id
        }
        return null
    }

    suspend fun deleteTask(taskId: Long) {
        occDao.deleteByTask(taskId)
        debtDao.deleteByTask(taskId)
        recoveryDao.deleteByTask(taskId)
        taskDao.archive(taskId)
        eventDao.insert(EventEntity(occurrenceId = 0L, taskId = taskId, type = EventType.DELETED))
    }

    suspend fun deleteOccurrence(occId: Long) {
        val occ = occDao.getById(occId) ?: return
        eventDao.insert(EventEntity(occurrenceId = occId, taskId = occ.taskId, type = EventType.DELETED))
        occDao.update(occ.copy(status = OccurrenceStatus.SKIPPED))
    }

    suspend fun getOccurrence(id: Long) = occDao.getById(id)
    suspend fun getTask(id: Long) = taskDao.getById(id)
    suspend fun getLatestOccurrenceForTask(taskId: Long) = occDao.getLatestForTask(taskId)
    suspend fun getAllPendingOccurrences(): List<OccurrenceEntity> = occDao.getAllPending()

    /**
     * Mark an occurrence as completed and award EP/HP once.
     * No-op if already COMPLETED or LATE.
     */
    suspend fun completeOccurrence(occId: Long, completedAt: Long = System.currentTimeMillis()): Boolean {
        val occ = occDao.getById(occId) ?: return false
        // guard: already done
        if (occ.status == OccurrenceStatus.COMPLETED ||
            occ.status == OccurrenceStatus.LATE ||
            occ.status == OccurrenceStatus.RECOVERED) return false

        val task = taskDao.getById(occ.taskId) ?: return false
        val stats = statsDao.get() ?: PlayerStatsEntity()

        val minutesLate = ((completedAt - occ.deadlineAt) / 60000L).toInt()
        val baseEp = task.difficulty.epReward
        val (newStats, epResult) = GamificationEngine.applyCompletion(stats, baseEp, minutesLate)

        occDao.update(
            occ.copy(
                status = if (minutesLate > 0) OccurrenceStatus.LATE else OccurrenceStatus.COMPLETED,
                completedAt = completedAt
            )
        )
        statsDao.upsert(newStats)
        eventDao.insert(
            EventEntity(
                occurrenceId = occId, taskId = task.id, type = EventType.COMPLETED,
                epDelta = epResult.epGained, healthDelta = epResult.healthGained
            )
        )

        debtDao.getActiveForOccurrence(occId)?.let {
            debtDao.update(it.copy(resolved = true, resolvedAt = completedAt))
            val active = recoveryDao.getActive()
            if (active != null && active.originOccurrenceId == occId) {
                val newEarned = active.earnedEp + epResult.epGained
                recoveryDao.update(
                    active.copy(
                        earnedEp = newEarned,
                        status = if (newEarned >= active.requiredEp) RecoveryStatus.COMPLETED else RecoveryStatus.ACTIVE,
                        completedAt = if (newEarned >= active.requiredEp) completedAt else null
                    )
                )
            }
        }
        return true
    }

    /**
     * Reverse a completion: uncheck a completed day-task.
     * Subtracts the EP it earned, reverses HP milestones, and resets status to PENDING.
     * No-op if not currently COMPLETED/LATE/RECOVERED.
     */
    suspend fun uncompleteOccurrence(occId: Long): Boolean {
        val occ = occDao.getById(occId) ?: return false
        if (occ.status != OccurrenceStatus.COMPLETED &&
            occ.status != OccurrenceStatus.LATE &&
            occ.status != OccurrenceStatus.RECOVERED) return false

        val task = taskDao.getById(occ.taskId) ?: return false
        val stats = statsDao.get() ?: PlayerStatsEntity()

        // Look up the EP that was awarded for this completion event
        val events = eventDao.recentForOccurrence(occId)
        val completionEvent = events.firstOrNull { it.type == EventType.COMPLETED }
        val epAwarded = completionEvent?.epDelta ?: 0
        val hpAwarded = completionEvent?.healthDelta ?: 0

        val newEp = (stats.ep - epAwarded).coerceAtLeast(0)
        val newHp = (stats.health - hpAwarded).coerceAtLeast(0)

        // Recompute highestEpMilestone from new EP
        val newMilestone = newEp / GameConstants.EP_PER_HEALTH_TICK

        val newStats = stats.copy(
            ep = newEp,
            health = newHp,
            highestEpMilestone = newMilestone,
            totalCompleted = (stats.totalCompleted - 1).coerceAtLeast(0),
            level = GamificationEngine.computeLevel(newEp)
        )
        statsDao.upsert(newStats)

        occDao.update(
            occ.copy(
                status = OccurrenceStatus.PENDING,
                completedAt = null,
                penaltyAppliedCount = 0
            )
        )
        eventDao.insert(
            EventEntity(
                occurrenceId = occId, taskId = task.id, type = EventType.RESCHEDULED,
                note = "uncheck", epDelta = -epAwarded, healthDelta = -hpAwarded
            )
        )
        return true
    }

    suspend fun markMissed(occId: Long): Boolean {
        val occ = occDao.getById(occId) ?: return false
        if (occ.status != OccurrenceStatus.PENDING) return false
        occDao.update(occ.copy(status = OccurrenceStatus.MISSED))
        val stats = statsDao.get() ?: PlayerStatsEntity()
        statsDao.upsert(GamificationEngine.applyMiss(stats))
        debtDao.insert(TaskDebtEntity(taskId = occ.taskId, occurrenceId = occId, originalDeadline = occ.deadlineAt))
        recoveryDao.insert(RecoveryQuestEntity(taskId = occ.taskId, originOccurrenceId = occId, requiredEp = 10, earnedEp = 0))
        eventDao.insert(EventEntity(occurrenceId = occId, taskId = occ.taskId, type = EventType.MISSED))
        return true
    }

    suspend fun snoozeOccurrence(occId: Long, newScheduledAt: Long, newDeadlineAt: Long) {
        val occ = occDao.getById(occId) ?: return
        occDao.update(occ.copy(scheduledAt = newScheduledAt, deadlineAt = newDeadlineAt))
        eventDao.insert(EventEntity(occurrenceId = occId, taskId = occ.taskId, type = EventType.SNOOZED))
    }

    suspend fun ensureStatsRow() {
        if (statsDao.get() == null) {
            statsDao.upsert(PlayerStatsEntity(cycleStartedAt = System.currentTimeMillis()))
        }
    }

    suspend fun getStatsNow(): PlayerStatsEntity = statsDao.get() ?: PlayerStatsEntity()

    suspend fun forceRefresh() { applyPenaltiesTick() }

    suspend fun applyPenaltiesTick(now: Long = System.currentTimeMillis()): Int {
        val overdue = occDao.getOverdue(now)
        val statsBefore = statsDao.get() ?: PlayerStatsEntity()
        var currentStats = statsBefore
        var chargedCount = 0

        for (occ in overdue) {
            val delta = PenaltyEngine.computePenalty(currentStats, occ, now)
            if (delta.minutesCharged > 0) {
                currentStats = currentStats.copy(
                    ep = delta.newEp, health = delta.newHp, focusLockActive = delta.focusLockActive
                )
                occDao.update(occ.copy(penaltyAppliedCount = occ.penaltyAppliedCount + delta.minutesCharged))
                eventDao.insert(
                    EventEntity(
                        occurrenceId = occ.id, taskId = occ.taskId, type = EventType.MISSED,
                        note = "penalty_tick", epDelta = -delta.epDrained, healthDelta = -delta.hpDrained
                    )
                )
                chargedCount++
            }
        }
        if (chargedCount > 0) {
            statsDao.upsert(currentStats)
            notifyIfNeeded(statsBefore, currentStats)
        }
        return chargedCount
    }

    private fun notifyIfNeeded(before: PlayerStatsEntity, after: PlayerStatsEntity) {
        if (before.health >= 50 && after.health < 50) {
            NotificationHelper.showHealthWarning(appContext, after.health)
        }
        if (!lastFocusLockActive && after.focusLockActive) {
            lastFocusLockActive = true
            NotificationHelper.showFocusLockActivated(appContext, after.health)
        }
        if (after.health >= 55) lastFocusLockActive = false
    }
}
