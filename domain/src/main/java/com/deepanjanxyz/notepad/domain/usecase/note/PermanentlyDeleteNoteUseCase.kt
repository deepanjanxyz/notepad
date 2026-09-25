package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class PermanentlyDeleteNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long) {
        repository.permanentlyDelete(id)
    }

    suspend operator fun invoke(ids: List<Long>) {
        repository.permanentlyDeleteNotes(ids)
    }
}
