package com.deepanjanxyz.notepad.domain.model

/** The number the first item of a numbered list starts on. */
const val FIRST_LIST_NUMBER = 1

/**
 * One line of a note, as the editor and the note card see it.
 *
 * A [Text] line is kept exactly as it was written - ordinary prose, a heading or
 * a blank line - and is never reformatted. [Task], [Bullet] and [Numbered] lines
 * each carry their own marker, and only the line the user touched is ever
 * rewritten. Modelling the body as segments is what stops the editor from
 * reformatting the rest of the note when a single line changes.
 */
sealed interface NoteSegment {
    data class Text(val text: String) : NoteSegment

    data class Task(val item: ChecklistItem) : NoteSegment

    data class Bullet(val text: String) : NoteSegment

    data class Numbered(val number: Int, val text: String) : NoteSegment
}

/** The line's own words, without its marker. */
fun NoteSegment.lineText(): String = when (this) {
    is NoteSegment.Text -> text
    is NoteSegment.Task -> item.text
    is NoteSegment.Bullet -> text
    is NoteSegment.Numbered -> text
}

/** True when the line carries no words at all. */
fun NoteSegment.hasNoText(): Boolean = lineText().isBlank()

/** Splits a note into one segment per line. */
fun parseNoteSegments(raw: String): List<NoteSegment> {
    if (raw.isEmpty()) return emptyList()
    return raw.split("\n").map { line -> parseNoteLine(line) }
}

/**
 * Reads one line as the kind of line its marker says it is.
 *
 * A task is recognised first, so a line that is somehow both keeps its checkbox,
 * and a bullet needs its trailing space, so prose such as "-5 degrees" stays prose.
 */
private fun parseNoteLine(line: String): NoteSegment = when {
    line.startsWith(CHECKLIST_CHECKED_PREFIX, ignoreCase = true) ->
        NoteSegment.Task(ChecklistItem(text = line.checklistLineText(), isChecked = true))

    line.startsWith(CHECKLIST_UNCHECKED_PREFIX) ->
        NoteSegment.Task(ChecklistItem(text = line.checklistLineText(), isChecked = false))

    line.isNumberedLine() ->
        NoteSegment.Numbered(
            number = line.numberedLineNumber() ?: FIRST_LIST_NUMBER,
            text = line.numberedLineText()
        )

    line.isBulletLine() -> NoteSegment.Bullet(text = line.bulletLineText())
    else -> NoteSegment.Text(line)
}

/**
 * Rebuilds the note from its segments, one line each, markers included.
 *
 * Text lines are written back exactly as they were, so a note round-trips through
 * the list editor unchanged except for the line the user actually touched.
 */
fun serializeNoteSegments(segments: List<NoteSegment>): String =
    segments.joinToString("\n") { segment -> serializeNoteSegment(segment) }

private fun serializeNoteSegment(segment: NoteSegment): String = when (segment) {
    is NoteSegment.Text -> segment.text
    is NoteSegment.Task -> checklistPrefix(segment.item.isChecked) + segment.item.text
    is NoteSegment.Bullet -> BULLET_PREFIX + segment.text
    is NoteSegment.Numbered -> segment.number.toString() + NUMBERED_SEPARATOR + segment.text
}

private fun checklistPrefix(isChecked: Boolean): String =
    if (isChecked) CHECKLIST_CHECKED_PREFIX else CHECKLIST_UNCHECKED_PREFIX

/**
 * Numbers each run of consecutive numbered lines from the number the run starts
 * on, so inserting or removing an item keeps the list in step without rewriting
 * the number a list deliberately begins with. Any other line ends the run.
 */
fun renumberNumberedSegments(segments: List<NoteSegment>): List<NoteSegment> {
    var nextNumber: Int? = null
    return segments.map { segment ->
        if (segment !is NoteSegment.Numbered) {
            nextNumber = null
            segment
        } else {
            val number = nextNumber ?: segment.number
            // Stopping at the largest Int keeps the counter from wrapping negative,
            // which would write a marker no reader accepts back as a number.
            nextNumber = if (number == Int.MAX_VALUE) Int.MAX_VALUE else number + 1
            segment.copy(number = number)
        }
    }
}
