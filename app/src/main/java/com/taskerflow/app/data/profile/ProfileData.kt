package com.taskerflow.app.data.profile

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class ProfileData(
    val name: String = "Hunter",
    val dobMillis: Long = 0L,
    val avatarUri: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK
)
