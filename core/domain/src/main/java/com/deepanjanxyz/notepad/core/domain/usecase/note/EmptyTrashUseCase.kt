package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class EmptyTrashUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke() {
        repository.emptyTrash()
    }
}
