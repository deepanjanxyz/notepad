package com.deepanjanxyz.notepad.domain.usecase.label

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class AddLabelUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(labelName: String) {
        val trimmed = labelName.replace("#", "").trim().take(30).trim()
        if (trimmed.isNotBlank()) {
            repository.insertLabel(trimmed)
        }
    }
}
