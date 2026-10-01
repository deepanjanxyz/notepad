package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class RestoreNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long) {
        repository.restoreFromTrash(id)
    }

    suspend operator fun invoke(ids: List<Long>) {
        repository.restoreNotesFromTrash(ids)
    }
}
