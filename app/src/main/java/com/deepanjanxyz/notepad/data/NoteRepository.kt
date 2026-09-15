package com.deepanjanxyz.notepad.data

import android.content.Context
import com.deepanjanxyz.notepad.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data layer: wraps the existing [DatabaseHelper] and exposes suspend-only APIs
 * so that no SQLite call ever runs on the main thread.
 */
class NoteRepository(context: Context) {

    private val helper = DatabaseHelper(context.applicationContext)

    suspend fun getNotes(query: String): List<Note> = withContext(Dispatchers.IO) {
        val cursor = if (query.isBlank()) helper.allNotes else helper.searchNotes(query.trim())
        val notes = mutableListOf<Note>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    notes += it.toNote()
                } while (it.moveToNext())
            }
        }
        notes
    }

    suspend fun getNote(id: Long): Note? = withContext(Dispatchers.IO) {
        helper.getNoteById(id)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toNote() else null
        }
    }

    suspend fun upsert(id: Long, title: String, content: String, date: String): Long =
        withContext(Dispatchers.IO) {
            if (id == -1L) {
                helper.insertNote(title, content, date)
            } else {
                helper.updateNote(id, title, content, date)
                id
            }
        }

    suspend fun deleteNotes(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { helper.deleteNote(it) }
    }

    private fun android.database.Cursor.toNote(): Note = Note(
        getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
        getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE)) ?: "",
        getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT)) ?: "",
        getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)) ?: "",
    )
}
