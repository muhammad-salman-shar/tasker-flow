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
        val end = c.timeInMillis + 30L * 24 * 3600 * 1000
        val start = c.timeInMillis - 30L * 24 * 3600 * 1000
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

    fun createTask(task: TaskEntity, scheduledAt: Long, deadlineAt: Long) = viewModelScope.launch {
        val (_, occId) = repo.createTaskWithOccurrence(task, scheduledAt, deadlineAt)
        AlarmScheduler.scheduleReminder(
            ctx = appCtx, occurrenceId = occId, triggerAt = scheduledAt,
            title = task.title, epReward = task.difficulty.epReward
        )
    }

    fun createDeadlineTask(task: TaskEntity, startMillis: Long, endMillis: Long) = viewModelScope.launch {
        val (_, occIds) = repo.createDeadlineTaskWithDays(task, startMillis, endMillis)
        // schedule reminder for day 1 only
        occIds.firstOrNull()?.let { firstOcc ->
            AlarmScheduler.scheduleReminder(
                ctx = appCtx, occurrenceId = firstOcc, triggerAt = startMillis,
                title = task.title, epReward = task.difficulty.epReward
            )
        }
    }

    fun updateTaskWithOccurrence(task: TaskEntity, scheduledAt: Long, deadlineAt: Long) = viewModelScope.launch {
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
