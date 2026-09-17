package com.neurasamu.build.solo_leveling_tasker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceStatus
import kotlinx.coroutines.delay

@Composable
fun CriticalTaskCard(
    title: String,
    category: String,
    timeText: String,
    epText: String,
    status: OccurrenceStatus,
    occurrence: OccurrenceEntity,
    criticalTimerMinutes: Int,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val isDone = status == OccurrenceStatus.COMPLETED || status == OccurrenceStatus.LATE
    val isRunning = occurrence.startedAt != null && occurrence.criticalEndsAt != null && !isDone

    // Live countdown
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(occurrence.criticalEndsAt) {
        while (isRunning) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val remainingMs = (occurrence.criticalEndsAt ?: 0L) - now
    val totalMs = criticalTimerMinutes * 60_000L
    val remainingSafe = remainingMs.coerceAtLeast(0L)
    val progress = if (totalMs > 0) (remainingSafe.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val animProgress by animateFloatAsState(progress, tween(400), label = "prog")

    val countdownText = if (isRunning) {
        val mins = (remainingSafe / 60_000L).toInt()
        val secs = ((remainingSafe % 60_000L) / 1000L).toInt()
        String.format("%02d:%02d", mins, secs)
    } else null

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFF0F1F14) else Color(0xFF1F0A0A)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(
                2.dp,
                if (isDone) Color(0xFF1E4A25) else Color(0xFFFF1744),
                RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null && !isRunning) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF1744).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "⚠ CRITICAL",
                        color = Color(0xFFFF1744),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = if (isDone) Color(0xFF9E9E9E) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$category • $timeText",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp
                    )
                }
                if (isDone) {
                    Text("✓", color = Color(0xFF66BB6A), fontSize = 20.sp, fontWeight = FontWeight.Black)
                } else {
                    Text(epText, color = Color(0xFFFF1744), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            if (isRunning) {
                // Active: show countdown + bar + FINISH button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            countdownText ?: "--:--",
                            color = Color(0xFFFF1744),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Device locked • Complete to unlock",
                            color = Color(0xFFFFAB91),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = remainingSafe <= 0L
                    ) {
                        Text(
                            if (remainingSafe <= 0L) "FINISH" else "LOCKED",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF2A1313))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF1744), Color(0xFFB71C1C))
                                )
                            )
                    )
                }
            } else if (!isDone) {
                // Idle: show START button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⏱ ${criticalTimerMinutes} min lock",
                        color = Color(0xFFFFAB91),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("START", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
