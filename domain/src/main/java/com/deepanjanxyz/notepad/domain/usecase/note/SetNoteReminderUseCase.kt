package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class SetNoteReminderUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(noteId: Long, reminderTime: Long?) {
        repository.updateReminderTime(noteId, reminderTime)
    }
}
