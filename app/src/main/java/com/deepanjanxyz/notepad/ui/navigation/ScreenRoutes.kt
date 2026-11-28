package com.deepanjanxyz.notepad.ui.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.screens.ArchiveScreen
import com.deepanjanxyz.notepad.ui.screens.DrawingScreen
import com.deepanjanxyz.notepad.ui.screens.HomeScreen
import com.deepanjanxyz.notepad.ui.screens.NoteEditorScreen
import com.deepanjanxyz.notepad.ui.screens.SettingsScreen
import com.deepanjanxyz.notepad.ui.screens.TrashScreen
import com.deepanjanxyz.notepad.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Draws whichever screen the app is on. */
@Composable
internal fun ScreenRoutes(routes: ScreenRouteState, screen: Screen) {
    when (screen) {
        is Screen.Home -> HomeRoute(routes = routes)
        is Screen.Editor -> EditorRoute(routes = routes, screen = screen)
        is Screen.Drawing -> DrawingRoute(routes = routes, screen = screen)
        is Screen.Archive -> ArchiveRoute(routes = routes)
        is Screen.Trash -> TrashRoute(routes = routes)
        is Screen.Settings -> SettingsRoute(routes = routes)
    }
}

/** Opens [note], on the drawing screen when it holds a drawing, back to [from]. */
private fun openNote(routes: ScreenRouteState, note: Note, from: Screen) {
    val target = if (DrawingSerializer.isDrawing(note.content)) {
        Screen.Drawing(note.id, from)
    } else {
        Screen.Editor(note.id, from)
    }
    routes.viewModel.navigateTo(target)
}

@Composable
private fun HomeRoute(routes: ScreenRouteState) {
    HomeScreen(
        uiState = routes.uiState,
        notes = routes.filteredNotes,
        allTags = routes.allTags,
        onOpenDrawer = { routes.scope.launch { routes.drawerState.open() } },
        isDrawerOpen = { routes.drawerState.isOpen },
        onSearchQueryChange = { routes.viewModel.onSearchQueryChange(it) },
        onColorFilterChange = { routes.viewModel.onColorFilterChange(it) },
        onTagFilterChange = { routes.viewModel.onTagFilterChange(it) },
        onToggleLayout = { routes.viewModel.toggleLayoutView() },
        onNoteClick = { note -> openNote(routes, note, Screen.Home) },
        onNoteLongClick = { note -> routes.viewModel.toggleSelection(note.id) },
        onTogglePinSelected = { routes.viewModel.togglePinForSelected() },
        onAddNewNote = { routes.viewModel.navigateTo(Screen.Editor(0L)) },
        onAddNewChecklistNote = {
            routes.viewModel.navigateTo(Screen.Editor(0L, startInChecklist = true))
        },
        onAddNewDrawingNote = { routes.viewModel.navigateTo(Screen.Drawing(0L)) },
        onClearSelection = { routes.viewModel.clearSelection() },
        onSelectAll = { routes.viewModel.selectAll(routes.filteredNotes) },
        onMoveSelectedToTrash = { selectedIds ->
            routes.viewModel.moveSelectedToTrash(selectedIds)
        },
        onMoveSelectedToArchive = { routes.viewModel.moveSelectedToArchive() },
        onArchiveNote = { noteId -> routes.viewModel.moveToArchive(noteId) },
        onUndoMoveToTrash = { ids -> routes.viewModel.undoMoveToTrash(ids) },
        onUndoMoveToArchive = { ids -> routes.viewModel.undoMoveToArchive(ids) },
        onSortOptionChange = { routes.viewModel.setSortOption(it) },
        onCommitCustomOrder = { orderedIds -> routes.viewModel.commitCustomOrder(orderedIds) },
        onSetSelectedColor = { ids, color -> routes.viewModel.setNotesColor(ids, color) },
        onSetSelectedLabel = { ids, label, enabled ->
            routes.viewModel.setTagForNotes(ids, label, enabled)
        },
        onCreateAndApplyLabel = { ids, label ->
            routes.viewModel.createAndApplyTag(ids, label)
        },
        homeSnackbarHostState = routes.homeSnackbarHostState,
        snackbarHeight = routes.snackbarHeight
    )
}

private const val EDITOR_TRASH_SNACKBAR_DELAY_MS = 140L

