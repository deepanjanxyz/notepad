package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note

/**
 * What a Markdown export or import did.
 *
 * Exporting lives in [MarkdownExport] and importing in [MarkdownImport]; this is
 * the one shape they both report their result in.
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
}
