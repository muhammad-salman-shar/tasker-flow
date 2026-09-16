package com.taskerflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.OccurrenceStatus

@Composable
fun TaskCard(
    title: String,
    category: String,
    timeText: String,
    epText: String,
    status: OccurrenceStatus,
    onClick: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null
) {
    val style = cardStyle(status)
    val isDone = status == OccurrenceStatus.COMPLETED || status == OccurrenceStatus.RECOVERED
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = style.bg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .scale(scale)
            .border(1.dp, style.border, RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big touch-friendly checkbox
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isDone && onComplete != null) {
                        pressed = true
                        onComplete?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDone) style.accent else Color.Transparent)
                        .border(2.dp, style.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isDone) Color(0xFF9E9E9E) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text("$category • $timeText", color = Color(0xFF9E9E9E), fontSize = 12.sp)
            }

            Text(epText, color = style.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class CardStyle(
    val bg: Color,
    val border: Color,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun cardStyle(status: OccurrenceStatus): CardStyle = when (status) {
    OccurrenceStatus.COMPLETED, OccurrenceStatus.RECOVERED -> CardStyle(
        bg = Color(0xFF0F1F14), border = Color(0xFF1E4A25), accent = Color(0xFF66BB6A),
        icon = Icons.Filled.CheckCircle
    )
    OccurrenceStatus.LATE -> CardStyle(
        bg = Color(0xFF221A0C), border = Color(0xFF5C4600), accent = Color(0xFFFFC107),
        icon = Icons.Filled.Schedule
    )
    OccurrenceStatus.MISSED, OccurrenceStatus.SKIPPED -> CardStyle(
        bg = Color(0xFF221010), border = Color(0xFF5C1A1A), accent = Color(0xFFEF5350),
        icon = Icons.Filled.ErrorOutline
    )
    OccurrenceStatus.PENDING -> CardStyle(
        bg = Color(0xFF17171E), border = Color(0xFF2A2A32), accent = Color(0xFFFFC107),
        icon = Icons.Filled.RadioButtonUnchecked
    )
}
