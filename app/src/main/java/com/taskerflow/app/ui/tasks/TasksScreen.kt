package com.taskerflow.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.data.model.TaskEntity
import com.taskerflow.app.data.model.TaskType
import com.taskerflow.app.ui.MainViewModel
import com.taskerflow.app.ui.components.DeadlineTaskCard
import com.taskerflow.app.ui.components.TaskCard
import java.text.SimpleDateFormat
import java.util.*

enum class TaskFilter(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    RECOVERY("Recovery")
}

@Composable
fun TasksScreen(vm: MainViewModel, onEdit: (Long) -> Unit = {}) {
    val state by vm.homeState.collectAsState()
    var filter by remember { mutableStateOf(TaskFilter.TODAY) }
    var actionTask by remember { mutableStateOf<TaskEntity?>(null) }

    val all = state.allOccurrences
    val now = System.currentTimeMillis()

    val filtered: List<OccurrenceEntity> = when (filter) {
        TaskFilter.TODAY -> all.filter {
            (it.status == OccurrenceStatus.PENDING || it.status == OccurrenceStatus.LATE) &&
            it.scheduledAt <= now + 24 * 3600 * 1000
        }
        TaskFilter.UPCOMING -> all.filter {
            it.status == OccurrenceStatus.PENDING && it.scheduledAt > now
        }
        TaskFilter.OVERDUE -> all.filter { it.status == OccurrenceStatus.MISSED }
        TaskFilter.COMPLETED -> all.filter {
            it.status == OccurrenceStatus.COMPLETED || it.status == OccurrenceStatus.RECOVERED
        }
        TaskFilter.RECOVERY -> emptyList()
    }

    // Group by taskId
    val grouped: Map<Long, List<OccurrenceEntity>> = filtered.groupBy { it.taskId }

    val completedCount = all.count {
        it.status == OccurrenceStatus.COMPLETED || it.status == OccurrenceStatus.RECOVERED
    }
    val activeCount = all.count {
        it.status == OccurrenceStatus.PENDING || it.status == OccurrenceStatus.LATE || it.status == OccurrenceStatus.MISSED
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("All Tasks", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "$activeCount / ${activeCount + completedCount} Active",
                    color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilter.values().forEach { f ->
                val selected = filter == f
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100)))
                            else Brush.horizontalGradient(listOf(Color(0xFF14141A), Color(0xFF14141A)))
                        )
                        .clickable { filter = f }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        f.label,
                        color = if (selected) Color.White else Color(0xFF79829C),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (filter == TaskFilter.RECOVERY) {
            RecoveryList(vm)
        } else if (grouped.isEmpty()) {
            EmptyState(filter)
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 90.dp)) {
                grouped.forEach { (taskId, occs) ->
                    val task = state.tasksById[taskId] ?: return@forEach
                    item(key = "task_$taskId") {
                        if (task.taskType == TaskType.DEADLINE && occs.size > 1) {
                            DeadlineTaskCard(
                                title = task.title,
                                category = task.category.name,
                                occurrences = occs,
                                epReward = task.difficulty.epReward,
                                onCompleteDay = { occId -> vm.completeOccurrence(occId) },
                                onDayTap = { _ -> actionTask = task }
                            )
                        } else {
                            val occ = occs.first()
                            TaskCard(
                                title = task.title,
                                category = task.category.name,
                                timeText = formatTime(occ.scheduledAt),
                                epText = "+${task.difficulty.epReward} EP",
                                status = occ.status,
                                onClick = { actionTask = task },
                                onComplete = { vm.completeOccurrence(occ.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    actionTask?.let { task ->
        AlertDialog(
            onDismissRequest = { actionTask = null },
            containerColor = Color(0xFF1C1C24),
            title = { Text(task.title, color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("What would you like to do?", color = Color(0xFF9E9E9E)) },
            confirmButton = {
                TextButton(onClick = {
                    actionTask = null
                    onEdit(task.id)
                }) { Text("EDIT", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.deleteTask(task.id)
                    actionTask = null
                }) { Text("DELETE", color = Color(0xFFFF3D57), fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
private fun RecoveryList(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    if (state.activeRecoveries.isEmpty()) {
        EmptyState(TaskFilter.RECOVERY)
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 90.dp)) {
            items(state.activeRecoveries, key = { "r${it.id}" }) { r ->
                val t = state.tasksById[r.taskId]
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1313)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("⚔️ RECOVERY QUEST", color = Color(0xFFFF3D57), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(t?.title ?: "Recovery task", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("EP ${r.earnedEp} / ${r.requiredEp}", color = Color(0xFFFFAB91), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    val (emoji, title, sub) = when (filter) {
        TaskFilter.TODAY -> Triple("🎯", "NOTHING FOR TODAY", "Create a task to start.")
        TaskFilter.UPCOMING -> Triple("📅", "NO UPCOMING", "Plan ahead and add tasks.")
        TaskFilter.OVERDUE -> Triple("✅", "NO OVERDUE", "You're on top of it.")
        TaskFilter.COMPLETED -> Triple("🏆", "NOTHING COMPLETED YET", "Complete your first quest.")
        TaskFilter.RECOVERY -> Triple("🛡️", "NO RECOVERIES", "You're all caught up.")
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(sub, color = Color(0xFF9E9E9E), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
