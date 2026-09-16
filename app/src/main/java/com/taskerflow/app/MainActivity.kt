package com.taskerflow.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.taskerflow.app.ui.MainScreen
import com.taskerflow.app.ui.MainViewModel
import com.taskerflow.app.ui.theme.TaskerFlowTheme
import com.taskerflow.app.worker.NotificationHelper

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val state by vm.homeState.collectAsState()
            TaskerFlowTheme(themeMode = state.profile.themeMode) {
                MainScreen(vm)
            }
        }
    }
}
