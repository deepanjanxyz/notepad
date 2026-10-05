package com.deepanjanxyz.notepad.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_DATASTORE_NAME = "notepad_settings"

/**
 * Name of the legacy SharedPreferences file used before settings moved to
 * DataStore. Its entries are migrated into DataStore on first access, so an
 * existing install keeps its theme, grid-layout and lock settings.
 */
private const val LEGACY_SETTINGS_PREFS = "notepad_prefs"

/**
 * Process-wide DataStore instance backing user settings.
 *
 * Declared as a single top-level delegate so the underlying file is only ever
 * opened once, regardless of how many consumers request it. A
 * [SharedPreferencesMigration] copies the values written by older builds out of
 * [LEGACY_SETTINGS_PREFS] before the first read, so reusing the same preference
 * key names is not enough on its own.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_SETTINGS_PREFS))
    }
)
