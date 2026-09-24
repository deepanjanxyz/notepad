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

    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query).map { notes ->
        notes.map { it.toDomain() }
    }

    override suspend fun getNoteById(id: Long): Note? = withContext(ioDispatcher) {
        noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun insertOrUpdate(note: Note): Long = withContext(ioDispatcher) {
        val entity = NoteEntity.fromDomain(note)
        if (note.id <= 0L) {
            noteDao.insertNote(entity)
        } else {
            noteDao.updateNote(entity)
            note.id
        }
    }

    override suspend fun moveToTrash(id: Long) = withContext(ioDispatcher) {
        noteDao.moveToTrash(id)
        Unit
    }

    override suspend fun moveNotesToTrash(ids: List<Long>) = withContext(ioDispatcher) {
        noteDao.moveNotesToTrash(ids)
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

    override suspend fun togglePin(id: Long, isPinned: Boolean) = withContext(ioDispatcher) {
        noteDao.setPinned(id, isPinned)
        Unit
    }

    override suspend fun updateReminderTime(id: Long, reminderTime: Long?) = withContext(ioDispatcher) {
        noteDao.updateReminderTime(id, reminderTime)
        Unit
    }

    override fun getAllLabels(): Flow<List<String>> = labelDao.getAllLabels().map { labels ->
        labels.map(LabelEntity::name)
    }

    override suspend fun insertLabel(name: String) = withContext(ioDispatcher) {
        val trimmed = name.trim().replace("#", "").take(30)
        if (trimmed.isNotEmpty()) {
            val existing = labelDao.getLabelByName(trimmed)
            if (existing == null) {
                labelDao.insertLabel(LabelEntity(name = trimmed))
            }
        }
        Unit
    }

    override suspend fun renameLabel(oldName: String, newName: String) = withContext(ioDispatcher) {
        val cleanNew = newName.trim().replace("#", "")
        if (cleanNew.isNotEmpty() && !cleanNew.equals(oldName, ignoreCase = true)) {
            labelDao.renameLabel(oldName, cleanNew)
            // Labels are denormalized into each note, so keep existing note tags in sync.
            val notes = noteDao.getAllNotesRaw()
            notes.forEach { noteEntity ->
                val tags = noteEntity.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (tags.any { it.equals(oldName, ignoreCase = true) }) {
                    val updatedTags = tags.map { if (it.equals(oldName, ignoreCase = true)) cleanNew else it }
                    noteDao.updateNote(noteEntity.copy(tags = updatedTags.joinToString(",")))
                }
            }
        }
        Unit
    }

    override suspend fun deleteLabel(name: String) = withContext(ioDispatcher) {
        labelDao.deleteByName(name)
        // Labels are denormalized into each note, so remove the tag there as well.
        val notes = noteDao.getAllNotesRaw()
        notes.forEach { noteEntity ->
            val tags = noteEntity.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.any { it.equals(name, ignoreCase = true) }) {
                val updatedTags = tags.filter { !it.equals(name, ignoreCase = true) }
                noteDao.updateNote(noteEntity.copy(tags = updatedTags.joinToString(",")))
            }
        }
        Unit
    }
}
