package com.deepanjanxyz.notepad.domain.model

import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

/** Checklist line prefixes, exactly as the editor writes them. */
const val CHECKLIST_UNCHECKED_PREFIX = "[ ] "
const val CHECKLIST_CHECKED_PREFIX = "[x] "

/**
 * Line-level helpers for callers that must not reformat a note - the widget
 * toggles one line and leaves every other line, including ordinary text and
 * blank lines, byte for byte as it was.
 */
fun String.isChecklistLine(): Boolean =
    startsWith(CHECKLIST_UNCHECKED_PREFIX) ||
        startsWith(CHECKLIST_CHECKED_PREFIX, ignoreCase = true)

fun String.isChecklistLineChecked(): Boolean =
    startsWith(CHECKLIST_CHECKED_PREFIX, ignoreCase = true)

/** The line's text with its checkbox prefix removed. */
fun String.checklistLineText(): String = when {
    startsWith(CHECKLIST_UNCHECKED_PREFIX) -> substring(CHECKLIST_UNCHECKED_PREFIX.length)
    startsWith(CHECKLIST_CHECKED_PREFIX, ignoreCase = true) ->
        substring(CHECKLIST_CHECKED_PREFIX.length)
    else -> this
}

/** The same line with its checkbox flipped, or null when it is not a checklist line. */
fun String.toggleChecklistLine(): String? = when {
    startsWith(CHECKLIST_UNCHECKED_PREFIX) ->
        CHECKLIST_CHECKED_PREFIX + substring(CHECKLIST_UNCHECKED_PREFIX.length)
    startsWith(CHECKLIST_CHECKED_PREFIX, ignoreCase = true) ->
        CHECKLIST_UNCHECKED_PREFIX + substring(CHECKLIST_CHECKED_PREFIX.length)
    else -> null
}

/** True when the note already contains at least one checklist line. */
fun String.containsChecklistLine(): Boolean =
    split("\n").any { line -> line.isChecklistLine() }
