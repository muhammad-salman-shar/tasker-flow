package com.taskerflow.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskerflow.app.TaskerApp
import com.taskerflow.app.data.model.*
import com.taskerflow.app.data.profile.ProfileData
import com.taskerflow.app.data.profile.ProfileRepository
import com.taskerflow.app.worker.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val stats: PlayerStatsEntity = PlayerStatsEntity(),
    val todayOccurrences: List<OccurrenceEntity> = emptyList(),
    val allOccurrences: List<OccurrenceEntity> = emptyList(),
    val tasksById: Map<Long, TaskEntity> = emptyMap(),
    val activeDebts: List<TaskDebtEntity> = emptyList(),
    val activeRecoveries: List<RecoveryQuestEntity> = emptyList(),
    val profile: ProfileData = ProfileData()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TaskerApp).repository
    private val profileRepo: ProfileRepository = (app as TaskerApp).profileRepository
    private val appCtx = app.applicationContext

    private val _stats = repo.observeStats().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _profile = profileRepo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileData())

    private val todayRange: Pair<Long, Long> = run {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
        start to c.timeInMillis
    }

    private val wideRange: Pair<Long, Long> = run {
        val c = Calendar.getInstance()
        val end = c.timeInMillis + 500L * 24 * 3600 * 1000
        val start = c.timeInMillis - 500L * 24 * 3600 * 1000
        start to end
    }

    val homeState: StateFlow<HomeUiState> = combine(
        _stats.filterNotNull(),
        repo.observeOccurrences(todayRange.first, todayRange.second),
        repo.observeAllOccurrences(wideRange.first, wideRange.second),
        repo.observeTasks(),
        repo.observeActiveDebts(),
        repo.observeActiveRecoveries(),
        _profile
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            stats = arr[0] as PlayerStatsEntity,
            todayOccurrences = arr[1] as List<OccurrenceEntity>,
            allOccurrences = arr[2] as List<OccurrenceEntity>,
            tasksById = (arr[3] as List<TaskEntity>).associateBy { it.id },
            activeDebts = arr[4] as List<TaskDebtEntity>,
            activeRecoveries = arr[5] as List<RecoveryQuestEntity>,
            profile = arr[6] as ProfileData
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun observeOccurrencesForTask(taskId: Long): Flow<List<OccurrenceEntity>> =
        repo.observeOccurrencesForTask(taskId)

    fun completeOccurrence(id: Long) = viewModelScope.launch { repo.completeOccurrence(id) }
    fun uncompleteOccurrence(id: Long) = viewModelScope.launch { repo.uncompleteOccurrence(id) }

    suspend fun saveTaskAndWait(
        task: TaskEntity,
        scheduledAt: Long,
        deadlineAt: Long,
        isDeadline: Boolean,
        deadlineEndMillis: Long
    ): Boolean {
        return if (isDeadline) {
            val result = repo.createDeadlineTaskWithDays(task, scheduledAt, deadlineEndMillis)
                ?: return false
            val (_, occs) = result
            // Schedule reminders for EVERY day
            val now = System.currentTimeMillis()
            occs.forEach { occ ->
                if (occ.scheduledAt > now) {
                    AlarmScheduler.scheduleReminder(
                        ctx = appCtx,
                        occurrenceId = occ.id,
                        triggerAt = occ.scheduledAt,
                        title = task.title,
                        epReward = task.difficulty.epReward
                    )
                }
            }
            true
        } else {
            val result = repo.createTaskWithOccurrence(task, scheduledAt, deadlineAt)
                ?: return false
            val (_, occId) = result
            AlarmScheduler.scheduleReminder(
                ctx = appCtx, occurrenceId = occId, triggerAt = scheduledAt,
                title = task.title, epReward = task.difficulty.epReward
            )
            true
        }
    }

    suspend fun updateTaskAndWait(task: TaskEntity, scheduledAt: Long, deadlineAt: Long) {
        val occId = repo.updateTaskWithOccurrence(task, scheduledAt, deadlineAt)
        if (occId != null && scheduledAt > System.currentTimeMillis()) {
            AlarmScheduler.cancel(appCtx, occId)
            AlarmScheduler.scheduleReminder(
                ctx = appCtx, occurrenceId = occId, triggerAt = scheduledAt,
                title = task.title, epReward = task.difficulty.epReward
            )
        }
    }

    fun deleteTask(taskId: Long) = viewModelScope.launch { repo.deleteTask(taskId) }
    fun deleteOccurrence(occId: Long) = viewModelScope.launch { repo.deleteOccurrence(occId) }

    suspend fun fetchTask(id: Long): TaskEntity? = repo.getTask(id)
    suspend fun fetchLatestOccurrence(taskId: Long): OccurrenceEntity? = repo.getLatestOccurrenceForTask(taskId)

    fun snoozeOccurrence(id: Long, minutes: Int) = viewModelScope.launch {
        val occ = repo.getOccurrence(id) ?: return@launch
        val newSched = occ.scheduledAt + minutes * 60_000L
        val newDeadline = occ.deadlineAt + minutes * 60_000L
        repo.snoozeOccurrence(id, newSched, newDeadline)
    }

    fun getTaskById(id: Long): TaskEntity? = homeState.value.tasksById[id]

    fun refresh() = viewModelScope.launch { repo.forceRefresh() }

    fun saveProfile(p: ProfileData) = viewModelScope.launch { profileRepo.save(p) }
    fun currentProfile(): ProfileData = _profile.value
}
