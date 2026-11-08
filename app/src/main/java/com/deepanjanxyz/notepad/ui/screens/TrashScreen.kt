package com.deepanjanxyz.notepad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.components.NoteCard
import com.deepanjanxyz.notepad.ui.components.ActionTooltip

/** The confirmation the trash screen is waiting on, if any. */
private sealed interface TrashDialog {
    data object EmptyTrash : TrashDialog
    data object DeleteSelected : TrashDialog
    data class DeleteForever(val noteId: Long) : TrashDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashNotes: List<Note>,
    isGridLayout: Boolean,
    onOpenDrawer: () -> Unit,
    isDrawerOpen: () -> Boolean,
    onNavigateBack: () -> Unit,
    onRestoreNote: (Long) -> Unit,
    onPermanentlyDeleteNote: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    onRestoreSelected: (List<Long>) -> Unit,
    onPermanentlyDeleteSelected: (List<Long>) -> Unit,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedNoteIds.isNotEmpty()
    var dialog by remember { mutableStateOf<TrashDialog?>(null) }

    fun toggleSelection(id: Long) {
        selectedNoteIds = if (id in selectedNoteIds) selectedNoteIds - id else selectedNoteIds + id
    }

    // The drawer takes the press while it is open, so a selection clears first and
    // only then does back leave the screen.
    BackHandler(enabled = !isDrawerOpen()) {
        if (isSelectionMode) {
            selectedNoteIds = emptySet()
        } else {
            onNavigateBack()
        }
    }

    TrashDialogs(
        dialog = dialog,
        onDismiss = { dialog = null },
        onEmptyTrash = onEmptyTrash,
        onDeleteSelected = {
            onPermanentlyDeleteSelected(selectedNoteIds.toList())
            selectedNoteIds = emptySet()
        },
        onDeleteForever = onPermanentlyDeleteNote
    )

    TrashContent(
        trashNotes = trashNotes,
        isGridLayout = isGridLayout,
        isSelectionMode = isSelectionMode,
        selectedNoteIds = selectedNoteIds,
        onOpenDrawer = onOpenDrawer,
        onRestoreNote = onRestoreNote,
        onRestoreSelected = {
            onRestoreSelected(selectedNoteIds.toList())
            selectedNoteIds = emptySet()
        },
        onNoteClick = onNoteClick,
        onClearSelection = { selectedNoteIds = emptySet() },
        onSelectAll = { selectedNoteIds = trashNotes.map { it.id }.toSet() },
        onDeleteSelected = { dialog = TrashDialog.DeleteSelected },
        onEmptyTrash = { dialog = TrashDialog.EmptyTrash },
        onRequestDeleteForever = { dialog = TrashDialog.DeleteForever(it) },
        onToggleSelection = ::toggleSelection,
        modifier = modifier
    )
}

/** The three confirmations the trash screen can put up. */
@Composable
private fun TrashDialogs(
    dialog: TrashDialog?,
    onDismiss: () -> Unit,
    onEmptyTrash: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteForever: (Long) -> Unit
) {
    when (dialog) {
        TrashDialog.EmptyTrash -> TrashConfirmDialog(
            title = "Empty Trash?",
            text = "All items in trash will be permanently deleted. This action cannot be reversed.",
            confirmLabel = "Empty Trash",
            confirmTag = "confirm_empty_trash_button",
            onConfirm = {
                onDismiss()
                onEmptyTrash()
            },
            onDismiss = onDismiss
        )

        TrashDialog.DeleteSelected -> TrashConfirmDialog(
            title = "Permanently Delete?",
            text = "Selected note(s) will be permanently deleted from your device.",
            confirmLabel = "Delete Permanently",
            confirmTag = "confirm_permanent_delete_button",
            onConfirm = {
                onDismiss()
                onDeleteSelected()
            },
            onDismiss = onDismiss
        )

        is TrashDialog.DeleteForever -> TrashConfirmDialog(
            title = "Permanently Delete?",
            text = "This note will be permanently deleted from your device. This action cannot be reversed.",
            confirmLabel = "Delete Permanently",
            confirmTag = "confirm_delete_note_forever_button",
            onConfirm = {
                onDismiss()
                onDeleteForever(dialog.noteId)
            },
            onDismiss = onDismiss
        )

        null -> Unit
    }
}

