package com.deepanjanxyz.notepad.domain.usecase.label

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class RenameLabelUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(oldName: String, newName: String) {
        val trimmed = newName.trim().take(30)
        if (trimmed.isNotBlank() && trimmed != oldName) {
            repository.renameLabel(oldName, trimmed)
        }
    }
}