@Composable
private fun EditorRoute(routes: ScreenRouteState, screen: Screen.Editor) {
    val trashedMessage = stringResource(R.string.archive_note_trashed)
    val undoLabel = stringResource(R.string.undo)
    NoteEditorScreen(
        noteId = screen.noteId,
        startInChecklist = screen.startInChecklist,
        availableTags = routes.allTags,
        onGetNote = { id -> routes.viewModel.getNote(id) },
        onSaveNote = {
            id, title, content, colorIndex, tags, isPinned,
            inArchive, reminderTime ->
            routes.viewModel.saveNote(
                id, title, content, colorIndex, tags, isPinned,
                inArchive, reminderTime
            ).id
        },
        onAddLabel = { label -> routes.viewModel.addLabel(label) },
        onMoveToArchive = { id -> routes.viewModel.moveToArchive(id) },
        onMoveToTrash = { id ->
            routes.viewModel.moveToTrash(id)
            routes.scope.launch {
                delay(EDITOR_TRASH_SNACKBAR_DELAY_MS)
                val result = routes.homeSnackbarHostState.showSnackbar(
                    message = trashedMessage,
                    actionLabel = undoLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    routes.viewModel.undoMoveToTrash(listOf(id))
                }
            }
        },
        onClearReminder = { id -> routes.viewModel.setNoteReminder(id, null) },
        onOpenDrawing = { noteId ->
            routes.viewModel.navigateTo(
                Screen.Drawing(noteId, screen.returnTo)
            )
        },
        onNavigateBack = { routes.viewModel.navigateTo(screen.returnTo) }
    )
}

@Composable
private fun DrawingRoute(routes: ScreenRouteState, screen: Screen.Drawing) {
    DrawingScreen(
        noteId = screen.noteId,
        initialToolPrefs = routes.viewModel.currentDrawingToolPrefs(),
        onToolPrefsChange = { routes.viewModel.saveDrawingToolPrefs(it) },
        onGetNote = { id -> routes.viewModel.getNote(id) },
        onSaveNote = {
            id, title, content, colorIndex, tags, isPinned,
            inArchive ->
            routes.viewModel.saveNote(
                id, title, content, colorIndex, tags,
                isPinned, inArchive
            ).id
        },
        onNavigateBack = { savedId ->
            if (savedId > 0L) {
                routes.viewModel.navigateTo(Screen.Editor(savedId, screen.returnTo))
            } else {
                routes.viewModel.navigateTo(screen.returnTo)
            }
        }
    )
}

@Composable
private fun ArchiveRoute(routes: ScreenRouteState) {
    ArchiveScreen(
        notes = routes.archiveNotes,
        isGridLayout = routes.uiState.isGridLayout,
        allTags = routes.allTags,
        onOpenDrawer = { routes.scope.launch { routes.drawerState.open() } },
        isDrawerOpen = { routes.drawerState.isOpen },
        onNavigateBack = { routes.viewModel.navigateTo(Screen.Home) },
        onRestoreNote = { routes.viewModel.restoreFromArchive(it) },
        onRestoreSelected = { selectedIds ->
            routes.viewModel.restoreSelectedArchiveNotes(selectedIds)
        },
        onMoveSelectedToTrash = { selectedIds ->
            routes.viewModel.moveSelectedToTrash(selectedIds)
        },
        onUndoMoveToTrash = { ids -> routes.viewModel.undoMoveToTrash(ids) },
        onSetSelectedColor = { ids, color -> routes.viewModel.setNotesColor(ids, color) },
        onSetSelectedLabel = { ids, label, enabled ->
            routes.viewModel.setTagForNotes(ids, label, enabled)
        },
        onCreateAndApplyLabel = { ids, label ->
            routes.viewModel.createAndApplyTag(ids, label)
        },
        onNoteClick = { note -> openNote(routes, note, Screen.Archive) }
    )
}

@Composable
private fun TrashRoute(routes: ScreenRouteState) {
    TrashScreen(
        trashNotes = routes.trashNotes,
        isGridLayout = routes.uiState.isGridLayout,
        onOpenDrawer = { routes.scope.launch { routes.drawerState.open() } },
        isDrawerOpen = { routes.drawerState.isOpen },
        onNavigateBack = { routes.viewModel.navigateTo(Screen.Home) },
        onRestoreNote = { routes.viewModel.restoreFromTrash(it) },
        onPermanentlyDeleteNote = { routes.viewModel.permanentlyDelete(it) },
        onEmptyTrash = { routes.viewModel.emptyTrash() },
        onRestoreSelected = { routes.viewModel.restoreSelectedTrashNotes(it) },
        onPermanentlyDeleteSelected = {
            routes.viewModel.permanentlyDeleteSelectedTrashNotes(it)
        },
        onNoteClick = { note -> openNote(routes, note, Screen.Trash) }
    )
}

@Composable
private fun SettingsRoute(routes: ScreenRouteState) {
    SettingsScreen(
        uiState = routes.uiState,
        notes = routes.rawActiveNotes,
        autoBackupStatus = routes.autoBackupStatus,
        importOutcome = routes.importOutcome,
        onThemeChange = { routes.viewModel.setTheme(it) },
        onLockToggle = { enabled -> routes.viewModel.setLockEnabled(enabled) },
        onAutoBackupChange = { enabled -> routes.viewModel.setAutoBackup(enabled) },
        onBackUpNow = { name -> routes.viewModel.backUp(name) },
        onImportNotes = { routes.viewModel.importNotes(it) },
        onImportOutcomeShown = { routes.viewModel.clearImportOutcome() },
        markdownCopy = routes.markdownCopy,
        onNavigateBack = { routes.viewModel.navigateTo(Screen.Home) }
    )
}
