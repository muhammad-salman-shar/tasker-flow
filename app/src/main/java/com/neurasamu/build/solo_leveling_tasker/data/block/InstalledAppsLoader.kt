package com.neurasamu.build.solo_leveling_tasker.data.block

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object InstalledAppsLoader {

    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val self = context.packageName
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .asSequence()
            .filter { it.packageName != self }
            .filter { app ->
                // Only show apps that have a launcher intent
                pm.getLaunchIntentForPackage(app.packageName) != null
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
