package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class TrashNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long) {
        repository.moveToTrash(id)
    }

    suspend operator fun invoke(ids: List<Long>) {
        repository.moveNotesToTrash(ids)
    }
}
