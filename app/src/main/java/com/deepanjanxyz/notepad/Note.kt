package com.deepanjanxyz.notepad

/**
 * Immutable model for a single note.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val date: String,
)
