package com.deepanjanxyz.notepad.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.components.FloatingSearchBar
import com.deepanjanxyz.notepad.ui.components.NewFeatureBadge
import com.deepanjanxyz.notepad.ui.components.NoteCard
import com.deepanjanxyz.notepad.ui.components.NoteTouchGestureBox
import com.deepanjanxyz.notepad.ui.components.PendingTrashDrop
import com.deepanjanxyz.notepad.ui.components.ReorderDragOverlay
import com.deepanjanxyz.notepad.ui.components.ReorderDragSession
import com.deepanjanxyz.notepad.ui.components.TrashDropOverlay
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.viewmodel.NoteSortOption
import com.deepanjanxyz.notepad.ui.viewmodel.NotesUiState
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.backup.DownloadsBackup
import com.deepanjanxyz.notepad.backup.MarkdownExport
import com.deepanjanxyz.notepad.ui.components.BulkColorPickerSheet
import com.deepanjanxyz.notepad.ui.components.BulkLabelsSheet
import com.deepanjanxyz.notepad.ui.components.MarkdownExportSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The gap left between the add button and a snackbar it has risen above.
private val FAB_SNACKBAR_GAP = 12.dp

/**
 * The home screen: the note list, its search, its selection bar and its empty state.
 *
 * `LongMethod` and `CyclomaticComplexMethod` are suppressed here rather than the
 * screen split. Its state, its callbacks and its layout are read together, and the
 * alternatives - threading a dozen parameters through new composables, or hiding the
 * flow behind them - would not make any of it simpler.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NotesUiState,
    notes: List<Note>,
    allTags: List<String>,
    onOpenDrawer: () -> Unit,
    isDrawerOpen: () -> Boolean,
    onSearchQueryChange: (String) -> Unit,
    onColorFilterChange: (Int?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onToggleLayout: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit,
    onTogglePinSelected: () -> Unit,
    onAddNewNote: () -> Unit,
    onAddNewChecklistNote: () -> Unit = {},
    onAddNewDrawingNote: () -> Unit = {},
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onMoveSelectedToTrash: (List<Long>) -> Unit,
    onMoveSelectedToArchive: () -> Unit,
    onArchiveNote: (Long) -> Unit = {},
    onUndoMoveToTrash: (List<Long>) -> Unit,
    onUndoMoveToArchive: (List<Long>) -> Unit = {},
    onSortOptionChange: (NoteSortOption) -> Unit,
    onCommitCustomOrder: (List<Long>) -> Unit = {},
    onSetSelectedColor: (List<Long>, Int) -> Unit = { _, _ -> },
    onSetSelectedLabel: (List<Long>, String, Boolean) -> Unit = { _, _, _ -> },
    onCreateAndApplyLabel: (List<Long>, String) -> Unit = { _, _ -> },
    homeSnackbarHostState: SnackbarHostState,
    snackbarHeight: State<Dp>,
    modifier: Modifier = Modifier
) {
    var showCreateOptionsSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showBulkColorSheet by remember { mutableStateOf(false) }
    var showBulkLabelsSheet by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingTrashDrop by remember { mutableStateOf<PendingTrashDrop?>(null) }
    var rootOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val noteCardBounds = remember { mutableStateMapOf<Long, Rect>() }
    val archivingNoteIds = remember { mutableStateMapOf<Long, Boolean>() }
    var activeGestureNoteId by remember { mutableStateOf<Long?>(null) }
    var reorderSession by remember { mutableStateOf<ReorderDragSession?>(null) }
    var liveReorderPinned by remember { mutableStateOf<List<Note>?>(null) }
    var liveReorderOther by remember { mutableStateOf<List<Note>?>(null) }
    var pendingCommittedOrderIds by remember { mutableStateOf<List<Long>?>(null) }
    var dragStartedFromPendingOrder by remember { mutableStateOf(false) }
    var lastSwappedCandidateId by remember { mutableStateOf<Long?>(null) }
    var lastReorderSwapTimeMs by remember { mutableStateOf(0L) }
    var suppressSortResetScroll by remember { mutableStateOf(false) }
    var gridBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    val deletingNoteIds = remember(pendingTrashDrop) {
        pendingTrashDrop?.notes?.map { it.id }?.toSet() ?: emptySet()
    }
    val undoLabel = stringResource(R.string.undo)
    val singleTrashedMessage = stringResource(R.string.archive_note_trashed)
    val noteArchivedMessage = stringResource(R.string.note_archived)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val gesturesEnabled = !uiState.isSelectionMode && pendingTrashDrop == null

    LaunchedEffect(notes) {
        val currentIds = notes.map { it.id }.toSet()
        archivingNoteIds.keys.filter { it !in currentIds }.forEach { archivingNoteIds.remove(it) }
        val pinnedSection = notes.filter { it.isPinned }
        val otherSection = notes.filter { !it.isPinned }
        if (reorderSession == null && (liveReorderPinned != null || liveReorderOther != null)) {
            val cachedMatchesMembership =
                liveReorderPinned?.map { it.id }?.toSet() == pinnedSection.map { it.id }.toSet() &&
                    liveReorderOther?.map { it.id }?.toSet() == otherSection.map { it.id }.toSet()
            if (!cachedMatchesMembership) {
                pendingCommittedOrderIds = null
                liveReorderPinned = null
                liveReorderOther = null
            }
        }
        val expected = pendingCommittedOrderIds
        if (expected != null) {
            val actualIds = (pinnedSection + otherSection).map { it.id }
            if (actualIds == expected) {
                pendingCommittedOrderIds = null
                if (reorderSession == null) {
                    liveReorderPinned = null
                    liveReorderOther = null
                }
            }
        }
    }

    LaunchedEffect(pendingCommittedOrderIds) {
        if (pendingCommittedOrderIds != null) {
            delay(600L)
            pendingCommittedOrderIds = null
            if (reorderSession == null) {
                liveReorderPinned = null
                liveReorderOther = null
            }
        }
    }

    LaunchedEffect(gesturesEnabled) {
        if (!gesturesEnabled) {
            activeGestureNoteId = null
            reorderSession = null
            pendingCommittedOrderIds = null
            liveReorderPinned = null
            liveReorderOther = null
        }
    }

    LaunchedEffect(uiState.searchQuery, uiState.selectedTagFilter, uiState.selectedColorFilter) {
        activeGestureNoteId = null
        reorderSession = null
        pendingCommittedOrderIds = null
        liveReorderPinned = null
        liveReorderOther = null
    }

    LaunchedEffect(uiState.sortOption) {
        if (uiState.sortOption != NoteSortOption.CUSTOM) {
            activeGestureNoteId = null
            reorderSession = null
            pendingCommittedOrderIds = null
            liveReorderPinned = null
            liveReorderOther = null
        }
    }

    LaunchedEffect(uiState.isSelectionMode) {
        if (!uiState.isSelectionMode) {
            showBulkColorSheet = false
            showBulkLabelsSheet = false
        }
    }

    // Read inside the export coroutine, so it sees the selection as it is when the
    // files are done rather than the one from the frame the export started in.
    val currentSelection by rememberUpdatedState(uiState.selectedNoteIds)

    // Below Android 10 an export writes to Downloads directly, which needs the
    // storage permission unless the backup folder itself has been handed over.
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingExport
        pendingExport = null
        if (granted && pending != null) {
            pending()
        } else {
            scope.launch {
                homeSnackbarHostState.showSnackbar("Storage permission is needed to export")
            }
        }
    }

    // Writes the selected notes out as Markdown and reports what happened. One
    // note goes straight to its own file; several ask how they should be bundled.
    fun exportSelected(selected: List<Note>, separateFiles: Boolean) {
        if (selected.isEmpty()) return
        val needsPermission = DownloadsBackup.needsLegacyPermission &&
            !DownloadsBackup.hasFolderAccess(context) &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingExport = { exportSelected(selected, separateFiles) }
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        val exportedIds = selected.map { it.id }.toSet()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (selected.size == 1) {
                    MarkdownExport.exportOne(context, selected.first())
                } else if (separateFiles) {
                    MarkdownExport.exportZip(context, selected)
                } else {
                    MarkdownExport.exportCombined(context, selected)
                }
            }
            // Only the selection this export started with is cleared, and before
            // the snackbar is awaited: the user may have cancelled it and picked
            // other notes while the files were being written.
            if (!outcome.error && currentSelection == exportedIds) onClearSelection()
            homeSnackbarHostState.showSnackbar(outcome.message)
        }
    }

    // The snackbars sit at the bottom of the screen and stack when two are showing,
    // so the add button rises by their combined measured height plus a gap, and
    // settles back afterwards.
    val fabLift by animateDpAsState(
        targetValue = if (snackbarHeight.value > 0.dp) {
            snackbarHeight.value + FAB_SNACKBAR_GAP
        } else {
            0.dp
        },
        label = "fab_snackbar_lift"
    )

    // A press while the drawer is open belongs to the drawer, which the activity
    // closes; this takes it only once the drawer is shut.
    BackHandler(enabled = uiState.isSelectionMode && !isDrawerOpen()) {
        onClearSelection()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    rootOffsetInRoot = coords.positionInRoot()
                    rootSize = coords.size
                }
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AnimatedVisibility(
                visible = uiState.isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                label = "home_selection_bar"
            ) {
                val selectedNotes = notes.filter { uiState.selectedNoteIds.contains(it.id) }
                val anyUnpinned = selectedNotes.any { !it.isPinned }
                val isAllSelected = notes.isNotEmpty() && uiState.selectedNoteIds.size == notes.size

                TopAppBar(
                    title = {
                        // Kept to one line: the bar carries several actions beside the
                        // title, and a larger style wrapped "Selected" in two.
                        Text(
                            text = "${uiState.selectedNoteIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        ActionTooltip("Cancel selection") {
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier.testTag("close_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel selection"
                                )
                            }
                        }
                    },
                    actions = {
                        // Select/Deselect All
                        val selectAllDesc = if (isAllSelected) {
                            stringResource(R.string.home_deselect_all)
                        } else {
                            stringResource(R.string.trash_select_all)
                        }
                        ActionTooltip(selectAllDesc) {
                            IconButton(
                                onClick = {
                                    if (isAllSelected) onClearSelection() else onSelectAll()
                                },
                                modifier = Modifier.testTag("select_all_button")
                            ) {
                                Icon(
                                    imageVector = if (isAllSelected) {
                                        Icons.Default.Deselect
                                    } else {
                                        Icons.Default.SelectAll
                                    },
                                    contentDescription = selectAllDesc
                                )
                            }
                        }

                        // Pin/Unpin Action Button
                        val pinDesc = if (anyUnpinned) {
                            stringResource(R.string.editor_pin)
                        } else {
                            stringResource(R.string.editor_unpin)
                        }
                        ActionTooltip(pinDesc) {
                            IconButton(
                                onClick = onTogglePinSelected,
                                modifier = Modifier.testTag("pin_selected_button")
                            ) {
                                Icon(
                                    imageVector = if (anyUnpinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = pinDesc,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        var showSelectionMoreMenu by remember(uiState.isSelectionMode) {
                            mutableStateOf(false)
                        }
                        Box {
                            ActionTooltip(stringResource(R.string.editor_more)) {
                                IconButton(
                                    onClick = { showSelectionMoreMenu = true },
                                    modifier = Modifier.testTag("selection_more_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.editor_more)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSelectionMoreMenu,
                                onDismissRequest = { showSelectionMoreMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.drawer_archive)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Archive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showSelectionMoreMenu = false
                                        onMoveSelectedToArchive()
                                    },
                                    modifier = Modifier.testTag("archive_selected_button")
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.bulk_color_action)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = { NewFeatureBadge() },
                                    onClick = {
                                        showSelectionMoreMenu = false
                                        showBulkColorSheet = true
                                    },
                                    modifier = Modifier.testTag("color_selected_button")
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.bulk_labels_action)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Label,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = { NewFeatureBadge() },
                                    onClick = {
                                        showSelectionMoreMenu = false
                                        showBulkLabelsSheet = true
                                    },
                                    modifier = Modifier.testTag("label_selected_button")
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.home_export_markdown)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FileDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = { NewFeatureBadge() },
                                    onClick = {
                                        showSelectionMoreMenu = false
                                        val chosen = notes.filter {
                                            uiState.selectedNoteIds.contains(it.id)
                                        }
                                        if (chosen.size == 1) {
                                            exportSelected(chosen, separateFiles = false)
                                        } else {
                                            showExportSheet = true
                                        }
                                    },
                                    modifier = Modifier.testTag("export_selected_button")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.editor_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showSelectionMoreMenu = false
                                        if (pendingTrashDrop != null) return@DropdownMenuItem
                                        val chosenNotes = notes.filter {
                                            uiState.selectedNoteIds.contains(it.id)
                                        }
                                        if (chosenNotes.isEmpty()) return@DropdownMenuItem
                                        pendingTrashDrop = PendingTrashDrop(
                                            notes = chosenNotes,
                                            boundsInRoot = noteCardBounds.toMap()
                                        )
                                        onClearSelection()
                                    },
                                    modifier = Modifier.testTag("delete_selected_button")
                                )
                            }
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
                visible = !uiState.isSelectionMode && pendingTrashDrop == null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showCreateOptionsSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    // The Scaffold zeroes its window insets, so the button has to
                    // clear the navigation bar itself or the bar covers its lower
                    // half on three-button navigation. It also rises above a
                    // snackbar while one is showing.
                    modifier = Modifier
                        .navigationBarsPadding()
                        .offset { IntOffset(0, -fabLift.roundToPx()) }
                        .testTag("fab_add_note")
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
            // Clears the side navigation bar the Scaffold zeroes out; top and
            // bottom are handled by the search bar, the list and the + button.
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        ) {
            // 1. Top Floating Search Bar (hidden in selection mode)
            AnimatedVisibility(
                visible = !uiState.isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
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
            AnimatedVisibility(
                visible = !uiState.isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tags_carousel")
                ) {
                    // Sort order selector
                    item {
                        var sortMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            ActionTooltip("Sort notes") {
                                // Same height and container treatment as the chips
                                // and colour dots, so the row reads as one system.
                                Surface(
                                    onClick = { sortMenuExpanded = true },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("sort_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Sort notes",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                // A softer, rounded menu instead of the default near-square one.
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                NoteSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        leadingIcon = {
                                            if (option == uiState.sortOption) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSortOptionChange(option)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                                    text = stringResource(R.string.filter_all),
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
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
                                    imageVector = Icons.Default.Description,
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
                val visiblePinnedNotes = liveReorderPinned ?: pinnedNotes
                val visibleOtherNotes = liveReorderOther ?: otherNotes

                val gridColumns = if (uiState.isGridLayout) StaggeredGridCells.Fixed(2) else StaggeredGridCells.Fixed(1)
                val gridState = rememberLazyStaggeredGridState()

                fun hasLeftNeighbor(note: Note, sectionNotes: List<Note>): Boolean {
                    if (!uiState.isGridLayout) return false
                    val selfRect = noteCardBounds[note.id] ?: return false
                    return sectionNotes.any { other ->
                        if (other.id == note.id) return@any false
                        val otherRect = noteCardBounds[other.id] ?: return@any false
                        otherRect.center.x < selfRect.left &&
                            otherRect.bottom > selfRect.top &&
                            otherRect.top < selfRect.bottom
                    }
                }

                fun handleStartReorder(
                    note: Note,
                    pointerRootPos: Offset,
                    grabOffsetInCard: Offset,
                    cardSizePx: IntSize
                ) {
                    dragStartedFromPendingOrder = pendingCommittedOrderIds != null
                    pendingCommittedOrderIds = null
                    liveReorderPinned = visiblePinnedNotes
                    liveReorderOther = visibleOtherNotes
                    lastSwappedCandidateId = null
                    lastReorderSwapTimeMs = 0L
                    reorderSession = ReorderDragSession(
                        note = note,
                        initialPointerRootPos = pointerRootPos,
                        grabOffsetInCard = grabOffsetInCard,
                        cardSizePx = cardSizePx
                    )
                }

                fun handleMoveReorder(pointerRootPos: Offset) {
                    val session = reorderSession ?: return
                    if (session.isSettling) return
                    session.pointerRootPos = pointerRootPos

                    val now = System.currentTimeMillis()
                    val center = session.overlayCenterInRoot
                    val draggedIsPinned = session.note.isPinned
                    val sectionList = if (draggedIsPinned) {
                        liveReorderPinned ?: pinnedNotes
                    } else {
                        liveReorderOther ?: otherNotes
                    }
                    val fromIndex = sectionList.indexOfFirst { it.id == session.note.id }
                    if (fromIndex < 0) return

                    // Only visible cards are valid targets so offscreen recycled bounds cannot trigger swaps.
                    val visibleIds = gridState.layoutInfo.visibleItemsInfo
                        .mapNotNull { it.key as? Long }
                        .toHashSet()

                    fun innerRect(candidate: Note): Rect? {
                        if (candidate.id == session.note.id || candidate.id !in visibleIds) return null
                        val rect = noteCardBounds[candidate.id] ?: return null
                        val marginX = rect.width * 0.12f
                        val marginY = rect.height * 0.12f
                        return Rect(
                            left = rect.left + marginX,
                            top = rect.top + marginY,
                            right = rect.right - marginX,
                            bottom = rect.bottom - marginY
                        )
                    }

                    val targetNote = sectionList.firstOrNull { innerRect(it)?.contains(pointerRootPos) == true }
                        ?: sectionList.firstOrNull { innerRect(it)?.contains(center) == true }
                        ?: return

                    val cooldownMs = if (targetNote.id == lastSwappedCandidateId) 260L else 60L
                    if (now - lastReorderSwapTimeMs < cooldownMs) return

                    val toIndex = sectionList.indexOfFirst { it.id == targetNote.id }
                    if (toIndex < 0 || toIndex == fromIndex) return

                    val mutable = sectionList.toMutableList()
                    val moved = mutable.removeAt(fromIndex)
                    mutable.add(toIndex, moved)
                    if (draggedIsPinned) {
                        liveReorderPinned = mutable
                    } else {
                        liveReorderOther = mutable
                    }
                    lastSwappedCandidateId = targetNote.id
                    lastReorderSwapTimeMs = now
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                fun handleEndReorder() {
                    val session = reorderSession ?: return
                    if (session.isSettling) return
                    val finalPinned = liveReorderPinned ?: visiblePinnedNotes
                    val finalOther = liveReorderOther ?: visibleOtherNotes
                    val newIds = (finalPinned + finalOther).map { it.id }
                    val oldIds = (pinnedNotes + otherNotes).map { it.id }
                    if (newIds != oldIds || dragStartedFromPendingOrder) {
                        pendingCommittedOrderIds = if (newIds != oldIds) newIds else null
                        if (uiState.sortOption != NoteSortOption.CUSTOM) {
                            suppressSortResetScroll = true
                        }
                        onCommitCustomOrder(newIds)
                    }
                    dragStartedFromPendingOrder = false
                    session.isSettling = true
                }

                fun handleArchiveCommitted(archivedId: Long) {
                    archivingNoteIds[archivedId] = true
                    onArchiveNote(archivedId)
                    scope.launch {
                        homeSnackbarHostState.currentSnackbarData?.dismiss()
                        val result = homeSnackbarHostState.showSnackbar(
                            message = noteArchivedMessage,
                            actionLabel = undoLabel,
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onUndoMoveToArchive(listOf(archivedId))
                        }
                    }
                }

                // Changing the search, filter or sort should show the results from
                // the top instead of keeping the previous scroll position.
                LaunchedEffect(
                    uiState.searchQuery,
                    uiState.selectedTagFilter,
                    uiState.selectedColorFilter,
                    uiState.sortOption
                ) {
                    if (suppressSortResetScroll) {
                        suppressSortResetScroll = false
                    } else {
                        gridState.scrollToItem(0)
                    }
                }

                val currentSessionForScroll by rememberUpdatedState(reorderSession)
                val densityForScroll = LocalDensity.current
                LaunchedEffect(reorderSession?.note?.id) {
                    if (reorderSession == null) return@LaunchedEffect
                    val edgePx = with(densityForScroll) { 64.dp.toPx() }
                    val maxStepPx = with(densityForScroll) { 14.dp.toPx() }
                    while (isActive) {
                        val session = currentSessionForScroll ?: break
                        if (!session.isSettling) {
                            val gridRect = gridBoundsInRoot
                            if (!gridRect.isEmpty) {
                                val py = session.pointerRootPos.y
                                val delta = when {
                                    py < gridRect.top + edgePx -> {
                                        val factor = ((gridRect.top + edgePx - py) / edgePx).coerceIn(0f, 1f)
                                        -maxStepPx * factor
                                    }
                                    py > gridRect.bottom - edgePx -> {
                                        val factor = ((py - (gridRect.bottom - edgePx)) / edgePx).coerceIn(0f, 1f)
                                        maxStepPx * factor
                                    }
                                    else -> 0f
                                }
                                if (delta != 0f) {
                                    val consumed = gridState.scrollBy(delta)
                                    if (consumed != 0f) {
                                        handleMoveReorder(session.pointerRootPos)
                                    }
                                }
                            }
                        }
                        withFrameMillis { }
                    }
                }

                val itemPlacementSpec = remember {
                    spring<IntOffset>(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                }

                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = gridColumns,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                gridBoundsInRoot = coords.boundsInRoot()
                            }
                        }
                ) {
                    // Do NOT permanently display a dedicated "Pinned Section" header unless
                    // pinned notes actually exist!
                    if (visiblePinnedNotes.isNotEmpty()) {
                        item(key = "pinned_header", span = StaggeredGridItemSpan.FullLine) {
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

                        items(visiblePinnedNotes, key = { it.id }) { note ->
                            NoteTouchGestureBox(
                                note = note,
                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                isSelectionMode = uiState.isSelectionMode,
                                gesturesEnabled = gesturesEnabled,
                                isBeingDraggedForReorder = reorderSession?.note?.id == note.id,
                                isHiddenForTrashOrArchive = note.id in deletingNoteIds || note.id in archivingNoteIds,
                                hasLeftNeighborInSameSection = hasLeftNeighbor(note, visiblePinnedNotes),
                                searchQuery = uiState.searchQuery,
                                activeGestureNoteId = activeGestureNoteId,
                                onActiveGestureNoteChange = { activeGestureNoteId = it },
                                onCardPositioned = { rect -> noteCardBounds[note.id] = rect },
                                onCardDisposed = { noteCardBounds.remove(note.id) },
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onNoteLongClick(note)
                                    } else {
                                        onNoteClick(note)
                                    }
                                },
                                onLongClick = { onNoteLongClick(note) },
                                onStartReorderDrag = ::handleStartReorder,
                                onMoveReorderDrag = ::handleMoveReorder,
                                onEndReorderDrag = ::handleEndReorder,
                                onArchiveCommitted = ::handleArchiveCommitted,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(180),
                                    fadeOutSpec = tween(180),
                                    placementSpec = itemPlacementSpec
                                )
                            )
                        }

                        if (visibleOtherNotes.isNotEmpty()) {
                            item(key = "others_header", span = StaggeredGridItemSpan.FullLine) {
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

                    items(visibleOtherNotes, key = { it.id }) { note ->
                        NoteTouchGestureBox(
                            note = note,
                            isSelected = uiState.selectedNoteIds.contains(note.id),
                            isSelectionMode = uiState.isSelectionMode,
                            gesturesEnabled = gesturesEnabled,
                            isBeingDraggedForReorder = reorderSession?.note?.id == note.id,
                            isHiddenForTrashOrArchive = note.id in deletingNoteIds || note.id in archivingNoteIds,
                            hasLeftNeighborInSameSection = hasLeftNeighbor(note, visibleOtherNotes),
                            searchQuery = uiState.searchQuery,
                            activeGestureNoteId = activeGestureNoteId,
                            onActiveGestureNoteChange = { activeGestureNoteId = it },
                            onCardPositioned = { rect -> noteCardBounds[note.id] = rect },
                            onCardDisposed = { noteCardBounds.remove(note.id) },
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onNoteLongClick(note)
                                } else {
                                    onNoteClick(note)
                                }
                            },
                            onLongClick = { onNoteLongClick(note) },
                            onStartReorderDrag = ::handleStartReorder,
                            onMoveReorderDrag = ::handleMoveReorder,
                            onEndReorderDrag = ::handleEndReorder,
                            onArchiveCommitted = ::handleArchiveCommitted,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                fadeOutSpec = tween(180),
                                placementSpec = itemPlacementSpec
                            )
                        )
                    }
                }
            }
        }
    }

        reorderSession?.let { session ->
            ReorderDragOverlay(
                session = session,
                targetBoundsProvider = { noteCardBounds[session.note.id] },
                searchQuery = uiState.searchQuery,
                rootOffsetInRoot = rootOffsetInRoot,
                onSettleFinished = {
                    reorderSession = null
                    if (pendingCommittedOrderIds == null) {
                        liveReorderPinned = null
                        liveReorderOther = null
                    }
                }
            )
        }

        pendingTrashDrop?.let { pending ->
            TrashDropOverlay(
                pending = pending,
                rootOffsetInRoot = rootOffsetInRoot,
                rootSize = rootSize,
                onBinClosedCommit = { trashedIds ->
                    onMoveSelectedToTrash(trashedIds)
                    val message = if (trashedIds.size == 1) {
                        singleTrashedMessage
                    } else {
                        context.getString(R.string.archive_note_trashed_count, trashedIds.size)
                    }
                    scope.launch {
                        homeSnackbarHostState.currentSnackbarData?.dismiss()
                        val result = homeSnackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = undoLabel,
                            withDismissAction = true,
                            // The countdown in the snackbar is what closes it.
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onUndoMoveToTrash(trashedIds)
                        }
                    }
                },
                onFinished = {
                    pendingTrashDrop = null
                }
            )
        }
    }

    // Bottom Sheet: Select Note Type ("Text Note" vs "Drawing Note")
    if (showExportSheet) {
        val selectedForExport = notes.filter { uiState.selectedNoteIds.contains(it.id) }
        MarkdownExportSheet(
            selectedCount = selectedForExport.size,
            onDismiss = { showExportSheet = false },
            onSeparateFiles = {
                showExportSheet = false
                exportSelected(selectedForExport, true)
            },
            onSingleFile = {
                showExportSheet = false
                exportSelected(selectedForExport, false)
            }
        )
    }

    if (showBulkColorSheet && uiState.selectedNoteIds.isNotEmpty()) {
        val selectedNotes = notes.filter { it.id in uiState.selectedNoteIds }
        BulkColorPickerSheet(
            selectedNotes = selectedNotes,
            onColorSelected = { colorIndex ->
                onSetSelectedColor(uiState.selectedNoteIds.toList(), colorIndex)
            },
            onDismiss = { showBulkColorSheet = false }
        )
    }

    if (showBulkLabelsSheet && uiState.selectedNoteIds.isNotEmpty()) {
        val selectedNotes = notes.filter { it.id in uiState.selectedNoteIds }
        BulkLabelsSheet(
            selectedNotes = selectedNotes,
            allUniqueLabels = allTags,
            onSetLabelForSelected = { label, enabled ->
                onSetSelectedLabel(uiState.selectedNoteIds.toList(), label, enabled)
            },
            onCreateAndApplyLabel = { newLabel ->
                onCreateAndApplyLabel(uiState.selectedNoteIds.toList(), newLabel)
            },
            onDismiss = { showBulkLabelsSheet = false }
        )
    }

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
                    text = stringResource(R.string.create_note_title),
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.create_text_note),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.create_text_note_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Checklist (Editor that opens straight into tickable items)
                Surface(
                    onClick = {
                        showCreateOptionsSheet = false
                        onAddNewChecklistNote()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_checklist_note_option")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.create_checklist),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.create_checklist_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: Drawing Note (Dedicated drawing canvas screen)
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
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.create_drawing_note),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.create_drawing_note_desc),
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
