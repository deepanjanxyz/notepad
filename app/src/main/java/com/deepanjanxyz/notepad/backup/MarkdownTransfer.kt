package com.deepanjanxyz.notepad.backup

import android.content.Context
import com.deepanjanxyz.notepad.domain.model.Note

/**
 * The Markdown copy of the notes coming back in.
 *
 * Exporting moved to [MarkdownExport], which the Notes screen calls with the notes
 * the user picked; reading the file back lives here, beside the format it uses.
 */
object MarkdownTransfer {

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

    /** The notes the Markdown file in Downloads holds, or the reason it holds none. */
    fun read(context: Context): Outcome {
        // The name can come back numbered when it is already taken, so the newest
        // Markdown file in the folder is read when the exact name finds nothing.
        val text = DownloadsBackup.read(context, MARKDOWN_FILE_NAME)
            ?: DownloadsBackup.readNewestWithSuffix(context, BackupFileNames.MARKDOWN_SUFFIX)
        if (text == null) {
            return Outcome(
                message = "No Markdown file was found at " +
                    DownloadsBackup.locationLabel(MARKDOWN_FILE_NAME),
                error = true
            )
        }
        val notes = parseMarkdownNotes(text)
        return if (notes.isEmpty()) {
            Outcome(message = "The Markdown file held no notes", error = true)
        } else {
            Outcome(message = "Imported ${notes.size} notes from Markdown", notes = notes)
        }
    }
}
