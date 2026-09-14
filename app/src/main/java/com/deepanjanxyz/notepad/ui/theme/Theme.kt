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

/** Purple FAB colour used across the app (matches the reference design). */
val FabPurple = Color(0xFF42526E)

/** Yellow/gold accent used for pin icons, selected states and radio buttons. */
val AccentYellow = Color(0xFFF9AB00)

/** Dark red used for trash sub-header. */
val TrashRed = Color(0xFFB3261E)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD3E3FD),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF9AA0A6),
    onSecondary = Color(0xFF1F1F1F),
    secondaryContainer = Color(0xFF2F3031),
    onSecondaryContainer = Color(0xFFE3E3E3),
    tertiary = Color(0xFFF9AB00),
    onTertiary = Color(0xFF3C2E00),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1E1F20),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2F3031),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFF3F4046),
    outlineVariant = Color(0xFF2F3031),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF5F6368),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = Color(0xFF3C4043),
    tertiary = Color(0xFFF9AB00),
    onTertiary = Color(0xFF3C2E00),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C6C9),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
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
