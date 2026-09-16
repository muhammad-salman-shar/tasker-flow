package com.taskerflow.app.ui.home

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.domain.GamificationEngine
import com.taskerflow.app.ui.MainViewModel
import com.taskerflow.app.ui.components.TaskCard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    val stats = state.stats
    val (prog, total) = GamificationEngine.levelProgress(stats.ep)
    val completed = state.todayOccurrences.count {
        it.status == OccurrenceStatus.COMPLETED ||
        it.status == OccurrenceStatus.LATE ||
        it.status == OccurrenceStatus.RECOVERED
    }
    val totalToday = state.todayOccurrences.size

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))

        // Header Health + EP
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("❤️ ${stats.health}%", "HEALTH", Color(0xFFEF5350))
            StatChip("⚡ ${stats.ep}", "EP", Color(0xFFFFC107))
        }
        Spacer(Modifier.height(12.dp))

        // Level card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("LEVEL ${stats.level}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else prog.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFFFFC107),
                    trackColor = Color(0xFF2A2A32)
                )
                Spacer(Modifier.height(6.dp))
                Text("$prog / $total EP", color = Color(0xFF9E9E9E), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Today card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TODAY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (stats.streak > 0) {
                        Text("🔥 ${stats.streak}d", color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("$completed / $totalToday Completed", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (totalToday == 0) 0f else completed.toFloat() / totalToday.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF66BB6A),
                    trackColor = Color(0xFF2A2A32)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Recovery banner
        if (state.activeRecoveries.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1313)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("⚔️ RECOVERY ACTIVE", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "${state.activeRecoveries.size} quest(s) pending",
                        color = Color(0xFFFFAB91), fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Tasks header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR TASKS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text("${state.todayOccurrences.size} today", color = Color(0xFF9E9E9E), fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))

        if (state.todayOccurrences.isEmpty()) {
            EmptyTasks()
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(state.todayOccurrences, key = { it.id }) { occ ->
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
private fun EmptyTasks() {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎯", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("NO QUESTS YET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Your first task starts your journey.",
            color = Color(0xFF9E9E9E), fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text("Tap ＋ to begin", color = Color(0xFFFFC107), fontSize = 12.sp)
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF9E9E9E), fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
