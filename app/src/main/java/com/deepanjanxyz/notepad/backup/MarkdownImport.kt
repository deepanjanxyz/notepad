package com.deepanjanxyz.notepad.backup

import android.content.Context
import android.net.Uri
import com.deepanjanxyz.notepad.domain.model.Note
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** How many entries of a zip are looked at, so a wrong or huge archive cannot hang the app. */
private const val MAX_ZIP_ENTRIES = 500

/**
 * Bringing notes back in from a file the user picked.
 *
 * The file comes from the system picker, so it can be anywhere on the device, and
 * it is either a Markdown file or a zip of them. Only the file that was picked is
 * read: nothing is looked up in the app's own folder, which belongs to the JSON
 * backup.
 */
object MarkdownImport {

    /**
     * What the picker is asked for.
     *
     * `application/octet-stream` is in the list because some providers label a
     * `.md` file that way, and a file the picker hides cannot be chosen at all.
     */
    val PICKER_MIME_TYPES = arrayOf(
        "text/markdown",
        "text/plain",
        "application/zip",
        "application/octet-stream"
    )

    /** Reads the picked file, and returns the notes it holds. */
    fun read(context: Context, uri: Uri): MarkdownTransfer.Outcome {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) {
            return MarkdownTransfer.Outcome(message = "The file could not be read", error = true)
        }
        val notes = if (isZip(bytes)) notesFromZip(bytes) else notesFromMarkdown(bytes)
        if (notes.isEmpty()) {
            return MarkdownTransfer.Outcome(
                message = "That file held no notes to import",
                error = true
            )
        }
        return MarkdownTransfer.Outcome(message = "Imported ${notes.size} notes", notes = notes)
    }

    /** True when the file starts with the signature every zip begins with. */
    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == ZIP_SIGNATURE &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 0x03.toByte()

    /** The notes a single Markdown file holds. */
    fun notesFromMarkdown(bytes: ByteArray): List<Note> = parseMarkdownNotes(bytes.decodeToString())

    /**
     * The notes a zip holds, one file at a time.
     *
     * Only `.md` entries count; folders and anything else in the archive are
     * skipped, and the walk stops after [MAX_ZIP_ENTRIES] entries.
     */
    fun notesFromZip(bytes: ByteArray): List<Note> {
        val notes = mutableListOf<Note>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            var seen = 0
            while (entry != null && seen < MAX_ZIP_ENTRIES) {
                val isMarkdown = entry.name.endsWith(
                    BackupFileNames.MARKDOWN_SUFFIX,
                    ignoreCase = true
                )
                if (!entry.isDirectory && isMarkdown) {
                    notes += parseMarkdownNotes(zip.readBytes().decodeToString())
                }
                zip.closeEntry()
                entry = zip.nextEntry
                seen++
            }
        }
        return notes
    }

    private const val ZIP_SIGNATURE = 0x50.toByte()
}
