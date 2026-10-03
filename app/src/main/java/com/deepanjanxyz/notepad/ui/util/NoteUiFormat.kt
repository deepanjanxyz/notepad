package com.deepanjanxyz.notepad.ui.util

import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.theme.NoteColorNames
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions

/** Which sentence the results sub-header should render. */
enum class FilterSummaryKind { ALL, QUERY, QUERY_IN_TAG, TAG, COLOR }

data class FilterSummary(
    val kind: FilterSummaryKind,
    val query: String = "",
    val tag: String? = null
)

/**
 * Small, dependency-free presentation helpers.
 *
 * They live outside the composables so they can be covered by fast JVM unit
 * tests instead of instrumented ones, and so the same rules are shared by every
 * screen (the colour-name list was previously declared but never used, which is
 * why the colour filter row had no accessible label at all).
 */
object NoteUiFormat {

    private val WHITESPACE = Regex("\\s+")

    /** True when a note uses one of the rich colour tints (index 0 means "no tint"). */
    fun isCustomTint(colorIndex: Int): Boolean = colorIndex in 1 until NoteColorOptions.size

    /** Human readable colour name, used for accessibility labels on the filter row. */
    fun colorNameForIndex(index: Int): String? =
        if (isCustomTint(index)) NoteColorNames.getOrNull(index) else null

    /**
     * Word count for the settings statistics card.
     *
     * Drawing notes are skipped: their content is a serialised drawing document,
     * so counting it produced nonsense totals (a single doodle added hundreds of
     * "words" to the workspace statistics).
     */
    fun countWords(notes: List<Note>): Int = notes.sumOf { note ->
        if (DrawingSerializer.isDrawing(note.content)) {
            0
        } else {
            note.content.split(WHITESPACE).count { it.isNotBlank() }
        }
    }

    /** Classifies the active filters so the UI can pick a localised sentence. */
    fun summarize(query: String, tag: String?, hasColorFilter: Boolean): FilterSummary {
        val trimmed = query.trim()
        return when {
            trimmed.isNotEmpty() && !tag.isNullOrBlank() ->
                FilterSummary(FilterSummaryKind.QUERY_IN_TAG, trimmed, tag)
            trimmed.isNotEmpty() -> FilterSummary(FilterSummaryKind.QUERY, trimmed)
            !tag.isNullOrBlank() -> FilterSummary(FilterSummaryKind.TAG, tag = tag)
            hasColorFilter -> FilterSummary(FilterSummaryKind.COLOR)
            else -> FilterSummary(FilterSummaryKind.ALL)
        }
    }

    /**
     * Text announced by a screen reader for a note card. The card itself has no
     * visible label, so without this TalkBack only reads the raw, clipped text.
     */
    fun noteContentDescription(
        title: String,
        isPinned: Boolean,
        isSelected: Boolean,
        isDrawing: Boolean,
        isInArchive: Boolean,
        isInTrash: Boolean
    ): String = buildString {
        append(if (title.isBlank()) "Untitled note" else title)
        if (isDrawing) append(", drawing")
        if (isInArchive) append(", archived")
        if (isInTrash) append(", in trash")
        if (isPinned) append(", pinned")
        append(if (isSelected) ", selected" else ", not selected")
    }
}
