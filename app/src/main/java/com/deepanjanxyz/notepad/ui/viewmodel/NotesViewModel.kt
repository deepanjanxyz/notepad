package com.deepanjanxyz.notepad.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.backup.DownloadsBackup
import com.deepanjanxyz.notepad.backup.buildBackupJson
import com.deepanjanxyz.notepad.backup.isBackupWorthy
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.usecase.label.LabelUseCases
import com.deepanjanxyz.notepad.domain.usecase.note.NoteUseCases
import com.deepanjanxyz.notepad.domain.usecase.note.SavedNote
import com.deepanjanxyz.notepad.domain.usecase.settings.SettingsUseCases
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface Screen {
    data object Home : Screen
    data class Editor(val noteId: Long = 0L, val returnTo: Screen = Home) : Screen
    data class Drawing(val noteId: Long = 0L, val returnTo: Screen = Home) : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Settings : Screen
}

/** Sort orders offered for the home note list. */
enum class NoteSortOption(val label: String) {
    LAST_MODIFIED("Last modified"),
    DATE_CREATED("Date created"),
    TITLE_ASC("Title A\u2013Z"),
    TITLE_DESC("Title Z\u2013A"),
    COLOR("Color")
}

/** Result of an import: how many notes were added and how many were skipped as duplicates. */
data class ImportOutcome(val imported: Int, val skipped: Int)

/**
 * Outcome of the most recent automatic backup. Settings shows this so it can
 * report what actually happened instead of claiming a save that never ran.
 */
data class AutoBackupStatus(
    val location: String,
    val lastModified: Long?,
    val error: String? = null
)

