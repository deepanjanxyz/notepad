package com.deepanjanxyz.notepad.features.notes

import com.deepanjanxyz.notepad.core.model.Note

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val isGridLayout: Boolean = true,
    val isBiometricLocked: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = true
) {
    val selectedCount: Int
        get() = selectedNoteIds.size

    val isAllSelected: Boolean
        get() = filteredNotes.isNotEmpty() && selectedNoteIds.size == filteredNotes.size
}
