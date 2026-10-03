package com.deepanjanxyz.notepad.domain.model

/**
 * Supported theme modes. Kept as string constants so the value can be persisted
 * and compared without leaking an Android enum into the domain layer.
 */
object ThemeMode {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val SYSTEM = "system"
}

/**
 * User-configurable application settings.
 *
 * This is a pure domain model: it carries no knowledge of how the values are
 * stored (DataStore, SharedPreferences, ...). Defaults describe a first-run
 * install.
 */
data class AppSettings(
    val themeMode: String = ThemeMode.DARK,
    val isGridLayout: Boolean = true,
    val lockEnabled: Boolean = false
)
