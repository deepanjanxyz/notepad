package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class ArchiveNoteUseCase(private val repository: NoteRepository) {
    suspend fun moveToArchive(id: Long) {
        repository.moveToArchive(id)
    }

    suspend fun moveNotesToArchive(ids: List<Long>) {
        repository.moveNotesToArchive(ids)
    }

    suspend fun restoreFromArchive(id: Long) {
        repository.restoreFromArchive(id)
    }

    suspend fun restoreNotesFromArchive(ids: List<Long>) {
        repository.restoreNotesFromArchive(ids)
    }
}
