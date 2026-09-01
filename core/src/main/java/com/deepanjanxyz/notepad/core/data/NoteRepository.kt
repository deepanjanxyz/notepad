package com.deepanjanxyz.notepad.core.data

import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.model.NoteStats
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun insertNote(title: String, content: String, date: String): Long
    suspend fun updateNote(id: Long, title: String, content: String, date: String): Boolean
    suspend fun deleteNote(id: Long): Boolean
    suspend fun deleteNotes(ids: List<Long>): Int
    fun observeStats(): Flow<NoteStats>
}
