package com.neurasamu.build.solo_leveling_tasker.data.block

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.blockDataStore by preferencesDataStore(name = "slt_blocked_apps")

class BlockedAppsRepository(private val context: Context) {

    private val KEY_BLOCKED = stringSetPreferencesKey("blocked_packages")
    private val KEY_STRICT = stringSetPreferencesKey("strict_mode")

    val blockedPackages: Flow<Set<String>> = context.blockDataStore.data.map { prefs ->
        prefs[KEY_BLOCKED] ?: emptySet()
    }

    val strictMode: Flow<Boolean> = context.blockDataStore.data.map { prefs ->
        (prefs[KEY_STRICT] ?: emptySet()).contains("on")
    }

    suspend fun toggleApp(packageName: String, blocked: Boolean) {
        context.blockDataStore.edit { prefs ->
            val current = prefs[KEY_BLOCKED] ?: emptySet()
            prefs[KEY_BLOCKED] = if (blocked) current + packageName else current - packageName
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.blockDataStore.edit { prefs ->
            prefs[KEY_STRICT] = if (enabled) setOf("on") else emptySet()
        }
    }

    suspend fun clearAll() {
        context.blockDataStore.edit { prefs ->
            prefs[KEY_BLOCKED] = emptySet()
            prefs[KEY_STRICT] = emptySet()
        }
    }
}
