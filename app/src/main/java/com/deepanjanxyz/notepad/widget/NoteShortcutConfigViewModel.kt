package com.deepanjanxyz.notepad.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.NoteShortcut
import com.deepanjanxyz.notepad.domain.model.NoteShortcutBuilder
import com.deepanjanxyz.notepad.domain.model.NoteShortcutKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One note offered on the shortcut picker. */
data class NoteShortcutChoice(
    val noteId: Long,
    val title: String,
    val preview: String,
    val isDrawing: Boolean,
    val colorIndex: Int,
    /** True when this note is already the one the widget is linked to. */
    val isCurrentlyLinked: Boolean = false
)

/** What the shortcut picker shows. */
data class NoteShortcutConfigUiState(
    val isLoading: Boolean = true,
    val choices: List<NoteShortcutChoice> = emptyList(),
    val searchQuery: String = "",
    /** The note id the widget already links to, or null when it has never been configured. */
    val currentNoteId: Long? = null
)

/**
 * Supplies the notes the shortcut picker offers, and stores the note that is
 * picked.
 *
 * The picker lists active notes only: the note store already leaves trashed and
 * archived notes out of that stream, so a shortcut can never be pointed at a note
 * the home screen does not show.
 *
 * When the picker is opened for a widget that is already configured (e.g. because
 * its note was deleted), the currently linked note is marked so the user can see
 * which one it replaced.
 */
class NoteShortcutConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as NotepadApplication).container

    val lockEnabled: StateFlow<Boolean?> = container.settingsUseCases.getSettings()
        .map { settings -> settings.lockEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _appWidgetId = MutableStateFlow(AppWidgetManager.INVALID_APPWIDGET_ID)
    private val _searchQuery = MutableStateFlow("")

    /**
     * Looks up the note id already linked to the given widget id, emitting null
     * for an unconfigured widget. A new value is emitted whenever [setAppWidgetId]
     * is called, which is once per activity launch.
     */
    private val currentNoteId: kotlinx.coroutines.flow.Flow<Long?> =
        _appWidgetId.flatMapLatest { widgetId ->
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                flowOf(null)
            } else {
                flow { emit(container.noteShortcutUseCases.getLinkedNoteId(widgetId)) }
            }
        }

    val uiState: StateFlow<NoteShortcutConfigUiState> = combine(
        container.noteUseCases.getNotes(),
        currentNoteId,
        _searchQuery
    ) { notes, linkedNoteId, query ->
        val allChoices = notes.map { note -> toChoice(note, linkedNoteId) }
        val filtered = if (query.isBlank()) {
            allChoices
        } else {
            allChoices.filter { choice ->
                choice.title.contains(query, ignoreCase = true) ||
                    choice.preview.contains(query, ignoreCase = true)
            }
        }
        NoteShortcutConfigUiState(
            isLoading = false,
            choices = filtered,
            searchQuery = query,
            currentNoteId = linkedNoteId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = NoteShortcutConfigUiState()
    )

    /**
     * Tells the view model which widget is being configured, so it can look up
     * the note already linked to it and mark it in the picker. Called once from
     * the activity before [uiState] is collected.
     */
    fun setAppWidgetId(appWidgetId: Int) {
        _appWidgetId.value = appWidgetId
    }

    /** Filters the note list to those whose title or preview matches [query]. */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Points [appWidgetId] at [noteId], replacing whatever it showed before. */
    suspend fun link(appWidgetId: Int, noteId: Long) {
        container.noteShortcutUseCases.linkNote(appWidgetId, noteId)
    }

    private fun toChoice(note: Note, currentNoteId: Long?): NoteShortcutChoice {
        val shortcut = NoteShortcutBuilder.build(note)
        return NoteShortcutChoice(
            noteId = note.id,
            title = shortcut.title,
            preview = shortcutPreview(shortcut),
            isDrawing = shortcut.kind == NoteShortcutKind.DRAWING,
            colorIndex = note.colorIndex,
            isCurrentlyLinked = note.id == currentNoteId
        )
    }

    /** The first line of a text note, and nothing for a drawing or an empty one. */
    private fun shortcutPreview(shortcut: NoteShortcut): String =
        shortcut.text.lineSequence().firstOrNull().orEmpty()

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
