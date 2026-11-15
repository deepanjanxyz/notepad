package com.deepanjanxyz.notepad.backup

import android.content.Context
import android.net.Uri
import com.deepanjanxyz.notepad.domain.model.Note
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/** How many entries of a zip are looked at before the archive is refused. */
internal const val MAX_ZIP_ENTRIES = 500

/** How large one note inside a zip may be once it is uncompressed. */
internal const val MAX_ENTRY_BYTES = 4_000_000

/** How large all the notes of one zip may be together once uncompressed. */
internal const val MAX_TOTAL_BYTES = 20_000_000

/** How much is read from the archive at a time. */
private const val READ_CHUNK = 8192

/** The four bytes every zip begins with, read as text: "PK\u0003\u0004". */
private const val ZIP_SIGNATURE = "PK\u0003\u0004"

/**
 * Bringing notes back in from a file the user picked.
 *
 * The file comes from the system picker, so it can be anywhere on the device, and
 * it is either a Markdown file or a zip of them. Only the file that was picked is
 * read: nothing is looked up in the app's own folder, which belongs to the JSON
 * backup.
 *
 * A file that cannot be read is reported as an error rather than as a partial
 * import: half a library brought in silently is worse than being told it failed.
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
        return runCatching {
            if (isZip(bytes)) notesFromZip(bytes) else notesFromMarkdown(bytes)
        }.fold(
            onSuccess = { notes ->
                if (notes.isEmpty()) {
                    MarkdownTransfer.Outcome(
                        message = "That file held no notes to import",
                        error = true
                    )
                } else {
                    MarkdownTransfer.Outcome(
                        message = "Imported ${notes.size} notes",
                        notes = notes
                    )
                }
            },
            onFailure = { error ->
                MarkdownTransfer.Outcome(
                    message = "That file could not be read as notes: ${error.message}",
                    error = true
                )
            }
        )
    }

    /** True when the file starts with the signature every zip begins with. */
    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= ZIP_SIGNATURE.length &&
            ZIP_SIGNATURE.indices.all { index ->
                bytes[index] == ZIP_SIGNATURE[index].code.toByte()
            }

    /** The notes a single Markdown file holds. */
    fun notesFromMarkdown(bytes: ByteArray): List<Note> = parseMarkdownNotes(bytes.decodeToString())

    /**
     * The notes a zip holds, one file at a time.
     *
     * Only `.md` entries count; folders and anything else in the archive are
     * skipped. An archive holding more than [MAX_ZIP_ENTRIES] entries, or a note
     * larger than [MAX_ENTRY_BYTES], or more than [MAX_TOTAL_BYTES] in all, is
     * refused rather than imported in part, so a whole selection is never quietly
     * cut short. A corrupt archive throws, and [read] turns that into an error.
     */
    fun notesFromZip(bytes: ByteArray): List<Note> {
        val notes = mutableListOf<Note>()
        var total = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            var seen = 0
            while (entry != null) {
                seen++
                if (seen > MAX_ZIP_ENTRIES) {
                    throw IllegalStateException(
                        "The zip holds more than $MAX_ZIP_ENTRIES files"
                    )
                }
                val isMarkdown = entry.name.endsWith(
                    BackupFileNames.MARKDOWN_SUFFIX,
                    ignoreCase = true
                )
                if (!entry.isDirectory && isMarkdown) {
                    val text = readEntry(zip)
                    total += text.size
                    notes += parseMarkdownNotes(text.decodeToString())
                } else {
                    // An entry that is skipped is inflated all the same before the
                    // next one can be reached, so its bytes count against the same
                    // limit rather than being read for free.
                    total += readEntry(zip).size
                }
                if (total > MAX_TOTAL_BYTES) {
                    throw IllegalStateException("The zip holds more text than can be read")
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return notes
    }

    /**
     * One entry of the archive, read a chunk at a time.
     *
     * The size is counted as it is read rather than trusted from the entry's
     * header: a small compressed entry can unpack to something far larger, and the
     * limit has to hold whatever the header claims.
     */
    private fun readEntry(zip: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(READ_CHUNK)
        var total = 0
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_ENTRY_BYTES) {
                throw IllegalStateException("A note in the zip is larger than can be read")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
