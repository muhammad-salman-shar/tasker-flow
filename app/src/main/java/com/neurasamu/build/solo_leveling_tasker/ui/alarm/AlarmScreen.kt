package com.neurasamu.build.solo_leveling_tasker.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.neurasamu.build.solo_leveling_tasker.data.model.AlarmEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.DismissMethod
import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
import com.neurasamu.build.solo_leveling_tasker.ui.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmScreen(vm: MainViewModel, onEdit: (Long?) -> Unit) {
    val alarms by vm.observeAlarms().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))

        // Live clock header
        LiveClockHeader()

        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ALARMS", color = Color(0xFF79829C), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Text("${alarms.count { it.enabled }} active", color = Color(0xFF79829C), fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))

        if (alarms.isEmpty()) {
            EmptyAlarms()
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        onToggle = { enabled ->
                            vm.updateAlarm(alarm.copy(enabled = enabled))
                        },
                        onTap = { onEdit(alarm.id) },
                        onLongPress = {
                            vm.deleteAlarm(alarm.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveClockHeader() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()) }
    val d = Date(now)

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                timeFmt.format(d),
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = -2.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                ampmFmt.format(d),
                color = Color(0xFFFFB300),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }
        Text(
            dateFmt.format(d),
            color = Color(0xFF79829C),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dim = !alarm.enabled
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (dim) Color(0xFF111116) else Color(0xFF17171E)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format("%02d:%02d", alarm.hour, alarm.minute),
                        color = if (dim) Color(0xFF555555) else Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = -1.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ampmOf(alarm.hour),
                        color = if (dim) Color(0xFF555555) else Color(0xFFFFB300),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (alarm.label.isBlank()) repeatSummary(alarm) else "${alarm.label} • ${repeatSummary(alarm)}",
                    color = if (dim) Color(0xFF555555) else Color(0xFF9E9E9E),
                    fontSize = 11.sp
                )
                if (alarm.dismissMethod == DismissMethod.PIN) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔐", fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("PIN required", color = Color(0xFFFF3D57), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFB300),
                    uncheckedThumbColor = Color(0xFF555555),
                    uncheckedTrackColor = Color(0xFF23232B)
                )
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1C1C24),
            title = { Text("Delete alarm?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("${alarm.hour}:${String.format("%02d", alarm.minute)} — ${alarm.label.ifBlank { "Alarm" }}", color = Color(0xFF9E9E9E)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onLongPress()
                }) { Text("DELETE", color = Color(0xFFFF3D57), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = Color(0xFF9E9E9E))
                }
            }
        )
    }
}

@Composable
private fun EmptyAlarms() {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏰", fontSize = 56.sp)
        Spacer(Modifier.height(14.dp))
        Text("NO ALARMS YET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap ＋ to set your first alarm.",
            color = Color(0xFF79829C), fontSize = 13.sp, textAlign = TextAlign.Center
        )
    }
}

private fun ampmOf(hour: Int): String = if (hour < 12) "AM" else "PM"

private fun repeatSummary(alarm: AlarmEntity): String = when (alarm.repeatRule) {
    RepeatRule.NEVER -> "Once"
    RepeatRule.DAILY -> "Every day"
    RepeatRule.WEEKDAYS -> "Mon–Fri"
    RepeatRule.WEEKENDS -> "Sat & Sun"
    RepeatRule.WEEKLY -> "Weekly"
    RepeatRule.MONTHLY -> "Monthly"
    RepeatRule.CUSTOM -> {
        if (alarm.customDays.isBlank()) "Custom"
        else {
            val names = mapOf(
                "1" to "Mon", "2" to "Tue", "3" to "Wed",
                "4" to "Thu", "5" to "Fri", "6" to "Sat", "7" to "Sun"
            )
            alarm.customDays.split(",").mapNotNull { names[it.trim()] }.joinToString(", ")
        }
    }
}
