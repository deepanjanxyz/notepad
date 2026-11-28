package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.RichLinkSpan
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.domain.model.RichTextDocument

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
    val doc = RichTextCodec.decode(note.content)
    val (normalizedText, normalizedLinks) = normalizeCrlfWithLinks(doc.text, doc.linkSpans)
    // Inside a fenced code block Markdown reads nothing as structure and honours
    // no escapes, so those lines are written exactly as the note holds them: a
    // `C:\Users` path must not come back as `C:\\Users` in another editor.
    var fence: Fence? = null
    var lineStart = 0
    val lines = normalizedText
        .split("\n")
        .map { line ->
            val lineEnd = lineStart + line.length
            val open = fence
            val result = when {
                open != null -> {
                    if (closesFence(line, open)) fence = null
                    line
                }
                else -> {
                    val opened = openingFence(line)
                    if (opened != null) {
                        fence = opened
                        escapedFenceOpener(line)
                    } else {
                        val lineLinks = normalizedLinks.mapNotNull { span ->
                            val s = maxOf(span.start, lineStart)
                            val e = minOf(span.end, lineEnd)
                            if (s < e) {
                                RichLinkSpan(s - lineStart, e - lineStart, span.url)
                            } else {
                                null
                            }
                        }
                        if (lineLinks.isEmpty()) {
                            escaped(line)
                        } else {
                            escapedWithLinks(line, lineLinks)
                        }
                    }
                }
            }
            lineStart = lineEnd + 1
            result
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

private fun normalizeCrlfWithLinks(
    text: String,
    links: List<RichLinkSpan>
): Pair<String, List<RichLinkSpan>> {
    if (!text.contains("\r\n")) return text to links
    val out = StringBuilder(text.length)
    val offsetMap = IntArray(text.length + 1)
    var i = 0
    while (i < text.length) {
        offsetMap[i] = out.length
        if (text[i] == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
            offsetMap[i + 1] = out.length
            out.append('\n')
            i += 2
            continue
        }
        out.append(text[i])
        i++
    }
    offsetMap[text.length] = out.length
    val remapped = links.mapNotNull { span ->
        val s = offsetMap[span.start.coerceIn(0, text.length)]
        val e = offsetMap[span.end.coerceIn(0, text.length)]
        if (s < e) span.copy(start = s, end = e) else null
    }
    return out.toString() to remapped
}

/** Escapes only backslashes on a fence opener so its info string stays intact without link escaping. */
private fun escapedFenceOpener(line: String): String = line.replace(ESCAPE, ESCAPE + ESCAPE)

/**
 * Keeps a line of a note from being read back as structure or rich-link markup.
 *
 * A line that looks like the separator between notes, or like a note heading,
 * would end the note early when the file is read again, so it gets a backslash in
 * front of it. The backslash is itself the escape character, so every backslash
 * the note already holds is doubled first, and any literal `[label](url)` syntax
 * is backslash-escaped so reimport keeps it as plain text.
 */
private fun escaped(line: String): String {
    val safe = escapePlainSlice(line)
    return if (isStructural(safe)) ESCAPE + safe else safe
}

private fun escapedWithLinks(line: String, links: List<RichLinkSpan>): String {
    val out = StringBuilder()
    var cursor = 0
    links.sortedBy { it.start }.forEach { link ->
        if (link.start > cursor) {
            out.append(escapePlainSlice(line.substring(cursor, link.start)))
        }
        val label = line.substring(link.start, link.end)
            .replace(ESCAPE, ESCAPE + ESCAPE)
            .replace("[", "\\[")
            .replace("]", "\\]")
        val safeUrl = link.url
            .replace("(", "%28")
            .replace(")", "%29")
        out.append('[').append(label).append("](").append(safeUrl).append(')')
        cursor = link.end
    }
    if (cursor < line.length) {
        out.append(escapePlainSlice(line.substring(cursor)))
    }
    val built = out.toString()
    return if (isStructural(built)) ESCAPE + built else built
}

/**
 * Doubles literal backslashes in a plain-text slice and escapes any literal
 * `[label](url)` constructs so they round-trip without turning into rich links.
 */
private fun escapePlainSlice(slice: String): String {
    val doubled = slice.replace(ESCAPE, ESCAPE + ESCAPE)
    if (!doubled.contains("](")) return doubled

    val out = StringBuilder(doubled.length)
    var plainStart = 0
    var i = 0
    while (i < doubled.length) {
        if (doubled[i] == '[' && hasEvenPrecedingBackslashes(doubled, i)) {
            val closeBracket = findClosingBracket(doubled, i + 1)
            if (closeBracket > i + 1 &&
                closeBracket + 1 < doubled.length &&
                doubled[closeBracket + 1] == '('
            ) {
                val closeParen = findClosingParen(doubled, closeBracket + 2)
                if (closeParen > closeBracket + 2) {
                    out.append(doubled, plainStart, i)
                    out.append("\\[")
                    out.append(doubled, i + 1, closeBracket)
                    out.append("\\]")
                    out.append(doubled, closeBracket + 1, closeParen + 1)
                    i = closeParen + 1
                    plainStart = i
                    continue
                }
            }
        }
        i++
    }
    if (plainStart == 0) return doubled
    if (plainStart < doubled.length) {
        out.append(doubled, plainStart, doubled.length)
    }
    return out.toString()
}

/** Puts back the backslashes [escapedFenceOpener] doubled. */
private fun unescapedBackslashes(line: String): String = line.replace(ESCAPE + ESCAPE, ESCAPE)

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

private enum class BodyLineKind { PLAIN, FENCE_OPENER, FENCE_BODY }

private data class ParsedBodyLine(val text: String, val kind: BodyLineKind)

/**
 * Stateful line parser kept separate from the public entry point so each
 * structural decision stays small and independently auditable.
 */
private class MarkdownNotesParser {
    private val notes = mutableListOf<Note>()
    private var title: String? = null
    private val body = mutableListOf<ParsedBodyLine>()
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
                if (title != null) body += ParsedBodyLine(line, BodyLineKind.FENCE_BODY)
            }
            line == FENCE_CLOSURE_MARKER && title != null -> removeSyntheticFence()
            line.startsWith(NOTE_HEADING) -> {
                collect()
                title = line.removePrefix(NOTE_HEADING).trim()
            }
            title == null -> Unit
            line.trim() == NOTE_BREAK -> collect()
            isMarkedAsContent(line) -> body += ParsedBodyLine(
                line.removePrefix(ESCAPE),
                BodyLineKind.PLAIN
            )
            else -> {
                val opened = openingFence(line)
                if (opened != null) {
                    fence = opened
                    body += ParsedBodyLine(line, BodyLineKind.FENCE_OPENER)
                } else {
                    body += ParsedBodyLine(line, BodyLineKind.PLAIN)
                }
            }
        }
    }

    private fun removeSyntheticFence() {
        if (body.lastOrNull()?.text?.let(::isFenceClosingLine) == true) {
            body.removeAt(body.lastIndex)
        }
    }

    private fun collect() {
        val heading = title ?: return
        val keptLines = body.dropWhile { it.text.isBlank() }
        val plainBuilder = StringBuilder()
        val linkSpans = mutableListOf<RichLinkSpan>()

        keptLines.forEachIndexed { index, entry ->
            val lineStart = plainBuilder.length
            when (entry.kind) {
                BodyLineKind.FENCE_BODY -> plainBuilder.append(entry.text)
                BodyLineKind.FENCE_OPENER -> plainBuilder.append(unescapedBackslashes(entry.text))
                BodyLineKind.PLAIN -> {
                    val (parsedText, lineLinks) = parseMarkdownLinksInLine(entry.text)
                    plainBuilder.append(parsedText)
                    lineLinks.forEach { link ->
                        linkSpans += link.copy(
                            start = lineStart + link.start,
                            end = lineStart + link.end
                        )
                    }
                }
            }
            if (index < keptLines.lastIndex) {
                plainBuilder.append('\n')
            }
        }

        val content = plainBuilder.toString().trimEnd()
        val encodedContent = if (linkSpans.isEmpty()) {
            content
        } else {
            val trimmedLinks = linkSpans.mapNotNull { span ->
                val e = minOf(span.end, content.length)
                if (span.start < e) span.copy(end = e) else null
            }
            RichTextCodec.encode(
                RichTextDocument(text = content, linkSpans = trimmedLinks)
            )
        }

        val note = Note(title = heading, content = encodedContent)
        if (note.isBackupWorthy()) notes += note
        title = null
        body.clear()
    }
}

