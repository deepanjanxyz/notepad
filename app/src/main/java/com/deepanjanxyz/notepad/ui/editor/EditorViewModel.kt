package com.deepanjanxyz.notepad.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.data.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val id: Long = -1L,
        val title: String = "",
        val content: String = "",
    )

    private val repository = NoteRepository(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var saveJob: Job? = null

    fun load(noteId: Long) {
        if (noteId == -1L || _state.value.id != -1L) return
        viewModelScope.launch {
            repository.getNote(noteId)?.let { note ->
                _state.update { it.copy(id = note.id, title = note.title, content = note.content) }
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value) }
        scheduleSave()
    }

    fun onContentChange(value: String) {
        _state.update { it.copy(content = value) }
        scheduleSave()
    }

    /**
     * Auto-save, but debounced — the legacy editor wrote to SQLite on every
     * keystroke; we coalesce edits and write at most once per [SAVE_DEBOUNCE_MS].
     */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            flush()
        }
    }

    suspend fun flush() {
        val current = _state.value
        if (current.title.isBlank() && current.content.isBlank()) return
        val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val id = repository.upsert(current.id, current.title, current.content, date)
        _state.update { it.copy(id = id) }
    }

    override fun onCleared() {
        saveJob?.cancel()
        // Last-chance save for edits made within the debounce window. This is a
        // single tiny SQLite write, exactly what the legacy app did per keystroke.
        if (_state.value.title.isNotBlank() || _state.value.content.isNotBlank()) {
            runBlocking { flush() }
        }
        super.onCleared()
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 500L
    }
}
