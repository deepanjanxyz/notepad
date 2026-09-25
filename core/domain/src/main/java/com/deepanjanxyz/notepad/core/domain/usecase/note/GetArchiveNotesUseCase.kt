package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetArchiveNotesUseCase(private val repository: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repository.getArchiveNotes()
}
