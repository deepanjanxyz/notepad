package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class TogglePinUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long, isPinned: Boolean) {
        repository.togglePin(id, isPinned)
    }
}
