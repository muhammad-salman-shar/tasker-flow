package com.taskerflow.app.ui.create

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.*
import com.taskerflow.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.OTHER) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var taskType by remember { mutableStateOf(TaskType.SCHEDULED) }
    var repeatRule by remember { mutableStateOf(RepeatRule.NEVER) }
    var durationMin by remember { mutableStateOf(30) }
    var error by remember { mutableStateOf<String?>(null) }

    // Default to today + 1 hour
    val cal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }
    var scheduledAt by remember { mutableStateOf(cal.timeInMillis) }
    var deadlineAt by remember { mutableStateOf(cal.timeInMillis) }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E0E12),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0E0E12)
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = null },
                label = { Text("Task name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel("Category")
            DropdownEnum(category, Category.values().toList()) { category = it }
            Spacer(Modifier.height(12.dp))

            SectionLabel("Priority")
            DropdownEnum(priority, Priority.values().toList()) { priority = it }
            Spacer(Modifier.height(12.dp))

            SectionLabel("Difficulty (EP reward)")
            DropdownEnum(difficulty, Difficulty.values().toList(), labelFn = { "${it.name} • +${it.epReward} EP" }) { difficulty = it }
            Spacer(Modifier.height(12.dp))

            SectionLabel("Task type")
            DropdownEnum(taskType, TaskType.values().toList()) { taskType = it }
            Spacer(Modifier.height(12.dp))

            SectionLabel("Date")
            Button(
                onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                    DatePickerDialog(ctx, { _, y, m, d ->
                        c.set(y, m, d)
                        scheduledAt = c.timeInMillis
                        deadlineAt = c.timeInMillis
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(dateFmt.format(Date(scheduledAt))) }
            Spacer(Modifier.height(12.dp))

            SectionLabel("Time")
            Button(
                onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                    TimePickerDialog(ctx, { _, h, m ->
                        c.set(Calendar.HOUR_OF_DAY, h)
                        c.set(Calendar.MINUTE, m)
                        scheduledAt = c.timeInMillis
                        deadlineAt = c.timeInMillis
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(timeFmt.format(Date(scheduledAt))) }
            Spacer(Modifier.height(12.dp))

            SectionLabel("Duration (minutes)")
            OutlinedTextField(
                value = durationMin.toString(),
                onValueChange = { durationMin = it.toIntOrNull() ?: 30 },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            SectionLabel("Repeat")
            DropdownEnum(repeatRule, RepeatRule.values().toList()) { repeatRule = it }

            Spacer(Modifier.height(24.dp))
            error?.let {
                Text(it, color = Color(0xFFEF5350), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (title.isBlank()) { error = "Title required"; return@Button }
                    vm.createTask(
                        TaskEntity(
                            title = title.trim(),
                            description = description.trim(),
                            category = category,
                            priority = priority,
                            taskType = taskType,
                            difficulty = difficulty,
                            repeatRule = repeatRule,
                            durationMinutes = durationMin
                        ),
                        scheduledAt = scheduledAt,
                        deadlineAt = deadlineAt + durationMin * 60_000L
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
            ) {
                Text("CREATE TASK  ⚡ +${difficulty.epReward} EP", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF9E9E9E), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownEnum(
    selected: T,
    options: List<T>,
    labelFn: (T) -> String = { it.toString() },
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = labelFn(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(labelFn(opt)) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
