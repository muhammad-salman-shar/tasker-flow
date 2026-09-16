package com.taskerflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val isDone = status == OccurrenceStatus.COMPLETED ||
                 status == OccurrenceStatus.RECOVERED
    val canComplete = !isDone && onComplete != null

    Card(
        colors = CardDefaults.cardColors(containerColor = style.bg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(1.dp, style.border, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: checkbox touch zone — ONLY completes
            Box(
                Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clickable(enabled = canComplete) { onComplete?.invoke() },
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
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // RIGHT: info + onClick zone (Edit/Delete dialog in Tasks screen)
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (onClick != null) Modifier.clickable { onClick() }
                        else Modifier
                    )
                    .padding(end = 14.dp, top = 14.dp, bottom = 14.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = if (isDone) Color(0xFF9E9E9E) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$category • $timeText",
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    epText,
                    color = style.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class CardStyle(
    val bg: Color,
    val border: Color,
    val accent: Color
)

private fun cardStyle(status: OccurrenceStatus): CardStyle = when (status) {
    OccurrenceStatus.COMPLETED, OccurrenceStatus.RECOVERED -> CardStyle(
        bg = Color(0xFF0F1F14), border = Color(0xFF1E4A25), accent = Color(0xFF66BB6A)
    )
    OccurrenceStatus.LATE -> CardStyle(
        bg = Color(0xFF221A0C), border = Color(0xFF5C4600), accent = Color(0xFFFFC107)
    )
    OccurrenceStatus.MISSED, OccurrenceStatus.SKIPPED -> CardStyle(
        bg = Color(0xFF221010), border = Color(0xFF5C1A1A), accent = Color(0xFFEF5350)
    )
    OccurrenceStatus.PENDING -> CardStyle(
        bg = Color(0xFF17171E), border = Color(0xFF2A2A32), accent = Color(0xFFFFC107)
    )
}