private fun parseMarkdownLinksInLine(line: String): Pair<String, List<RichLinkSpan>> {
    if (!line.contains("](")) return unescapeMarkdownText(line) to emptyList()

    val out = StringBuilder()
    val links = mutableListOf<RichLinkSpan>()
    var plainStart = 0
    var i = 0
    while (i < line.length) {
        if (line[i] == '[' && hasEvenPrecedingBackslashes(line, i)) {
            val closeBracket = findClosingBracket(line, i + 1)
            if (closeBracket > i + 1 &&
                closeBracket + 1 < line.length &&
                line[closeBracket + 1] == '('
            ) {
                val closeParen = findClosingParen(line, closeBracket + 2)
                if (closeParen > closeBracket + 2) {
                    val rawLabel = line.substring(i + 1, closeBracket)
                    val rawUrl = line.substring(closeBracket + 2, closeParen)
                    val safeUrl = RichTextCodec.sanitizeLinkUrl(rawUrl)
                    val cleanLabel = unescapeMarkdownText(rawLabel)
                    if (safeUrl != null && cleanLabel.isNotEmpty()) {
                        if (i > plainStart) {
                            out.append(unescapeMarkdownText(line.substring(plainStart, i)))
                        }
                        val linkStart = out.length
                        out.append(cleanLabel)
                        links += RichLinkSpan(linkStart, out.length, safeUrl)
                        i = closeParen + 1
                        plainStart = i
                        continue
                    }
                }
            }
        }
        i++
    }
    if (plainStart < line.length) {
        out.append(unescapeMarkdownText(line.substring(plainStart)))
    }
    return out.toString() to links
}

