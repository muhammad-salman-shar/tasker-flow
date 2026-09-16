package com.taskerflow.app.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.ui.MainViewModel

@Composable
fun StatsScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    val s = state.stats
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Statistics", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        StatLine("❤️ Health", "${s.health}%")
        StatLine("⚡ EP", "${s.ep}")
        StatLine("🏆 Level", "${s.level}")
        StatLine("🔥 Streak", "${s.streak} days")
        StatLine("✅ Completed", "${s.totalCompleted}")
        StatLine("❌ Missed", "${s.totalMissed}")
        StatLine("💪 Recoveries", "${s.totalRecoveries}")
        Spacer(Modifier.height(16.dp))
        Text("Active Debts: ${state.activeDebts.size}", color = Color(0xFFFF5252))
        Text("Active Recoveries: ${state.activeRecoveries.size}", color = Color(0xFFFFAB91))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White)
    }
}
