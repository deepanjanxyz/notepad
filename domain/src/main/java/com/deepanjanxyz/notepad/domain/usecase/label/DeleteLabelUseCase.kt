package com.deepanjanxyz.notepad.domain.usecase.label

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class DeleteLabelUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(name: String) {
        repository.deleteLabel(name)
    }
}
