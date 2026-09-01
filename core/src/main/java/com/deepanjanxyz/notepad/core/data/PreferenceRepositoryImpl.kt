package com.deepanjanxyz.notepad.core.data

import android.content.Context
import android.content.SharedPreferences
import com.deepanjanxyz.notepad.core.model.ThemeMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class PreferenceRepositoryImpl(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PreferenceRepository {

    companion object {
        private const val PREFS_NAME = "com.deepanjanxyz.notepad_preferences"
        private const val KEY_THEME = "pref_theme"
        private const val KEY_LOCK_PRIMARY = "pref_lock"
        private const val KEY_LOCK_SECONDARY = "pref_biometric"
        private const val KEY_GRID_LAYOUT = "pref_grid_layout"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeFlow = MutableStateFlow(readCurrentTheme())
    private val _lockFlow = MutableStateFlow(readCurrentLock())
    private val _gridFlow = MutableStateFlow(readCurrentGrid())

    private fun readCurrentTheme(): ThemeMode {
        val raw = sharedPreferences.getString(KEY_THEME, ThemeMode.SYSTEM.storageKey)
        return ThemeMode.fromStorageKey(raw)
    }

    private fun readCurrentLock(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOCK_PRIMARY, false) ||
            sharedPreferences.getBoolean(KEY_LOCK_SECONDARY, false)
    }

    private fun readCurrentGrid(): Boolean {
        return sharedPreferences.getBoolean(KEY_GRID_LAYOUT, true)
    }

    override fun observeThemeMode(): Flow<ThemeMode> = _themeFlow.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) = withContext(ioDispatcher) {
        sharedPreferences.edit().putString(KEY_THEME, mode.storageKey).apply()
        _themeFlow.value = mode
    }

    override fun observeBiometricLockEnabled(): Flow<Boolean> = _lockFlow.asStateFlow()

    override suspend fun setBiometricLockEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        sharedPreferences.edit()
            .putBoolean(KEY_LOCK_PRIMARY, enabled)
            .putBoolean(KEY_LOCK_SECONDARY, enabled)
            .apply()
        _lockFlow.value = enabled
    }

    override fun observeIsGridLayout(): Flow<Boolean> = _gridFlow.asStateFlow()

    override suspend fun setGridLayout(isGrid: Boolean) = withContext(ioDispatcher) {
        sharedPreferences.edit().putBoolean(KEY_GRID_LAYOUT, isGrid).apply()
        _gridFlow.value = isGrid
    }
}
