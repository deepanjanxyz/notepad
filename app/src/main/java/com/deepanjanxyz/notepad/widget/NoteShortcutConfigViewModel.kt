package com.deepanjanxyz.notepad.widget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.NoteShortcut
import com.deepanjanxyz.notepad.domain.model.NoteShortcutBuilder
import com.deepanjanxyz.notepad.domain.model.NoteShortcutKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One note offered on the shortcut picker. */
data class NoteShortcutChoice(
    val noteId: Long,
    val title: String,
    val preview: String,
    val isDrawing: Boolean,
    val colorIndex: Int
)

/** What the shortcut picker shows. */
data class NoteShortcutConfigUiState(
    val isLoading: Boolean = true,
    val choices: List<NoteShortcutChoice> = emptyList()
)

/**
 * Supplies the notes the shortcut picker offers, and stores the note that is
 * picked.
 *
 * The picker lists active notes only: the note store already leaves trashed and
 * archived notes out of that stream, so a shortcut can never be pointed at a note
 * the home screen does not show.
 */
class NoteShortcutConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NotepadApplication).container

    val uiState: StateFlow<NoteShortcutConfigUiState> = container.noteUseCases.getNotes()
        .map { notes ->
            NoteShortcutConfigUiState(
                isLoading = false,
                choices = notes.map { note -> toChoice(note) }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = NoteShortcutConfigUiState()
        )

    /** Points [appWidgetId] at [noteId], replacing whatever it showed before. */
    suspend fun link(appWidgetId: Int, noteId: Long) {
        container.noteShortcutUseCases.linkNote(appWidgetId, noteId)
    }

    private fun toChoice(note: Note): NoteShortcutChoice {
        val shortcut = NoteShortcutBuilder.build(note)
        return NoteShortcutChoice(
            noteId = note.id,
            title = shortcut.title,
            preview = shortcutPreview(shortcut),
            isDrawing = shortcut.kind == NoteShortcutKind.DRAWING,
            colorIndex = note.colorIndex
        )
    }

    /** The first line of a text note, and nothing for a drawing or an empty one. */
    private fun shortcutPreview(shortcut: NoteShortcut): String =
        shortcut.text.lineSequence().firstOrNull().orEmpty()

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
