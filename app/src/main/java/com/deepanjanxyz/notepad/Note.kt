package com.deepanjanxyz.notepad

/**
 * A single note stored in the local SQLite database.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val date: String,
    val pinned: Boolean = false,
)
