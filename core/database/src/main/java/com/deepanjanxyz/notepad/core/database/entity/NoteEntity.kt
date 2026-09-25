package com.deepanjanxyz.notepad.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deepanjanxyz.notepad.core.model.Note

@Entity(tableName = "notes_table")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,

    @ColumnInfo(name = "TITLE")
    val title: String = "",

    @ColumnInfo(name = "CONTENT")
    val content: String = "",

    @ColumnInfo(name = "DATE")
    val date: String = "",

    @ColumnInfo(name = "PINNED", defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "COLOR_INDEX", defaultValue = "0")
    val colorIndex: Int = 0,

    @ColumnInfo(name = "TAGS", defaultValue = "''")
    val tags: String = "",

    @ColumnInfo(name = "IN_TRASH", defaultValue = "0")
    val inTrash: Boolean = false,

    @ColumnInfo(name = "IN_ARCHIVE", defaultValue = "0")
    val inArchive: Boolean = false,

    @ColumnInfo(name = "REMINDER_TIME")
    val reminderTime: Long? = null
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        date = date,
        isPinned = isPinned,
        colorIndex = colorIndex,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        inTrash = inTrash,
        inArchive = inArchive,
        reminderTime = reminderTime
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            date = note.date,
            isPinned = note.isPinned,
            colorIndex = note.colorIndex,
            tags = note.tags.joinToString(","),
            inTrash = note.inTrash,
            inArchive = note.inArchive,
            reminderTime = note.reminderTime
        )
    }
}
