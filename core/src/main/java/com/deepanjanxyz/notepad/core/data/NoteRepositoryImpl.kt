package com.deepanjanxyz.notepad.core.data

import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.model.NoteStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepositoryImpl(
    private val databaseHelper: DatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    private val _notesFlow = MutableStateFlow<List<Note>>(emptyList())

    suspend fun initialize() {
        refreshNotes()
    }

    private suspend fun refreshNotes() {
        withContext(ioDispatcher) {
            val notes = databaseHelper.getAllNotes()
            _notesFlow.value = notes
        }
    }

    override fun observeNotes(): Flow<List<Note>> {
        return _notesFlow.asStateFlow()
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        val trimmed = query.trim()
        return _notesFlow.map { notes ->
            if (trimmed.isEmpty()) {
                notes
            } else {
                notes.filter { note ->
                    note.title.contains(trimmed, ignoreCase = true) ||
                        note.content.contains(trimmed, ignoreCase = true)
                }
            }
        }
    }

    override suspend fun getNoteById(id: Long): Note? = withContext(ioDispatcher) {
        databaseHelper.getNoteById(id)
    }

    override suspend fun insertNote(title: String, content: String, date: String): Long =
        withContext(ioDispatcher) {
            val newId = databaseHelper.insertNote(title, content, date)
            if (newId != -1L) {
                refreshNotes()
            }
            newId
        }

    override suspend fun updateNote(
        id: Long,
        title: String,
        content: String,
        date: String
    ): Boolean = withContext(ioDispatcher) {
        val rows = databaseHelper.updateNote(id, title, content, date)
        if (rows > 0) {
            refreshNotes()
            true
        } else {
            false
        }
    }

    override suspend fun deleteNote(id: Long): Boolean = withContext(ioDispatcher) {
        val rows = databaseHelper.deleteNote(id)
        if (rows > 0) {
            refreshNotes()
            true
        } else {
            false
        }
    }

    override suspend fun deleteNotes(ids: List<Long>): Int = withContext(ioDispatcher) {
        val count = databaseHelper.deleteNotes(ids)
        if (count > 0) {
            refreshNotes()
        }
        count
    }

    override fun observeStats(): Flow<NoteStats> {
        return _notesFlow.map { notes ->
            NoteStats(
                totalNotes = notes.size,
                totalWords = notes.sumOf { it.wordCount },
                totalCharacters = notes.sumOf { it.charCount }
            )
        }
    }
}