/** A destructive confirmation: the action in red, and a cancel beside it. */
@Composable
private fun TrashConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    confirmTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag(confirmTag)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** The trash screen's bar, list and empty state. */
@Composable
private fun TrashContent(
    trashNotes: List<Note>,
    isGridLayout: Boolean,
    isSelectionMode: Boolean,
    selectedNoteIds: Set<Long>,
    onOpenDrawer: () -> Unit,
    onRestoreNote: (Long) -> Unit,
    onRestoreSelected: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEmptyTrash: () -> Unit,
    onRequestDeleteForever: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TrashTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedNoteIds.size,
                trashCount = trashNotes.size,
                onClearSelection = onClearSelection,
                onSelectAll = onSelectAll,
                onRestoreSelected = onRestoreSelected,
                onDeleteSelected = onDeleteSelected,
                onOpenDrawer = onOpenDrawer,
                onEmptyTrash = onEmptyTrash
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TrashInfoCard()

            if (trashNotes.isEmpty()) {
                EmptyTrashState()
            } else {
                TrashGrid(
                    trashNotes = trashNotes,
                    isGridLayout = isGridLayout,
                    isSelectionMode = isSelectionMode,
                    selectedNoteIds = selectedNoteIds,
                    onToggleSelection = onToggleSelection,
                    onNoteClick = onNoteClick,
                    onRestoreNote = onRestoreNote,
                    onRequestDeleteForever = onRequestDeleteForever
                )
            }
        }
    }
}

/** Cross-fades between the default bar and the contextual selection bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    trashCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenDrawer: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    AnimatedContent(
        targetState = isSelectionMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "trash_top_bar"
    ) { selecting ->
        if (selecting) {
            TrashSelectionTopBar(
                selectedCount = selectedCount,
                onClearSelection = onClearSelection,
                onSelectAll = onSelectAll,
                onRestoreSelected = onRestoreSelected,
                onDeleteSelected = onDeleteSelected
            )
        } else {
            TrashDefaultTopBar(
                trashCount = trashCount,
                onOpenDrawer = onOpenDrawer,
                onEmptyTrash = onEmptyTrash
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashSelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = {
            Text("$selectedCount Selected")
        },
        navigationIcon = {
            ActionTooltip("Clear selection") {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }
            }
        },
        actions = {
            ActionTooltip("Select all") {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
            }
            ActionTooltip("Restore selected") {
                IconButton(
                    onClick = onRestoreSelected,
                    modifier = Modifier.testTag("restore_selected_button")
                ) {
                    Icon(
                        Icons.Default.RestoreFromTrash,
                        contentDescription = "Restore selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            ActionTooltip("Permanently delete selected") {
                IconButton(
                    onClick = onDeleteSelected,
                    modifier = Modifier.testTag("delete_selected_trash_button")
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Permanently delete selected",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashDefaultTopBar(
    trashCount: Int,
    onOpenDrawer: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            ActionTooltip("Open navigation menu") {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("trash_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation menu"
                    )
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trash & Recycler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "$trashCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        },
        actions = {
            if (trashCount > 0) {
                TextButton(
                    onClick = onEmptyTrash,
                    modifier = Modifier.testTag("empty_trash_button")
                ) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/** The note explaining that trashed notes go after thirty days. */
@Composable
private fun TrashInfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Notes in trash are automatically permanently deleted after 30 days. " +
                    "You can restore or delete them manually before then.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Shown when there is nothing in the trash. */
@Composable
private fun EmptyTrashState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Trash is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Deleted notes will appear here so you can restore them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The trashed notes, one or two to a row. */
@Composable
private fun TrashGrid(
    trashNotes: List<Note>,
    isGridLayout: Boolean,
    isSelectionMode: Boolean,
    selectedNoteIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onNoteClick: (Note) -> Unit,
    onRestoreNote: (Long) -> Unit,
    onRequestDeleteForever: (Long) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = if (isGridLayout) StaggeredGridCells.Fixed(2) else StaggeredGridCells.Fixed(1),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        items(trashNotes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                isSelected = selectedNoteIds.contains(note.id),
                isSelectionMode = isSelectionMode,
                searchQuery = "",
                isInTrash = true,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(note.id)
                    } else {
                        onNoteClick(note)
                    }
                },
                onLongClick = { onToggleSelection(note.id) },
                onRestore = { onRestoreNote(note.id) },
                onDeleteForever = { onRequestDeleteForever(note.id) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
