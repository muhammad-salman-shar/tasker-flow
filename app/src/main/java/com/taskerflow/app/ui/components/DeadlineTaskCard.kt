package com.taskerflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeadlineTaskCard(
    title: String,
    category: String,
    occurrences: List<OccurrenceEntity>,
    epReward: Int,
    onCompleteDay: (Long) -> Unit,
    onDayTap: (Long) -> Unit = {}
) {
    val sorted = remember(occurrences) { occurrences.sortedBy { it.scheduledAt } }
    var expanded by remember { mutableStateOf(false) }

    val completedCount = sorted.count {
        it.status == OccurrenceStatus.COMPLETED ||
        it.status == OccurrenceStatus.LATE ||
        it.status == OccurrenceStatus.RECOVERED
    }
    val total = sorted.size
    val allDone = completedCount == total && total > 0
    val progress = if (total == 0) 0f else completedCount.toFloat() / total
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(500), label = "p"
    )

    // Find first PENDING or LATE day = "current unlockable"
    val currentUnlockIdx = sorted.indexOfFirst {
        it.status == OccurrenceStatus.PENDING || it.status == OccurrenceStatus.LATE
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) Color(0xFF0F1F14) else Color(0xFF17171E)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(
                1.dp,
                if (allDone) Color(0xFF1E4A25) else Color(0xFF2A2A32),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column {
            // Header row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = if (allDone) Color(0xFF9E9E9E) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "$completedCount / $total days",
                                color = Color(0xFFFFB300),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$category • ${formatDate(sorted.firstOrNull()?.scheduledAt)} → ${formatDate(sorted.lastOrNull()?.scheduledAt)}",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (allDone) Color(0xFF66BB6A) else Color(0xFFFFB300),
                        trackColor = Color(0xFF2A2A32)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${epReward * total} EP", color = if (allDone) Color(0xFF66BB6A) else Color(0xFFFFB300), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Sub-days list (only when expanded)
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    sorted.forEachIndexed { idx, occ ->
                        val isDone = occ.status == OccurrenceStatus.COMPLETED ||
                            occ.status == OccurrenceStatus.LATE ||
                            occ.status == OccurrenceStatus.RECOVERED
                        val isCurrent = idx == currentUnlockIdx
                        val locked = !isDone && !isCurrent

                        DayRow(
                            dayNumber = idx + 1,
                            date = formatDate(occ.scheduledAt),
                            done = isDone,
                            locked = locked,
                            current = isCurrent,
                            onComplete = { if (!locked && !isDone) onCompleteDay(occ.id) },
                            onTap = { if (!locked) onDayTap(occ.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    dayNumber: Int,
    date: String,
    done: Boolean,
    locked: Boolean,
    current: Boolean,
    onComplete: () -> Unit,
    onTap: () -> Unit
) {
    val bg = when {
        done -> Color(0xFF132A18)
        current -> Color(0xFF1E1A0F)
        locked -> Color(0xFF121216)
        else -> Color(0xFF14141A)
    }
    val accent = when {
        done -> Color(0xFF66BB6A)
        current -> Color(0xFFFFB300)
        locked -> Color(0xFF444444)
        else -> Color(0xFF9E9E9E)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = !locked) { onTap() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // tiny checkbox / lock
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = !locked && !done) { onComplete() },
            contentAlignment = Alignment.Center
        ) {
            when {
                locked -> Icon(
                    Icons.Filled.Lock, null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(14.dp)
                )
                done -> Box(
                    Modifier.size(20.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(13.dp))
                }
                else -> Box(
                    Modifier.size(20.dp).clip(CircleShape).border(2.dp, accent, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Day $dayNumber",
                color = if (locked) Color(0xFF666666) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                date,
                color = if (locked) Color(0xFF444444) else Color(0xFF9E9E9E),
                fontSize = 10.sp
            )
        }
        Text(
            when {
                done -> "DONE"
                locked -> "LOCKED"
                current -> "START"
                else -> ""
            },
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

private fun formatDate(millis: Long?): String {
    if (millis == null) return "-"
    return SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))
}
