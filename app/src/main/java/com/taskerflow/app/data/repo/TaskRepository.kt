package com.taskerflow.app.data.repo

import com.taskerflow.app.data.db.AppDatabase
import com.taskerflow.app.data.model.*
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val db: AppDatabase) {

    private val taskDao = db.taskDao()
    private val occDao = db.occurrenceDao()
    private val eventDao = db.eventDao()
    private val debtDao = db.taskDebtDao()
    private val recoveryDao = db.recoveryQuestDao()
    private val statsDao = db.playerStatsDao()

    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeActive()
    fun observeOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeActiveDebts() = debtDao.observeActive()
    fun observeActiveRecoveries() = recoveryDao.observeActive()
    fun observeStats() = statsDao.observe()

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

    suspend fun getOccurrence(id: Long) = occDao.getById(id)
    suspend fun getTask(id: Long) = taskDao.getById(id)

    suspend fun completeOccurrence(occId: Long, completedAt: Long = System.currentTimeMillis()): Boolean {
        val occ = occDao.getById(occId) ?: return false
        val task = taskDao.getById(occ.taskId) ?: return false
        val stats = statsDao.get() ?: PlayerStatsEntity()

        val minutesLate = ((completedAt - occ.deadlineAt) / 60000L).toInt()
        val baseEp = task.difficulty.epReward
        val (newStats, epResult) = com.taskerflow.app.domain.GamificationEngine
            .applyCompletion(stats, baseEp, minutesLate)

        occDao.update(
            occ.copy(
                status = if (minutesLate > 0) OccurrenceStatus.LATE else OccurrenceStatus.COMPLETED,
                completedAt = completedAt
            )
        )
        statsDao.upsert(newStats)
        eventDao.insert(
            EventEntity(
                occurrenceId = occId,
                taskId = task.id,
                type = EventType.COMPLETED,
                epDelta = epResult.epGained,
                healthDelta = epResult.healthGained
            )
        )

        // resolve debt if any
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
        statsDao.upsert(com.taskerflow.app.domain.GamificationEngine.applyMiss(stats))
        debtDao.insert(
            TaskDebtEntity(
                taskId = occ.taskId,
                occurrenceId = occId,
                originalDeadline = occ.deadlineAt
            )
        )
        recoveryDao.insert(
            RecoveryQuestEntity(
                taskId = occ.taskId,
                originOccurrenceId = occId,
                requiredEp = 10,
                earnedEp = 0
            )
        )
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
}
