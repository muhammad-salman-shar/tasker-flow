package com.taskerflow.app.ui.me

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.ui.MainViewModel

@Composable
fun MeScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Me", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C))) {
            Column(Modifier.padding(16.dp)) {
                Text("Level ${state.stats.level}", color = Color(0xFFFFC107), fontSize = 20.sp)
                Text("${state.stats.ep} EP total", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Achievements", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text("Coming soon...", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Text("Focus Lock", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(
            if (state.stats.focusLockActive) "🔒 ACTIVE" else "🔓 Inactive",
            color = if (state.stats.focusLockActive) Color(0xFFFF5252) else Color.Gray
        )
    }
}
