package com.deepanjanxyz.notepad.domain.usecase.widget

import com.deepanjanxyz.notepad.domain.repository.NoteShortcutRepository

/** The note a shortcut widget shows, or null when it shows none. */
class GetLinkedNoteIdUseCase(private val repository: NoteShortcutRepository) {
    suspend operator fun invoke(appWidgetId: Int): Long? = repository.noteIdFor(appWidgetId)
}
