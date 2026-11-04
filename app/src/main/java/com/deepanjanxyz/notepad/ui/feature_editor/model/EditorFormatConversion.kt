package com.deepanjanxyz.notepad.ui.feature_editor.model

import com.deepanjanxyz.notepad.domain.model.ChecklistItem
import com.deepanjanxyz.notepad.domain.model.FIRST_LIST_NUMBER
import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.domain.model.lineText

/**
 * Rewrites every line of the note into [format]'s kind.
 *
 * Switching to a lines format turns the words on each line into an item of that
 * kind. A blank line is spacing rather than an item: it stays as it is and ends
 * the numbered run above it. Switching back to [EditorFormat.PlainText] strips
 * the markers and keeps the words, so changing format never loses text.
 */
fun List<NoteSegment>.convertTo(format: EditorFormat): List<NoteSegment> {
    if (!format.isLinesMode) return map { segment -> NoteSegment.Text(segment.lineText()) }
    var nextNumber = FIRST_LIST_NUMBER
    return map { segment ->
        if (segment is NoteSegment.Text && segment.text.isBlank()) {
            nextNumber = FIRST_LIST_NUMBER
            segment
        } else {
            val number = nextNumber
            nextNumber = number + 1
            format.asSegment(segment.lineText(), number)
        }
    }
}

/** One line of [format]'s kind holding [text]. */
fun EditorFormat.asSegment(text: String, number: Int = FIRST_LIST_NUMBER): NoteSegment =
    when (this) {
        EditorFormat.PlainText -> NoteSegment.Text(text)
        EditorFormat.Checklist -> NoteSegment.Task(ChecklistItem(text = text))
        EditorFormat.BulletedList -> NoteSegment.Bullet(text = text)
        EditorFormat.NumberedList -> NoteSegment.Numbered(number = number, text = text)
    }
