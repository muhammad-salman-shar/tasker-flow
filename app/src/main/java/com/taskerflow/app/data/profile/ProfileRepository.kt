package com.taskerflow.app.data.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tasker_profile")

class ProfileRepository(private val context: Context) {

    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_DOB = longPreferencesKey("dob")
    private val KEY_AVATAR = stringPreferencesKey("avatar_uri")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    val profile: Flow<ProfileData> = context.dataStore.data.map { prefs ->
        ProfileData(
            name = prefs[KEY_NAME] ?: "Hunter",
            dobMillis = prefs[KEY_DOB] ?: 0L,
            avatarUri = prefs[KEY_AVATAR] ?: "",
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK)
        )
    }

    suspend fun save(p: ProfileData) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NAME] = p.name
            prefs[KEY_DOB] = p.dobMillis
            prefs[KEY_AVATAR] = p.avatarUri
            prefs[KEY_THEME] = p.themeMode.name
        }
    }
}
