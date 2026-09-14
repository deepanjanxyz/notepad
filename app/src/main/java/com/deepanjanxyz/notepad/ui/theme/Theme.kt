package com.deepanjanxyz.notepad.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.deepanjanxyz.notepad.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2FF),
    onPrimaryContainer = Color(0xFF00174A),
    secondary = Color(0xFF004BA0),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF03DAC5),
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8FAFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF00315C),
    tertiary = Color(0xFF03DAC5),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
)

/**
 * Dynamic theme management for the whole app.
 *
 * - Light / dark / system-following mode is controlled by [ThemeMode].
 * - On Android 12+ the palette can follow the user's wallpaper
 *   (Material You dynamic color).
 */
@Composable
fun EliteMemoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
