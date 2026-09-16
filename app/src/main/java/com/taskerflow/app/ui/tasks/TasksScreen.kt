package com.taskerflow.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.ui.MainViewModel

@Composable
fun TasksScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("All Tasks", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        if (state.tasksById.isEmpty()) {
            Text("Koi task nahi. Home se add kar.", color = Color.Gray)
        } else {
            LazyColumn {
                items(state.tasksById.values.toList(), key = { it.id }) { t ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(t.title, color = Color.White)
                            Text("${t.category} • ${t.priority} • ${t.taskType}", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
