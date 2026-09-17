package com.neurasamu.build.solo_leveling_tasker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.solo_leveling_tasker.TaskerApp
import com.neurasamu.build.solo_leveling_tasker.data.model.*
import com.neurasamu.build.solo_leveling_tasker.data.profile.ProfileData
import com.neurasamu.build.solo_leveling_tasker.data.profile.ProfileRepository
import com.neurasamu.build.solo_leveling_tasker.worker.AlarmScheduler
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
                            epReward = task.difficulty.epReward,
                            offsetMinutes = task.reminderOffsetMinutes
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
                title = task.title,
                epReward = task.difficulty.epReward,
                offsetMinutes = task.reminderOffsetMinutes
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
                title = task.title,
                epReward = task.difficulty.epReward,
                offsetMinutes = task.reminderOffsetMinutes
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

    fun cloneDayTask(taskId: Long) = viewModelScope.launch {
        val result = repo.cloneDayTask(taskId) ?: return@launch
        val (_, occId) = result
        val task = repo.getTask(taskId) ?: return@launch
        val occ = repo.getOccurrence(occId) ?: return@launch
        com.neurasamu.build.solo_leveling_tasker.worker.AlarmScheduler.scheduleReminder(
            ctx = appCtx, occurrenceId = occId, triggerAt = occ.scheduledAt,
            title = task.title, epReward = task.difficulty.epReward,
            offsetMinutes = task.reminderOffsetMinutes
        )
    }

    fun cloneDeadlineTask(taskId: Long) = viewModelScope.launch {
        val result = repo.cloneDeadlineTask(taskId) ?: return@launch
        val (_, occs) = result
        val task = repo.getTask(taskId) ?: return@launch
        val now = System.currentTimeMillis()
        occs.forEach { occ ->
            if (occ.scheduledAt > now) {
                com.neurasamu.build.solo_leveling_tasker.worker.AlarmScheduler.scheduleReminder(
                    ctx = appCtx, occurrenceId = occ.id, triggerAt = occ.scheduledAt,
                    title = task.title, epReward = task.difficulty.epReward,
                    offsetMinutes = task.reminderOffsetMinutes
                )
            }
        }
    }

    fun resetAll(onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.resetAll()
        onDone()
    }

    // ---- App Blocker ----
    fun blockedPackages(): Flow<Set<String>> =
        (getApplication() as TaskerApp).blockedAppsRepository.blockedPackages
    fun strictMode(): Flow<Boolean> =
        (getApplication() as TaskerApp).blockedAppsRepository.strictMode
    fun toggleBlockedApp(pkg: String, blocked: Boolean) = viewModelScope.launch {
        (getApplication() as TaskerApp).blockedAppsRepository.toggleApp(pkg, blocked)
    }
    fun setStrictMode(on: Boolean) = viewModelScope.launch {
        (getApplication() as TaskerApp).blockedAppsRepository.setStrictMode(on)
    }

    fun saveProfile(p: ProfileData) = viewModelScope.launch { profileRepo.save(p) }
    fun currentProfile(): ProfileData = _profile.value
}
