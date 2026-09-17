package com.neurasamu.build.solo_leveling_tasker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.neurasamu.build.solo_leveling_tasker.TaskerApp
import com.neurasamu.build.solo_leveling_tasker.ui.block.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var blockedSet: Set<String> = emptySet()
    @Volatile private var strictMode: Boolean = false
    @Volatile private var healthBelow50: Boolean = false
    @Volatile private var criticalActive: Boolean = false

    private var lastBlockedPkg: String? = null
    private var lastBlockedAt: Long = 0L

    companion object {
        val testMode = MutableStateFlow(false)
        val running = MutableStateFlow(false)

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
            "com.neurasamu.build.solo_leveling_tasker"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        running.value = true

        val app = application as TaskerApp

        scope.launch {
            app.blockedAppsRepository.blockedPackages.collect { blockedSet = it }
        }
        scope.launch {
            app.blockedAppsRepository.strictMode.collect { strictMode = it }
        }
        scope.launch {
            app.repository.observeStats().collect { stats ->
                healthBelow50 = stats != null && stats.health < 50
                criticalActive = stats != null && stats.criticalActiveOccurrenceId != 0L
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        // Fast path: no lock active → skip
        if (!healthBelow50 && !testMode.value && !criticalActive) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg in ESSENTIALS) return
        if (pkg.startsWith("com.android.")) return

        val shouldBlock = when {
            // Critical active → lock only user-selected apps (respect whitelist)
            criticalActive -> pkg in blockedSet
            // Test mode → use selected apps only (demo)
            testMode.value -> if (strictMode) true else pkg in blockedSet
            // Health < 50 → strict mode blocks all, else selected
            healthBelow50 -> if (strictMode) true else pkg in blockedSet
            else -> false
        }
        if (!shouldBlock) return

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && (now - lastBlockedAt) < 1500L) return
        lastBlockedPkg = pkg
        lastBlockedAt = now

        launchBlockerScreen(pkg)
    }

    private fun launchBlockerScreen(pkg: String) {
        val label = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) { pkg }

        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
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
