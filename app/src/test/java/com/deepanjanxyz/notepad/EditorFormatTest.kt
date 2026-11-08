package com.deepanjanxyz.notepad

import com.deepanjanxyz.notepad.domain.model.parseNoteSegments
import com.deepanjanxyz.notepad.domain.model.serializeNoteSegments
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.feature_editor.model.asSegment
import com.deepanjanxyz.notepad.ui.feature_editor.model.convertTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorFormatTest {

    @Test
    fun testConvertingToABulletedListKeepsTheWords() {
        val converted = parseNoteSegments("Milk\nBread").convertTo(EditorFormat.BulletedList)

        assertEquals("- Milk\n- Bread", serializeNoteSegments(converted))
    }

    @Test
    fun testConvertingToANumberedListNumbersEveryLine() {
        val converted = parseNoteSegments("Milk\n\nBread").convertTo(EditorFormat.NumberedList)

        assertEquals("1. Milk\n\n1. Bread", serializeNoteSegments(converted))
    }

    @Test
    fun testConvertingBackToPlainTextStripsEveryMarker() {
        val converted = parseNoteSegments("- Milk\n1. Bread\n[x] Eggs").convertTo(EditorFormat.PlainText)

        assertEquals("Milk\nBread\nEggs", serializeNoteSegments(converted))
    }

    @Test
    fun testFormatIsReadFromTheMarkersANoteCarries() {
        assertEquals(EditorFormat.Checklist, EditorFormat.of("[ ] Milk"))
        assertEquals(EditorFormat.BulletedList, EditorFormat.of("- Milk"))
        assertEquals(EditorFormat.NumberedList, EditorFormat.of("1. Milk"))
        assertEquals(EditorFormat.PlainText, EditorFormat.of("Just words"))
    }

    @Test
    fun testOnlyTheLinesFormatsEditTheBodyLineByLine() {
        assertFalse(EditorFormat.PlainText.isLinesMode)
        assertTrue(EditorFormat.Checklist.isLinesMode)
        assertTrue(EditorFormat.BulletedList.isLinesMode)
        assertTrue(EditorFormat.NumberedList.isLinesMode)
    }

    @Test
    fun testANewLineTakesTheKindOfTheFormatItIsAddedIn() {
        assertEquals("- Milk", serializeNoteSegments(listOf(EditorFormat.BulletedList.asSegment("Milk"))))
        assertEquals("1. Milk", serializeNoteSegments(listOf(EditorFormat.NumberedList.asSegment("Milk"))))
        assertEquals("[ ] Milk", serializeNoteSegments(listOf(EditorFormat.Checklist.asSegment("Milk"))))
    }
}
