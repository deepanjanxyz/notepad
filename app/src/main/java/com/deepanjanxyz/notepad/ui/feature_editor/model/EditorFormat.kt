package com.deepanjanxyz.notepad.ui.feature_editor.model

import com.deepanjanxyz.notepad.domain.model.containsBulletLine
import com.deepanjanxyz.notepad.domain.model.containsChecklistLine
import com.deepanjanxyz.notepad.domain.model.containsNumberedLine

/**
 * The format the note body is edited in.
 *
 * [PlainText] is the free-form body. The other three turn the editor into a
 * line-by-line editor and say which kind of line a new item gets; a note may
 * still hold all three kinds at once, and the editor shows each as what it is.
 */
enum class EditorFormat(val label: String, val testTag: String) {
    PlainText("Plain text", "format_plain_text"),
    Checklist("Checklist", "format_checklist"),
    BulletedList("Bulleted list", "format_bulleted_list"),
    NumberedList("Numbered list", "format_numbered_list");

    /** True for the formats that edit the body line by line. */
    val isLinesMode: Boolean
        get() = this != PlainText

    companion object {
        /**
         * The format a note opens in, judged by the markers it already carries.
         *
         * A checklist wins over a list, because ticking an item is the more
         * specific intent and the editor shows bullets and numbers either way.
         */
        fun of(content: String): EditorFormat = when {
            content.containsChecklistLine() -> Checklist
            content.containsBulletLine() -> BulletedList
            content.containsNumberedLine() -> NumberedList
            else -> PlainText
        }
    }
}