private fun findClosingBracket(line: String, startIndex: Int): Int {
    var idx = startIndex
    while (idx < line.length) {
        if (hasEvenPrecedingBackslashes(line, idx)) {
            when (line[idx]) {
                ']' -> return idx
                '[' -> return -1
            }
        }
        idx++
    }
    return -1
}

private fun findClosingParen(line: String, startIndex: Int): Int {
    var depth = 1
    var idx = startIndex
    while (idx < line.length) {
        when (line[idx]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return idx
            }
            ' ', '\t', '\r', '\n' -> return -1
        }
        idx++
    }
    return -1
}

private fun hasEvenPrecedingBackslashes(s: String, idx: Int): Boolean {
    var backslashes = 0
    var p = idx - 1
    while (p >= 0 && s[p] == '\\') {
        backslashes++
        p--
    }
    return backslashes % 2 == 0
}

private fun unescapeMarkdownText(rawText: String): String {
    if (!rawText.contains('\\')) return rawText
    val out = StringBuilder(rawText.length)
    var i = 0
    while (i < rawText.length) {
        val c = rawText[i]
        if (c == '\\' && i + 1 < rawText.length) {
            val next = rawText[i + 1]
            if (next == '\\' || next == '[' || next == ']') {
                out.append(next)
                i += 2
                continue
            }
        }
        out.append(c)
        i++
    }
    return out.toString()
}
