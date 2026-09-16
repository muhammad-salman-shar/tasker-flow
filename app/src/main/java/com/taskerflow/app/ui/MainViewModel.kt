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

    val homeState: StateFlow<HomeUiState> = combine(
        _stats.filterNotNull(),
        repo.observeOccurrences(todayRange.first, todayRange.second),
        repo.observeTasks(),
        repo.observeActiveDebts(),
        repo.observeActiveRecoveries()
    ) { stats, occs, tasks, debts, recoveries ->
        HomeUiState(
            stats = stats,
            todayOccurrences = occs,
            tasksById = tasks.associateBy { it.id },
            activeDebts = debts,
            activeRecoveries = recoveries
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun completeOccurrence(id: Long) = viewModelScope.launch { repo.completeOccurrence(id) }

    fun createTask(task: TaskEntity, scheduledAt: Long, deadlineAt: Long) = viewModelScope.launch {
        repo.createTaskWithOccurrence(task, scheduledAt, deadlineAt)
    }

    fun snoozeOccurrence(id: Long, minutes: Int) = viewModelScope.launch {
        val occ = repo.getOccurrence(id) ?: return@launch
        val newSched = occ.scheduledAt + minutes * 60_000L
        val newDeadline = occ.deadlineAt + minutes * 60_000L
        repo.snoozeOccurrence(id, newSched, newDeadline)
    }

    fun levelProgress(): Pair<Int, Int> {
        val ep = homeState.value.stats.ep
        return GamificationEngine.levelProgress(ep)
    }
}
