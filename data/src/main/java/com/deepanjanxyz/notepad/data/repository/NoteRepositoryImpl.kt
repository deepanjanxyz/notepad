package com.deepanjanxyz.notepad.data.repository

import com.deepanjanxyz.notepad.data.local.dao.LabelDao
import com.deepanjanxyz.notepad.data.local.dao.NoteDao
import com.deepanjanxyz.notepad.data.local.entity.LabelEntity
import com.deepanjanxyz.notepad.data.local.entity.NoteEntity
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes().map { notes ->
        notes.map { it.toDomain() }
    }

    override fun getArchiveNotes(): Flow<List<Note>> = noteDao.getArchiveNotes().map { notes ->
        notes.map { it.toDomain() }
    }

    override fun getTrashNotes(): Flow<List<Note>> = noteDao.getTrashNotes().map { notes ->
        notes.map { it.toDomain() }
    }

    override suspend fun getPinnedChecklistNotes(): List<Note> = withContext(ioDispatcher) {
        noteDao.getPinnedChecklistNotes().map { it.toDomain() }
    }

    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query).map { notes ->
        notes.map { it.toDomain() }
    }

    override suspend fun getNoteById(id: Long): Note? = withContext(ioDispatcher) {
        noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun insertOrUpdate(note: Note): Long = withContext(ioDispatcher) {
        val entity = NoteEntity.fromDomain(note)
        val id = if (note.id <= 0L) {
            noteDao.insertNote(entity)
        } else {
            noteDao.updateNote(entity)
            note.id
        }
        // A note can arrive carrying tags that are not in the label table yet - a
        // backup restore, an import, or a note recovered after clearing app data -
        // and a tag missing from that table cannot be renamed or deleted from the
        // label screen. Registering the note's tags here keeps every stored note's
        // tags manageable.
        registerTags(note.tags)
        id
    }

    /** Adds any of [tags] that the label table does not already hold. */
    private suspend fun registerTags(tags: List<String>) {
        tags.forEach { tag ->
            // The same 30-character limit the label editor and the label use cases
            // apply, so a restored backup cannot introduce a label the editor is
            // unable to recreate.
            val name = tag.replace("#", "").trim().take(30).trim()
            if (name.isNotEmpty() && labelDao.getLabelByName(name) == null) {
                labelDao.insertLabel(LabelEntity(name = name))
            }
        }
    }

    override suspend fun moveToTrash(id: Long) = withContext(ioDispatcher) {
        noteDao.moveToTrash(id, System.currentTimeMillis())
        Unit
    }

    override suspend fun moveNotesToTrash(ids: List<Long>) = withContext(ioDispatcher) {
        noteDao.moveNotesToTrash(ids, System.currentTimeMillis())
        Unit
    }

    override suspend fun restoreFromTrash(id: Long) = withContext(ioDispatcher) {
        noteDao.restoreFromTrash(id)
        Unit
    }

    override suspend fun restoreNotesFromTrash(ids: List<Long>) = withContext(ioDispatcher) {
        noteDao.restoreNotesFromTrash(ids)
        Unit
    }

    override suspend fun moveToArchive(id: Long) = withContext(ioDispatcher) {
        noteDao.moveToArchive(id)
        Unit
    }

    override suspend fun moveNotesToArchive(ids: List<Long>) = withContext(ioDispatcher) {
        noteDao.moveNotesToArchive(ids)
        Unit
    }

    override suspend fun restoreFromArchive(id: Long) = withContext(ioDispatcher) {
        noteDao.restoreFromArchive(id)
        Unit
    }

    override suspend fun restoreNotesFromArchive(ids: List<Long>) = withContext(ioDispatcher) {
        noteDao.restoreNotesFromArchive(ids)
        Unit
    }

    override suspend fun permanentlyDelete(id: Long) = withContext(ioDispatcher) {
        noteDao.deleteNoteById(id)
        Unit
    }

    override suspend fun permanentlyDeleteNotes(ids: List<Long>) = withContext(ioDispatcher) {
        noteDao.deleteNotesByIds(ids)
        Unit
    }

    override suspend fun emptyTrash() = withContext(ioDispatcher) {
        noteDao.emptyTrash()
        Unit
    }

    override suspend fun purgeExpiredTrash() = withContext(ioDispatcher) {
        val cutoff = System.currentTimeMillis() - NoteRepository.TRASH_RETENTION_MILLIS
        noteDao.purgeTrashOlderThan(cutoff)
        Unit
    }

    override suspend fun togglePin(id: Long, isPinned: Boolean) = withContext(ioDispatcher) {
        noteDao.setPinned(id, isPinned)
        Unit
    }

    override suspend fun toggleChecklistLine(noteId: Long, lineIndex: Int) = withContext(ioDispatcher) {
        noteDao.toggleChecklistLine(noteId, lineIndex)
        Unit
    }

    override suspend fun updateReminderTime(id: Long, reminderTime: Long?) = withContext(ioDispatcher) {
        noteDao.updateReminderTime(id, reminderTime)
        Unit
    }

    override suspend fun updateNotesColor(ids: List<Long>, colorIndex: Int) = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext
        noteDao.updateNotesColor(ids, colorIndex, System.currentTimeMillis())
        Unit
    }

    override suspend fun setTagForNotes(ids: List<Long>, tag: String, enabled: Boolean) = withContext(ioDispatcher) {
        val cleanTag = tag.replace("#", "").trim().take(30).trim()
        if (ids.isEmpty() || cleanTag.isEmpty()) return@withContext
        if (enabled) {
            registerTags(listOf(cleanTag))
        }
        noteDao.setTagForNotes(ids, cleanTag, enabled, System.currentTimeMillis())
        Unit
    }

    override fun getAllLabels(): Flow<List<String>> = labelDao.getAllLabels().map { labels ->
        labels.map(LabelEntity::name)
    }

    override suspend fun insertLabel(name: String) = withContext(ioDispatcher) {
        val trimmed = name.replace("#", "").trim().take(30).trim()
        if (trimmed.isNotEmpty()) {
            val existing = labelDao.getLabelByName(trimmed)
            if (existing == null) {
                labelDao.insertLabel(LabelEntity(name = trimmed))
            }
        }
        Unit
    }

    override suspend fun renameLabel(oldName: String, newName: String) = withContext(ioDispatcher) {
        val cleanNew = newName.replace("#", "").trim().take(30).trim()
        if (cleanNew.isNotEmpty() && !cleanNew.equals(oldName, ignoreCase = true)) {
            labelDao.renameLabel(oldName, cleanNew)
            // Labels are denormalized into each note, so the note rows are kept
            // in sync inside a single database transaction rather than one write
            // per note.
            noteDao.renameTag(oldName, cleanNew)
        }
        Unit
    }

    override suspend fun deleteLabel(name: String) = withContext(ioDispatcher) {
        labelDao.deleteByName(name)
        // Labels are denormalized into each note, so the tag is removed there too.
        noteDao.deleteTag(name)
        Unit
    }
}
