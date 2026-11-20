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
private const val FENCE_CLOSURE_MARKER = "<!-- elite-memo-pro-fence-closure -->"

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
    val lines = note.content
        .replace("\r\n", "\n")
        .split("\n")
        .map { line ->
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
        .toMutableList()
    // A note that ends inside a fence gets the fence closed here. Left open, the
    // block would run past the end of the note and swallow the next note's heading
    // when the file is read back, merging two notes into one.
    fence?.let { open ->
        lines += open.marker.toString().repeat(open.length)
        // The importer removes this marker and synthetic fence so reimport
        // preserves the original note content.
        lines += FENCE_CLOSURE_MARKER
    }
    val body = lines.joinToString("\n").trim('\n').trimEnd()
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
    val marker = trimmed.firstOrNull() ?: return null
    val run = if (marker == '`' || marker == '~') trimmed.takeWhile { it == marker }.length else 0
    val opens = (marker == '`' || marker == '~') &&
        run >= FENCE_MIN &&
        line.length - trimmed.length <= FENCE_INDENT_MAX &&
        (marker != '`' || !trimmed.substring(run).contains('`'))
    return if (opens) Fence(marker, run) else null
}

/**
 * True when [line] closes [fence]: the same character as the opening run, at
 * least as long, and nothing else on the line.
 */
private fun closesFence(line: String, fence: Fence): Boolean {
    val trimmed = line.trim()
    val indent = line.length - line.trimStart().length
    return indent <= FENCE_INDENT_MAX &&
        trimmed.length >= fence.length &&
        trimmed.all { it == fence.marker }
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

/** True when [line] is a standalone closing fence, regardless of marker. */
private fun isFenceClosingLine(line: String): Boolean {
    val trimmed = line.trim()
    val indent = line.length - line.trimStart().length
    val marker = trimmed.firstOrNull()
    return indent <= FENCE_INDENT_MAX &&
        (marker == '`' || marker == '~') &&
        trimmed.length >= FENCE_MIN &&
        trimmed.all { it == marker }
}

/** True when [line] would be read as the separator or a note heading. */
private fun isStructural(line: String): Boolean =
    line.trim() == NOTE_BREAK ||
        line.trimStart().startsWith(NOTE_HEADING) ||
        line == FENCE_CLOSURE_MARKER

/** True when [line] carries the mark that says it is a line of the note. */
private fun isMarkedAsContent(line: String): Boolean =
    line.startsWith(ESCAPE) && isStructural(line.removePrefix(ESCAPE))

/**
 * The notes a Markdown file holds, ready to be added as new notes.
 *
 * Anything before the first note heading is treated as the file's own header and
 * skipped, so a file this app did not write imports as long as it marks its notes
 * with "## " headings. Every note comes back with a title and text only; the
 * fields the file does not carry keep their defaults.
 */
fun parseMarkdownNotes(text: String): List<Note> = MarkdownNotesParser().parse(text)

/**
 * Stateful line parser kept separate from the public entry point so each
 * structural decision stays small and independently auditable.
 */
private class MarkdownNotesParser {
    private val notes = mutableListOf<Note>()
    private var title: String? = null
    private val body = mutableListOf<String>()
    private var fence: Fence? = null

    fun parse(text: String): List<Note> {
        text.replace("\r\n", "\n").split("\n").forEach(::appendLine)
        collect()
        return notes
    }

    private fun appendLine(line: String) {
        val open = fence
        when {
            open != null -> {
                if (closesFence(line, open)) fence = null
                if (title != null) body += line
            }
            line == FENCE_CLOSURE_MARKER && title != null -> removeSyntheticFence()
            line.startsWith(NOTE_HEADING) -> {
                collect()
                title = line.removePrefix(NOTE_HEADING).trim()
            }
            title == null -> Unit
            line.trim() == NOTE_BREAK -> collect()
            isMarkedAsContent(line) -> body += unescaped(line.removePrefix(ESCAPE))
            else -> {
                openingFence(line)?.let { fence = it }
                body += unescaped(line)
            }
        }
    }

    private fun removeSyntheticFence() {
        if (body.lastOrNull()?.let(::isFenceClosingLine) == true) {
            body.removeAt(body.lastIndex)
        }
    }

    private fun collect() {
        val heading = title ?: return
        val content = body.dropWhile { it.isBlank() }.joinToString("\n").trimEnd()
        val note = Note(title = heading, content = content)
        if (note.isBackupWorthy()) notes += note
        title = null
        body.clear()
    }
}
