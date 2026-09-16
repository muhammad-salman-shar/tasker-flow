package com.taskerflow.app.data.repo

import android.content.Context
import com.taskerflow.app.data.db.AppDatabase
import com.taskerflow.app.data.model.*
import com.taskerflow.app.domain.GamificationEngine
import com.taskerflow.app.domain.PenaltyEngine
import com.taskerflow.app.worker.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

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

    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeActive()
    fun observeOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeAllOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeActiveDebts() = debtDao.observeActive()
    fun observeActiveRecoveries() = recoveryDao.observeActive()
    fun observeStats() = statsDao.observe()
    fun observeOccurrencesForTask(taskId: Long) = occDao.observeForTask(taskId)

    suspend fun createTaskWithOccurrence(task: TaskEntity, scheduledAt: Long, deadlineAt: Long): Pair<Long, Long> {
        val taskId = taskDao.insert(task)
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
    }

    /**
     * Deadline task with a date range: creates one occurrence per day.
     * Each day: scheduledAt = start of day, deadlineAt = end of day (23:59).
     * Day 1 is unlocked; day N unlocks only after day N-1 is no longer PENDING.
     */
    suspend fun createDeadlineTaskWithDays(task: TaskEntity, startMillis: Long, endMillis: Long): Pair<Long, List<Long>> {
        val taskId = taskDao.insert(task)
        val occIds = mutableListOf<Long>()

        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
        }
        var dayIndex = 1
        while (cal.timeInMillis <= endCal.timeInMillis && dayIndex <= 120) {
            val dayStart = cal.timeInMillis
            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val id = occDao.insert(
                OccurrenceEntity(
                    taskId = taskId,
                    scheduledAt = dayStart,
                    deadlineAt = dayEnd,
                    durationMinutes = 0,
                    status = OccurrenceStatus.PENDING
                )
            )
            eventDao.insert(EventEntity(occurrenceId = id, taskId = taskId, type = EventType.CREATED, note = "day_$dayIndex"))
            occIds.add(id)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            dayIndex++
        }
        return taskId to occIds
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
        // Only allow deleting if not the last remaining occurrence
        eventDao.insert(EventEntity(occurrenceId = occId, taskId = occ.taskId, type = EventType.DELETED))
        occDao.update(occ.copy(status = OccurrenceStatus.SKIPPED))
    }

    suspend fun getOccurrence(id: Long) = occDao.getById(id)
    suspend fun getTask(id: Long) = taskDao.getById(id)
    suspend fun getLatestOccurrenceForTask(taskId: Long) = occDao.getLatestForTask(taskId)
    suspend fun getAllPendingOccurrences(): List<OccurrenceEntity> = occDao.getAllPending()

    suspend fun completeOccurrence(occId: Long, completedAt: Long = System.currentTimeMillis()): Boolean {
        val occ = occDao.getById(occId) ?: return false
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
