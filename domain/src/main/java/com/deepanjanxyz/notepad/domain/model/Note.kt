package com.deepanjanxyz.notepad.domain.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val isPinned: Boolean = false,
    val colorIndex: Int = 0,
    val tags: List<String> = emptyList(),
    val inTrash: Boolean = false,
    val inArchive: Boolean = false,
    val reminderTime: Long? = null
)
