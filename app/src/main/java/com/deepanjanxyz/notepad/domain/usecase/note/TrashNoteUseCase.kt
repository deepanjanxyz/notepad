package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class TrashNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long) {
        repository.moveToTrash(id)
    }

    suspend operator fun invoke(ids: List<Long>) {
        repository.moveNotesToTrash(ids)
    }
}
