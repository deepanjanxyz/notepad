package com.deepanjanxyz.notepad.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import com.deepanjanxyz.notepad.domain.util.NoteFilter
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val prefs = application.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE)
    private val repository: NoteRepository =
        (application as NotepadApplication).container.repository

    private val _uiState = MutableStateFlow(
        NotesUiState(
            lockEnabled = prefs.getBoolean("pref_lock", false),
            isLocked = prefs.getBoolean("pref_lock", false),
            themeMode = prefs.getString("pref_theme", "dark") ?: "dark",
            isGridLayout = prefs.getBoolean("pref_grid_layout", true)
        )
    )
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<Int?>(null)
    val selectedColorFilter: StateFlow<Int?> = _selectedColorFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    // Room DB Labels Stream - starts completely clean
    val roomLabels: StateFlow<List<String>> = repository.getAllLabels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All active notes stream (not trash, not archive)
    val rawActiveNotes: StateFlow<List<Note>> = repository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Archive notes stream
    val archiveNotes: StateFlow<List<Note>> = repository.getArchiveNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All trash notes stream
    val trashNotes: StateFlow<List<Note>> = repository.getTrashNotes()
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
        NoteFilter.filterNotes(
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
        prefs.edit().putBoolean("pref_grid_layout", newLayout).apply()
        _uiState.value = _uiState.value.copy(isGridLayout = newLayout)
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
                    repository.insertLabel(trimmed)
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
                    repository.renameLabel(oldName, trimmed)
                }
            }
        }
    }

    fun deleteLabel(labelName: String) {
        viewModelScope.launch {
            repository.deleteLabel(labelName)
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
        prefs.edit().putBoolean("pref_lock", enabled).apply()
        _uiState.value = _uiState.value.copy(
            lockEnabled = enabled,
            isLocked = enabled
        )
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("pref_theme", theme).apply()
        _uiState.value = _uiState.value.copy(themeMode = theme)
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
            repository.togglePin(noteId, !currentPinned)
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
                repository.togglePin(id, shouldPin)
            }
            clearSelection()
        }
    }

    // Move to Trash (Recycler)
    fun moveToTrash(noteId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
            NoteReminderScheduler.cancelReminder(getApplication(), noteId)
        }
    }

    fun moveSelectedToTrash() {
        moveSelectedToTrash(_uiState.value.selectedNoteIds.toList())
    }

    fun moveSelectedToTrash(selectedIds: List<Long>) {
        val idsToTrash = selectedIds.distinct()
        viewModelScope.launch {
            repository.moveNotesToTrash(idsToTrash)
            idsToTrash.forEach { id ->
                NoteReminderScheduler.cancelReminder(getApplication(), id)
            }
            clearSelection()
        }
    }

    // Restore from Trash
    fun restoreFromTrash(noteId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
        }
    }

    fun restoreSelectedTrashNotes() {
        val idsToRestore = _uiState.value.selectedNoteIds.toList()
        viewModelScope.launch {
            repository.restoreNotesFromTrash(idsToRestore)
            clearSelection()
        }
    }

    // Archive / Unarchive
    fun moveToArchive(noteId: Long) {
        viewModelScope.launch {
            repository.moveToArchive(noteId)
        }
    }

    fun moveSelectedToArchive() {
        val ids = _uiState.value.selectedNoteIds.toList()
        viewModelScope.launch {
            repository.moveNotesToArchive(ids)
            clearSelection()
        }
    }

    fun restoreFromArchive(noteId: Long) {
        viewModelScope.launch {
            repository.restoreFromArchive(noteId)
        }
    }

    fun restoreSelectedArchiveNotes(selectedIds: List<Long>) {
        val ids = selectedIds.distinct()
        if (ids.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            repository.restoreNotesFromArchive(ids)
            clearSelection()
        }
    }

    // Permanent deletion in Trash
    fun permanentlyDelete(noteId: Long) {
        viewModelScope.launch {
            repository.permanentlyDelete(noteId)
            NoteReminderScheduler.cancelReminder(getApplication(), noteId)
        }
    }

    fun permanentlyDeleteSelectedTrashNotes() {
        val idsToDelete = _uiState.value.selectedNoteIds.toList()
        viewModelScope.launch {
            repository.permanentlyDeleteNotes(idsToDelete)
            idsToDelete.forEach { id ->
                NoteReminderScheduler.cancelReminder(getApplication(), id)
            }
            clearSelection()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            clearSelection()
        }
    }

    // Note Editor actions
    suspend fun getNote(noteId: Long): Note? {
        return repository.getNoteById(noteId)
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
        val existing = if (id != 0L) repository.getNoteById(id) else null
        val finalReminderTime = if (reminderTime != null) {
            reminderTime
        } else {
            existing?.reminderTime
        }
        val noteToSave = Note(
            id = id,
            title = title,
            content = content,
            colorIndex = colorIndex,
            tags = tags,
            date = existing?.date ?: SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
            isPinned = isPinned ?: existing?.isPinned ?: false,
            inTrash = existing?.inTrash ?: false,
            inArchive = inArchive ?: existing?.inArchive ?: false,
            reminderTime = finalReminderTime
        )
        val savedId = repository.insertOrUpdate(noteToSave)
        if (finalReminderTime != null && finalReminderTime > System.currentTimeMillis()) {
            NoteReminderScheduler.scheduleReminder(
                context = getApplication(),
                noteId = savedId,
                noteTitle = title,
                noteContent = content,
                triggerAtMillis = finalReminderTime
            )
        }
        return savedId
    }

    fun setNoteReminder(noteId: Long, reminderTime: Long?, title: String = "", content: String = "") {
        viewModelScope.launch {
            repository.updateReminderTime(noteId, reminderTime)
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
