package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note

/**
 * Name of the Markdown file the export writes and the import reads.
 *
 * It sits beside the JSON backup in the folder the app already writes to and
 * reads from, so there is one place to look for both.
 */
const val MARKDOWN_FILE_NAME = "elite-memo-notes.md"

/** The first line of the exported file, and the line that names each note. */
private const val FILE_HEADING = "# Elite Memo Pro notes"
private const val NOTE_HEADING = "## "

/** A line of its own between two notes, and the mark that keeps it out of a body. */
private const val NOTE_BREAK = "---"
private const val ESCAPE = "\\"

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
    val body = note.content
        .replace("\r\n", "\n")
        .split("\n")
        .joinToString("\n") { line -> escaped(line) }
        .trim('\n')
        .trimEnd()
    return if (body.isEmpty()) NOTE_HEADING + title else NOTE_HEADING + title + "\n\n" + body
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

    text.replace("\r\n", "\n").split("\n").forEach { line ->
        val escapedLine = line.removePrefix(ESCAPE)
        when {
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
            else -> body += unescaped(line)
        }
    }
    collect()
    return notes
}
