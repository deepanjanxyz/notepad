package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class GetNoteByIdUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: Long): Note? = repository.getNoteById(id)
}
