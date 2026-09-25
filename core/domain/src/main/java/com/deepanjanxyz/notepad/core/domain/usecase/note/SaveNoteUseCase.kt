package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.domain.repository.NoteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    ): Long {
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
        return repository.insertOrUpdate(noteToSave)
    }

    suspend operator fun invoke(note: Note): Long {
        return repository.insertOrUpdate(note)
    }
}
