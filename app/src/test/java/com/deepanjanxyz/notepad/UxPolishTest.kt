package com.deepanjanxyz.notepad

import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.util.FilterSummaryKind
import com.deepanjanxyz.notepad.ui.util.NoteUiFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure UX helpers introduced by the UX/accessibility pass.
 * They are deliberately free of Compose/Android dependencies so they run fast on
 * the JVM in CI.
 */
class UxPolishTest {

    @Test
    fun everyTintHasAnAccessibilityColorName() {
        for (index in 1 until NoteColorOptions.size) {
            assertNotNull(
                "Every color tint needs a name so the filter row can be labelled for screen readers, missing index $index",
                NoteUiFormat.colorNameForIndex(index)
            )
        }
    }

    @Test
    fun defaultTintHasNoColorFilterLabel() {
        assertNull(NoteUiFormat.colorNameForIndex(0))
        assertFalse(NoteUiFormat.isCustomTint(0))
        assertTrue(NoteUiFormat.isCustomTint(1))
    }

    @Test
    fun colorNameMatchesTintOrder() {
        assertEquals("Red", NoteUiFormat.colorNameForIndex(1))
        assertEquals("Mustard", NoteUiFormat.colorNameForIndex(3))
        assertEquals("Slate", NoteUiFormat.colorNameForIndex(11))
    }

    @Test
    fun wordCountIgnoresDrawingPayloads() {
        val textNote = Note(id = 1L, title = "Plan", content = "plan the weekly review")
        val drawingNote = Note(
            id = 2L,
            title = "Sketch",
            content = DrawingSerializer.serialize(strokes = emptyList(), backgroundColor = 0xFF131314L)
        )

        assertEquals(4, NoteUiFormat.countWords(listOf(textNote, drawingNote)))
        assertEquals(0, NoteUiFormat.countWords(listOf(drawingNote)))
    }

    @Test
    fun wordCountToleratesBlankContent() {
        val blank = Note(id = 3L, title = "Empty", content = "   \n  ")
        assertEquals(0, NoteUiFormat.countWords(listOf(blank)))
    }

    @Test
    fun filterSummaryClassifiesSearchAndTagCombinations() {
        assertEquals(FilterSummaryKind.ALL, NoteUiFormat.summarize("", null, false).kind)
        assertEquals(FilterSummaryKind.COLOR, NoteUiFormat.summarize("  ", null, true).kind)
        assertEquals(FilterSummaryKind.QUERY, NoteUiFormat.summarize(" budget ", null, false).kind)
        assertEquals(FilterSummaryKind.TAG, NoteUiFormat.summarize("", "Work", false).kind)

        val combined = NoteUiFormat.summarize("budget", "Work", false)
        assertEquals(FilterSummaryKind.QUERY_IN_TAG, combined.kind)
        assertEquals("budget", combined.query)
        assertEquals("Work", combined.tag)
    }

    @Test
    fun noteContentDescriptionAnnouncesCardState() {
        val plain = NoteUiFormat.noteContentDescription(
            title = "Grocery list",
            isPinned = false,
            isSelected = false,
            isDrawing = false,
            isInArchive = false,
            isInTrash = false
        )
        assertEquals("Grocery list, not selected", plain)

        val rich = NoteUiFormat.noteContentDescription(
            title = "Mountain sketch",
            isPinned = true,
            isSelected = true,
            isDrawing = true,
            isInArchive = true,
            isInTrash = false
        )
        assertTrue(rich.contains("drawing"))
        assertTrue(rich.contains("archived"))
        assertTrue(rich.contains("pinned"))
        assertTrue(rich.endsWith("selected"))
    }

    @Test
    fun noteContentDescriptionFallsBackForUntitledNotes() {
        val description = NoteUiFormat.noteContentDescription(
            title = "",
            isPinned = false,
            isSelected = false,
            isDrawing = false,
            isInArchive = false,
            isInTrash = true
        )
        assertTrue(description.startsWith("Untitled note"))
        assertTrue(description.contains("in trash"))
    }
}
