package com.taskerflow.app.ui.create

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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun CreateTaskScreen(vm: MainViewModel, onBack: () -> Unit, editingTaskId: Long? = null) {
    val ctx = LocalContext.current
    val isEdit = editingTaskId != null && editingTaskId > 0

    // ---- form state ----
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.CODING) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var isDeadline by remember { mutableStateOf(false) }
    var durationMinStr by remember { mutableStateOf("30") }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(!isEdit) }

    val defaultStart = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    val defaultEnd = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        }.timeInMillis
    }

    var scheduledAt by remember { mutableStateOf(defaultStart) }
    var deadlineAt by remember { mutableStateOf(defaultEnd) }

    // ---- LOAD existing task for edit ----
    LaunchedEffect(editingTaskId) {
        if (isEdit) {
            val t = vm.fetchTask(editingTaskId!!)
            if (t != null) {
                title = t.title
                note = t.description
                category = t.category
                priority = t.priority
                difficulty = t.difficulty
                isDeadline = t.taskType == TaskType.DEADLINE
                durationMinStr = t.durationMinutes.toString()
            }
            val occ = vm.fetchLatestOccurrence(editingTaskId!!)
            if (occ != null) {
                scheduledAt = occ.scheduledAt
                deadlineAt = occ.deadlineAt
            }
            loaded = true
        }
    }

    val dateFmt = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val scope = rememberCoroutineScope()

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

            Spacer(Modifier.height(14.dp))

            Label("Task Type")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131318))
                    .padding(4.dp)
            ) {
                SegBtn(Modifier.weight(1f), "Day Task", !isDeadline) { isDeadline = false }
                SegBtn(Modifier.weight(1f), "Deadline", isDeadline) { isDeadline = true }
            }

            Spacer(Modifier.height(14.dp))

            if (!isDeadline) {
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
                Spacer(Modifier.height(10.dp))
                Label("Duration (min)")
                OutlinedTextField(
                    value = durationMinStr,
                    onValueChange = { s -> if (s.length <= 4 && s.all { it.isDigit() }) durationMinStr = s },
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
            } else {
                Label("Start Timeline")
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
                Spacer(Modifier.height(10.dp))
                Label("End Deadline")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerBtn(Modifier.weight(1.2f), dateFmt.format(Date(deadlineAt))) {
                        val c = Calendar.getInstance().apply { timeInMillis = deadlineAt }
                        DatePickerDialog(ctx, { _, y, m, d ->
                            c.set(y, m, d); deadlineAt = c.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    PickerBtn(Modifier.weight(1f), timeFmt.format(Date(deadlineAt))) {
                        val c = Calendar.getInstance().apply { timeInMillis = deadlineAt }
                        TimePickerDialog(ctx, { _, h, m ->
                            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
                            deadlineAt = c.timeInMillis
                        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                    }
                }
            }

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
                        if (title.isBlank()) { error = "Title required"; return@clickable }
                        val dur = durationMinStr.toIntOrNull() ?: 30
                        val finalDeadline = if (isDeadline) deadlineAt else scheduledAt + dur * 60_000L
                        val task = TaskEntity(
                            id = editingTaskId ?: 0L,
                            title = title.trim(),
                            description = note.trim(),
                            category = category,
                            priority = priority,
                            taskType = if (isDeadline) TaskType.DEADLINE else TaskType.SCHEDULED,
                            difficulty = difficulty,
                            repeatRule = RepeatRule.NEVER,
                            durationMinutes = dur
                        )
                        scope.launch {
                            if (isEdit) {
                                vm.updateTaskAndWait(task, scheduledAt, finalDeadline)
                            } else {
                                vm.saveTaskAndWait(
                                    task = task,
                                    scheduledAt = scheduledAt,
                                    deadlineAt = finalDeadline,
                                    isDeadline = isDeadline,
                                    deadlineEndMillis = deadlineAt
                                )
                            }
                            onBack()
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
private fun SegBtn(modifier: Modifier, text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else Color(0xFF79829C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
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
