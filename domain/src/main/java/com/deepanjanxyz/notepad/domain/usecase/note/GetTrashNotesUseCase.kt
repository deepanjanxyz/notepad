package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetTrashNotesUseCase(private val repository: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repository.getTrashNotes()
}
