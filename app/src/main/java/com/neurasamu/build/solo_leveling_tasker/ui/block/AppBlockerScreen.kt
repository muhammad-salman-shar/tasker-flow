package com.neurasamu.build.solo_leveling_tasker.ui.block

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.neurasamu.build.solo_leveling_tasker.data.block.InstalledApp
import com.neurasamu.build.solo_leveling_tasker.data.block.InstalledAppsLoader
import com.neurasamu.build.solo_leveling_tasker.service.AppBlockerAccessibilityService
import com.neurasamu.build.solo_leveling_tasker.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlockerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val blocked by vm.blockedPackages().collectAsState(initial = emptySet())
    val strict by vm.strictMode().collectAsState(initial = false)
    val serviceRunning by AppBlockerAccessibilityService.running.collectAsState()
    var testOn by remember { mutableStateOf(AppBlockerAccessibilityService.testMode.value) }
    val criticalActive = vm.criticalActive().collectAsState(initial = false).value

    // Reload apps when screen first appears
    val allApps = remember { InstalledAppsLoader.load(ctx) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, allApps) {
        if (query.isBlank()) allApps
        else allApps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Blocker", fontWeight = FontWeight.Black) },
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

            if (criticalActive) {
                // Critical lock overlay — blocks all interactions
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F0A0A)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔒", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "UNAVAILABLE",
                            color = Color(0xFFFF1744),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Critical task in progress",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "App Blocker settings are locked until the critical timer ends. Complete the critical task to unlock.",
                            color = Color(0xFFFFAB91),
                            fontSize = 12.sp
                        )
                    }
                }
                return@Column
            }

        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

            // Accessibility permission status card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceRunning) Color(0xFF0F1F14) else Color(0xFF2A1A13)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (serviceRunning) "✅" else "⚠️", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (serviceRunning) "Blocker Service Active"
                            else "Blocker Service Disabled",
                            color = if (serviceRunning) Color(0xFF66BB6A) else Color(0xFFFFB300),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (serviceRunning)
                            "Your selected apps will be blocked when Health drops below 50%."
                        else
                            "To block apps, you must enable Accessibility for Tasker Flow.",
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp
                    )
                    if (!serviceRunning) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                ctx.startActivity(i)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB300),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ENABLE ACCESSIBILITY", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // TEST MODE demo toggle (does NOT affect real state)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A0F)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🧪 Test Mode", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Simulate Health < 50 to preview blocking. No real effect.",
                            color = Color(0xFF79829C), fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = testOn,
                        onCheckedChange = { testOn = it; AppBlockerAccessibilityService.testMode.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFB300)
                        )
                    )
                }
            }

            // Strict mode toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Strict Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (strict) "All apps blocked except essentials"
                            else "Only selected apps blocked",
                            color = Color(0xFF79829C), fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = strict,
                        onCheckedChange = { vm.setStrictMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFB300)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!strict) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps…", color = Color(0xFF79829C)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color(0xFF79829C)) },
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
                Spacer(Modifier.height(8.dp))
                Text(
                    "${allApps.size} apps found • ${blocked.size} blocked",
                    color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 40.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            blocked = blocked.contains(app.packageName),
                            onToggle = { isBlocked ->
                                vm.toggleBlockedApp(app.packageName, isBlocked)
                            }
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("STRICT MODE ON", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Essentials (phone, messages, clock, Tasker Flow)\nstay open. Everything else will be blocked.",
                        color = Color(0xFF79829C), fontSize = 12.sp
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    blocked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (blocked) listOf(Color(0xFFFF3D57), Color(0xFF9B1D56))
                            else listOf(Color(0xFF23232B), Color(0xFF17171E))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.label.firstOrNull()?.uppercase() ?: "?",
                    color = if (blocked) Color.White else Color(0xFF79829C),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(app.packageName, color = Color(0xFF79829C), fontSize = 10.sp, maxLines = 1)
            }
            Switch(
                checked = blocked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFF3D57)
                )
            )
        }
    }
}
