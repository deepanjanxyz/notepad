package com.deepanjanxyz.notepad.features.editor

data class NoteEditorUiState(
    val noteId: Long = -1L,
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val isSaved: Boolean = true,
    val isMarkdownPreview: Boolean = false,
    val isLoading: Boolean = false
) {
    val wordCount: Int
        get() {
            val trimmed = content.trim()
            return if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
        }

    val charCount: Int
        get() = content.length

    val isBlank: Boolean
        get() = title.isBlank() && content.isBlank()
}
