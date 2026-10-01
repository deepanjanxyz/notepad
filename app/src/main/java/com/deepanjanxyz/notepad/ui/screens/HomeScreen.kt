package com.deepanjanxyz.notepad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.components.FloatingSearchBar
import com.deepanjanxyz.notepad.ui.components.NoteCard
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.viewmodel.NotesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NotesUiState,
    notes: List<Note>,
    allTags: List<String>,
    onOpenDrawer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onColorFilterChange: (Int?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onToggleLayout: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onTogglePinSelected: () -> Unit,
    onAddNewNote: () -> Unit,
    onAddNewDrawingNote: () -> Unit = {},
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onMoveSelectedToTrash: () -> Unit,
    onMoveSelectedToArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateOptionsSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        onClearSelection()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (uiState.isSelectionMode) {
                val selectedNotes = notes.filter { uiState.selectedNoteIds.contains(it.id) }
                val anyUnpinned = selectedNotes.any { !it.isPinned }
                val isAllSelected = notes.isNotEmpty() && uiState.selectedNoteIds.size == notes.size

                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedNoteIds.size} Selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.testTag("close_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection"
                            )
                        }
                    },
                    actions = {
                        // Select/Deselect All
                        IconButton(
                            onClick = {
                                if (isAllSelected) onClearSelection() else onSelectAll()
                            },
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (isAllSelected) "Deselect all" else "Select all"
                            )
                        }

                        // Pin/Unpin Action Button
                        IconButton(
                            onClick = onTogglePinSelected,
                            modifier = Modifier.testTag("pin_selected_button")
                        ) {
                            Icon(
                                imageVector = if (anyUnpinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (anyUnpinned) "Pin selected notes" else "Unpin selected notes",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Archive Action Button
                        IconButton(
                            onClick = onMoveSelectedToArchive,
                            modifier = Modifier.testTag("archive_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archive selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Delete (Trash) Action Button
                        IconButton(
                            onClick = onMoveSelectedToTrash,
                            modifier = Modifier.testTag("delete_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move to Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showCreateOptionsSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_add_note")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_note)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Top Floating Search Bar (hidden in selection mode)
            if (!uiState.isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    FloatingSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        isGridLayout = uiState.isGridLayout,
                        onToggleLayout = onToggleLayout,
                        onOpenDrawer = onOpenDrawer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Tags Carousel & Color Palette
            if (!uiState.isSelectionMode) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tags_carousel")
                ) {
                    // "All" filter chip
                    item {
                        val isAllSelected = uiState.selectedTagFilter == null && uiState.selectedColorFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = {
                                onTagFilterChange(null)
                                onColorFilterChange(null)
                            },
                            label = {
                                Text(
                                    text = "All",
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }

                    // User Defined Tags (clean list from Room DB)
                    items(allTags) { tag ->
                        val isSelected = uiState.selectedTagFilter.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) onTagFilterChange(null) else onTagFilterChange(tag)
                            },
                            label = {
                                Text(
                                    text = tag,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_chip_tag_$tag")
                        )
                    }

                    // Color Palette Indicators (14 modern M3 Pastel Dark container tints)
                    itemsIndexed(NoteColorOptions) { index, color ->
                        if (index > 0) { // Skip default clear in filter indicators
                            val isSelected = uiState.selectedColorFilter == index
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (isSelected) onColorFilterChange(null) else onColorFilterChange(index)
                                    }
                                    .testTag("filter_color_dot_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected color filter",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active filter and search subheader
                if (uiState.searchQuery.isNotBlank() || uiState.selectedTagFilter != null || uiState.selectedColorFilter != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterText = when {
                            uiState.searchQuery.isNotBlank() && uiState.selectedTagFilter != null ->
                                "Searching \"${uiState.searchQuery}\" in ${uiState.selectedTagFilter}"
                            uiState.searchQuery.isNotBlank() ->
                                "Results for \"${uiState.searchQuery}\""
                            uiState.selectedTagFilter != null ->
                                "Filtered by ${uiState.selectedTagFilter}"
                            else ->
                                "Filtered by Color Tint"
                        }
                        Text(
                            text = filterText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${notes.size} found)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    onSearchQueryChange("")
                                    onTagFilterChange(null)
                                    onColorFilterChange(null)
                                }
                                .testTag("clear_filter_button")
                        )
                    }
                }
            }

            // 3. Notes Grid / List View
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.searchQuery.isNotBlank()) Icons.Default.Description else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "No notes matching \"${uiState.searchQuery}\""
                            } else if (uiState.selectedTagFilter != null || uiState.selectedColorFilter != null) {
                                "No Matching Notes"
                            } else {
                                stringResource(R.string.empty_notes_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "Check your spelling or try different keywords in note title or content."
                            } else if (uiState.selectedTagFilter != null || uiState.selectedColorFilter != null) {
                                "Try adjusting or clearing your active filters."
                            } else {
                                stringResource(R.string.empty_notes_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("empty_clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Search")
                            }
                        }
                    }
                }
            } else {
                val pinnedNotes = notes.filter { it.isPinned }
                val otherNotes = notes.filter { !it.isPinned }

                // Grid mode: 2-column grid; List mode: 1-column flat full-width list
                val gridColumns = if (uiState.isGridLayout) StaggeredGridCells.Fixed(2) else StaggeredGridCells.Fixed(1)

                LazyVerticalStaggeredGrid(
                    columns = gridColumns,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Do NOT permanently display a dedicated "Pinned Section" header unless pinned notes actually exist!
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "PINNED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                isSelectionMode = uiState.isSelectionMode,
                                searchQuery = uiState.searchQuery,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onNoteLongClick(note)
                                    } else {
                                        onNoteClick(note)
                                    }
                                },
                                onLongClick = { onNoteLongClick(note) },
                                onTogglePin = { onTogglePin(note) }
                            )
                        }

                        if (otherNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "OTHERS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }

                    items(otherNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isSelected = uiState.selectedNoteIds.contains(note.id),
                            isSelectionMode = uiState.isSelectionMode,
                            searchQuery = uiState.searchQuery,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onNoteLongClick(note)
                                } else {
                                    onNoteClick(note)
                                }
                            },
                            onLongClick = { onNoteLongClick(note) },
                            onTogglePin = { onTogglePin(note) }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet: Select Note Type ("Text Note" vs "Drawing Note")
    if (showCreateOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateOptionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.testTag("create_note_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 4.dp)
            ) {
                Text(
                    text = "Create Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Text Note (Standard text & checklist editor)
                Surface(
                    onClick = {
                        showCreateOptionsSheet = false
                        onAddNewNote()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_text_note_option")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = Color(0xFF1B1B1F),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Text Note",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Standard text, checklist, and labels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Drawing Note (Dedicated drawing canvas screen)
                Surface(
                    onClick = {
                        showCreateOptionsSheet = false
                        onAddNewDrawingNote()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_drawing_note_option")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Drawing Note",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Free-hand sketch and doodle canvas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
