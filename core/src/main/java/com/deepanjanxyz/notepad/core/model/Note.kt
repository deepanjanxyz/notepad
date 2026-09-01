package com.deepanjanxyz.notepad.core.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val date: String = ""
) {
    val wordCount: Int
        get() {
            val trimmed = content.trim()
            return if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
        }

    val charCount: Int
        get() = content.length

    val previewText: String
        get() {
            val lines = content.lines().filter { it.isNotBlank() }
            return lines.take(3).joinToString("\n")
        }
}
