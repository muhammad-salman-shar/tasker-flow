package com.taskerflow.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import com.taskerflow.app.data.model.TaskEntity
import com.taskerflow.app.data.model.TaskType
import com.taskerflow.app.domain.GamificationEngine
import com.taskerflow.app.ui.MainViewModel
import com.taskerflow.app.ui.components.DeadlineTaskCard
import com.taskerflow.app.ui.components.LiveClock
import com.taskerflow.app.ui.components.TaskCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    val stats = state.stats
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // All occurrences of every task that still has work left (today or later)
    val now = System.currentTimeMillis()
    val todayEnd = now + 24L * 3600 * 1000

    // Active occurrences: not COMPLETED/RECOVERED and scheduled <= today end
    val activeOccs = state.allOccurrences.filter {
        it.status != OccurrenceStatus.COMPLETED &&
        it.status != OccurrenceStatus.LATE &&
        it.status != OccurrenceStatus.RECOVERED &&
    }

    // Group by task
    val activeByTask: Map<Long, List<OccurrenceEntity>> = activeOccs.groupBy { it.taskId }

    // Separate day vs deadline
    val dayTasks = mutableListOf<Pair<TaskEntity, OccurrenceEntity>>()
    val deadlineTasks = mutableListOf<Pair<TaskEntity, List<OccurrenceEntity>>>()

    activeByTask.forEach { (taskId, occs) ->
        val task = state.tasksById[taskId] ?: return@forEach
        // Grab ALL occurrences for this task (needed for deadline card to be accurate)
        val allForTask = state.allOccurrences.filter { it.taskId == taskId }
        if (task.taskType == TaskType.DEADLINE && allForTask.size > 1) {
            deadlineTasks.add(task to allForTask)
        } else {
            val occ = occs.minByOrNull { it.scheduledAt } ?: return@forEach
            dayTasks.add(task to occ)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                vm.refresh()
                delay(400)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Good day", color = Color(0xFF79829C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tasker Flow", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp)
                }
                LiveClock()
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalCard(
                    modifier = Modifier.weight(1f),
                    value = stats.health,
                    maxValue = 100,
                    numberText = "${stats.health}%",
                    label = "HEALTH",
                    statusText = when {
                        stats.health >= 80 -> "Optimal"
                        stats.health >= 50 -> "Stable"
                        stats.health >= 25 -> "Low"
                        else -> "Critical"
                    },
                    accent = Color(0xFFFF2A6D),
                    iconPath = "heart"
                )
                VitalCard(
                    modifier = Modifier.weight(1f),
                    value = GamificationEngine.levelProgress(stats.ep).first,
                    maxValue = 90,
                    numberText = "${GamificationEngine.levelProgress(stats.ep).first} / 90",
                    label = "EP • LVL ${stats.level}",
                    statusText = when {
                        stats.ep >= 100 -> "Surge Active"
                        stats.ep >= 50 -> "Charged"
                        stats.ep >= 20 -> "Steady"
                        else -> "Depleted"
                    },
                    accent = Color(0xFFFFB300),
                    iconPath = "bolt"
                )
            }

            Spacer(Modifier.height(18.dp))

            if (state.activeRecoveries.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1313)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔️", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("RECOVERY ACTIVE", color = Color(0xFFFF3D57), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text("${state.activeRecoveries.size} quest(s) pending", color = Color(0xFFFFAB91), fontSize = 11.sp)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR QUESTS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp, letterSpacing = 1.2.sp)
                Spacer(Modifier.weight(1f))
                Text("${dayTasks.size + deadlineTasks.size} active", color = Color(0xFF79829C), fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))

            if (dayTasks.isEmpty() && deadlineTasks.isEmpty()) {
                EmptyTasks()
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 90.dp)) {
                    items(dayTasks, key = { "day_${it.first.id}" }) { (task, occ) ->
                        TaskCard(
                            title = task.title,
                            category = task.category.name,
                            timeText = formatTime(occ.scheduledAt),
                            epText = "+${task.difficulty.epReward} EP",
                            status = occ.status,
                            onComplete = null
                        )
                    }
                    items(deadlineTasks, key = { "dl_${it.first.id}" }) { (task, occs) ->
                        DeadlineTaskCard(
                            title = task.title,
                            category = task.category.name,
                            occurrences = occs,
                            epReward = task.difficulty.epReward,
                            readOnly = true,
                            onCompleteDay = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalCard(
    modifier: Modifier,
    value: Int,
    maxValue: Int,
    numberText: String,
    label: String,
    statusText: String,
    accent: Color,
    iconPath: String
) {
    val progress = (value.toFloat() / maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progress, animationSpec = tween(900), label = "vital"
    )

    Box(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.14f), Color(0xFF121624).copy(alpha = 0.85f))
                )
            )
            .padding(vertical = 18.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(76.dp)) {
                    val stroke = 6.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Color(0xFF23232B), startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent, startAngle = -90f, sweepAngle = 360f * animated,
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(if (iconPath == "heart") "❤️" else "⚡", fontSize = 22.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(label, color = Color(0xFF79829C), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))
            Text(numberText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusText, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
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
        Text("Your first task starts your journey.", color = Color(0xFF79829C), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Tap ＋ to begin", color = Color(0xFFFFB300), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
