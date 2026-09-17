package com.neurasamu.build.solo_leveling_tasker.ui.alarm

import android.app.TimePickerDialog
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.data.model.AlarmEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.DismissMethod
import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
import com.neurasamu.build.solo_leveling_tasker.ui.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditAlarmScreen(
    vm: MainViewModel,
    editingAlarmId: Long?,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val isEdit = editingAlarmId != null && editingAlarmId > 0

    var loaded by remember { mutableStateOf(!isEdit) }
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("") }
    var repeatRule by remember { mutableStateOf(RepeatRule.NEVER) }
    var customDays by remember { mutableStateOf(setOf<Int>()) }
    var soundUri by remember { mutableStateOf("") }
    var soundName by remember { mutableStateOf("Default alarm tone") }
    var vibrate by remember { mutableStateOf(true) }
    var snoozeEnabled by remember { mutableStateOf(true) }
    var snoozeMinutes by remember { mutableStateOf(5) }
    var dismissMethod by remember { mutableStateOf(DismissMethod.EASY) }
    var pinCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    // Load existing alarm
    LaunchedEffect(editingAlarmId) {
        if (isEdit) {
            val a = vm.getAlarm(editingAlarmId!!)
            if (a != null) {
                hour = a.hour
                minute = a.minute
                label = a.label
                repeatRule = a.repeatRule
                customDays = a.customDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                soundUri = a.soundUri
                soundName = if (a.soundUri.isBlank()) "Default alarm tone" else {
                    try { RingtoneManager.getRingtone(ctx, android.net.Uri.parse(a.soundUri))
                        ?.getTitle(ctx) ?: "Custom" } catch (_: Exception) { "Custom" }
                }
                vibrate = a.vibrate
                snoozeEnabled = a.snoozeEnabled
                snoozeMinutes = a.snoozeMinutes
                dismissMethod = a.dismissMethod
                pinCode = a.pinCode
            }
            loaded = true
        }
    }

    // Ringtone picker
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                soundUri = uri.toString()
                soundName = try { RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) ?: "Custom" }
                           catch (_: Exception) { "Custom" }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Alarm" else "New Alarm", fontWeight = FontWeight.Black) },
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
            Spacer(Modifier.height(8.dp))

            // ---- Time picker (big) ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF17171E))
                    .clickable {
                        TimePickerDialog(ctx, { _, h, m ->
                            hour = h; minute = m; error = null
                        }, hour, minute, false).show()
                    }
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format("%02d:%02d", hour, minute),
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = -2.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (hour < 12) "AM" else "PM",
                        color = Color(0xFFFFB300),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Label("Label")
            OutlinedTextField(
                value = label,
                onValueChange = { if (it.length <= 30) label = it },
                placeholder = { Text("Wake up, Namaz, Medicine…", color = Color(0xFF79829C)) },
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

            Spacer(Modifier.height(14.dp))

            Label("Repeat")
            AlarmDropdown(
                selected = repeatRule,
                options = listOf(
                    RepeatRule.NEVER, RepeatRule.DAILY, RepeatRule.WEEKDAYS,
                    RepeatRule.WEEKENDS, RepeatRule.WEEKLY, RepeatRule.CUSTOM
                ),
                labelFn = {
                    when (it) {
                        RepeatRule.NEVER -> "Once"
                        RepeatRule.DAILY -> "Every day"
                        RepeatRule.WEEKDAYS -> "Mon–Fri"
                        RepeatRule.WEEKENDS -> "Sat & Sun"
                        RepeatRule.WEEKLY -> "Weekly"
                        RepeatRule.MONTHLY -> "Monthly"
                        RepeatRule.CUSTOM -> "Custom days"
                    }
                }
            ) { repeatRule = it }

            if (repeatRule == RepeatRule.CUSTOM) {
                Spacer(Modifier.height(10.dp))
                DayToggles(selected = customDays, onToggle = { d ->
                    customDays = if (customDays.contains(d)) customDays - d else customDays + d
                })
            }

            Spacer(Modifier.height(14.dp))

            Label("Sound")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14141A))
                        .clickable {
                            soundUri = ""; soundName = "Default alarm tone"
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Default",
                        color = if (soundUri.isBlank()) Color(0xFFFFB300) else Color(0xFF79829C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    Modifier
                        .weight(1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14141A))
                        .clickable {
                            val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pick alarm tone")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    if (soundUri.isBlank()) null else android.net.Uri.parse(soundUri))
                            }
                            ringtonePicker.launch(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        soundName,
                        color = if (soundUri.isNotBlank()) Color(0xFFFFB300) else Color(0xFF79829C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Vibrate
            ToggleRow(
                title = "Vibrate",
                subtitle = "Vibrate when alarm rings",
                checked = vibrate,
                onChange = { vibrate = it }
            )

            Spacer(Modifier.height(10.dp))

            // Snooze
            ToggleRow(
                title = "Snooze",
                subtitle = if (snoozeEnabled) "Snooze for $snoozeMinutes min" else "No snooze option",
                checked = snoozeEnabled,
                onChange = { snoozeEnabled = it }
            )

            if (snoozeEnabled) {
                Spacer(Modifier.height(10.dp))
                Label("Snooze duration")
                AlarmDropdown(
                    selected = snoozeMinutes,
                    options = listOf(5, 10, 15, 30),
                    labelFn = { "$it min" }
                ) { snoozeMinutes = it }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Dismiss method ----
            Label("Dismiss method")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131318))
                    .padding(4.dp)
            ) {
                AlarmSegBtn(
                    modifier = Modifier.weight(1f),
                    text = "Easy (swipe)",
                    selected = dismissMethod == DismissMethod.EASY,
                    onClick = { dismissMethod = DismissMethod.EASY; error = null }
                )
                AlarmSegBtn(
                    modifier = Modifier.weight(1f),
                    text = "🔐 PIN lock",
                    selected = dismissMethod == DismissMethod.PIN,
                    onClick = { dismissMethod = DismissMethod.PIN }
                )
            }

            if (dismissMethod == DismissMethod.PIN) {
                Spacer(Modifier.height(10.dp))
                Label("PIN code (min 6 digits)")
                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { s ->
                        if (s.length <= 10 && s.all { it.isDigit() }) pinCode = s
                        error = null
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF3D57),
                        unfocusedBorderColor = Color(0xFF23232B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF3D57)
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "You must enter this PIN to dismiss the alarm. No snooze available.",
                    color = Color(0xFFFFAB91), fontSize = 10.sp
                )
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF3D57), fontSize = 13.sp)
            }

            Spacer(Modifier.height(22.dp))

            // ---- Save ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100))))
                    .clickable(enabled = !saving) {
                        // Validation
                        if (dismissMethod == DismissMethod.PIN && pinCode.length < 6) {
                            error = "PIN must be at least 6 digits"
                            return@clickable
                        }
                        if (repeatRule == RepeatRule.CUSTOM && customDays.isEmpty()) {
                            error = "Pick at least one day"
                            return@clickable
                        }

                        saving = true
                        val alarm = AlarmEntity(
                            id = editingAlarmId ?: 0L,
                            hour = hour,
                            minute = minute,
                            label = label.trim(),
                            repeatRule = repeatRule,
                            customDays = customDays.sorted().joinToString(","),
                            soundUri = soundUri,
                            vibrate = vibrate,
                            snoozeEnabled = snoozeEnabled,
                            snoozeMinutes = snoozeMinutes,
                            dismissMethod = dismissMethod,
                            pinCode = if (dismissMethod == DismissMethod.PIN) pinCode else "",
                            enabled = true
                        )

                        if (isEdit) {
                            vm.updateAlarm(alarm)
                        } else {
                            vm.insertAlarm(alarm)
                        }
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isEdit) "SAVE CHANGES" else "CREATE ALARM",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
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
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF79829C), fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFB300)
                )
            )
        }
    }
}

@Composable
private fun AlarmSegBtn(modifier: Modifier, text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
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
private fun DayToggles(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val days = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { (d, label) ->
            val on = selected.contains(d)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (on) Color(0xFFFFB300) else Color(0xFF14141A))
                    .clickable { onToggle(d) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (on) Color.Black else Color(0xFF9E9E9E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun <T> AlarmDropdown(
    selected: T,
    options: List<T>,
    labelFn: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
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
            Text(labelFn(selected), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
