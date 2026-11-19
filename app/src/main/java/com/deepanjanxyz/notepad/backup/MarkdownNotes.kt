package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note

/**
 * Name of the Markdown file the export writes and the import reads.
 *
 * It sits beside the JSON backup in the folder the app already writes to and
 * reads from, so there is one place to look for both.
 */
const val MARKDOWN_FILE_NAME = "elite-memo-notes.md"

/**
 * The type a Markdown file is stored as.
 *
 * The folder and the media store both name a new file after the type they are
 * given, so a Markdown file has to be stored as Markdown: handed over as JSON it
 * comes back called `elite-memo-notes.md.json`, and the import then finds nothing
 * under the name it looks for.
 */
const val MARKDOWN_MIME_TYPE = "text/markdown"

/** The first line of the exported file, and the line that names each note. */
private const val FILE_HEADING = "# Elite Memo Pro notes"
private const val NOTE_HEADING = "## "

/** A line of its own between two notes, and the mark that keeps it out of a body. */
private const val NOTE_BREAK = "---"
private const val ESCAPE = "\\"

/** The shortest run of a fence character that CommonMark reads as a fence. */
private const val FENCE_MIN = 3

/** How far a fence may be indented before CommonMark stops reading it as one. */
private const val FENCE_INDENT_MAX = 3

/**
 * Writes every note into one Markdown file, a heading per note.
 *
 * The file is an interchange format, not a backup: it carries each note's title
 * and text so the notes can be read in any editor or dropped into another app.
 * Colour, labels, pinning and reminders are not in it - the JSON backup stays
 * the copy that restores a library exactly as it was.
 *
 * A note that holds nothing is left out, matching what the JSON backup stores.
 */
fun buildMarkdownNotes(notes: List<Note>): String {
    val sections = notes.filter { it.isBackupWorthy() }.map { markdownSection(it) }
    return (listOf(FILE_HEADING) + sections).joinToString("\n\n", postfix = "\n")
}

/** One note as a heading line followed by its text. */
private fun markdownSection(note: Note): String {
    // A title has to fit on the heading line, so a break inside it becomes a space.
    val title = note.title.replace("\r\n", "\n").replace("\n", " ").trim()
    // Inside a fenced code block Markdown reads nothing as structure and honours
    // no escapes, so those lines are written exactly as the note holds them: a
    // `C:\Users` path must not come back as `C:\\Users` in another editor.
    var fence: Fence? = null
    val body = note.content
        .replace("\r\n", "\n")
        .split("\n")
        .joinToString("\n") { line ->
            val open = fence
            when {
                open != null -> {
                    if (closesFence(line, open)) fence = null
                    line
                }
                else -> {
                    openingFence(line)?.let { opened -> fence = opened }
                    escaped(line)
                }
            }
        }
        .trim('\n')
        .trimEnd()
    return if (body.isEmpty()) NOTE_HEADING + title else NOTE_HEADING + title + "\n\n" + body
}

/** A fenced code block that is open: its marker and the length of the opening run. */
private data class Fence(val marker: Char, val length: Int)

/**
 * The fence [line] opens, or null when it opens none.
 *
 * CommonMark reads a fence as three or more backticks or tildes at the start of a
 * line, indented by no more than three spaces, and a backtick fence's info string
 * may not itself hold a backtick - which is what keeps an inline `code` run from
 * opening a block.
 */
private fun openingFence(line: String): Fence? {
    val trimmed = line.trimStart()
    if (line.length - trimmed.length > FENCE_INDENT_MAX) return null
    val marker = trimmed.firstOrNull() ?: return null
    if (marker != '`' && marker != '~') return null
    val run = trimmed.takeWhile { it == marker }.length
    if (run < FENCE_MIN) return null
    if (marker == '`' && trimmed.substring(run).contains('`')) return null
    return Fence(marker, run)
}

/**
 * True when [line] closes [fence]: the same character as the opening run, at
 * least as long, and nothing else on the line.
 */
private fun closesFence(line: String, fence: Fence): Boolean {
    val trimmed = line.trim()
    return trimmed.length >= fence.length && trimmed.all { it == fence.marker }
}

/**
 * Keeps a line of a note from being read back as structure.
 *
 * A line that looks like the separator between notes, or like a note heading,
 * would end the note early when the file is read again, so it gets a backslash in
 * front of it. The backslash is itself the escape character, so every backslash
 * the note already holds is doubled first: without that a note holding a literal
 * "\---" would come back as a plain separator line, and the text would quietly
 * change. Both escapes render as the plain characters in Markdown.
 */
private fun escaped(line: String): String {
    val doubled = line.replace(ESCAPE, ESCAPE + ESCAPE)
    return if (isStructural(doubled)) ESCAPE + doubled else doubled
}

/** Puts back the backslashes [escaped] doubled. */
private fun unescaped(line: String): String = line.replace(ESCAPE + ESCAPE, ESCAPE)

/** True when [line] would be read as the separator or a note heading. */
private fun isStructural(line: String): Boolean =
    line.trim() == NOTE_BREAK || line.trimStart().startsWith(NOTE_HEADING)

/**
 * The notes a Markdown file holds, ready to be added as new notes.
 *
 * Anything before the first note heading is treated as the file's own header and
 * skipped, so a file this app did not write imports as long as it marks its notes
 * with "## " headings. Every note comes back with a title and text only; the
 * fields the file does not carry keep their defaults.
 */
fun parseMarkdownNotes(text: String): List<Note> {
    val notes = mutableListOf<Note>()
    var title: String? = null
    var body = mutableListOf<String>()

    fun collect() {
        val heading = title ?: return
        val content = body.dropWhile { it.isBlank() }.joinToString("\n").trimEnd()
        val note = Note(title = heading, content = content)
        if (note.isBackupWorthy()) notes += note
        title = null
        body = mutableListOf()
    }

    var fence: Fence? = null

    text.replace("\r\n", "\n").split("\n").forEach { line ->
        val escapedLine = line.removePrefix(ESCAPE)
        val open = fence
        when {
            // Inside a fenced block nothing is structure and nothing is escaped:
            // the export wrote those lines literally, so they are read back the
            // same way, a separator or a note heading among them included.
            open != null -> {
                if (closesFence(line, open)) fence = null
                if (title != null) body += line
            }
            line.startsWith(NOTE_HEADING) -> {
                collect()
                title = line.removePrefix(NOTE_HEADING).trim()
            }
            // Still inside the file's header: nothing to attach this line to yet.
            title == null -> Unit
            line.trim() == NOTE_BREAK -> collect()
            // A line the export marked is a line of the note, whatever it looks like.
            line.startsWith(ESCAPE) && isStructural(escapedLine) ->
                body += unescaped(escapedLine)
            else -> {
                openingFence(line)?.let { opened -> fence = opened }
                body += unescaped(line)
            }
        }
    }
    collect()
    return notes
}
