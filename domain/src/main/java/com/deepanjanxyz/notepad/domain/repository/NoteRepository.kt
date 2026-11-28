package com.deepanjanxyz.notepad.domain.repository

import com.deepanjanxyz.notepad.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getArchiveNotes(): Flow<List<Note>>
    fun getTrashNotes(): Flow<List<Note>>

    /** The pinned checklist notes the home-screen widget shows. */
    suspend fun getPinnedChecklistNotes(): List<Note>
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

    /** Permanently deletes trashed notes kept longer than the retention window. */
    suspend fun purgeExpiredTrash()

    suspend fun togglePin(id: Long, isPinned: Boolean)
    suspend fun updateReminderTime(id: Long, reminderTime: Long?)

    /** Flips a single checklist line on a note, atomically. */
    suspend fun toggleChecklistLine(noteId: Long, lineIndex: Int)

    suspend fun updateNotesColor(ids: List<Long>, colorIndex: Int)
    suspend fun setTagForNotes(ids: List<Long>, tag: String, enabled: Boolean)

    // Room Label management
    fun getAllLabels(): Flow<List<String>>
    suspend fun insertLabel(name: String)
    suspend fun renameLabel(oldName: String, newName: String)
    suspend fun deleteLabel(name: String)

    companion object {
        /** How long a deleted note stays in the trash before it is purged. */
        const val TRASH_RETENTION_DAYS = 30
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        /** The same window in milliseconds. */
        const val TRASH_RETENTION_MILLIS = TRASH_RETENTION_DAYS * MILLIS_PER_DAY
    }
}
