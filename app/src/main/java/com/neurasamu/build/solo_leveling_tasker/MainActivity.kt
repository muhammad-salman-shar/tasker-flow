package com.neurasamu.build.solo_leveling_tasker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neurasamu.build.solo_leveling_tasker.ui.MainScreen
import com.neurasamu.build.solo_leveling_tasker.ui.MainViewModel
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TaskerFlowTheme
import com.neurasamu.build.solo_leveling_tasker.worker.NotificationHelper

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

    override fun onResume() {
        super.onResume()
        // Recalculate penalty state + fresh UI on every foreground entry
        vm.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // App brought to front via existing instance — force refresh
        vm.refresh()
    }
}
