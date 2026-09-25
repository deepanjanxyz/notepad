package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class TogglePinUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long, isPinned: Boolean) {
        repository.togglePin(id, isPinned)
    }
}
