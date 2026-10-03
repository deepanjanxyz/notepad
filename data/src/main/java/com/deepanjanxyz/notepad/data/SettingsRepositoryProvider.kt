package com.deepanjanxyz.notepad.data

import android.content.Context
import com.deepanjanxyz.notepad.data.local.datastore.settingsDataStore
import com.deepanjanxyz.notepad.data.repository.SettingsRepositoryImpl
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository

/** Wires the DataStore-backed settings repository for the app container. */
object SettingsRepositoryProvider {
    fun create(context: Context): SettingsRepository =
        SettingsRepositoryImpl(context.applicationContext.settingsDataStore)
}
