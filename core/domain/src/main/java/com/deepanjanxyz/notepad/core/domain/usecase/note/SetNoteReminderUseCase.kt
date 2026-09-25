package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class SetNoteReminderUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(noteId: Long, reminderTime: Long?) {
        repository.updateReminderTime(noteId, reminderTime)
    }
}
