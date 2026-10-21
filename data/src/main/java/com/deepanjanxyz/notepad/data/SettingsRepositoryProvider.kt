package com.deepanjanxyz.notepad.data

import android.content.Context
import com.deepanjanxyz.notepad.data.repository.SettingsRepositoryImpl
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository

/** Wires the SharedPreferences-backed settings repository for the app container. */
object SettingsRepositoryProvider {

    private const val PREFS_NAME = "notepad_prefs"

    fun create(context: Context): SettingsRepository =
        SettingsRepositoryImpl(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
}
