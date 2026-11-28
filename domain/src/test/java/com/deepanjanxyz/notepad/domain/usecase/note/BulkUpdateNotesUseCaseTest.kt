package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkUpdateNotesUseCaseTest {

    @Test
    fun `setTag strips hash before trimming and caps at 30 chars`() = runBlocking {
        val repo = RecordingNoteRepository()
        val useCase = BulkUpdateNotesUseCase(repo)

        useCase.setTag(
            ids = listOf(1L, 2L, 1L),
            tag = "  #   Work Project   ",
            enabled = true
        )

        assertEquals(listOf(Triple(listOf(1L, 2L), "Work Project", true)), repo.tagCalls)
    }

    @Test
    fun `setTag trims trailing spaces revealed by 30-char cap and skips blank tags`() = runBlocking {
        val repo = RecordingNoteRepository()
        val useCase = BulkUpdateNotesUseCase(repo)

        useCase.setTag(listOf(5L), "###", enabled = true)
        useCase.setTag(listOf(5L), "12345678901234567890123456789 trailing", enabled = false)

        assertEquals(
            listOf(Triple(listOf(5L), "12345678901234567890123456789", false)),
            repo.tagCalls
        )
    }

    @Test
    fun `setColor deduplicates ids and clamps negative color index`() = runBlocking {
        val repo = RecordingNoteRepository()
        val useCase = BulkUpdateNotesUseCase(repo)

        useCase.setColor(emptyList(), 3)
        assertTrue(repo.colorCalls.isEmpty())

        useCase.setColor(listOf(10L, 11L, 10L), -2)
        assertEquals(listOf(listOf(10L, 11L) to 0), repo.colorCalls)
    }

    private class RecordingNoteRepository : NoteRepository {
        val tagCalls = mutableListOf<Triple<List<Long>, String, Boolean>>()
        val colorCalls = mutableListOf<Pair<List<Long>, Int>>()

        override suspend fun updateNotesColor(ids: List<Long>, colorIndex: Int) {
            colorCalls.add(ids to colorIndex)
        }

        override suspend fun setTagForNotes(ids: List<Long>, tag: String, enabled: Boolean) {
            tagCalls.add(Triple(ids, tag, enabled))
        }

        override fun getAllNotes(): Flow<List<Note>> = emptyFlow()
        override fun getArchiveNotes(): Flow<List<Note>> = emptyFlow()
        override fun getTrashNotes(): Flow<List<Note>> = emptyFlow()
        override suspend fun getPinnedChecklistNotes(): List<Note> = emptyList()
        override fun searchNotes(query: String): Flow<List<Note>> = emptyFlow()
        override suspend fun getNoteById(id: Long): Note? = null
        override suspend fun insertOrUpdate(note: Note): Long = 0L
        override suspend fun moveToTrash(id: Long) = Unit
        override suspend fun moveNotesToTrash(ids: List<Long>) = Unit
        override suspend fun restoreFromTrash(id: Long) = Unit
        override suspend fun restoreNotesFromTrash(ids: List<Long>) = Unit
        override suspend fun moveToArchive(id: Long) = Unit
        override suspend fun moveNotesToArchive(ids: List<Long>) = Unit
        override suspend fun restoreFromArchive(id: Long) = Unit
        override suspend fun restoreNotesFromArchive(ids: List<Long>) = Unit
        override suspend fun permanentlyDelete(id: Long) = Unit
        override suspend fun permanentlyDeleteNotes(ids: List<Long>) = Unit
        override suspend fun emptyTrash() = Unit
        override suspend fun purgeExpiredTrash() = Unit
        override suspend fun togglePin(id: Long, isPinned: Boolean) = Unit
        override suspend fun updateReminderTime(id: Long, reminderTime: Long?) = Unit
        override suspend fun toggleChecklistLine(noteId: Long, lineIndex: Int) = Unit
        override fun getAllLabels(): Flow<List<String>> = emptyFlow()
        override suspend fun insertLabel(name: String) = Unit
        override suspend fun renameLabel(oldName: String, newName: String) = Unit
        override suspend fun deleteLabel(name: String) = Unit
    }
}
