package com.deepanjanxyz.notepad.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.Dp
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.viewmodel.AutoBackupStatus
import com.deepanjanxyz.notepad.ui.viewmodel.ImportOutcome
import com.deepanjanxyz.notepad.ui.viewmodel.NotesUiState
import com.deepanjanxyz.notepad.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.CoroutineScope

/** Everything the screen routes need to draw the current screen. */
internal data class ScreenRouteState(
    val viewModel: NotesViewModel,
    val uiState: NotesUiState,
    val filteredNotes: List<Note>,
    val rawActiveNotes: List<Note>,
    val archiveNotes: List<Note>,
    val trashNotes: List<Note>,
    val allTags: List<String>,
    val autoBackupStatus: AutoBackupStatus?,
    val importOutcome: ImportOutcome?,
    val snackbarHostState: SnackbarHostState,
    val homeSnackbarHostState: SnackbarHostState,
    val snackbarHeight: MutableState<Dp>,
    val drawerState: DrawerState,
    val scope: CoroutineScope
)
