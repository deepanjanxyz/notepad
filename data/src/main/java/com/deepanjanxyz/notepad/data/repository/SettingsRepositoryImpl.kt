package com.deepanjanxyz.notepad.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.deepanjanxyz.notepad.domain.model.AppSettings
import com.deepanjanxyz.notepad.domain.model.ThemeMode
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore-backed implementation of [SettingsRepository].
 *
 * The preference keys are preserved from the legacy SharedPreferences
 * implementation so that values already written by older builds are picked up
 * unchanged.
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("pref_theme")
        val GRID_LAYOUT = booleanPreferencesKey("pref_grid_layout")
        val LOCK_ENABLED = booleanPreferencesKey("pref_lock")
    }

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            // A corrupt or unreadable file should fall back to defaults rather
            // than crash the app; genuine errors are rethrown.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[Keys.THEME_MODE] ?: ThemeMode.DARK,
                isGridLayout = preferences[Keys.GRID_LAYOUT] ?: true,
                lockEnabled = preferences[Keys.LOCK_ENABLED] ?: false
            )
        }

    override suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences -> preferences[Keys.THEME_MODE] = themeMode }
    }

    override suspend fun setGridLayout(isGridLayout: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.GRID_LAYOUT] = isGridLayout }
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.LOCK_ENABLED] = enabled }
    }
}
