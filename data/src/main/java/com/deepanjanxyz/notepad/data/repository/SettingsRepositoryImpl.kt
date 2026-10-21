package com.deepanjanxyz.notepad.data.repository

import android.content.SharedPreferences
import com.deepanjanxyz.notepad.domain.model.AppSettings
import com.deepanjanxyz.notepad.domain.model.ThemeMode
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * SharedPreferences-backed implementation of [SettingsRepository].
 *
 * The key names are the ones the settings have always used, so a value written
 * by an older build is still read unchanged.
 */
class SettingsRepositoryImpl(
    private val prefs: SharedPreferences
) : SettingsRepository {

    private val state = MutableStateFlow(read())

    // SharedPreferences keeps only a weak reference to listeners, so the field
    // holds the strong one.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        state.value = read()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(themeMode: String) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode).apply()
    }

    override suspend fun setGridLayout(isGridLayout: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_LAYOUT, isGridLayout).apply()
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    override suspend fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    private fun read(): AppSettings = AppSettings(
        themeMode = prefs.getString(KEY_THEME_MODE, null) ?: ThemeMode.DARK,
        isGridLayout = prefs.getBoolean(KEY_GRID_LAYOUT, true),
        lockEnabled = prefs.getBoolean(KEY_LOCK_ENABLED, false),
        autoBackup = prefs.getBoolean(KEY_AUTO_BACKUP, false)
    )

    private companion object {
        const val KEY_THEME_MODE = "pref_theme"
        const val KEY_GRID_LAYOUT = "pref_grid_layout"
        const val KEY_LOCK_ENABLED = "pref_lock"
        const val KEY_AUTO_BACKUP = "pref_auto_backup"
    }
}
