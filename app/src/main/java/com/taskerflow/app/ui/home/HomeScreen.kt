package com.taskerflow.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
        Spacer(Modifier.height(12.dp))

        // ---- Health & EP top row (gradient pills) ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientStat(
                modifier = Modifier.weight(1f),
                icon = "❤️",
                value = "${stats.health}%",
                label = "HEALTH",
                gradient = listOf(Color(0xFFFF1744), Color(0xFFD81B60))
            )
            GradientStat(
                modifier = Modifier.weight(1f),
                icon = "⚡",
                value = "${stats.ep}",
                label = "EP",
                gradient = listOf(Color(0xFFFFD54F), Color(0xFFFFA000))
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- Level card ----
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LEVEL", color = Color(0xFF9E9E9E), fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${stats.level}",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.weight(1f))
                    Text("$prog / $total EP", color = Color(0xFFFFC107), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                GradientProgressBar(
                    progress = if (total == 0) 0f else prog.toFloat() / total,
                    gradient = listOf(Color(0xFFFFD54F), Color(0xFFFF6F00))
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Today card ----
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("TODAY", color = Color(0xFF9E9E9E), fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    if (stats.streak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("${stats.streak}d", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$completed",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        " / $totalToday",
                        color = Color(0xFF9E9E9E),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text("Completed", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                GradientProgressBar(
                    progress = if (totalToday == 0) 0f else completed.toFloat() / totalToday,
                    gradient = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Recovery banner ----
        if (state.activeRecoveries.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1313)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔️", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("RECOVERY ACTIVE", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Text("${state.activeRecoveries.size} quest(s) pending", color = Color(0xFFFFAB91), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Tasks header ----
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR QUESTS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${state.todayOccurrences.size} today", color = Color(0xFF9E9E9E), fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))

        if (state.todayOccurrences.isEmpty()) {
            EmptyTasks()
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 90.dp)) {
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
private fun GradientStat(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    gradient: List<Color>
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun GradientProgressBar(progress: Float, gradient: List<Color>) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progress"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF23232B))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(gradient))
        )
    }
}

@Composable
private fun EmptyTasks() {
    Column(
        Modifier.fillMaxWidth().padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎯", fontSize = 56.sp)
        Spacer(Modifier.height(14.dp))
        Text("NO QUESTS YET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Your first task starts your journey.",
            color = Color(0xFF9E9E9E), fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text("Tap ＋ to begin", color = Color(0xFFFFC107), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
