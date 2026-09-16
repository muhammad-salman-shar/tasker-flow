package com.taskerflow.app.data.block

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object InstalledAppsLoader {

    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val self = context.packageName
        val launchable = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return launchable
            .asSequence()
            .filter { it.packageName != self }
            .filter { app ->
                // Only apps with a launch intent (user-facing apps)
                pm.getLaunchIntentForPackage(app.packageName) != null ||
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { app ->
                InstalledApp(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
