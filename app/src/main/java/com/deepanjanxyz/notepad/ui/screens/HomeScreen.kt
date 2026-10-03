package com.deepanjanxyz.notepad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.components.EmptyState
import com.deepanjanxyz.notepad.ui.components.FloatingSearchBar
import com.deepanjanxyz.notepad.ui.components.LoadingState
import com.deepanjanxyz.notepad.ui.components.NoteCard
import com.deepanjanxyz.notepad.ui.components.StateAction
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.theme.Spacing
import com.deepanjanxyz.notepad.ui.util.FilterSummaryKind
import com.deepanjanxyz.notepad.ui.util.NoteUiFormat
import com.deepanjanxyz.notepad.ui.viewmodel.NotesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NotesUiState,
    notes: List<Note>,
    allTags: List<String>,
    isContentReady: Boolean,
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

    // Presentation-only model for the results sub-header: keeps the wording out
    // of the view model and out of string concatenation.
    val filterSummary = NoteUiFormat.summarize(
        query = uiState.searchQuery,
        tag = uiState.selectedTagFilter,
        hasColorFilter = uiState.selectedColorFilter != null
    )
    val filterSummaryText = when (filterSummary.kind) {
        FilterSummaryKind.QUERY_IN_TAG -> stringResource(
            R.string.filter_summary_query_in_tag,
            filterSummary.query,
            filterSummary.tag.orEmpty()
        )
        FilterSummaryKind.QUERY -> stringResource(
            R.string.filter_summary_query,
            filterSummary.query
        )
        FilterSummaryKind.TAG -> stringResource(
            R.string.filter_summary_tag,
            filterSummary.tag.orEmpty()
        )
        FilterSummaryKind.COLOR -> stringResource(R.string.filter_summary_color)
        FilterSummaryKind.ALL -> stringResource(R.string.filter_summary_all)
    }
    val hasActiveFilters = filterSummary.kind != FilterSummaryKind.ALL

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
                            text = stringResource(R.string.selection_count, uiState.selectedNoteIds.size),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.testTag("close_selection_button")
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
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (isAllSelected) {
                                    stringResource(R.string.action_deselect_all)
                                } else {
                                    stringResource(R.string.action_select_all)
                                }
                            )
                        }

                        IconButton(
                            onClick = onTogglePinSelected,
                            modifier = Modifier.testTag("pin_selected_button")
                        ) {
                            Icon(
                                imageVector = if (anyUnpinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (anyUnpinned) {
                                    stringResource(R.string.action_pin_selected)
                                } else {
                                    stringResource(R.string.action_unpin_selected)
                                },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onMoveSelectedToArchive,
                            modifier = Modifier.testTag("archive_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = stringResource(R.string.action_archive_selected),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onMoveSelectedToTrash,
                            modifier = Modifier.testTag("delete_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete_selected),
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
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
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
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tags_carousel")
                ) {
                    item {
                        val isAllSelected = uiState.selectedTagFilter == null &&
                            uiState.selectedColorFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = {
                                onTagFilterChange(null)
                                onColorFilterChange(null)
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.filter_chip_all),
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

                    items(allTags) { tag ->
                        val isSelected = uiState.selectedTagFilter.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onTagFilterChange(if (isSelected) null else tag)
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

                    // Colour tints. Each swatch is a real toggle target with a
                    // spoken label and a 48 dp hit area; the tint dot itself stays
                    // small so the row still reads as a palette.
                    itemsIndexed(NoteColorOptions) { index, color ->
                        val colorName = NoteUiFormat.colorNameForIndex(index)
                        if (colorName != null) {
                            val isSelected = uiState.selectedColorFilter == index
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                },
                                label = "color_swatch_border"
                            )
                            Box(
                                modifier = Modifier
                                    .size(Spacing.minTouchTarget)
                                    .clip(CircleShape)
                                    .toggleable(
                                        value = isSelected,
                                        onValueChange = {
                                            onColorFilterChange(if (isSelected) null else index)
                                        },
                                        role = Role.Checkbox,
                                        contentDescription = stringResource(
                                            R.string.a11y_color_filter,
                                            colorName
                                        )
                                    )
                                    .testTag("filter_color_dot_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Spacing.colorBadge)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = borderColor,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(Spacing.iconSmall)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active filter and search sub-header
                if (hasActiveFilters) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xl, vertical = Spacing.xs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filterSummaryText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = stringResource(R.string.filter_results_count, notes.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                onSearchQueryChange("")
                                onTagFilterChange(null)
                                onColorFilterChange(null)
                            },
                            modifier = Modifier.testTag("clear_filter_button")
                        ) {
                            Text(stringResource(R.string.action_clear_filters))
                        }
                    }
                }
            }

            // 3. Notes Grid / List View
            when {
                !isContentReady -> LoadingState(
                    message = stringResource(R.string.loading_notes),
                    testTag = "home_loading_state"
                )

                notes.isEmpty() -> EmptyState(
                    icon = if (hasActiveFilters) Icons.Default.Search else Icons.Default.EditNote,
                    title = when {
                        hasActiveFilters -> stringResource(R.string.empty_filtered_title)
                        else -> stringResource(R.string.empty_notes_title)
                    },
                    message = when {
                        hasActiveFilters -> stringResource(R.string.empty_filtered_subtitle)
                        else -> stringResource(R.string.empty_notes_subtitle)
                    },
                    action = if (hasActiveFilters) {
                        StateAction(
                            label = stringResource(R.string.action_clear_filters),
                            onClick = {
                                onSearchQueryChange("")
                                onTagFilterChange(null)
                                onColorFilterChange(null)
                            },
                            testTag = "empty_clear_filter_button"
                        )
                    } else {
                        StateAction(
                            label = stringResource(R.string.add_note),
                            onClick = onAddNewNote,
                            testTag = "empty_create_note_button"
                        )
                    },
                    testTag = "home_empty_state"
                )

                else -> {
                    val pinnedNotes = notes.filter { it.isPinned }
                    val otherNotes = notes.filter { !it.isPinned }
                    val pinnedHeader = stringResource(R.string.filter_chip_all)

                    LazyVerticalStaggeredGrid(
                        columns = if (uiState.isGridLayout) {
                            StaggeredGridCells.Fixed(2)
                        } else {
                            StaggeredGridCells.Fixed(1)
                        },
                        contentPadding = PaddingValues(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = Spacing.sm,
                            bottom = Spacing.listBottomInset
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.listItemSpacing),
                        verticalItemSpacing = Spacing.listItemSpacing,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Only show a pinned section when pinned notes exist.
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PushPin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(Spacing.iconSmall)
                                    )
                                    Spacer(modifier = Modifier.size(Spacing.xs))
                                    Text(
                                        text = pinnedHeader.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(pinnedNotes, key = { it.id }) { note ->
                                HomeNoteGridItem(
                                    note = note,
                                    uiState = uiState,
                                    onNoteClick = onNoteClick,
                                    onNoteLongClick = onNoteLongClick,
                                    onTogglePin = onTogglePin
                                )
                            }

                            if (otherNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = stringResource(R.string.title_notes).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            top = Spacing.md,
                                            bottom = Spacing.xs
                                        )
                                    )
                                }
                            }
                        }

                        items(otherNotes, key = { it.id }) { note ->
                            HomeNoteGridItem(
                                note = note,
                                uiState = uiState,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onTogglePin = onTogglePin
                            )
                        }
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
                    .padding(
                        start = Spacing.xl,
                        end = Spacing.xl,
                        bottom = Spacing.xxxl,
                        top = Spacing.xs
                    )
            ) {
                Text(
                    text = stringResource(R.string.add_note),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                NoteTypeOption(
                    icon = Icons.Default.EditNote,
                    iconContainer = Color(0xFFFFB300),
                    iconTint = Color(0xFF1B1B1F),
                    title = stringResource(R.string.note_type_text),
                    subtitle = stringResource(R.string.note_type_text_desc),
                    testTag = "create_text_note_option",
                    onClick = {
                        showCreateOptionsSheet = false
                        onAddNewNote()
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                NoteTypeOption(
                    icon = Icons.Default.Brush,
                    iconContainer = Color(0xFF2196F3),
                    iconTint = Color.White,
                    title = stringResource(R.string.note_type_drawing),
                    subtitle = stringResource(R.string.note_type_drawing_desc),
                    testTag = "create_drawing_note_option",
                    onClick = {
                        showCreateOptionsSheet = false
                        onAddNewDrawingNote()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeNoteGridItem(
    note: Note,
    uiState: NotesUiState,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit
) {
    NoteCard(
        note = note,
        isSelected = uiState.selectedNoteIds.contains(note.id),
        isSelectionMode = uiState.isSelectionMode,
        searchQuery = uiState.searchQuery,
        onClick = {
            if (uiState.isSelectionMode) onNoteLongClick(note) else onNoteClick(note)
        },
        onLongClick = { onNoteLongClick(note) },
        onTogglePin = { onTogglePin(note) }
    )
}

@Composable
private fun NoteTypeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconContainer: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Spacing.iconLarge)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
