package com.neurasamu.build.solo_leveling_tasker.data.repo

import android.content.Context
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import com.neurasamu.build.solo_leveling_tasker.data.model.*
import com.neurasamu.build.solo_leveling_tasker.domain.GamificationEngine
import com.neurasamu.build.solo_leveling_tasker.domain.PenaltyEngine
import com.neurasamu.build.solo_leveling_tasker.domain.RecurrenceHelper
import com.neurasamu.build.solo_leveling_tasker.worker.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

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
    private val alarmDao = db.alarmDao()

    private var lastFocusLockActive = false
    private val saveInFlight = AtomicBoolean(false)

    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeActive()
    fun observeOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeAllOccurrences(from: Long, to: Long) = occDao.observeRange(from, to)
    fun observeActiveDebts() = debtDao.observeActive()
    fun observeActiveRecoveries() = recoveryDao.observeActive()
    fun observeStats() = statsDao.observe()
    fun observeOccurrencesForTask(taskId: Long) = occDao.observeForTask(taskId)

    suspend fun createTaskWithOccurrence(task: TaskEntity, scheduledAt: Long, deadlineAt: Long): Pair<Long, List<OccurrenceEntity>>? {
        if (!saveInFlight.compareAndSet(false, true)) return null
        try {
            val fixedTask = task.copy(difficulty = Difficulty.EASY)
            val taskId = taskDao.insert(fixedTask)
            val created = mutableListOf<OccurrenceEntity>()
            val durationMs = (deadlineAt - scheduledAt).coerceAtLeast(0L)

            // Build schedule: first occurrence + repeated (if repeatRule != NEVER)
            val schedule = mutableListOf<Long>()
            schedule.add(scheduledAt)
            if (task.repeatRule != RepeatRule.NEVER) {
                val horizonMs = scheduledAt + 30L * 24 * 3600 * 1000
                var cursor = scheduledAt
                var guard = 0
                while (guard < 60) {
                    val next = RecurrenceHelper.nextOccurrence(cursor, task.repeatRule) ?: break
                    if (next > horizonMs) break
                    schedule.add(next)
                    cursor = next
                    guard++
                }
            }

            schedule.forEach { when_ ->
                val id = occDao.insert(
                    OccurrenceEntity(
                        taskId = taskId,
                        scheduledAt = when_,
                        deadlineAt = when_ + durationMs,
                        durationMinutes = task.durationMinutes,
                        status = OccurrenceStatus.PENDING
                    )
                )
                created.add(
                    OccurrenceEntity(
                        id = id,
                        taskId = taskId,
                        scheduledAt = when_,
                        deadlineAt = when_ + durationMs,
                        durationMinutes = task.durationMinutes,
                        status = OccurrenceStatus.PENDING
                    )
                )
                eventDao.insert(EventEntity(occurrenceId = id, taskId = taskId, type = EventType.CREATED))
            }
            return taskId to created
        } finally {
            saveInFlight.set(false)
        }
    }

    suspend fun createDeadlineTaskWithDays(
        task: TaskEntity,
        startMillis: Long,
        endMillis: Long
    ): Pair<Long, List<OccurrenceEntity>>? {
        if (!saveInFlight.compareAndSet(false, true)) return null
        try {
            val zone = ZoneId.systemDefault()
            val startZ = Instant.ofEpochMilli(startMillis).atZone(zone)
            val startDate = startZ.toLocalDate()
            val dailyTime = startZ.toLocalTime()   // daily reminder time
            val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
            val (from, to) = if (startDate.isAfter(endDate)) endDate to startDate else startDate to endDate
            val totalDays = ChronoUnit.DAYS.between(from, to).toInt() + 1

            val diff = when {
                totalDays <= 7 -> Difficulty.EASY
                totalDays <= 31 -> Difficulty.NORMAL
                else -> Difficulty.HARD
            }

            val taskId = taskDao.insert(task.copy(difficulty = diff))
            val createdOccurrences = mutableListOf<OccurrenceEntity>()

            var current = from
            var dayIndex = 1
            while (!current.isAfter(to) && dayIndex <= 3650) {
                // scheduledAt = current date + daily reminder time (e.g., 07:00)
                val scheduledInstant = current.atTime(dailyTime).atZone(zone).toInstant().toEpochMilli()
                // deadlineAt = end of that day (23:59:59)
                val deadlineInstant = current.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

                val occ = OccurrenceEntity(
                    taskId = taskId,
                    scheduledAt = scheduledInstant,
                    deadlineAt = deadlineInstant,
                    durationMinutes = 0,
                    status = OccurrenceStatus.PENDING
                )
                val id = occDao.insert(occ)
                val saved = occ.copy(id = id)
                createdOccurrences.add(saved)

                eventDao.insert(
                    EventEntity(
                        occurrenceId = id, taskId = taskId,
                        type = EventType.CREATED, note = "day_$dayIndex"
                    )
                )
                current = current.plusDays(1)
                dayIndex++
            }
            return taskId to createdOccurrences
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
    suspend fun countMissed(): Int = occDao.countMissed()

    /**
     * Complete an occurrence.
     *
     * Reward rules:
     * - Normal: base EP for the task, HP += floor(epGained / 5)
     * - Recovery mode (health < 50 AND there are other MISSED occurrences):
     *   EP → forced cycle fill (→ level up)
     *   HP  → refill share: (100 - health) / pendingRecoveryCount
     */
    suspend fun completeOccurrence(occId: Long, completedAt: Long = System.currentTimeMillis()): Boolean {
        val occ = occDao.getById(occId) ?: return false
        if (occ.status == OccurrenceStatus.COMPLETED ||
            occ.status == OccurrenceStatus.LATE ||
            occ.status == OccurrenceStatus.RECOVERED) return false

        val task = taskDao.getById(occ.taskId) ?: return false
        val stats = statsDao.get() ?: PlayerStatsEntity()

        val minutesLate = ((completedAt - occ.deadlineAt) / 60000L).toInt()
        val baseEp = task.difficulty.epReward

        val recoveryMode = stats.health < 50
        val missedCount = occDao.countMissed().coerceAtLeast(1)

        val hpOverride: Int?
        val forceFill: Boolean
        if (recoveryMode) {
            // HP gain: split the deficit across remaining missed tasks
            val deficit = (100 - stats.health)
            hpOverride = (deficit / missedCount).coerceAtLeast(1)
            forceFill = true
        } else {
            hpOverride = null
            forceFill = false
        }

        val (newStats, epResult) = GamificationEngine.applyCompletion(
            stats = stats,
            baseEp = baseEp,
            minutesLate = minutesLate,
            hpOverride = hpOverride,
            forceCycleFill = forceFill
        )

        occDao.update(
            occ.copy(
                status = if (minutesLate > 0) OccurrenceStatus.LATE else OccurrenceStatus.COMPLETED,
                completedAt = completedAt
            )
        )
        statsDao.upsert(newStats)
        // Cancel focus lock notifications if health recovered
        if (newStats.health >= 55) {
            NotificationHelper.cancelFocusLock(appContext)
            NotificationHelper.cancelHealthWarning(appContext)
        }
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

    suspend fun uncompleteOccurrence(occId: Long): Boolean {
        val occ = occDao.getById(occId) ?: return false
        if (occ.status != OccurrenceStatus.COMPLETED &&
            occ.status != OccurrenceStatus.LATE &&
            occ.status != OccurrenceStatus.RECOVERED) return false

        val task = taskDao.getById(occ.taskId) ?: return false
        val stats = statsDao.get() ?: PlayerStatsEntity()

        val events = eventDao.recentForOccurrence(occId)
        val completionEvent = events.firstOrNull { it.type == EventType.COMPLETED }
        val epAwarded = completionEvent?.epDelta ?: 0
        val hpAwarded = completionEvent?.healthDelta ?: 0

        // Rollback EP cycle-aware
        var newEp = stats.ep - epAwarded
        var newLevel = stats.level
        while (newEp < 0 && newLevel > 1) {
            newLevel--
            newEp += 90
        }
        newEp = newEp.coerceAtLeast(0)

        val newHp = (stats.health - hpAwarded).coerceIn(0, 100)

        val newStats = stats.copy(
            ep = newEp,
            health = newHp,
            level = newLevel,
            totalCompleted = (stats.totalCompleted - 1).coerceAtLeast(0)
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

    /** Start a critical task: records startedAt + criticalEndsAt, locks apps. */
    suspend fun startCriticalOccurrence(occId: Long, now: Long = System.currentTimeMillis()): Boolean {
        val occ = occDao.getById(occId) ?: return false
        val task = taskDao.getById(occ.taskId) ?: return false
        if (task.priority != Priority.CRITICAL) return false
        if (task.criticalTimerMinutes <= 0) return false
        if (occ.status != OccurrenceStatus.PENDING) return false

        val stats = statsDao.get() ?: PlayerStatsEntity()
        if (stats.criticalActiveOccurrenceId != 0L) return false  // already active

        val endsAt = now + task.criticalTimerMinutes * 60_000L
        occDao.update(occ.copy(startedAt = now, criticalEndsAt = endsAt))
        statsDao.upsert(stats.copy(criticalActiveOccurrenceId = occId))
        eventDao.insert(
            EventEntity(
                occurrenceId = occId, taskId = task.id, type = EventType.CREATED,
                note = "critical_started", timestamp = now
            )
        )
        NotificationHelper.showCriticalStarted(appContext, task.title, endsAt)
        return true
    }

    /** Auto-complete a critical task when timer finishes; release lock. */
    suspend fun finishCriticalOccurrence(occId: Long, now: Long = System.currentTimeMillis()): Boolean {
        val occ = occDao.getById(occId) ?: return false
        val task = taskDao.getById(occ.taskId) ?: return false
        val stats = statsDao.get() ?: PlayerStatsEntity()

        val minutesLate = ((now - occ.deadlineAt) / 60000L).toInt()
        val baseEp = task.difficulty.epReward
        val (newStats, epResult) = GamificationEngine.applyCompletion(stats, baseEp, minutesLate)

        occDao.update(
            occ.copy(
                status = if (minutesLate > 0) OccurrenceStatus.LATE else OccurrenceStatus.COMPLETED,
                completedAt = now,
                criticalEndsAt = null
            )
        )
        statsDao.upsert(newStats.copy(criticalActiveOccurrenceId = 0L))
        eventDao.insert(
            EventEntity(
                occurrenceId = occId, taskId = task.id, type = EventType.COMPLETED,
                epDelta = epResult.epGained, healthDelta = epResult.healthGained,
                timestamp = now, note = "critical_finished"
            )
        )
        NotificationHelper.showCriticalFinished(appContext, task.title)
        return true
    }

    /** Called by ticker: if critical timer elapsed, auto-finish. */
    suspend fun checkCriticalTimer(now: Long = System.currentTimeMillis()): Boolean {
        val stats = statsDao.get() ?: return false
        val occId = stats.criticalActiveOccurrenceId
        if (occId == 0L) return false
        val occ = occDao.getById(occId) ?: run {
            statsDao.upsert(stats.copy(criticalActiveOccurrenceId = 0L))
            return false
        }
        val endsAt = occ.criticalEndsAt ?: return false
        if (now >= endsAt) {
            finishCriticalOccurrence(occId, endsAt)
            return true
        }
        return false
    }

    /** Is a critical task currently locking the device? */
    suspend fun isCriticalActive(): Boolean {
        val stats = statsDao.get() ?: return false
        return stats.criticalActiveOccurrenceId != 0L
    }

    /** Clone a day task. */
    suspend fun cloneDayTask(taskId: Long): Pair<Long, Long>? {
        val task = taskDao.getById(taskId) ?: return null
        val occ = occDao.getLatestForTask(taskId) ?: return null
        val now = System.currentTimeMillis()
        // next slot: tomorrow same time OR 24h after last scheduled
        val base = maxOf(occ.scheduledAt, now)
        val dayMs = 24L * 3600 * 1000
        val newScheduled = base + dayMs
        val durationMs = occ.deadlineAt - occ.scheduledAt
        val newDeadline = newScheduled + durationMs
        val newTask = task.copy(id = 0L, createdAt = now)
        val newTaskId = taskDao.insert(newTask)
        val newOccId = occDao.insert(
            OccurrenceEntity(
                taskId = newTaskId,
                scheduledAt = newScheduled,
                deadlineAt = newDeadline,
                durationMinutes = newTask.durationMinutes,
                status = OccurrenceStatus.PENDING
            )
        )
        eventDao.insert(EventEntity(occurrenceId = newOccId, taskId = newTaskId, type = EventType.CREATED, note = "cloned_from_$taskId"))
        return newTaskId to newOccId
    }

    /** Clone a deadline task: shifts all occurrences forward by the span length. */
    suspend fun cloneDeadlineTask(taskId: Long): Pair<Long, List<OccurrenceEntity>>? {
        val task = taskDao.getById(taskId) ?: return null
        val occs = occDao.getAllForTask(taskId)
        if (occs.isEmpty()) return null
        val spanMs = occs.last().deadlineAt - occs.first().scheduledAt
        val shiftMs = spanMs + 24L * 3600 * 1000  // span + 1 day gap
        val now = System.currentTimeMillis()
        val newTask = task.copy(id = 0L, createdAt = now)
        val newTaskId = taskDao.insert(newTask)
        val created = mutableListOf<OccurrenceEntity>()
        for (occ in occs) {
            val id = occDao.insert(
                OccurrenceEntity(
                    taskId = newTaskId,
                    scheduledAt = occ.scheduledAt + shiftMs,
                    deadlineAt = occ.deadlineAt + shiftMs,
                    durationMinutes = occ.durationMinutes,
                    status = OccurrenceStatus.PENDING
                )
            )
            val saved = occ.copy(id = id, taskId = newTaskId, scheduledAt = occ.scheduledAt + shiftMs, deadlineAt = occ.deadlineAt + shiftMs, status = OccurrenceStatus.PENDING, completedAt = null, penaltyAppliedCount = 0)
            created.add(saved)
        }
        eventDao.insert(EventEntity(occurrenceId = 0L, taskId = newTaskId, type = EventType.CREATED, note = "cloned_from_$taskId"))
        return newTaskId to created
    }

    // ---- Alarms ----
    fun observeAlarms(): Flow<List<AlarmEntity>> = alarmDao.observeAll()
    suspend fun getAlarm(id: Long) = alarmDao.getById(id)
    suspend fun insertAlarm(a: AlarmEntity): Long = alarmDao.insert(a)
    suspend fun updateAlarm(a: AlarmEntity) { alarmDao.update(a) }
    suspend fun deleteAlarm(id: Long) { alarmDao.deleteById(id) }
    suspend fun getAllEnabledAlarms(): List<AlarmEntity> = alarmDao.getAllEnabled()

    /** Reset everything to default. */
    suspend fun resetAll() {
        occDao.deleteAll()
        taskDao.deleteAll()
        eventDao.deleteAll()
        debtDao.deleteAll()
        alarmDao.deleteAll()
        recoveryDao.deleteAll()
        statsDao.upsert(PlayerStatsEntity(cycleStartedAt = System.currentTimeMillis()))
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
        // Health crossed DOWN below 50 → warn + lock
        if (before.health >= 50 && after.health < 50) {
            NotificationHelper.showHealthWarning(appContext, after.health)
        }
        if (!lastFocusLockActive && after.focusLockActive) {
            lastFocusLockActive = true
            NotificationHelper.showFocusLockActivated(appContext, after.health)
        }
        // Health recovered above threshold → cancel persistent Focus Lock notification
        if (before.health < 55 && after.health >= 55) {
            lastFocusLockActive = false
            NotificationHelper.cancelFocusLock(appContext)
            NotificationHelper.cancelHealthWarning(appContext)
        }
    }
}
