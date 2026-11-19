package com.deepanjanxyz.notepad.domain.usecase.widget

import com.deepanjanxyz.notepad.domain.repository.NoteShortcutRepository

/** Drops the note a removed shortcut widget was showing. */
class UnlinkNoteUseCase(private val repository: NoteShortcutRepository) {
    suspend operator fun invoke(appWidgetId: Int) {
        repository.unlink(appWidgetId)
    }
}
