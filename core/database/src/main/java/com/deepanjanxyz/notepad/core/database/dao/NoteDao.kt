package com.deepanjanxyz.notepad.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepanjanxyz.notepad.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 0 AND IN_ARCHIVE = 0 ORDER BY PINNED DESC, ID DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 0 AND IN_ARCHIVE = 1 ORDER BY ID DESC")
    fun getArchiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 1 ORDER BY ID DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 0 AND IN_ARCHIVE = 0 AND (TITLE LIKE '%' || :query || '%' OR CONTENT LIKE '%' || :query || '%' OR TAGS LIKE '%' || :query || '%') ORDER BY PINNED DESC, ID DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table")
    suspend fun getAllNotesRaw(): List<NoteEntity>

    @Query("SELECT * FROM notes_table WHERE ID = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(entity: NoteEntity): Long

    @Update
    suspend fun updateNote(entity: NoteEntity): Int

    @Query("UPDATE notes_table SET IN_TRASH = 1 WHERE ID = :id")
    suspend fun moveToTrash(id: Long): Int

    @Query("UPDATE notes_table SET IN_TRASH = 1 WHERE ID IN (:ids)")
    suspend fun moveNotesToTrash(ids: List<Long>): Int

    @Query("UPDATE notes_table SET IN_TRASH = 0 WHERE ID = :id")
    suspend fun restoreFromTrash(id: Long): Int

    @Query("UPDATE notes_table SET IN_TRASH = 0 WHERE ID IN (:ids)")
    suspend fun restoreNotesFromTrash(ids: List<Long>): Int

    @Query("UPDATE notes_table SET IN_ARCHIVE = 1, PINNED = 0 WHERE ID = :id")
    suspend fun moveToArchive(id: Long): Int

    @Query("UPDATE notes_table SET IN_ARCHIVE = 1, PINNED = 0 WHERE ID IN (:ids)")
    suspend fun moveNotesToArchive(ids: List<Long>): Int

    @Query("UPDATE notes_table SET IN_ARCHIVE = 0 WHERE ID = :id")
    suspend fun restoreFromArchive(id: Long): Int

    @Query("UPDATE notes_table SET IN_ARCHIVE = 0 WHERE ID IN (:ids)")
    suspend fun restoreNotesFromArchive(ids: List<Long>): Int

    @Query("DELETE FROM notes_table WHERE ID = :id")
    suspend fun deleteNoteById(id: Long): Int

    @Query("DELETE FROM notes_table WHERE ID IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<Long>): Int

    @Query("DELETE FROM notes_table WHERE IN_TRASH = 1")
    suspend fun emptyTrash(): Int

    @Query("UPDATE notes_table SET PINNED = :isPinned WHERE ID = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean): Int

    @Query("UPDATE notes_table SET REMINDER_TIME = :reminderTime WHERE ID = :id")
    suspend fun updateReminderTime(id: Long, reminderTime: Long?): Int
}
