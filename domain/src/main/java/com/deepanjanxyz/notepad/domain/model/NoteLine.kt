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
 * The digit run is capped so a pasted wall of digits is not scanned at all. The
 * cap is ten digits rather than nine because renumbering can carry a run past
 * 999,999,999, and a marker the writer can produce has to be one the reader
 * accepts back. The cap is only a fast guard: a ten-digit run that does not fit
 * an [Int] is rejected by [numberedLineNumber], not by the pattern.
 */
private val NUMBERED_LINE = Regex("^(\\d{1,10})\\. ")

/** True when the line is an unordered list item. */
fun String.isBulletLine(): Boolean = startsWith(BULLET_PREFIX)

/** The line's text with its bullet marker removed. */
fun String.bulletLineText(): String =
    if (isBulletLine()) substring(BULLET_PREFIX.length) else this

/**
 * The number the line starts with, or null when the line is not a numbered item.
 *
 * A digit run too large to fit an [Int] is not a number this model can hold, so
 * the line is not a numbered one: it stays text and round-trips exactly as it was
 * written, instead of being read as item 1 and rewritten on the next save.
 */
fun String.numberedLineNumber(): Int? =
    NUMBERED_LINE.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()

/** True when the line is a numbered list item, which means its number fits an [Int]. */
fun String.isNumberedLine(): Boolean = numberedLineNumber() != null

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
