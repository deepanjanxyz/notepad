package com.deepanjanxyz.notepad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.components.EmptyState
import com.deepanjanxyz.notepad.ui.components.LoadingState
import com.deepanjanxyz.notepad.ui.components.NoteCard
import com.deepanjanxyz.notepad.ui.theme.Spacing

/**
 * Archived notes.
 *
 * Selection is owned by the view model and passed in, so bulk restore / delete
 * act on the same set the view model reads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    notes: List<Note>,
    isGridLayout: Boolean,
    isContentReady: Boolean,
    selectedNoteIds: Set<Long>,
    onOpenDrawer: () -> Unit,
    onRestoreNote: (Long) -> Unit,
    onMoveToTrash: (Long) -> Unit,
    onRestoreSelected: (List<Long>) -> Unit,
    onMoveSelectedToTrash: (List<Long>) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelectionMode = selectedNoteIds.isNotEmpty()
    val isAllSelected = notes.isNotEmpty() && selectedNoteIds.size == notes.size

    BackHandler(enabled = isSelectionMode) {
        onClearSelection()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.selection_count, selectedNoteIds.size),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.testTag("close_archive_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close_selection)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { if (isAllSelected) onClearSelection() else onSelectAll() },
                            modifier = Modifier.testTag("select_all_archive_button")
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) {
                                    Icons.Default.Deselect
                                } else {
                                    Icons.Default.SelectAll
                                },
                                contentDescription = if (isAllSelected) {
                                    stringResource(R.string.action_deselect_all)
                                } else {
                                    stringResource(R.string.action_select_all)
                                }
                            )
                        }
                        IconButton(
                            onClick = { onRestoreSelected(selectedNoteIds.toList()) },
                            modifier = Modifier.testTag("unarchive_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = stringResource(R.string.action_restore_selected),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onMoveSelectedToTrash(selectedNoteIds.toList()) },
                            modifier = Modifier.testTag("delete_selected_archive_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete_selected),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.title_archive),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.size(Spacing.sm))
                            CountBadge(count = notes.size)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("archive_drawer_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.a11y_open_drawer)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                !isContentReady -> LoadingState(
                    message = stringResource(R.string.loading_archive),
                    testTag = "archive_loading_state"
                )

                notes.isEmpty() -> EmptyState(
                    icon = Icons.Default.Archive,
                    title = stringResource(R.string.empty_archive_title),
                    message = stringResource(R.string.empty_archive_subtitle),
                    testTag = "archive_empty_state"
                )

                else -> LazyVerticalStaggeredGrid(
                    columns = if (isGridLayout) {
                        StaggeredGridCells.Fixed(2)
                    } else {
                        StaggeredGridCells.Fixed(1)
                    },
                    contentPadding = PaddingValues(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.listItemSpacing),
                    verticalItemSpacing = Spacing.listItemSpacing,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isSelected = selectedNoteIds.contains(note.id),
                            isSelectionMode = isSelectionMode,
                            isInArchive = true,
                            onUnarchive = { onRestoreNote(note.id) },
                            searchQuery = "",
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(note.id)
                                } else {
                                    onNoteClick(note)
                                }
                            },
                            onLongClick = { onToggleSelection(note.id) }
                        )
                    }
                }
            }
        }
    }
}

/** Small rounded counter shared by the archive and trash toolbars. */
@Composable
internal fun CountBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
        )
    }
}
