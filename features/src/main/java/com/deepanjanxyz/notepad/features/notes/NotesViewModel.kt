package com.deepanjanxyz.notepad.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.core.data.NoteRepository
import com.deepanjanxyz.notepad.core.data.PreferenceRepository
import com.deepanjanxyz.notepad.core.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteRepository: NoteRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _internalState = MutableStateFlow(NotesUiState())

    val uiState: StateFlow<NotesUiState> = combine(
        _internalState,
        noteRepository.observeNotes(),
        preferenceRepository.observeBiometricLockEnabled(),
        preferenceRepository.observeIsGridLayout()
    ) { internal, notes, lockEnabled, isGrid ->
        val filtered = if (internal.searchQuery.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(internal.searchQuery, ignoreCase = true) ||
                    note.content.contains(internal.searchQuery, ignoreCase = true)
            }
        }

        val effectiveAuth = if (!lockEnabled) true else internal.isAuthenticated

        internal.copy(
            notes = notes,
            filteredNotes = filtered,
            isBiometricLocked = lockEnabled,
            isAuthenticated = effectiveAuth,
            isGridLayout = isGrid,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _internalState.update { it.copy(searchQuery = query) }
    }

    fun onSearchToggled(active: Boolean) {
        _internalState.update {
            it.copy(
                isSearching = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
    }

    fun toggleLayoutMode() {
        viewModelScope.launch {
            val currentGrid = uiState.value.isGridLayout
            preferenceRepository.setGridLayout(!currentGrid)
        }
    }

    fun onNoteClick(note: Note, onNavigateToEditor: (Long) -> Unit) {
        val current = _internalState.value
        if (current.isSelectionMode) {
            toggleNoteSelection(note.id)
        } else {
            onNavigateToEditor(note.id)
        }
    }

    fun onNoteLongClick(note: Note) {
        val current = _internalState.value
        if (!current.isSelectionMode) {
            _internalState.update {
                it.copy(
                    isSelectionMode = true,
                    selectedNoteIds = setOf(note.id)
                )
            }
        } else {
            toggleNoteSelection(note.id)
        }
    }

    fun toggleNoteSelection(noteId: Long) {
        _internalState.update { state ->
            val updated = state.selectedNoteIds.toMutableSet()
            if (updated.contains(noteId)) {
                updated.remove(noteId)
            } else {
                updated.add(noteId)
            }

            if (updated.isEmpty()) {
                state.copy(isSelectionMode = false, selectedNoteIds = emptySet())
            } else {
                state.copy(isSelectionMode = true, selectedNoteIds = updated)
            }
        }
    }

    fun selectAll() {
        _internalState.update { state ->
            val allIds = uiState.value.filteredNotes.map { it.id }.toSet()
            state.copy(selectedNoteIds = allIds)
        }
    }

    fun clearSelection() {
        _internalState.update {
            it.copy(
                isSelectionMode = false,
                selectedNoteIds = emptySet()
            )
        }
    }

    fun deleteSelectedNotes() {
        val selected = _internalState.value.selectedNoteIds.toList()
        if (selected.isNotEmpty()) {
            viewModelScope.launch {
                noteRepository.deleteNotes(selected)
                clearSelection()
            }
        }
    }

    fun onBiometricAuthenticated() {
        _internalState.update { it.copy(isAuthenticated = true) }
    }
}
