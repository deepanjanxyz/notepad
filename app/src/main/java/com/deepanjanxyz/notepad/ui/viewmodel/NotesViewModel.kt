package com.deepanjanxyz.notepad.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.usecase.label.LabelUseCases
import com.deepanjanxyz.notepad.domain.usecase.note.NoteUseCases
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Editor(val noteId: Long = 0L) : Screen
    data class Drawing(val noteId: Long = 0L) : Screen
    data object Archive : Screen
    data object Trash : Screen
    data object Settings : Screen
}

/** Kind of transient feedback shown after a note operation. */
enum class FeedbackType {
    MOVED_TO_TRASH,
    ARCHIVED,
    RESTORED,
    DELETED_PERMANENTLY,
    ACTION_FAILED
}

/**
 * A user-facing result message. It is deliberately resource-free so the view
 * model stays Android-UI agnostic; the host activity maps it to a localised
 * string and decides whether to offer an Undo action.
 */
data class FeedbackMessage(
    val type: FeedbackType,
    val count: Int = 0,
    val isUndoable: Boolean = false
)

data class NotesUiState(
    val currentScreen: Screen = Screen.Home,
    val isSelectionMode: Boolean = false,
    /**
     * Single source of truth for multi-selection.
     *
     * Home, Archive and Trash all read this set. They used to keep private
     * copies, which meant archive/trash bulk actions operated on a different set
     * than the one the view model read and therefore did nothing.
     */
    val selectedNoteIds: Set<Long> = emptySet(),
    val isLocked: Boolean = false,
    val lockEnabled: Boolean = false,
    val themeMode: String = "dark",
    val isGridLayout: Boolean = true,
    val searchQuery: String = "",
    val selectedColorFilter: Int? = null,
    val selectedTagFilter: String? = null,
    val showEditLabelsDialog: Boolean = false,
    val feedback: FeedbackMessage? = null
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE)
    private val noteUseCases: NoteUseCases = (application as NotepadApplication).container.noteUseCases
    private val labelUseCases: LabelUseCases = (application as NotepadApplication).container.labelUseCases

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

    /** Undo history for the most recent destructive action. */
    private var lastTrashedIds: List<Long> = emptyList()
    private var lastArchivedIds: List<Long> = emptyList()

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

    /**
     * False only while the first database emission is still pending on a screen
     * that shows a note list. The UI uses it to tell "loading" apart from
     * "genuinely empty", which the old empty-state flash got wrong.
     */
    val isContentReady: StateFlow<Boolean> = combine(
        uiState,
        rawActiveNotes,
        archiveNotes,
        trashNotes
    ) { state, active, archive, trash ->
        state.currentScreen is Screen.Settings ||
            active.isNotEmpty() || archive.isNotEmpty() || trash.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
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
                viewModelScope.launch { labelUseCases.addLabel(trimmed) }
            }
        }
    }

    fun renameLabel(oldName: String, newName: String) {
        val trimmed = newName.trim().replace("#", "").take(30)
        if (trimmed.isNotBlank() && !trimmed.equals(oldName, ignoreCase = true)) {
            val exists = allTags.value.any { it.equals(trimmed, ignoreCase = true) }
            if (!exists) {
                viewModelScope.launch { labelUseCases.renameLabel(oldName, trimmed) }
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
        if (current.contains(noteId)) current.remove(noteId) else current.add(noteId)
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
            selectedIds.forEach { id -> noteUseCases.togglePin(id, shouldPin) }
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
        if (idsToTrash.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            try {
                noteUseCases.trashNote(idsToTrash)
                idsToTrash.forEach { id ->
                    NoteReminderScheduler.cancelReminder(getApplication(), id)
                }
                lastTrashedIds = idsToTrash
                clearSelection()
                emitFeedback(FeedbackType.MOVED_TO_TRASH, idsToTrash.size, undoable = true)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    // Restore from Trash
    fun restoreFromTrash(noteId: Long) {
        viewModelScope.launch {
            try {
                noteUseCases.restoreNote(noteId)
                emitFeedback(FeedbackType.RESTORED, 1)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun restoreSelectedTrashNotes(ids: List<Long>) {
        val idsToRestore = ids.distinct()
        if (idsToRestore.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            try {
                noteUseCases.restoreNote(idsToRestore)
                clearSelection()
                emitFeedback(FeedbackType.RESTORED, idsToRestore.size)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    // Archive / Unarchive
    fun moveToArchive(noteId: Long) {
        viewModelScope.launch {
            try {
                noteUseCases.archiveNote.moveToArchive(noteId)
                lastArchivedIds = listOf(noteId)
                emitFeedback(FeedbackType.ARCHIVED, 1, undoable = true)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun moveSelectedToArchive() {
        moveSelectedToArchive(_uiState.value.selectedNoteIds.toList())
    }

    fun moveSelectedToArchive(ids: List<Long>) {
        val idsToArchive = ids.distinct()
        if (idsToArchive.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            try {
                noteUseCases.archiveNote.moveNotesToArchive(idsToArchive)
                lastArchivedIds = idsToArchive
                clearSelection()
                emitFeedback(FeedbackType.ARCHIVED, idsToArchive.size, undoable = true)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun restoreFromArchive(noteId: Long) {
        viewModelScope.launch {
            try {
                noteUseCases.archiveNote.restoreFromArchive(noteId)
                emitFeedback(FeedbackType.RESTORED, 1)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun restoreSelectedArchiveNotes(selectedIds: List<Long>) {
        val ids = selectedIds.distinct()
        if (ids.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            try {
                noteUseCases.archiveNote.restoreNotesFromArchive(ids)
                clearSelection()
                emitFeedback(FeedbackType.RESTORED, ids.size)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    // Permanent deletion in Trash
    fun permanentlyDelete(noteId: Long) {
        viewModelScope.launch {
            try {
                noteUseCases.permanentlyDeleteNote(noteId)
                NoteReminderScheduler.cancelReminder(getApplication(), noteId)
                emitFeedback(FeedbackType.DELETED_PERMANENTLY, 1)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun permanentlyDeleteSelectedTrashNotes(ids: List<Long>) {
        val idsToDelete = ids.distinct()
        if (idsToDelete.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            try {
                noteUseCases.permanentlyDeleteNote(idsToDelete)
                idsToDelete.forEach { id ->
                    NoteReminderScheduler.cancelReminder(getApplication(), id)
                }
                clearSelection()
                emitFeedback(FeedbackType.DELETED_PERMANENTLY, idsToDelete.size)
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try {
                noteUseCases.emptyTrash()
                clearSelection()
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    /** Reverts the most recent destructive action, if it is still undoable. */
    fun undoLastAction() {
        val trashed = lastTrashedIds
        val archived = lastArchivedIds
        lastTrashedIds = emptyList()
        lastArchivedIds = emptyList()
        viewModelScope.launch {
            try {
                when {
                    trashed.isNotEmpty() -> {
                        noteUseCases.restoreNote(trashed)
                        emitFeedback(FeedbackType.RESTORED, trashed.size)
                    }
                    archived.isNotEmpty() -> {
                        noteUseCases.archiveNote.restoreNotesFromArchive(archived)
                        emitFeedback(FeedbackType.RESTORED, archived.size)
                    }
                    else -> dismissFeedback()
                }
            } catch (error: Exception) {
                emitFeedback(FeedbackType.ACTION_FAILED)
            }
        }
    }

    fun dismissFeedback() {
        _uiState.value = _uiState.value.copy(feedback = null)
    }

    private fun emitFeedback(type: FeedbackType, count: Int = 0, undoable: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            feedback = FeedbackMessage(type = type, count = count, isUndoable = undoable)
        )
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
