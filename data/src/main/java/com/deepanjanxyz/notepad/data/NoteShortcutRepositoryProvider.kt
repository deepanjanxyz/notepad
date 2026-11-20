package com.deepanjanxyz.notepad.data

import android.content.Context
import com.deepanjanxyz.notepad.data.repository.NoteShortcutRepositoryImpl
import com.deepanjanxyz.notepad.domain.repository.NoteShortcutRepository

object NoteShortcutRepositoryProvider {
    /**
     * Builds the shortcut store for [context]. The values live in a preferences
     * file that Android hands back as one shared instance per process, so callers
     * may build this freely instead of holding on to it.
     */
    fun create(context: Context): NoteShortcutRepository = NoteShortcutRepositoryImpl(context)
}
