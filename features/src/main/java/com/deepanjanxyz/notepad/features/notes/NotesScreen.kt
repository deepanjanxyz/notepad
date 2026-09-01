package com.deepanjanxyz.notepad.features.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepanjanxyz.notepad.core.designsystem.component.ConfirmationDialog
import com.deepanjanxyz.notepad.core.designsystem.component.EmptyStateView
import com.deepanjanxyz.notepad.core.security.BiometricHelper
import com.deepanjanxyz.notepad.features.notes.components.BiometricLockScreen
import com.deepanjanxyz.notepad.features.notes.components.NoteCard
import com.deepanjanxyz.notepad.features.notes.components.NotesSearchBar
import com.deepanjanxyz.notepad.features.notes.components.NotesTopBar

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun triggerBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            onSuccess = { viewModel.onBiometricAuthenticated() },
            onError = {},
            onCancel = {}
        )
    }

    LaunchedEffect(uiState.isBiometricLocked, uiState.isAuthenticated) {
        if (uiState.isBiometricLocked && !uiState.isAuthenticated) {
            triggerBiometricPrompt()
        }
    }

    BackHandler(enabled = uiState.isSelectionMode || uiState.isSearching) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (uiState.isSearching) {
            viewModel.onSearchToggled(false)
        }
    }

    if (uiState.isBiometricLocked && !uiState.isAuthenticated) {
        BiometricLockScreen(
            onUnlockClicked = { triggerBiometricPrompt() },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                NotesTopBar(
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedCount,
                    isGridLayout = uiState.isGridLayout,
                    onToggleLayout = { viewModel.toggleLayoutMode() },
                    onOpenSearch = { viewModel.onSearchToggled(true) },
                    onOpenSettings = onNavigateToSettings,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeleteSelected = { showDeleteConfirmDialog = true }
                )

                AnimatedVisibility(visible = uiState.isSearching && !uiState.isSelectionMode) {
                    NotesSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onClose = { viewModel.onSearchToggled(false) }
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.isSelectionMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { onNavigateToEditor(-1L) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create note"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.filteredNotes.isEmpty() && !uiState.isLoading) {
                if (uiState.searchQuery.isNotBlank()) {
                    EmptyStateView(
                        title = "No Matches Found",
                        subtitle = "Try searching for something else"
                    )
                } else {
                    EmptyStateView()
                }
            } else {
                if (uiState.isGridLayout) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.filteredNotes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    viewModel.onNoteClick(note, onNavigateToEditor)
                                },
                                onLongClick = {
                                    viewModel.onNoteLongClick(note)
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.filteredNotes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    viewModel.onNoteClick(note, onNavigateToEditor)
                                },
                                onLongClick = {
                                    viewModel.onNoteLongClick(note)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val count = uiState.selectedCount
        ConfirmationDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            onConfirm = { viewModel.deleteSelectedNotes() },
            title = "Delete $count Note${if (count > 1) "s" else ""}?",
            text = "Are you sure you want to delete the selected notes? This action cannot be undone.",
            confirmText = "Delete",
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }
}
