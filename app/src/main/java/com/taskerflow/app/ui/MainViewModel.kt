package com.taskerflow.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskerflow.app.TaskerApp
import com.taskerflow.app.data.model.*
import com.taskerflow.app.domain.GamificationEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val stats: PlayerStatsEntity = PlayerStatsEntity(),
    val todayOccurrences: List<OccurrenceEntity> = emptyList(),
    val allOccurrences: List<OccurrenceEntity> = emptyList(),
    val tasksById: Map<Long, TaskEntity> = emptyMap(),
    val activeDebts: List<TaskDebtEntity> = emptyList(),
    val activeRecoveries: List<RecoveryQuestEntity> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TaskerApp).repository

    private val _stats = repo.observeStats().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val todayRange: Pair<Long, Long> = run {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
        start to c.timeInMillis
    }

    // Wide range for tasks list (30 days back, 30 days forward)
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
        repo.observeActiveRecoveries()
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val stats = arr[0] as PlayerStatsEntity
        val today = arr[1] as List<OccurrenceEntity>
        val all = arr[2] as List<OccurrenceEntity>
        val tasks = arr[3] as List<TaskEntity>
        val debts = arr[4] as List<TaskDebtEntity>
        val recoveries = arr[5] as List<RecoveryQuestEntity>
        HomeUiState(
            stats = stats,
            todayOccurrences = today,
            allOccurrences = all,
            tasksById = tasks.associateBy { it.id },
            activeDebts = debts,
            activeRecoveries = recoveries
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun completeOccurrence(id: Long) = viewModelScope.launch { repo.completeOccurrence(id) }

    fun createTask(task: TaskEntity, scheduledAt: Long, deadlineAt: Long) = viewModelScope.launch {
        repo.createTaskWithOccurrence(task, scheduledAt, deadlineAt)
    }

    fun updateTask(task: TaskEntity) = viewModelScope.launch { repo.updateTask(task) }

    fun deleteTask(taskId: Long) = viewModelScope.launch { repo.deleteTask(taskId) }

    fun snoozeOccurrence(id: Long, minutes: Int) = viewModelScope.launch {
        val occ = repo.getOccurrence(id) ?: return@launch
        val newSched = occ.scheduledAt + minutes * 60_000L
        val newDeadline = occ.deadlineAt + minutes * 60_000L
        repo.snoozeOccurrence(id, newSched, newDeadline)
    }

    fun getTaskById(id: Long): TaskEntity? = homeState.value.tasksById[id]
}
