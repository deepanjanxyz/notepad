package com.deepanjanxyz.notepad.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val theme: String = SettingsRepository.THEME_SYSTEM,
    val lockEnabled: Boolean = false,
)

/**
 * Data layer: settings backed by SharedPreferences (same backing file the legacy
 * PreferenceManager-based screens used, so existing user preferences survive).
 * Exposed as a [StateFlow] so the Compose UI can react to changes instantly.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _settings.value = read()
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK, enabled).apply()
        _settings.value = read()
    }

    private fun read(): AppSettings = AppSettings(
        theme = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM,
        lockEnabled = prefs.getBoolean(KEY_LOCK, false),
    )

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        // Keys kept identical to the legacy preferences.xml / MainActivity so
        // existing user settings carry over.
        private const val KEY_THEME = "pref_theme"
        private const val KEY_LOCK = "pref_lock"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
