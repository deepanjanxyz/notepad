package com.deepanjanxyz.notepad.features.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.core.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteEditorViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    fun loadNote(noteId: Long) {
        if (noteId <= 0L) {
            _uiState.update { NoteEditorUiState() }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                _uiState.update {
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        date = note.date,
                        isSaved = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, isSaved = false) }
        autoSave()
    }

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent, isSaved = false) }
        autoSave()
    }

    fun toggleMarkdownPreview() {
        _uiState.update { it.copy(isMarkdownPreview = !it.isMarkdownPreview) }
    }

    fun insertMarkdown(prefix: String, suffix: String = "") {
        val currentContent = _uiState.value.content
        val newContent = if (currentContent.isEmpty()) {
            "$prefix$suffix"
        } else {
            "$currentContent\n$prefix$suffix"
        }
        onContentChanged(newContent)
    }

    private fun autoSave() {
        saveNoteLocally()
    }

    fun saveNoteLocally() {
        val current = _uiState.value
        val title = current.title.trim()
        val content = current.content.trim()

        if (title.isEmpty() && content.isEmpty()) {
            return
        }

        val dateStr = dateFormat.format(Date())

        viewModelScope.launch {
            if (current.noteId <= 0L) {
                val newId = noteRepository.insertNote(title, content, dateStr)
                if (newId != -1L) {
                    _uiState.update { it.copy(noteId = newId, date = dateStr, isSaved = true) }
                }
            } else {
                val success = noteRepository.updateNote(current.noteId, title, content, dateStr)
                if (success) {
                    _uiState.update { it.copy(date = dateStr, isSaved = true) }
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val noteId = _uiState.value.noteId
        if (noteId > 0L) {
            viewModelScope.launch {
                noteRepository.deleteNote(noteId)
                onDeleted()
            }
        } else {
            onDeleted()
        }
    }
}
