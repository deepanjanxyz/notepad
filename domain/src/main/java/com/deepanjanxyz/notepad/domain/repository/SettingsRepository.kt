package com.deepanjanxyz.notepad.domain.repository

import com.deepanjanxyz.notepad.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and writing user settings.
 *
 * The implementation lives in the data layer; the presentation layer only ever
 * observes [settings] or mutates it through the setters below.
 */
interface SettingsRepository {

    /** Emits the current settings and every subsequent change. */
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: String)

    suspend fun setGridLayout(isGridLayout: Boolean)

    suspend fun setLockEnabled(enabled: Boolean)
}
