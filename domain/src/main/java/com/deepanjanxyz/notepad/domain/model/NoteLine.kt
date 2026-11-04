package com.deepanjanxyz.notepad.domain.model

/**
 * The marker an unordered list line starts with.
 *
 * A note body is one plain string, so a list has to survive as text: a line that
 * opens with this marker is a bullet, exactly as `[ ] ` and `[x] ` mark a
 * checklist line. Keeping lists in the text itself is what lets search, share,
 * backup and the widget keep working without knowing that lists exist.
 */
const val BULLET_PREFIX = "- "

/** Sits between a numbered line's number and its text, as in "3. Buy milk". */
const val NUMBERED_SEPARATOR = ". "

/**
 * A numbered line's marker: digits, a full stop and a space.
 *
 * The digit run is capped so a pasted wall of digits cannot overflow the Int the
 * number is read into.
 */
private val NUMBERED_LINE = Regex("^(\\d{1,9})\\. ")

/** True when the line is an unordered list item. */
fun String.isBulletLine(): Boolean = startsWith(BULLET_PREFIX)

/** The line's text with its bullet marker removed. */
fun String.bulletLineText(): String =
    if (isBulletLine()) substring(BULLET_PREFIX.length) else this

/** True when the line is a numbered list item. */
fun String.isNumberedLine(): Boolean = NUMBERED_LINE.containsMatchIn(this)

/** The number the line starts with, or null when the line is not a numbered item. */
fun String.numberedLineNumber(): Int? =
    NUMBERED_LINE.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()

/** The line's text with its number and separator removed. */
fun String.numberedLineText(): String =
    NUMBERED_LINE.find(this)?.let { marker -> substring(marker.value.length) } ?: this

/** True when the note contains at least one bullet line. */
fun String.containsBulletLine(): Boolean = split("\n").any { line -> line.isBulletLine() }

/** True when the note contains at least one numbered line. */
fun String.containsNumberedLine(): Boolean = split("\n").any { line -> line.isNumberedLine() }

/** True when the note contains at least one bullet or numbered line. */
fun String.containsListLine(): Boolean = containsBulletLine() || containsNumberedLine()

/** True when the note carries any structured line - a task, a bullet or a number. */
fun String.containsStructuredLine(): Boolean = containsChecklistLine() || containsListLine()
