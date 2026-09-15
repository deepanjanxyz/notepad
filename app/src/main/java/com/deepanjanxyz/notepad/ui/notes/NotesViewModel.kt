package com.deepanjanxyz.notepad.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.data.NoteRepository
import com.deepanjanxyz.notepad.domain.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val loading: Boolean = true,
        val notes: List<Note> = emptyList(),
        val query: String = "",
        val selection: Set<Long> = emptySet(),
    )

    private val repository = NoteRepository(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val notes = repository.getNotes(_state.value.query)
            _state.update { it.copy(loading = false, notes = notes) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            refresh()
        }
    }

    fun toggleSelection(id: Long) {
        _state.update { state ->
            state.copy(
                selection = if (id in state.selection) state.selection - id else state.selection + id,
            )
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = emptySet()) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteNotes(_state.value.selection.toList())
            _state.update { it.copy(selection = emptySet()) }
            refresh()
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
