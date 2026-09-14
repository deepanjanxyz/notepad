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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.process.ProcessLifecycleOwner
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fully rounded pill shape used by the filter / sort chips. */
private val PillShape = RoundedCornerShape(50)

class MainActivity : FragmentActivity() {

    private val settings by lazy { AppSettings(this) }
    private val dbHelper by lazy { DatabaseHelper(this) }
    private val lockManager by lazy { BiometricLockManager(this) }

    /** True while the note list must stay hidden behind the biometric gate. */
    private var requireUnlock by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only lock when the user enabled it AND some authenticator is actually available.
        requireUnlock = settings.lockOnLaunch && lockManager.canLock()
        // Re-arm the lock only when the whole application goes to the
        // background, so internal navigation (editor, settings) never
        // triggers an unlock prompt on return.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && settings.lockOnLaunch) {
                    requireUnlock = true
                }
            },
        )
        setContent {
            EliteMemoRoot()
        }
    }

    @Composable
    private fun EliteMemoRoot() {
        var snapshot by remember { mutableStateOf(settings.snapshot()) }
        DisposableEffect(Unit) {
            val unregister = settings.observe { snapshot = it }
            onDispose { unregister() }
        }
        EliteMemoTheme(
            themeMode = snapshot.themeMode,
            dynamicColor = snapshot.dynamicColors,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
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
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.biometric_prompt_title),
                style = MaterialTheme.typography.titleLarge,
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

        // Active reload job; cancelled whenever a newer reload starts so
        // obsolete query results can never overwrite the current list.
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

        // Reload whenever the query or any filter changes.
        LaunchedEffect(query, searchScope, sortOrder) { reloadNotes() }

        // Reload when coming back from the editor.
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

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (selectionMode) {
                                    stringResource(R.string.selected_count, selectedIds.size)
                                } else {
                                    stringResource(R.string.app_name)
                                },
                            )
                        },
                        navigationIcon = {
                            if (selectionMode) {
                                IconButton(onClick = { selectedIds = emptySet() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_cancel_selection),
                                    )
                                }
                            }
                        },
                        actions = {
                            if (selectionMode) {
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.action_settings),
                                    )
                                }
                            }
                        },
                    )
                    if (searchVisible) {
                        SearchField(
                            query = query,
                            onQueryChange = { query = it },
                            onClose = {
                                query = ""
                                searchVisible = false
                            },
                        )
                    }
                    FilterRow(
                        searchScope = searchScope,
                        onSearchScopeChange = { searchScope = it },
                        sortOrder = sortOrder,
                        onSortOrderChange = { sortOrder = it },
                    )
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { openEditor(null) }
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_note),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.take_a_note),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (notes.isEmpty()) {
                EmptyState(searching = query.isNotBlank())
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 108.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    items(notes, key = { it.id }) { note ->
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
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_notes_title)) },
                text = { Text(stringResource(R.string.delete_notes_message, selectedIds.size)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val toDelete = selectedIds.toSet()
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    toDelete.forEach { dbHelper.deleteNote(it) }
                                }
                                selectedIds = emptySet()
                                showDeleteDialog = false
                                reloadNotes()
                            }
                        },
                    ) { Text(stringResource(R.string.action_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }

    private fun openEditor(note: Note?) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        if (note != null) {
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id)
            intent.putExtra(NoteEditorActivity.EXTRA_TITLE, note.title)
            intent.putExtra(NoteEditorActivity.EXTRA_CONTENT, note.content)
        }
        startActivity(intent)
    }

    @Composable
    private fun SearchField(
        query: String,
        onQueryChange: (String) -> Unit,
        onClose: () -> Unit,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_clear_search),
                    )
                }
            },
        )
    }

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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchScope.entries.forEach { scope ->
                FilterChip(
                    selected = searchScope == scope,
                    onClick = { onSearchScopeChange(scope) },
                    label = { Text(stringResource(scope.labelRes)) },
                    shape = PillShape,
                )
            }
            Text(
                text = stringResource(R.string.sort_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box {
                AssistChip(
                    onClick = { sortMenuExpanded = true },
                    label = { Text(stringResource(sortOrder.labelRes)) },
                    shape = PillShape,
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
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NoteCard(
        note: Note,
        selectionMode: Boolean,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
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
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun EmptyState(searching: Boolean) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (searching) {
                    stringResource(R.string.no_search_results)
                } else {
                    stringResource(R.string.no_notes_title)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (searching) {
                    stringResource(R.string.no_search_results_subtitle)
                } else {
                    stringResource(R.string.no_notes_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
