package com.neurasamu.build.solo_leveling_tasker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.neurasamu.build.solo_leveling_tasker.data.profile.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB300),
    secondary = Color(0xFFFF2A6D),
    background = Color(0xFF090A10),
    surface = Color(0xFF17171E),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE65100),
    secondary = Color(0xFF9B1D56),
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF111114),
    onSurface = Color(0xFF111114)
)

@Composable
fun TaskerFlowTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (useDark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
