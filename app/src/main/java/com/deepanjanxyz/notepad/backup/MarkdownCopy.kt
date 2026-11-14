package com.deepanjanxyz.notepad.backup

import android.content.Context
import com.deepanjanxyz.notepad.domain.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Markdown copy coming back in, as the screen sees it: an import and the
 * outcome of the last one.
 *
 * Export left for the Notes screen, where the notes being exported are the ones
 * on screen, so what is left here is the way back in. It runs on the scope it is
 * given and asks for the place to add the notes, so the view model holds a
 * reference to it rather than another stretch of file work and message building.
 */
class MarkdownCopy(
    private val context: Context,
    private val scope: CoroutineScope,
    private val addNotes: suspend (List<Note>) -> Unit
) {
    private val _status = MutableStateFlow<MarkdownTransfer.Outcome?>(null)

    /** What the last export or import did, for the screen to report. */
    val status: StateFlow<MarkdownTransfer.Outcome?> = _status.asStateFlow()

    /**
     * Adds every note in the Markdown file in Downloads as a new note.
     *
     * A note already held on every field is skipped by [addNotes], so importing
     * the same file twice does not double the library.
     */
    fun importNotes() {
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { MarkdownTransfer.read(context) }
            if (outcome.notes.isEmpty()) {
                _status.value = outcome
            } else {
                addNotes(outcome.notes)
            }
        }
    }

    /** Clears the outcome once the screen has shown it. */
    fun clear() {
        _status.value = null
    }
}
