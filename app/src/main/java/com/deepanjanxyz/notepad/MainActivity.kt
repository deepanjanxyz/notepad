package com.deepanjanxyz.notepad

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.deepanjanxyz.notepad.ui.theme.AccentYellow
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme
import com.deepanjanxyz.notepad.ui.theme.FabPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fully rounded pill shape used by the search bar and chips. */
private val PillShape = RoundedCornerShape(50)

class MainActivity : FragmentActivity() {

    private val settings by lazy { AppSettings(this) }
    private val dbHelper by lazy { DatabaseHelper(this) }
    private val lockManager by lazy { BiometricLockManager(this) }

    /** True while the note list must stay hidden behind the biometric gate. */
    private var requireUnlock by mutableStateOf(false)

    /** Holds the ProcessLifecycleOwner observer so it can be removed in onDestroy(). */
    private var processLifecycleObserver: LifecycleEventObserver? = null

    /**
     * Set by the process-level ON_STOP callback; consumed by [onStart] so that
     * the biometric prompt is only launched when the activity is actually
     * coming to the foreground, not while it is still backgrounded.
     */
    private var pendingLock: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only lock when the user enabled it AND some authenticator is actually available.
        requireUnlock = settings.lockOnLaunch && lockManager.canLock()
        processLifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && settings.lockOnLaunch) {
                pendingLock = true
            }
        }
        processLifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.addObserver(it) }
        setContent {
            EliteMemoRoot()
        }
    }

    override fun onStart() {
        super.onStart()
        if (pendingLock) {
            pendingLock = false
            requireUnlock = true
        }
    }

    override fun onDestroy() {
        processLifecycleObserver?.let {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(it)
        }
        processLifecycleObserver = null
        super.onDestroy()
    }

    @Composable
    private fun EliteMemoRoot() {
        val snapshot by remember { mutableStateOf(settings.snapshot()) }
        DisposableEffect(Unit) {
            val unregister = settings.observe { snapshot = it }
            onDispose { unregister() }
        }
        EliteMemoTheme(
            themeMode = snapshot.themeMode,
            dynamicColor = snapshot.dynamicColors,
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (requireUnlock) LockScreen() else NotesScreen()
            }
        }
    }

    @Composable
    private fun LockScreen() {
        LaunchedEffect(Unit) {
            lockManager.authenticate(
                onSuccess = { requireUnlock = false },
                onError = { finish() },
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = AccentYellow,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.biometric_prompt_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.biometric_prompt_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NotesScreen() {
        val coroutineScope = rememberCoroutineScope()
        var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
        var query by rememberSaveable { mutableStateOf("") }
        var searchScope by rememberSaveable { mutableStateOf(SearchScope.ALL) }
        var sortOrder by rememberSaveable { mutableStateOf(NoteSortOrder.NEWEST) }
        var searchVisible by rememberSaveable { mutableStateOf(false) }
        var selectedIds by remember { mutableStateOf(setOf<Long>()) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        var reloadJob by remember { mutableStateOf<Job?>(null) }

        fun reloadNotes() {
            reloadJob?.cancel()
            reloadJob = coroutineScope.launch {
                notes = withContext(Dispatchers.IO) {
                    if (query.isBlank()) {
                        dbHelper.getAllNotes(sortOrder)
                    } else {
                        dbHelper.searchNotes(query, searchScope, sortOrder)
                    }
                }
            }
        }

        LaunchedEffect(query, searchScope, sortOrder) { reloadNotes() }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) reloadNotes()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val selectionMode = selectedIds.isNotEmpty()
        BackHandler(enabled = selectionMode) { selectedIds = emptySet() }

        val pinnedNotes = notes.filter { it.pinned }
        val otherNotes = notes.filter { !it.pinned }
        val searching = query.isNotBlank()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (selectionMode) {
                        SelectionTopBar(
                            selectedCount = selectedIds.size,
                            onClear = { selectedIds = emptySet() },
                            onDelete = { showDeleteDialog = true },
                        )
                    } else if (searchVisible) {
                        SearchBar(
                            query = query,
                            onQueryChange = { query = it },
                            onClose = {
                                query = ""
                                searchVisible = false
                            },
                        )
                    } else {
                        HomeSearchBar(
                            onSearchClick = { searchVisible = true },
                        )
                    }
                    if (!selectionMode) {
                        FilterRow(
                            searchScope = searchScope,
                            onSearchScopeChange = { searchScope = it },
                            sortOrder = sortOrder,
                            onSortOrderChange = { sortOrder = it },
                         )
                    }
                    }
            },
            floatingActionButton = {
                 floatingActionButton(
                    onClick = { openEditor(null) },
                    containerColor = FabPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
                }
            },
        ) { innerPadding ->
            if (notes.isEmpty()) {
                EmptyState(searching = searching)
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            SectionHeader(stringResource(R.string.section_pinned))
                        }
                        items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                            NoteCard(
                                note = note,
                                selectionMode = selectionMode,
                                isSelected = note.id in selectedIds,
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = selectedIds.toMutableSet().apply {
                                            if (!add(note.id)) remove(note.id)
                                        }
                                    } else {
                                        openEditor(note)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds.toMutableSet().apply {
                                        if (!add(note.id)) remove(note.id)
                                   }
                                 },
                                onPinToggle = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) { dbHelper.togglePin(note.id) }
                                        reloadNotes()
                                    }
                                },
                            )
                        }
                    }
                    if (otherNotes.isNotEmpty()) {
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            SectionHeader(stringResource(R.string.section_others))
                        }
                        items(otherNotes, key = { "other_${it.id}" }) { note ->
                            NoteCard(
                                note = note,
                                selectionMode = selectionMode,
                                isSelected = note.id in selectedIds,
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = selectedIds.toMutableSet().apply {
                                            if (!add(note.id)) remove(note.id)
                                        }
                                    } else {
                                        openEditor(note)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds.toMutableSet().apply {
                                        if (!add(note.id)) remove(note.id)
                                   }
                                 },
                                onPinToggle = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) { dbHelper.togglePin(note.id) }
                                        reloadNotes()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

    @Composable
    private fun HomeSearchBar(onSearchClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onSearchClick() }
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { /* drawer placeholder */ }) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.action_menu),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.search_your_notes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { /* grid toggle placeholder */ }) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = stringResource(R.string.action_grid_view),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun SearchBar(
        query: String,
        onQueryChange: (String) -> Unit,
        onClose: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        stringResource(R.string.search_your_notes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = AccentYellow,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun SelectionTopBar(
        selectedCount: Int,
        onClear: () -> Unit,
        onDelete: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_cancel_selection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    // ---------- Chips ----------

    @Composable
    private fun FilterRow(
        searchScope: SearchScope,
        onSearchScopeChange: (SearchScope) -> Unit,
        sortOrder: NoteSortOrder,
        onSortOrderChange: (NoteSortOrder) -> Unit,
    ) {
        var sortMenuExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchScope.entries.forEach { scope ->
                FilterChip(
                    selected = searchScope == scope,
                    onClick = { onSearchScopeChange(scope) },
                    label = { Text(stringResource(scope.labelRes)) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
            Box {
                FilterChip(
                    selected = true,
                    onClick = { sortMenuExpanded = true },
                    label = { Text(stringResource(sortOrder.labelRes)) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    NoteSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(stringResource(order.labelRes)) },
                            onClick = {
                                onSortOrderChange(order)
                                sortMenuExpanded = false
                            },
                        )
                    }
            }
        }
    }

    // ---------- Note card ----------

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NoteCard(
        note: Note,
        selectionMode: Boolean,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        onPinToggle: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onLongClick() },
                            )
                        }
                        Text(
                            text = note.title.ifBlank { stringResource(R.string.untitled_note) },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Pin icon in top-right corner
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = stringResource(R.string.action_pin),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp)
                        .clickable { onPinToggle() },
                    tint = if (note.pinned) AccentYellow else MaterialTheme.colorScheme.onSurfaceVariant,
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 4.dp, end = 4.dp,
                top = 8.dp, bottom = 4.dp,
            ),
        )
    }

    @Composable
    private fun EmptyState(searching: Boolean) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (searching) {
                Text(
                    text = stringResource(R.string.no_search_results),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.no_search_results_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.no_notes_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.no_notes_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
