package com.deepanjanxyz.notepad.domain.usecase.widget

import com.deepanjanxyz.notepad.domain.repository.NoteShortcutRepository

/** Points a shortcut widget at the note it should open. */
class LinkNoteUseCase(private val repository: NoteShortcutRepository) {
    suspend operator fun invoke(appWidgetId: Int, noteId: Long) {
        repository.link(appWidgetId, noteId)
    }
}
