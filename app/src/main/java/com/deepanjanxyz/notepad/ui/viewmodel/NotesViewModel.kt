package com.deepanjanxyz.notepad.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.usecase.label.LabelUseCases
import com.deepanjanxyz.notepad.domain.usecase.note.NoteUseCases
import com.deepanjanxyz.notepad.domain.usecase.settings.SettingsUseCases
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface Screen {
    data object Home : Screen
    data class Editor(val noteId: Long = 0L) : Screen
    data class Drawing(val noteId: Long = 0L) : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Settings : Screen
}

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
    val showEditLabelsDialog: Boolean = false
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
                        isLocked = if (isFirstEmission) settings.lockEnabled else current.isLocked
                    )
                }
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

    // Filtered active notes combining query, color tag, and user-defined tag
    val filteredNotes: StateFlow<List<Note>> = combine(
        rawActiveNotes,
        _searchQuery,
        _selectedColorFilter,
        _selectedTagFilter
    ) { allNotes, query, colorFilter, tagFilter ->
        noteUseCases.filterNotes(
            notes = allNotes,
            query = query,
            colorFilter = colorFilter,
            tagFilter = tagFilter
        )
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
        viewModelScope.launch {
            noteUseCases.trashNote(idsToTrash)
            idsToTrash.forEach { id ->
                NoteReminderScheduler.cancelReminder(getApplication(), id)
            }
            clearSelection()
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
    ): Long {
        val savedId = noteUseCases.saveNote(
            id = id,
            title = title,
            content = content,
            colorIndex = colorIndex,
            tags = tags,
            isPinned = isPinned,
            inArchive = inArchive,
            reminderTime = reminderTime
        )
        val effectiveReminder = reminderTime ?: noteUseCases.getNoteById(savedId)?.reminderTime
        if (effectiveReminder != null && effectiveReminder > System.currentTimeMillis()) {
            NoteReminderScheduler.scheduleReminder(
                context = getApplication(),
                noteId = savedId,
                noteTitle = title,
                noteContent = content,
                triggerAtMillis = effectiveReminder
            )
        }
        return savedId
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

    fun addCustomTag(tag: String) {
        addLabel(tag)
    }

    fun deleteCustomTag(tag: String) {
        deleteLabel(tag)
    }
}
