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
fun CreateTaskScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.CODING) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var isDeadline by remember { mutableStateOf(false) }
    var repeatRule by remember { mutableStateOf(RepeatRule.NEVER) }
    var durationMin by remember { mutableStateOf(30) }
    var error by remember { mutableStateOf<String?>(null) }

    val cal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }
    val endCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
    }
    var scheduledAt by remember { mutableStateOf(cal.timeInMillis) }
    var deadlineAt by remember { mutableStateOf(endCal.timeInMillis) }

    val dateFmt = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Quest", fontWeight = FontWeight.Black) },
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
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(4.dp))

            // Title
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
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(12.dp))
            Label("Description (optional)")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add sub-goals...", color = Color(0xFF79829C)) },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB300),
                    unfocusedBorderColor = Color(0xFF23232B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(14.dp))

            // Inline trio: Category / Priority / Difficulty
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Category",
                    selected = category,
                    options = Category.values().toList(),
                    labelFn = { it.name.take(6) }
                ) { category = it }
                CompactDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Priority",
                    selected = priority,
                    options = Priority.values().toList(),
                    labelFn = { it.name.take(6) }
                ) { priority = it }
                CompactDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Difficulty",
                    selected = difficulty,
                    options = Difficulty.values().toList(),
                    labelFn = { "+${it.epReward}" }
                ) { difficulty = it }
            }

            Spacer(Modifier.height(14.dp))

            // Segmented: Day Task / Deadline
            Label("Task Type")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131318))
                    .padding(4.dp)
            ) {
                SegBtn(
                    modifier = Modifier.weight(1f),
                    text = "Day Task",
                    selected = !isDeadline,
                    onClick = { isDeadline = false }
                )
                SegBtn(
                    modifier = Modifier.weight(1f),
                    text = "Deadline",
                    selected = isDeadline,
                    onClick = { isDeadline = true }
                )
            }

            Spacer(Modifier.height(14.dp))

            if (!isDeadline) {
                Label("Schedule")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerBtn(
                        modifier = Modifier.weight(1.2f),
                        text = dateFmt.format(Date(scheduledAt))
                    ) {
                        val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                        DatePickerDialog(ctx, { _, y, m, d ->
                            c.set(y, m, d); scheduledAt = c.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    PickerBtn(
                        modifier = Modifier.weight(1f),
                        text = timeFmt.format(Date(scheduledAt))
                    ) {
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
                    value = durationMin.toString(),
                    onValueChange = { durationMin = it.toIntOrNull() ?: 30 },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color(0xFF23232B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            } else {
                Label("Start Timeline")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerBtn(
                        modifier = Modifier.weight(1.2f),
                        text = dateFmt.format(Date(scheduledAt))
                    ) {
                        val c = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                        DatePickerDialog(ctx, { _, y, m, d ->
                            c.set(y, m, d); scheduledAt = c.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    PickerBtn(
                        modifier = Modifier.weight(1f),
                        text = timeFmt.format(Date(scheduledAt))
                    ) {
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
                    PickerBtn(
                        modifier = Modifier.weight(1.2f),
                        text = dateFmt.format(Date(deadlineAt))
                    ) {
                        val c = Calendar.getInstance().apply { timeInMillis = deadlineAt }
                        DatePickerDialog(ctx, { _, y, m, d ->
                            c.set(y, m, d); deadlineAt = c.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    PickerBtn(
                        modifier = Modifier.weight(1f),
                        text = timeFmt.format(Date(deadlineAt))
                    ) {
                        val c = Calendar.getInstance().apply { timeInMillis = deadlineAt }
                        TimePickerDialog(ctx, { _, h, m ->
                            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
                            deadlineAt = c.timeInMillis
                        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Label("Repeat")
            DropdownFullWidth(
                selected = repeatRule,
                options = RepeatRule.values().toList()
            ) { repeatRule = it }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF3D57), fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))

            // Create button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100))))
                    .clickable {
                        if (title.isBlank()) { error = "Title required"; return@clickable }
                        val finalDeadline = if (isDeadline) deadlineAt else scheduledAt + durationMin * 60_000L
                        vm.createTask(
                            TaskEntity(
                                title = title.trim(),
                                description = description.trim(),
                                category = category,
                                priority = priority,
                                taskType = if (isDeadline) TaskType.DEADLINE else TaskType.SCHEDULED,
                                difficulty = difficulty,
                                repeatRule = repeatRule,
                                durationMinutes = durationMin
                            ),
                            scheduledAt = scheduledAt,
                            deadlineAt = finalDeadline
                        )
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "CREATE TASK  •  +${difficulty.epReward} EP",
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
        Text(
            text,
            color = if (selected) Color.White else Color(0xFF79829C),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CompactDropdown(
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
            color = Color(0xFF79829C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownFullWidth(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF14141A))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(selected.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.toString()) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
