package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Outcome of persisting a note: the stored row id together with the reminder
 * time that is now in the database.
 *
 * Returning the effective reminder time here lets the caller decide whether a
 * reminder needs to be (re)scheduled without issuing a second read of the row
 * it just wrote.
 */
data class SavedNote(
    val id: Long,
    val reminderTime: Long?
)

class SaveNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(
        id: Long,
        title: String,
        content: String,
        colorIndex: Int,
        tags: List<String>,
        isPinned: Boolean? = null,
        inArchive: Boolean? = null,
        reminderTime: Long? = null
    ): SavedNote {
        val existing = if (id != 0L) repository.getNoteById(id) else null
        val finalReminderTime = reminderTime ?: existing?.reminderTime
        val noteToSave = Note(
            id = id,
            title = title,
            content = content,
            colorIndex = colorIndex,
            tags = tags,
            date = existing?.date ?: SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
            isPinned = isPinned ?: existing?.isPinned ?: false,
            inTrash = existing?.inTrash ?: false,
            inArchive = inArchive ?: existing?.inArchive ?: false,
            reminderTime = finalReminderTime
        )
        val savedId = repository.insertOrUpdate(noteToSave)
        return SavedNote(id = savedId, reminderTime = finalReminderTime)
    }

    suspend operator fun invoke(note: Note): Long {
        return repository.insertOrUpdate(note)
    }
}
