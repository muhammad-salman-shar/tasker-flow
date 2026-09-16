package com.taskerflow.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.taskerflow.app.TaskerApp
import com.taskerflow.app.ui.block.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastForegroundPkg: String? = null
    private var healthBelow50 = false

    companion object {
        // Exposed so UI can show "enabled / disabled"
        val running = MutableStateFlow(false)

        // Apps always allowed (essential)
        val ESSENTIALS = setOf(
            "com.android.dialer",
            "com.android.phone",
            "com.google.android.dialer",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.android.systemui",
            "com.android.settings",
            "com.taskerflow.app"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        running.value = true

        // Observe health in background to know when to start blocking
        scope.launch {
            val app = application as TaskerApp
            app.repository.observeStats().collect { stats ->
                healthBelow50 = stats != null && stats.health < 50
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == lastForegroundPkg) return
        lastForegroundPkg = pkg

        if (!healthBelow50) return
        if (pkg == packageName) return
        if (pkg in ESSENTIALS) return
        if (pkg.startsWith("com.android.")) return

        scope.launch {
            val app = application as TaskerApp
            val strict = app.blockedAppsRepository.strictMode.first()
            val blockedSet = app.blockedAppsRepository.blockedPackages.first()

            val shouldBlock = if (strict) {
                // Strict: everything not essential is blocked
                true
            } else {
                pkg in blockedSet
            }

            if (shouldBlock) {
                launchBlockerScreen(pkg)
            }
        }
    }

    private fun launchBlockerScreen(pkg: String) {
        val label = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) { pkg }

        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("blocked_app", label)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        running.value = false
        super.onDestroy()
    }
}
