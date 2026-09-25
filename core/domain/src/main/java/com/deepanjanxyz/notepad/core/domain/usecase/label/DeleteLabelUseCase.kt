package com.deepanjanxyz.notepad.core.domain.usecase.label

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class DeleteLabelUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(name: String) {
        repository.deleteLabel(name)
    }
}