data class NotesUiState(
    val currentScreen: Screen = Screen.Home,
    val isSelectionMode: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val isLocked: Boolean = false,
    val lockEnabled: Boolean = false,
    val themeMode: String = "dark",
    val isGridLayout: Boolean = true,
    val searchQuery: String = "",
    val selectedColorFilter: Int? = null,
    val selectedTagFilter: String? = null,
    val showEditLabelsDialog: Boolean = false,
    val sortOption: NoteSortOption = NoteSortOption.LAST_MODIFIED,
    val autoBackup: Boolean = false
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteUseCases: NoteUseCases = (application as NotepadApplication).container.noteUseCases
    private val labelUseCases: LabelUseCases = (application as NotepadApplication).container.labelUseCases
    private val settingsUseCases: SettingsUseCases = (application as NotepadApplication).container.settingsUseCases

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "NotesViewModel"
    }

    init {
        observeSettings()
    }

    /**
     * Mirrors the persisted settings into the UI state. The lock flag is applied
     * only on the first emission so that unlocking during a session is not undone
     * by later settings changes (e.g. switching the theme).
     */
    private fun observeSettings() {
        viewModelScope.launch {
            var isFirstEmission = true
            settingsUseCases.getSettings().collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        themeMode = settings.themeMode,
                        isGridLayout = settings.isGridLayout,
                        lockEnabled = settings.lockEnabled,
                        autoBackup = settings.autoBackup,
                        isLocked = if (isFirstEmission) settings.lockEnabled else current.isLocked
                    )
                }
                updateAutoBackup(settings.autoBackup)
                isFirstEmission = false
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<Int?>(null)
    val selectedColorFilter: StateFlow<Int?> = _selectedColorFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(NoteSortOption.LAST_MODIFIED)

    // Ids of the notes most recently moved to Trash, so the home screen's Undo
    // action can restore exactly that batch.
    private var lastTrashedIds: List<Long> = emptyList()

    // One-shot result of the most recent import, surfaced to the Settings screen.
    private val _importOutcome = MutableStateFlow<ImportOutcome?>(null)
    val importOutcome: StateFlow<ImportOutcome?> = _importOutcome.asStateFlow()

    // Room DB Labels Stream - starts completely clean
    val roomLabels: StateFlow<List<String>> = labelUseCases.getLabels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All active notes stream (not trash, not archive)
    val rawActiveNotes: StateFlow<List<Note>> = noteUseCases.getNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Archive notes stream
    val archiveNotes: StateFlow<List<Note>> = noteUseCases.getArchiveNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All trash notes stream
    val trashNotes: StateFlow<List<Note>> = noteUseCases.getTrashNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered active notes combining query, color tag, user-defined tag, and
    // the selected sort order.
    val filteredNotes: StateFlow<List<Note>> = combine(
        rawActiveNotes,
        _searchQuery,
        _selectedColorFilter,
        _selectedTagFilter,
        _sortOption
    ) { allNotes, query, colorFilter, tagFilter, sortOption ->
        val filtered = noteUseCases.filterNotes(
            notes = allNotes,
            query = query,
            colorFilter = colorFilter,
            tagFilter = tagFilter
        )
        sortNotes(filtered, sortOption)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All unique tags combining Room DB labels and note tags
    val allTags: StateFlow<List<String>> = combine(rawActiveNotes, roomLabels) { notes, labels ->
        val tagsFromNotes = notes.flatMap { it.tags }
        (labels + tagsFromNotes)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Every note the user can back up: active notes plus archived notes.
    // Trashed notes are intentionally excluded from a backup.
    val backupNotes: StateFlow<List<Note>> = combine(
        rawActiveNotes,
        archiveNotes
    ) { active, archived -> active + archived }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onColorFilterChange(colorIndex: Int?) {
        _selectedColorFilter.value = colorIndex
        _uiState.value = _uiState.value.copy(selectedColorFilter = colorIndex)
    }

    fun onTagFilterChange(tag: String?) {
        _selectedTagFilter.value = tag
        _uiState.value = _uiState.value.copy(selectedTagFilter = tag)
    }

    fun setSortOption(option: NoteSortOption) {
        _sortOption.value = option
        _uiState.value = _uiState.value.copy(sortOption = option)
    }

    fun toggleLayoutView() {
        val newLayout = !_uiState.value.isGridLayout
        // Update synchronously so two quick toggles don't both read the same value.
        _uiState.update { it.copy(isGridLayout = newLayout) }
        persistSetting(
            label = "layout",
            onFailure = {
                // Restore the persisted value, but only if no newer toggle has
                // superseded this optimistic one in the meantime.
                if (_uiState.value.isGridLayout == newLayout) {
                    val persisted = settingsUseCases.getSettings().first().isGridLayout
                    _uiState.update { it.copy(isGridLayout = persisted) }
                }
            }
        ) {
            settingsUseCases.saveSettings.setGridLayout(newLayout)
        }
    }

    fun setEditLabelsDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showEditLabelsDialog = visible)
    }

    fun addLabel(labelName: String) {
        val trimmed = labelName.trim().replace("#", "").take(30)
        if (trimmed.isNotBlank()) {
            val exists = allTags.value.any { it.equals(trimmed, ignoreCase = true) }
            if (!exists) {
                viewModelScope.launch {
                    labelUseCases.addLabel(trimmed)
                }
            }
        }
    }

    fun renameLabel(oldName: String, newName: String) {
        val trimmed = newName.trim().replace("#", "").take(30)
        if (trimmed.isNotBlank() && !trimmed.equals(oldName, ignoreCase = true)) {
            val exists = allTags.value.any { it.equals(trimmed, ignoreCase = true) }
            if (!exists) {
                viewModelScope.launch {
                    labelUseCases.renameLabel(oldName, trimmed)
                }
            }
        }
    }

    fun deleteLabel(labelName: String) {
        viewModelScope.launch {
            labelUseCases.deleteLabel(labelName)
            if (_uiState.value.selectedTagFilter.equals(labelName, ignoreCase = true)) {
                onTagFilterChange(null)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            isSelectionMode = false,
            selectedNoteIds = emptySet()
        )
    }

    fun unlockApp() {
        _uiState.value = _uiState.value.copy(isLocked = false)
    }

    fun setLockEnabled(enabled: Boolean) {
        // Reflect the change only once it is persisted, so a failed write is never
        // reported to the user as saved.
        persistSetting(
            label = "lock",
            onSuccess = { _uiState.update { it.copy(lockEnabled = enabled, isLocked = enabled) } }
        ) {
            settingsUseCases.saveSettings.setLockEnabled(enabled)
        }
    }

    fun setTheme(theme: String) {
        persistSetting("theme") { settingsUseCases.saveSettings.setThemeMode(theme) }
    }

    /**
     * Persists a settings change, logging (instead of crashing on) a failed write.
     * [onSuccess] runs only after the value has been durably stored, and
     * [onFailure] runs after a write that could not be persisted.
     */
    private fun persistSetting(
        label: String,
        onSuccess: () -> Unit = {},
        onFailure: suspend () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
                onSuccess()
            } catch (e: IOException) {
                Log.w(TAG, "Failed to persist $label setting", e)
                onFailure()
            }
        }
    }

    // --- Backup ---------------------------------------------------------------

    // Outcome of the most recent automatic backup, so Settings can report the
    // real result rather than a save that never happened.
    private val _autoBackupStatus = MutableStateFlow<AutoBackupStatus?>(null)
    val autoBackupStatus: StateFlow<AutoBackupStatus?> = _autoBackupStatus.asStateFlow()

    private var autoBackupJob: Job? = null

    // The automatic backup and the "Back up now" button both write the same file,
    // each on its own coroutine. Serialising them means one write can never clear
    // the other's working file or collide with its rename.
    private val backupWriteMutex = Mutex()

    /**
     * Turns automatic backup on or off. The switch is reflected only once the
     * choice is persisted, mirroring the other settings.
     */
    fun setAutoBackup(enabled: Boolean) {
        persistSetting(
            label = "auto backup",
            onSuccess = { _uiState.update { it.copy(autoBackup = enabled) } }
        ) {
            settingsUseCases.saveSettings.setAutoBackup(enabled)
        }
    }

    /**
     * Starts or stops the automatic backup to match the persisted setting. While
     * it is on, the backup file in Downloads is rewritten whenever the set of
     * notes changes, so a new note, an edit or a delete is reflected at once.
     */
    private fun updateAutoBackup(enabled: Boolean) {
        if (!enabled) {
            autoBackupJob?.cancel()
            autoBackupJob = null
            return
        }
        if (autoBackupJob?.isActive == true) return
        autoBackupJob = viewModelScope.launch {
            // Take a snapshot straight away, so turning the switch on is all the
            // user has to do - no manual "Back up now" step is needed.
            writeAutoBackup()
            backupNotes
                .drop(1) // ignore the initial empty value emitted before the database loads
                .collectLatest {
                    // Wait for a quiet moment so a burst of changes becomes one write.
                    delay(600)
                    writeAutoBackup()
                }
        }
    }

    /** Writes a backup to Downloads now, used by the manual "Back up now" action. */
    fun backUp(name: String = DownloadsBackup.FILE_NAME) {
        viewModelScope.launch { writeBackup(name) }
    }

    private suspend fun writeAutoBackup() = writeBackup(DownloadsBackup.FILE_NAME)

    private suspend fun writeBackup(name: String) {
        backupWriteMutex.withLock { writeBackupLocked(name) }
    }

    /** The body of [writeBackup], run with the backup write lock held. */
    private suspend fun writeBackupLocked(name: String) {
        val context = getApplication<Application>()
        // Never write an empty backup file. When there is nothing to store - every
        // note deleted, say - the last non-empty backup is deliberately kept
        // rather than replaced with an empty one: a backup still holding
        // yesterday's notes is worth more than one holding nothing.
        val notes = currentBackupNotes().filter { it.isBackupWorthy() }
        if (notes.isEmpty()) {
            Log.d(TAG, "Skipped the backup: there are no notes to store")
            return
        }
        runCatching {
            withContext(Dispatchers.IO) {
                DownloadsBackup.write(context, buildBackupJson(notes), name)
            }
        }.onSuccess {
            _autoBackupStatus.value = AutoBackupStatus(
                location = DownloadsBackup.locationLabel(name),
                lastModified = System.currentTimeMillis()
            )
        }.onFailure { error ->
            Log.w(TAG, "Backup to Downloads failed", error)
            _autoBackupStatus.value = AutoBackupStatus(
                location = DownloadsBackup.locationLabel(name),
                lastModified = null,
                error = error.message
            )
        }
    }

    /** Every note a backup contains: active notes plus archived notes. */
    private suspend fun currentBackupNotes(): List<Note> =
        noteUseCases.getNotes().first() + noteUseCases.getArchiveNotes().first()

    fun toggleSelection(noteId: Long) {
        val current = _uiState.value.selectedNoteIds.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _uiState.value = _uiState.value.copy(
            selectedNoteIds = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedNoteIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun selectAll(noteList: List<Note> = filteredNotes.value) {
        val allIds = noteList.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedNoteIds = allIds,
            isSelectionMode = allIds.isNotEmpty()
        )
    }

    fun togglePin(noteId: Long, currentPinned: Boolean) {
        viewModelScope.launch {
            noteUseCases.togglePin(noteId, !currentPinned)
        }
    }

    fun togglePinForSelected() {
        val selectedIds = _uiState.value.selectedNoteIds.toList()
        if (selectedIds.isEmpty()) return

        val active = rawActiveNotes.value
        val selectedNotes = active.filter { selectedIds.contains(it.id) }
        val shouldPin = selectedNotes.any { !it.isPinned }

        viewModelScope.launch {
            selectedIds.forEach { id ->
                noteUseCases.togglePin(id, shouldPin)
            }
            clearSelection()
        }
    }

    // Move to Trash (Recycler)
    fun moveToTrash(noteId: Long) {
        viewModelScope.launch {
            noteUseCases.trashNote(noteId)
            NoteReminderScheduler.cancelReminder(getApplication(), noteId)
        }
    }

    fun moveSelectedToTrash() {
        moveSelectedToTrash(_uiState.value.selectedNoteIds.toList())
    }

    fun moveSelectedToTrash(selectedIds: List<Long>) {
        val idsToTrash = selectedIds.distinct()
        if (idsToTrash.isEmpty()) return
        lastTrashedIds = idsToTrash
        viewModelScope.launch {
            noteUseCases.trashNote(idsToTrash)
            idsToTrash.forEach { id ->
                NoteReminderScheduler.cancelReminder(getApplication(), id)
            }
            clearSelection()
        }
    }

    /** Restores the notes most recently moved to Trash, backing the Undo action. */
    fun undoMoveToTrash() {
        val ids = lastTrashedIds
        if (ids.isEmpty()) return
        lastTrashedIds = emptyList()
        viewModelScope.launch {
            noteUseCases.restoreNote(ids)
        }
    }

    // Restore from Trash
    fun restoreFromTrash(noteId: Long) {
        viewModelScope.launch {
            noteUseCases.restoreNote(noteId)
        }
    }

    fun restoreSelectedTrashNotes() {
        val idsToRestore = _uiState.value.selectedNoteIds.toList()
        viewModelScope.launch {
            noteUseCases.restoreNote(idsToRestore)
            clearSelection()
        }
    }

    // Archive / Unarchive
    fun moveToArchive(noteId: Long) {
        viewModelScope.launch {
            noteUseCases.archiveNote.moveToArchive(noteId)
        }
    }

    fun moveSelectedToArchive() {
        val ids = _uiState.value.selectedNoteIds.toList()
        viewModelScope.launch {
            noteUseCases.archiveNote.moveNotesToArchive(ids)
            clearSelection()
        }
    }

    fun restoreFromArchive(noteId: Long) {
        viewModelScope.launch {
            noteUseCases.archiveNote.restoreFromArchive(noteId)
        }
    }

    fun restoreSelectedArchiveNotes(selectedIds: List<Long>) {
        val ids = selectedIds.distinct()
        if (ids.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            noteUseCases.archiveNote.restoreNotesFromArchive(ids)
            clearSelection()
        }
    }

    // Permanent deletion in Trash
    fun permanentlyDelete(noteId: Long) {
        viewModelScope.launch {
            noteUseCases.permanentlyDeleteNote(noteId)
            NoteReminderScheduler.cancelReminder(getApplication(), noteId)
        }
    }

    fun permanentlyDeleteSelectedTrashNotes() {
        val idsToDelete = _uiState.value.selectedNoteIds.toList()
        viewModelScope.launch {
            noteUseCases.permanentlyDeleteNote(idsToDelete)
            idsToDelete.forEach { id ->
                NoteReminderScheduler.cancelReminder(getApplication(), id)
            }
            clearSelection()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteUseCases.emptyTrash()
            clearSelection()
        }
    }

    // Note Editor actions
    suspend fun getNote(noteId: Long): Note? {
        return noteUseCases.getNoteById(noteId)
    }

    suspend fun saveNote(
        id: Long,
        title: String,
        content: String,
        colorIndex: Int,
        tags: List<String>,
        isPinned: Boolean? = null,
        inArchive: Boolean? = null,
        reminderTime: Long? = null
    ): SavedNote {
        val saved = noteUseCases.saveNote(
            id = id,
            title = title,
            content = content,
            colorIndex = colorIndex,
            tags = tags,
            isPinned = isPinned,
            inArchive = inArchive,
            reminderTime = reminderTime
        )
        val effectiveReminder = saved.reminderTime
        if (effectiveReminder != null && effectiveReminder > System.currentTimeMillis()) {
            NoteReminderScheduler.scheduleReminder(
                context = getApplication(),
                noteId = saved.id,
                noteTitle = title,
                noteContent = content,
                triggerAtMillis = effectiveReminder
            )
        }
        return saved
    }

    fun setNoteReminder(noteId: Long, reminderTime: Long?, title: String = "", content: String = "") {
        viewModelScope.launch {
            noteUseCases.setNoteReminder(noteId, reminderTime)
            val context = getApplication<Application>()
            if (reminderTime != null) {
                NoteReminderScheduler.scheduleReminder(
                    context = context,
                    noteId = noteId,
                    noteTitle = title,
                    noteContent = content,
                    triggerAtMillis = reminderTime
                )
            } else {
                NoteReminderScheduler.cancelReminder(context, noteId)
            }
        }
    }

    /**
     * Imports notes from a backup file, inserting each as a new note.
     *
     * The note's own creation date and timestamps are carried across, and any
     * reminder still in the future is scheduled, so a restored note behaves like
     * one that was never lost. A note that matches an existing one on every field
     * (including notes added by an earlier import in this same batch) is skipped,
     * so restoring the same backup twice does not duplicate.
     */
    fun importNotes(notes: List<Note>) {
        if (notes.isEmpty()) {
            _importOutcome.value = ImportOutcome(imported = 0, skipped = 0)
            return
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val now = System.currentTimeMillis()
            val seen = (rawActiveNotes.value + archiveNotes.value + trashNotes.value)
                .map { noteKey(it) }
                .toMutableSet()
            var imported = 0
            var skipped = 0
            notes.forEach { note ->
                if (!seen.add(noteKey(note))) {
                    skipped++
                    return@forEach
                }
                // Stored through the Note overload rather than the field-by-field
                // one, so the backup's own creation date and timestamps survive
                // the restore instead of being regenerated as today.
                val newId = noteUseCases.saveNote(
                    note.copy(
                        id = 0L,
                        inTrash = false,
                        createdAt = note.createdAt.takeIf { it > 0L } ?: now,
                        updatedAt = note.updatedAt.takeIf { it > 0L } ?: now
                    )
                )
                // A reminder time in the database does not fire anything on its
                // own, so the alarm is scheduled for every future reminder that
                // came in with the backup.
                val trigger = note.reminderTime
                if (trigger != null && trigger > now) {
                    NoteReminderScheduler.scheduleReminder(
                        context = context,
                        noteId = newId,
                        noteTitle = note.title,
                        noteContent = note.content,
                        triggerAtMillis = trigger
                    )
                }
                imported++
            }
            _importOutcome.value = ImportOutcome(imported = imported, skipped = skipped)
        }
    }

    /** Consumes the one-shot import result so it is only shown once. */
    fun clearImportOutcome() {
        _importOutcome.value = null
    }

    /**
     * Identity used to decide whether an imported note is one we already hold.
     *
     * Every field that can tell two notes apart is part of the key, not just the
     * text: two notes written on the same day with the same title and body are
     * still different notes if their tags, reminder, colour or flags differ, and
     * neither should be dropped. Only a note matching on every field counts as
     * one we already have, which is what makes restoring the same backup twice a
     * no-op rather than a way to duplicate the whole list.
     */
    private fun noteKey(note: Note): String = listOf(
        note.title.trim(),
        note.content.trim(),
        note.date.trim(),
        note.colorIndex.toString(),
        note.isPinned.toString(),
        note.inArchive.toString(),
        note.reminderTime?.toString().orEmpty(),
        note.tags.map { it.trim().lowercase() }.sorted().joinToString(",")
    ).joinToString("\u0000")

    private fun sortNotes(notes: List<Note>, option: NoteSortOption): List<Note> {
        val comparator = when (option) {
            // Order by the real timestamps, falling back to the id only for rows
            // that predate them, so editing an old note actually moves it up.
            NoteSortOption.LAST_MODIFIED ->
                compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }.thenByDescending { it.id }
            NoteSortOption.DATE_CREATED ->
                compareByDescending<Note> { it.isPinned }.thenBy { it.createdAt }.thenBy { it.id }
            NoteSortOption.TITLE_ASC -> compareByDescending<Note> { it.isPinned }.thenBy { it.title.lowercase() }
            NoteSortOption.TITLE_DESC -> compareByDescending<Note> { it.isPinned }.thenByDescending { it.title.lowercase() }
            NoteSortOption.COLOR -> compareByDescending<Note> { it.isPinned }.thenBy { it.colorIndex }
        }
        return notes.sortedWith(comparator)
    }

    fun addCustomTag(tag: String) {
        addLabel(tag)
    }

    fun deleteCustomTag(tag: String) {
        deleteLabel(tag)
    }
}
