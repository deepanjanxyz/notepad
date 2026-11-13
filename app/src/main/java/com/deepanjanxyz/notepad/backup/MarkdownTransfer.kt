package com.deepanjanxyz.notepad.backup

import android.content.Context
import android.util.Log
import com.deepanjanxyz.notepad.domain.model.Note

/**
 * The Markdown copy of the notes: writing it and reading it back.
 *
 * The file work lives here, beside the format it uses, so both directions are
 * described in one place and the view model only has to report what happened.
 */
object MarkdownTransfer {

    private const val TAG = "MarkdownTransfer"

    /**
     * What a transfer did, worded so it can be shown as it is.
     *
     * A read that found notes carries them in [notes]; a read that found none
     * carries the reason instead and is marked as an error.
     */
    data class Outcome(
        val message: String,
        val error: Boolean = false,
        val notes: List<Note> = emptyList()
    )

    /** Writes [notes] to the Markdown file in Downloads. */
    fun write(context: Context, notes: List<Note>): Outcome {
        val worth = notes.filter { it.isBackupWorthy() }
        if (worth.isEmpty()) {
            return Outcome(message = "There are no notes to export yet", error = true)
        }
        return runCatching {
            DownloadsBackup.write(context, buildMarkdownNotes(worth), MARKDOWN_FILE_NAME)
        }.fold(
            onSuccess = {
                Outcome(
                    message = "Exported ${worth.size} notes to " +
                        DownloadsBackup.locationLabel(MARKDOWN_FILE_NAME)
                )
            },
            onFailure = { error ->
                Log.w(TAG, "The Markdown export failed", error)
                Outcome(
                    message = "The Markdown file could not be written: ${error.message}",
                    error = true
                )
            }
        )
    }

    /** The notes the Markdown file in Downloads holds, or the reason it holds none. */
    fun read(context: Context): Outcome {
        val text = DownloadsBackup.read(context, MARKDOWN_FILE_NAME)
            ?: return Outcome(
                message = "No Markdown file was found at " +
                    DownloadsBackup.locationLabel(MARKDOWN_FILE_NAME),
                error = true
            )
        val notes = parseMarkdownNotes(text)
        if (notes.isEmpty()) {
            return Outcome(message = "The Markdown file held no notes", error = true)
        }
        return Outcome(message = "Imported ${notes.size} notes from Markdown", notes = notes)
    }
}
