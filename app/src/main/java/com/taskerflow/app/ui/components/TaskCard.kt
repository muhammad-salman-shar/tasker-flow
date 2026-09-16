package com.taskerflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onClick: () -> Unit = {},
    onComplete: (() -> Unit)? = null
) {
    val style = cardStyle(status)
    Card(
        colors = CardDefaults.cardColors(containerColor = style.bg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, style.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("$category • $timeText", color = Color(0xFF9E9E9E), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(epText, color = style.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (onComplete != null && status == OccurrenceStatus.PENDING) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "TAP ✓",
                        color = Color(0xFFFFC107),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onComplete)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
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
        bg = Color(0xFF132A18), border = Color(0xFF1B5E20), accent = Color(0xFF66BB6A),
        icon = Icons.Filled.CheckCircle
    )
    OccurrenceStatus.LATE -> CardStyle(
        bg = Color(0xFF2A2113), border = Color(0xFF7A5C00), accent = Color(0xFFFFC107),
        icon = Icons.Filled.Schedule
    )
    OccurrenceStatus.MISSED, OccurrenceStatus.SKIPPED -> CardStyle(
        bg = Color(0xFF2A1313), border = Color(0xFF7A1A1A), accent = Color(0xFFEF5350),
        icon = Icons.Filled.ErrorOutline
    )
    OccurrenceStatus.PENDING -> CardStyle(
        bg = Color(0xFF16161C), border = Color(0xFF2A2A32), accent = Color(0xFFFFC107),
        icon = Icons.Filled.RadioButtonUnchecked
    )
}
