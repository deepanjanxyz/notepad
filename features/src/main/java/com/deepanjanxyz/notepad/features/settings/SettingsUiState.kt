package com.deepanjanxyz.notepad.features.settings

import com.deepanjanxyz.notepad.core.model.NoteStats
import com.deepanjanxyz.notepad.core.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isBiometricLockEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val stats: NoteStats = NoteStats(),
    val appVersion: String = "1.0.5 (Build 5)"
)
