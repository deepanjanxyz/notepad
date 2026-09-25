package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository

class GetNoteByIdUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long): Note? = repository.getNoteById(id)
}
