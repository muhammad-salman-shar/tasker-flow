package com.neurasamu.build.solo_leveling_tasker.data.block

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object InstalledAppsLoader {

    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val self = context.packageName

        // Launcher apps (HOME category) — often lack getLaunchIntentForPackage
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val launcherPkgs: Set<String> = pm.queryIntentActivities(homeIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

        // Core system packages to always exclude from user-facing list
        val systemInternal = setOf(
            "com.android.systemui",
            "com.android.providers.settings",
            "com.android.providers.contacts",
            "com.android.providers.media",
            "com.android.providers.telephony",
            "com.android.keychain",
            "com.android.certinstaller",
            "com.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.android.shell"
        )

        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps
            .asSequence()
            .filter { it.packageName != self }
            .filter { it.packageName !in systemInternal }
            .filter { app ->
                val hasLaunch = pm.getLaunchIntentForPackage(app.packageName) != null
                val isLauncher = app.packageName in launcherPkgs
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                // Include if launchable OR home launcher OR a non-system user app
                hasLaunch || isLauncher || !isSystemApp
            }
            .map { app ->
                InstalledApp(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
            .toList()
    }
}
