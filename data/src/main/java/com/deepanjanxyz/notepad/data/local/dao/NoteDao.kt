package com.deepanjanxyz.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.deepanjanxyz.notepad.data.local.entity.NoteEntity
import com.deepanjanxyz.notepad.domain.model.toggleChecklistLine
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 0 AND IN_ARCHIVE = 0 ORDER BY PINNED DESC, ID DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 0 AND IN_ARCHIVE = 1 ORDER BY ID DESC")
    fun getArchiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE IN_TRASH = 1 ORDER BY ID DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    /**
     * The pinned checklist notes the home-screen widget renders.
     *
     * Filtered in SQL so the widget never pulls every note out of the database
     * and inspects it in Kotlin - only the handful of rows it actually shows are
     * read, which keeps a widget refresh cheap however many notes exist.
     */
    @Query(
        "SELECT * FROM notes_table WHERE IN_TRASH = 0 AND IN_ARCHIVE = 0 AND PINNED = 1 " +
            "AND (CONTENT LIKE '%[ ] %' OR CONTENT LIKE '%[x] %') ORDER BY ID DESC"
    )
    suspend fun getPinnedChecklistNotes(): List<NoteEntity>

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

    @Query("UPDATE notes_table SET IN_TRASH = 1, TRASHED_AT = :trashedAt WHERE ID = :id")
    suspend fun moveToTrash(id: Long, trashedAt: Long): Int

    /**
     * Moves every listed note to Trash, however long the list is.
     *
     * The ids are split into batches because one statement cannot bind a whole
     * library of ids, and the batches run in one transaction so a partial move
     * can never be left behind.
     */
    @Transaction
    suspend fun moveNotesToTrash(ids: List<Long>, trashedAt: Long) {
        ids.toStatementBatches().forEach { batch ->
            moveNotesToTrashBatch(batch, trashedAt)
        }
    }

    /** Moves one batch of notes to Trash; the caller keeps batches to a safe size. */
    @Query("UPDATE notes_table SET IN_TRASH = 1, TRASHED_AT = :trashedAt WHERE ID IN (:ids)")
    suspend fun moveNotesToTrashBatch(ids: List<Long>, trashedAt: Long): Int

    @Query("UPDATE notes_table SET IN_TRASH = 0 WHERE ID = :id")
    suspend fun restoreFromTrash(id: Long): Int

    /** Restores every listed note from Trash, in one transaction. */
    @Transaction
    suspend fun restoreNotesFromTrash(ids: List<Long>) {
        ids.toStatementBatches().forEach { batch -> restoreNotesFromTrashBatch(batch) }
    }

    /** Restores one batch of notes from Trash. */
    @Query("UPDATE notes_table SET IN_TRASH = 0 WHERE ID IN (:ids)")
    suspend fun restoreNotesFromTrashBatch(ids: List<Long>): Int

    @Query("UPDATE notes_table SET IN_ARCHIVE = 1, PINNED = 0 WHERE ID = :id")
    suspend fun moveToArchive(id: Long): Int

    /**
     * Archives every listed note, however long the list is.
     *
     * Batched for the same reason as [moveNotesToTrash]: a whole-library
     * selection is more ids than one statement can bind.
     */
    @Transaction
    suspend fun moveNotesToArchive(ids: List<Long>) {
        ids.toStatementBatches().forEach { batch -> moveNotesToArchiveBatch(batch) }
    }

    /** Archives one batch of notes. */
    @Query("UPDATE notes_table SET IN_ARCHIVE = 1, PINNED = 0 WHERE ID IN (:ids)")
    suspend fun moveNotesToArchiveBatch(ids: List<Long>): Int

    @Query("UPDATE notes_table SET IN_ARCHIVE = 0 WHERE ID = :id")
    suspend fun restoreFromArchive(id: Long): Int

    /** Restores every listed note from the archive, in one transaction. */
    @Transaction
    suspend fun restoreNotesFromArchive(ids: List<Long>) {
        ids.toStatementBatches().forEach { batch -> restoreNotesFromArchiveBatch(batch) }
    }

    /** Restores one batch of notes from the archive. */
    @Query("UPDATE notes_table SET IN_ARCHIVE = 0 WHERE ID IN (:ids)")
    suspend fun restoreNotesFromArchiveBatch(ids: List<Long>): Int

    @Query("DELETE FROM notes_table WHERE ID = :id")
    suspend fun deleteNoteById(id: Long): Int

    /** Permanently deletes every listed note, in one transaction. */
    @Transaction
    suspend fun deleteNotesByIds(ids: List<Long>) {
        ids.toStatementBatches().forEach { batch -> deleteNotesByIdsBatch(batch) }
    }

    /** Permanently deletes one batch of notes. */
    @Query("DELETE FROM notes_table WHERE ID IN (:ids)")
    suspend fun deleteNotesByIdsBatch(ids: List<Long>): Int

    @Query("DELETE FROM notes_table WHERE IN_TRASH = 1")
    suspend fun emptyTrash(): Int

    /**
     * Drops trashed notes whose keep-window has passed.
     *
     * Only notes with a real trash timestamp are considered: a row whose
     * TRASHED_AT is still 0 was never trashed by this version, so its fate in
     * the trash is unknown and it is left alone rather than deleted.
     */
    @Query("DELETE FROM notes_table WHERE IN_TRASH = 1 AND TRASHED_AT > 0 AND TRASHED_AT <= :cutoff")
    suspend fun purgeTrashOlderThan(cutoff: Long): Int

    @Query("UPDATE notes_table SET PINNED = :isPinned WHERE ID = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean): Int

    @Query("UPDATE notes_table SET REMINDER_TIME = :reminderTime WHERE ID = :id")
    suspend fun updateReminderTime(id: Long, reminderTime: Long?): Int

    /**
     * Flips one checklist line on a note.
     *
     * Only the tapped line is rewritten, so ordinary text, blank lines and the
     * rest of the formatting come back untouched. The read, the edit and the
     * write happen inside a single transaction, so two rapid taps (or a toggle
     * racing a save from the app) cannot clobber each other.
     *
     * Ticking a task is an edit, so UPDATED_AT moves to now exactly as it would
     * for a save from the editor. Without that the note kept its old "Last
     * modified" time and stayed where it was under that sort order even though
     * the widget had just changed it.
     */
    @Transaction
    suspend fun toggleChecklistLine(id: Long, lineIndex: Int): Boolean {
        val entity = getNoteById(id)
        val lines = entity?.let { it.content.split("\n").toMutableList() }
        val toggled = if (lines != null && lineIndex in lines.indices) {
            lines[lineIndex].toggleChecklistLine()
        } else {
            null
        }
        if (entity == null || lines == null || toggled == null) {
            return false
        }
        lines[lineIndex] = toggled
        updateNote(
            entity.copy(
                content = lines.joinToString("\n"),
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    /**
     * Renames a tag across every note in a single transaction.
     *
     * Tags are denormalized into each note's TAGS column, so a rename has to
     * touch every row that carries the old name. Running the whole sweep inside
     * one transaction avoids the per-row commit that made this an N-write loop.
     */
    @Transaction
    suspend fun renameTag(oldName: String, newName: String) {
        getAllNotesRaw().forEach { entity ->
            val tags = entity.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.any { it.equals(oldName, ignoreCase = true) }) {
                val updatedTags = tags.map { if (it.equals(oldName, ignoreCase = true)) newName else it }
                updateNote(entity.copy(tags = updatedTags.joinToString(",")))
            }
        }
    }

    /** Removes a tag from every note in a single transaction. */
    @Transaction
    suspend fun deleteTag(name: String) {
        getAllNotesRaw().forEach { entity ->
            val tags = entity.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.any { it.equals(name, ignoreCase = true) }) {
                val updatedTags = tags.filterNot { it.equals(name, ignoreCase = true) }
                updateNote(entity.copy(tags = updatedTags.joinToString(",")))
            }
        }
    }
}
