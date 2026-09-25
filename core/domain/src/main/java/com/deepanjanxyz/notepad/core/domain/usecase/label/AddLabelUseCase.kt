package com.deepanjanxyz.notepad.core.domain.usecase.label

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class AddLabelUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(labelName: String) {
        val trimmed = labelName.trim().replace("#", "").take(30)
        if (trimmed.isNotBlank()) {
            repository.insertLabel(trimmed)
        }
    }
}
