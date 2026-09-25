package com.deepanjanxyz.notepad.core.domain.repository

import com.deepanjanxyz.notepad.core.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getArchiveNotes(): Flow<List<Note>>
    fun getTrashNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun insertOrUpdate(note: Note): Long
    suspend fun moveToTrash(id: Long)
    suspend fun moveNotesToTrash(ids: List<Long>)
    suspend fun restoreFromTrash(id: Long)
    suspend fun restoreNotesFromTrash(ids: List<Long>)
    suspend fun moveToArchive(id: Long)
    suspend fun moveNotesToArchive(ids: List<Long>)
    suspend fun restoreFromArchive(id: Long)
    suspend fun restoreNotesFromArchive(ids: List<Long>)
    suspend fun permanentlyDelete(id: Long)
    suspend fun permanentlyDeleteNotes(ids: List<Long>)
    suspend fun emptyTrash()
    suspend fun togglePin(id: Long, isPinned: Boolean)
    suspend fun updateReminderTime(id: Long, reminderTime: Long?)

    // Room Label management
    fun getAllLabels(): Flow<List<String>>
    suspend fun insertLabel(name: String)
    suspend fun renameLabel(oldName: String, newName: String)
    suspend fun deleteLabel(name: String)
}
