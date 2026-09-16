package com.taskerflow.app.ui.me

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.taskerflow.app.data.profile.ProfileData
import com.taskerflow.app.data.profile.ThemeMode
import com.taskerflow.app.domain.GamificationEngine
import com.taskerflow.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MeScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    val s = state.stats
    val profile = state.profile
    var showEditDialog by remember { mutableStateOf(false) }

    val (prog, total) = GamificationEngine.levelProgress(s.ep)
    val rank = rankFor(s.level)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Me", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(rank, color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Profile hero card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF2A6D)))
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF111422)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            profile.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color(0xFFFFB300),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (profile.dobMillis > 0) {
                        val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                        Text("🎂 ${df.format(Date(profile.dobMillis))}", color = Color(0xFF79829C), fontSize = 12.sp)
                    } else {
                        Text("Tap edit to set profile", color = Color(0xFF79829C), fontSize = 12.sp)
                    }
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Filled.Edit, "Edit", tint = Color(0xFFFFB300))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Level card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tier", color = Color(0xFF79829C), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text("Level ${s.level}", color = Color(0xFFFFB300), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Text("${s.ep} EP total", color = Color(0xFF79829C), fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF23232B))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(if (total == 0) 0f else prog.toFloat() / total)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100))))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("$prog / $total EP to next level", color = Color(0xFF79829C), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(18.dp))

        // Stats quick row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(Modifier.weight(1f), "🔥", "${s.streak}d", "Streak")
            MiniStat(Modifier.weight(1f), "✅", "${s.totalCompleted}", "Done")
            MiniStat(Modifier.weight(1f), "❌", "${s.totalMissed}", "Missed")
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel("Appearance")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Theme", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        val selected = profile.themeMode == mode
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected)
                                        Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFE65100)))
                                    else Brush.horizontalGradient(listOf(Color(0xFF14141A), Color(0xFF14141A)))
                                )
                                .clickable {
                                    vm.saveProfile(profile.copy(themeMode = mode))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (selected) Color.Black else Color(0xFF9E9E9E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.height(90.dp))
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { newProfile ->
                vm.saveProfile(newProfile)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFF79829C),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun MiniStat(modifier: Modifier, icon: String, value: String, label: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color(0xFF79829C), fontSize = 10.sp)
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: ProfileData,
    onDismiss: () -> Unit,
    onSave: (ProfileData) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var dob by remember { mutableStateOf(profile.dobMillis) }

    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C24),
        title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
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
                Text("Date of Birth", color = Color(0xFF79829C), fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14141A))
                        .clickable {
                            val c = Calendar.getInstance()
                            if (dob > 0) c.timeInMillis = dob
                            DatePickerDialog(
                                ctx,
                                { _, y, m, d ->
                                    val cc = Calendar.getInstance()
                                    cc.set(y, m, d)
                                    dob = cc.timeInMillis
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        if (dob > 0) df.format(Date(dob)) else "Tap to select",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(profile.copy(name = name.trim().ifBlank { "Hunter" }, dobMillis = dob))
            }) {
                Text("SAVE", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF9E9E9E))
            }
        }
    )
}

private fun rankFor(level: Int): String = when {
    level < 3 -> "BEGINNER"
    level < 6 -> "HUNTER"
    level < 10 -> "VANGUARD"
    level < 20 -> "ELITE"
    level < 50 -> "MASTER"
    else -> "OVERLORD"
}
