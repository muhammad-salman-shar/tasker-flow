package com.taskerflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.taskerflow.app.ui.MainScreen
import com.taskerflow.app.ui.MainViewModel
import com.taskerflow.app.ui.theme.TaskerFlowTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.homeState.collectAsState()
            TaskerFlowTheme(themeMode = state.profile.themeMode) {
                MainScreen(vm)
            }
        }
    }
}
