package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class EmptyTrashUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke() {
        repository.emptyTrash()
    }
}
