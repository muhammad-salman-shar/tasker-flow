package com.taskerflow.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.*
import com.taskerflow.app.domain.GamificationEngine
import com.taskerflow.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    val stats = state.stats
    val (prog, total) = GamificationEngine.levelProgress(stats.ep)
    val completed = state.todayOccurrences.count { it.status == OccurrenceStatus.COMPLETED || it.status == OccurrenceStatus.LATE }
    val totalToday = state.todayOccurrences.size

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Header: Health + EP + Level
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("❤️ ${stats.health}%", "HEALTH", Color(0xFFE53935))
            StatChip("⚡ ${stats.ep}", "EXECUTION", Color(0xFFFFC107))
        }
        Spacer(Modifier.height(16.dp))
        Text("LEVEL ${stats.level}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else prog.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 8.dp),
            color = Color(0xFFFFC107),
            trackColor = Color(0xFF2A2A32)
        )
        Text("$prog / $total EP", color = Color.Gray, fontSize = 12.sp)

        Spacer(Modifier.height(20.dp))
        Text("TODAY", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("$completed / $totalToday Tasks Completed", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (totalToday == 0) 0f else completed.toFloat() / totalToday.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF2A2A32)
        )
        if (stats.streak > 0) {
            Spacer(Modifier.height(8.dp))
            Text("🔥 ${stats.streak} Day Streak", color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
        Text("YOUR TASKS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(state.todayOccurrences, key = { it.id }) { occ ->
                val task = state.tasksById[occ.taskId]
                TaskRow(
                    title = task?.title ?: "(deleted)",
                    timeText = formatTime(occ.scheduledAt),
                    status = occ.status,
                    onComplete = { vm.completeOccurrence(occ.id) }
                )
            }
            if (state.activeRecoveries.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("⚠️ RECOVERY", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
                items(state.activeRecoveries, key = { "r${it.id}" }) { r ->
                    val t = state.tasksById[r.taskId]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(t?.title ?: "Recovery task", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("EP ${r.earnedEp} / ${r.requiredEp}", color = Color(0xFFFFAB91), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun TaskRow(title: String, timeText: String, status: OccurrenceStatus, onComplete: () -> Unit) {
    val done = status == OccurrenceStatus.COMPLETED || status == OccurrenceStatus.LATE
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (done) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                Text("$timeText • ${status.name}", color = Color.Gray, fontSize = 11.sp)
            }
            if (!done) {
                TextButton(onClick = onComplete) { Text("✓", color = Color(0xFFFFC107), fontSize = 18.sp) }
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
