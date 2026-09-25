package com.deepanjanxyz.notepad.core.ui

sealed interface Screen {
    data object Home : Screen
    data class Editor(val noteId: Long = 0L) : Screen
    data class Drawing(val noteId: Long = 0L) : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Settings : Screen
}

data class NotesUiState(
    val currentScreen: Screen = Screen.Home,
    val isSelectionMode: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val isLocked: Boolean = false,
    val lockEnabled: Boolean = false,
    val themeMode: String = "dark",
    val isGridLayout: Boolean = true,
    val searchQuery: String = "",
    val selectedColorFilter: Int? = null,
    val selectedTagFilter: String? = null,
    val showEditLabelsDialog: Boolean = false
)
