package com.deepanjanxyz.notepad.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteShortcutBuilderTest {

    @Test
    fun `a note with no title and no content is empty`() {
        val shortcut = NoteShortcutBuilder.build(Note(id = 7L, title = "   ", content = "\n\n"))

        assertEquals(NoteShortcutKind.EMPTY, shortcut.kind)
        assertEquals("", shortcut.title)
        assertEquals("", shortcut.text)
    }

    @Test
    fun `a text note keeps its trimmed title and drops blank lines from the preview`() {
        val note = Note(
            id = 3L,
            title = "  Groceries  ",
            content = "Milk\n\n  \nBread\nEggs",
            colorIndex = 4
        )

        val shortcut = NoteShortcutBuilder.build(note)

        assertEquals(NoteShortcutKind.TEXT, shortcut.kind)
        assertEquals("Groceries", shortcut.title)
        assertEquals("Milk\nBread\nEggs", shortcut.text)
        assertEquals(4, shortcut.colorIndex)
        assertNull(shortcut.drawing)
    }

    @Test
    fun `a preview stops at the line limit`() {
        val content = (1..NoteShortcutBuilder.PREVIEW_LINES + 3).joinToString("\n") { "line $it" }

        val shortcut = NoteShortcutBuilder.build(Note(id = 1L, content = content))

        val lines = shortcut.text.lines()
        assertEquals(NoteShortcutBuilder.PREVIEW_LINES, lines.size)
        assertEquals("line 1", lines.first())
        assertEquals("line ${NoteShortcutBuilder.PREVIEW_LINES}", lines.last())
    }

    @Test
    fun `a preview is cut to the character limit and marked as cut`() {
        val content = "a".repeat(NoteShortcutBuilder.PREVIEW_CHARS * 2)

        val shortcut = NoteShortcutBuilder.build(Note(id = 1L, content = content))

        assertEquals(NoteShortcutBuilder.PREVIEW_CHARS, shortcut.text.length)
        assertTrue(shortcut.text.endsWith("\u2026"))
    }

    @Test
    fun `a drawing note carries its strokes instead of text`() {
        val note = Note(id = 11L, title = "Sketch", content = drawingContent())

        val shortcut = NoteShortcutBuilder.build(note)

        assertEquals(NoteShortcutKind.DRAWING, shortcut.kind)
        assertEquals("Sketch", shortcut.title)
        assertEquals("", shortcut.text)
        assertNotNull(shortcut.drawing)
        assertEquals(1, shortcut.drawing?.strokes?.size)
    }

    @Test
    fun `a drawing note with no strokes falls back to its caption`() {
        val content = DrawingSerializer.combine(
            drawingPart = DrawingSerializer.serialize(emptyList(), 0xFFFFFFFFL),
            textPart = "Ideas for the logo"
        )

        val shortcut = NoteShortcutBuilder.build(Note(id = 12L, content = content))

        assertEquals(NoteShortcutKind.TEXT, shortcut.kind)
        assertEquals("Ideas for the logo", shortcut.text)
        assertNull(shortcut.drawing)
    }

    private fun drawingContent(): String = DrawingSerializer.serialize(
        strokes = listOf(
            DrawingStroke(
                points = listOf(DrawingPoint(0f, 0f), DrawingPoint(40f, 20f)),
                color = 0xFF000000L,
                strokeWidth = 3f
            )
        ),
        backgroundColor = 0xFFFFFFFFL
    )
}
