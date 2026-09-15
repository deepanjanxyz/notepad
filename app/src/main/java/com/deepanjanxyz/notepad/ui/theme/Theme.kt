package com.deepanjanxyz.notepad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = Blue300,
    onPrimary = Color(0xFF00325C),
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

@Composable
fun EliteMemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
