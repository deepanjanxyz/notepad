package com.deepanjanxyz.notepad.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_DATASTORE_NAME = "notepad_settings"

/**
 * Process-wide DataStore instance backing user settings.
 *
 * Declared as a single top-level delegate so the underlying file is only ever
 * opened once, regardless of how many consumers request it.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME
)
