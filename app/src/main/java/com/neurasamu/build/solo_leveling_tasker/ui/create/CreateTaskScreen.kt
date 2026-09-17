package com.neurasamu.build.solo_leveling_tasker.ui.create

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.data.model.*
import com.neurasamu.build.solo_leveling_tasker.ui.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(vm: MainViewModel, onBack: () -> Unit, editingTaskId: Long? = null) {
    val ctx = LocalContext.current
    val isEdit = editingTaskId != null && editingTaskId > 0
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.CODING) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var repeatRule by remember { mutableStateOf(RepeatRule.NEVER) }
    var reminderOffset by remember { mutableStateOf(0) }
    var criticalTimerStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(!isEdit) }

    val cal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }
    var scheduledAt by remember { mutableStateOf(cal.timeInMillis) }

    LaunchedEffect(editingTaskId) {
        if (isEdit) {
            val t = vm.fetchTask(editingTaskId!!)
            if (t != null) {
                title = t.title
                note = t.description
                category = t.category
                priority = t.priority
                difficulty = t.difficulty
                repeatRule = t.repeatRule
                reminderOffset = t.reminderOffsetMinutes
                criticalTimerStr = if (t.criticalTimerMinutes > 0) t.criticalTimerMinutes.toString() else ""
            }
            val occ = vm.fetchLatestOccurrence(editingTaskId!!)
            if (occ != null) {
                scheduledAt = occ.scheduledAt
            }
            loaded = true
        }
    }

    val dateFmt = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Quest" else "New Quest", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090A10),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF090A10)
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFB300))
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(4.dp))

            Label("Title")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = null },
                placeholder = { Text("What's the quest?", color = Color(0xFF79829C)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB300),
                    unfocusedBorderColor = Color(0xFF23232B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFFB300)
                )
            )

            Spacer(Modifier.height(12.dp))
            Label("Note (optional)")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add sub-goals...", color = Color(0xFF79829C)) },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB300),
                    unfocusedBorderColor = Color(0xFF23232B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFFB300)
                )
            )

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Category",
                    selected = category,
                    options = Category.values().toList(),
                    labelFn = { it.name.take(6) }
                ) { category = it }
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Priority",
                    selected = priority,
                    options = Priority.values().toList(),
                    labelFn = { it.name.take(6) }
                ) { priority = it }
                SimpleDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Difficulty",
                    selected = difficulty,
                    options = Difficulty.values().toList(),
                    labelFn = { "+${it.epReward}" }
                ) { difficulty = it }
            }

            if (priority == Priority.CRITICAL) {
                Spacer(Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F0A0A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠ CRITICAL TASK", color = Color(0xFFFF1744), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(1f))
                            Text("Lock apps until timer ends", color = Color(0xFFFFAB91), fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Set Timer (minutes)", color = Color(0xFF79829C), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = criticalTimerStr,
                            onValueChange = { s -> if (s.length <= 4 && s.all { it.isDigit() }) criticalTimerStr = s },
                            placeholder = { Text("e.g. 60", color = Color(0xFF555555)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF1744),
                                unfocusedBorderColor = Color(0xFF23232B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFFFF1744)
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Task becomes locked once you tap START. Cannot be checked manually.",
                            color = Color(0xFF9E9E9E), fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Label("Schedule")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerBtn(Modifier.weight(1.2f), dateFmt.format(Date(scheduledAt))) {
                    val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                    DatePickerDialog(ctx, { _, y, m, d ->
                        c.set(y, m, d); scheduledAt = c.timeInMillis
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }
                PickerBtn(Modifier.weight(1f), timeFmt.format(Date(scheduledAt))) {
                    val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                    TimePickerDialog(ctx, { _, h, m ->
                        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
                        scheduledAt = c.timeInMillis
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                }
            }

            Spacer(Modifier.height(14.dp))

            Label("Repeat")
            SimpleDropdown(
                modifier = Modifier.fillMaxWidth(),
                label = "Repeat",
                selected = repeatRule,
                options = RepeatRule.values().toList(),
                labelFn = { it.name.replace("_", " ") }
            ) { repeatRule = it }

            Spacer(Modifier.height(14.dp))

            Label("Reminder")
            SimpleDropdown(
                modifier = Modifier.fillMaxWidth(),
                label = "Reminder",
                selected = reminderOffset,
                options = listOf(0, 5, 15, 30, 60, 120),
                labelFn = {
                    when (it) {
                        0 -> "Exact time"
                        else -> "$it min before"
                    }
                }
            ) { reminderOffset = it }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF3D57), fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100))))
                    .clickable {
                        if (isSaving) return@clickable
                        if (title.isBlank()) { error = "Title required"; return@clickable }
                        if (priority == Priority.CRITICAL) {
                            val timer = criticalTimerStr.toIntOrNull() ?: 0
                            if (timer <= 0) { error = "Enter timer minutes for critical task"; return@clickable }
                        }
                        isSaving = true
                        val task = TaskEntity(
                            id = editingTaskId ?: 0L,
                            title = title.trim(),
                            description = note.trim(),
                            category = category,
                            priority = priority,
                            taskType = TaskType.SCHEDULED,
                            difficulty = difficulty,
                            repeatRule = repeatRule,
                            durationMinutes = 0,
                            reminderOffsetMinutes = reminderOffset,
                            criticalTimerMinutes = if (priority == Priority.CRITICAL) (criticalTimerStr.toIntOrNull() ?: 0) else 0
                        )
                        // deadlineAt = scheduledAt for simple tasks
                        val deadlineAt = scheduledAt
                        scope.launch {
                            if (isEdit) {
                                vm.updateTaskAndWait(task, scheduledAt, deadlineAt)
                                onBack()
                            } else {
                                val saved = vm.saveTaskAndWait(
                                    task = task,
                                    scheduledAt = scheduledAt,
                                    deadlineAt = deadlineAt,
                                    isDeadline = false,
                                    deadlineEndMillis = deadlineAt
                                )
                                if (saved) onBack() else isSaving = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isEdit) "SAVE CHANGES  •  +${difficulty.epReward} EP"
                    else "SAVE TASK  •  +${difficulty.epReward} EP",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFF79829C),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
    )
}

@Composable
private fun PickerBtn(modifier: Modifier, text: String, onClick: () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14141A))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun <T> SimpleDropdown(
    modifier: Modifier = Modifier,
    label: String,
    selected: T,
    options: List<T>,
    labelFn: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            label.uppercase(),
            color = Color(0xFF79829C), fontSize = 9.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14141A))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(labelFn(selected), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color(0xFF1C1C24)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(labelFn(opt), color = Color.White) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}
