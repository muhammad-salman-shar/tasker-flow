package com.taskerflow.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.ui.MainViewModel
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
fun TasksScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    var filter by remember { mutableStateOf(TaskFilter.TODAY) }

    // Local all-occurrences fetch (today window from VM). For simplicity we use homeState.
    val allOccs = state.todayOccurrences
    val now = System.currentTimeMillis()

    val filtered: List<OccurrenceEntity> = when (filter) {
        TaskFilter.TODAY -> allOccs.filter {
            it.status == OccurrenceStatus.PENDING || it.status == OccurrenceStatus.LATE
        }
        TaskFilter.UPCOMING -> allOccs.filter {
            it.status == OccurrenceStatus.PENDING && it.scheduledAt > now
        }
        TaskFilter.OVERDUE -> allOccs.filter {
            it.status == OccurrenceStatus.MISSED
        }
        TaskFilter.COMPLETED -> allOccs.filter {
            it.status == OccurrenceStatus.COMPLETED || it.status == OccurrenceStatus.RECOVERED
        }
        TaskFilter.RECOVERY -> emptyList() // handled separately below
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("All Tasks", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // Filter chips
        ScrollableTabRow(
            selectedTabIndex = filter.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFFC107),
            divider = {}
        ) {
            TaskFilter.values().forEach { f ->
                Tab(
                    selected = filter == f,
                    onClick = { filter = f },
                    text = { Text(f.label, fontSize = 13.sp) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filter == TaskFilter.RECOVERY) {
            RecoveryList(vm)
        } else if (filtered.isEmpty()) {
            EmptyState(filter)
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                items(filtered, key = { it.id }) { occ ->
                    val task = state.tasksById[occ.taskId]
                    val timeText = formatTime(occ.scheduledAt)
                    val ep = task?.difficulty?.epReward ?: 10
                    TaskCard(
                        title = task?.title ?: "(deleted)",
                        category = task?.category?.name ?: "OTHER",
                        timeText = timeText,
                        epText = "+$ep EP",
                        status = occ.status,
                        onComplete = { vm.completeOccurrence(occ.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoveryList(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    if (state.activeRecoveries.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().padding(top = 60.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("🛡️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("NO ACTIVE RECOVERIES", color = Color.White, fontWeight = FontWeight.Bold)
            Text("You're all caught up.", color = Color(0xFF9E9E9E), fontSize = 13.sp)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(state.activeRecoveries, key = { "r${it.id}" }) { r ->
                val t = state.tasksById[r.taskId]
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1313)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("⚔️ RECOVERY QUEST", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
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
