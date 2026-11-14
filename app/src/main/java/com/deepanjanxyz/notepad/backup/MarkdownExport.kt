package com.deepanjanxyz.notepad.backup

import android.content.Context
import android.util.Log
import com.deepanjanxyz.notepad.domain.model.Note
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The name a note without a title is exported under. */
private const val UNTITLED_NAME = "Elite Memo Pro"

/** The file several notes are combined into, and the zip several notes are bundled as. */
private const val COMBINED_FILE_NAME = "Elite Memo Pro notes.md"
private const val ZIP_FILE_NAME = "Elite Memo Pro notes.zip"

private const val ZIP_MIME_TYPE = "application/zip"
private const val TAG = "MarkdownExport"

/** A title longer than this is shortened, because a file name has to stay readable. */
private const val MAX_NAME_LENGTH = 60

/** Characters a file name cannot hold, and the space they are replaced with. */
private val ILLEGAL_IN_NAME = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

/**
 * Exporting notes as Markdown, one note or many.
 *
 * One note goes to its own file, named after the note. Several notes either go
 * into one combined file, or into a zip holding one file per note, which is what
 * keeps a large library from becoming a folder of hundreds of loose files.
 *
 * The naming and the zip are plain Kotlin so they can be tested without a device;
 * only the writing touches storage.
 */
object MarkdownExport {

    /**
     * The file name a note is exported under.
     *
     * The title is used as it is, with the characters a file name cannot hold
     * replaced by spaces, runs of spaces collapsed and a very long title cut
     * short. A note with no title, or one whose title is nothing but those
     * characters, is exported as [UNTITLED_NAME].
     */
    fun fileNameFor(title: String): String {
        val cleaned = title
            .map { character -> if (character in ILLEGAL_IN_NAME || character.isISOControl()) ' ' else character }
            .joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(MAX_NAME_LENGTH)
            .trim()
        val base = cleaned.ifEmpty { UNTITLED_NAME }
        return base + BackupFileNames.MARKDOWN_SUFFIX
    }

    /**
     * A file name for each note, with the repeats told apart.
     *
     * Two notes can share a title, and a note can share one with [UNTITLED_NAME],
     * so a name already taken is numbered the way the file system numbers one:
     * `Trip.md`, `Trip (1).md`, and so on.
     */
    fun fileNamesFor(notes: List<Note>): List<String> {
        val taken = mutableSetOf<String>()
        return notes.map { note ->
            val base = fileNameFor(note.title).removeSuffix(BackupFileNames.MARKDOWN_SUFFIX)
            var name = base
            var copy = 0
            while (!taken.add(name.lowercase())) {
                copy++
                name = "$base ($copy)"
            }
            name + BackupFileNames.MARKDOWN_SUFFIX
        }
    }

    /** Every note as one Markdown file, the way the Settings export writes it. */
    fun buildCombined(notes: List<Note>): String = buildMarkdownNotes(notes)

    /**
     * Every note in a zip, one Markdown file per note.
     *
     * The entries are written in the order the notes are given, so the archive
     * reads the way the selection looked.
     */
    fun buildZip(notes: List<Note>): ByteArray {
        val worth = notes.filter { it.isBackupWorthy() }
        val names = fileNamesFor(worth)
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            worth.forEachIndexed { index, note ->
                zip.putNextEntry(ZipEntry(names[index]))
                zip.write(buildMarkdownNotes(listOf(note)).toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    /** Writes one note to its own file in Downloads. */
    fun exportOne(context: Context, note: Note): MarkdownTransfer.Outcome {
        if (!note.isBackupWorthy()) {
            return MarkdownTransfer.Outcome(message = "There is nothing in this note to export", error = true)
        }
        val name = fileNameFor(note.title)
        return runCatching {
            DownloadsBackup.write(
                context,
                buildMarkdownNotes(listOf(note)),
                name,
                MARKDOWN_MIME_TYPE
            )
        }.fold(
            onSuccess = { written(name, 1) },
            onFailure = { failure("The note could not be exported", it) }
        )
    }

    /** Writes the selected notes into one Markdown file. */
    fun exportCombined(context: Context, notes: List<Note>): MarkdownTransfer.Outcome {
        val worth = notes.filter { it.isBackupWorthy() }
        if (worth.isEmpty()) {
            return MarkdownTransfer.Outcome(message = "There are no notes to export yet", error = true)
        }
        return runCatching {
            DownloadsBackup.write(
                context,
                buildCombined(worth),
                COMBINED_FILE_NAME,
                MARKDOWN_MIME_TYPE
            )
        }.fold(
            onSuccess = { written(COMBINED_FILE_NAME, worth.size) },
            onFailure = { failure("The notes could not be exported", it) }
        )
    }

    /** Writes the selected notes into a zip holding one Markdown file per note. */
    fun exportZip(context: Context, notes: List<Note>): MarkdownTransfer.Outcome {
        val worth = notes.filter { it.isBackupWorthy() }
        if (worth.isEmpty()) {
            return MarkdownTransfer.Outcome(message = "There are no notes to export yet", error = true)
        }
        return runCatching {
            DownloadsBackup.writeBytes(context, buildZip(worth), ZIP_FILE_NAME, ZIP_MIME_TYPE)
        }.fold(
            onSuccess = { written(ZIP_FILE_NAME, worth.size) },
            onFailure = { failure("The notes could not be exported", it) }
        )
    }

    private fun written(name: String, count: Int): MarkdownTransfer.Outcome =
        MarkdownTransfer.Outcome(
            message = "Exported $count notes to " + DownloadsBackup.locationLabel(name)
        )

    private fun failure(what: String, error: Throwable): MarkdownTransfer.Outcome {
        Log.w(TAG, "$what: ${error.message}")
        return MarkdownTransfer.Outcome(
            message = "$what: ${error.message}",
            error = true
        )
    }
}
