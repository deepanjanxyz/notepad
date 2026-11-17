package com.deepanjanxyz.notepad.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.deepanjanxyz.notepad.domain.repository.NoteShortcutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores each shortcut widget's note in a small preferences file.
 *
 * Preferences are used rather than the Room database because this is a handful of
 * widget-id keys, not user content: it has to be readable from an app widget
 * provider, which runs outside any screen, and it is written once when a widget is
 * placed. Writes commit instead of applying, so the widget that is drawn straight
 * after a choice already sees it.
 */
class NoteShortcutRepositoryImpl(context: Context) : NoteShortcutRepository {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun noteIdFor(appWidgetId: Int): Long? = withContext(Dispatchers.IO) {
        preferences.getLong(keyFor(appWidgetId), NO_NOTE).takeIf { noteId -> noteId != NO_NOTE }
    }

    override suspend fun link(appWidgetId: Int, noteId: Long) {
        withContext(Dispatchers.IO) {
            val stored = preferences.edit().putLong(keyFor(appWidgetId), noteId).commit()
            check(stored) { "The link for widget $appWidgetId could not be stored" }
        }
    }

    override suspend fun unlink(appWidgetId: Int) {
        withContext(Dispatchers.IO) {
            preferences.edit(commit = true) { remove(keyFor(appWidgetId)) }
        }
    }

    private fun keyFor(appWidgetId: Int): String = "$KEY_PREFIX$appWidgetId"

    private companion object {
        const val PREFERENCES_NAME = "note_shortcut_widgets"
        const val KEY_PREFIX = "widget_note_"

        /** The value stored for a widget that has never been configured. */
        const val NO_NOTE = -1L
    }
}
