package com.deepanjanxyz.notepad

import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.domain.model.parseNoteSegments
import com.deepanjanxyz.notepad.domain.model.renumberNumberedSegments
import com.deepanjanxyz.notepad.domain.model.serializeNoteSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLinesTest {

    @Test
    fun testBulletLinesAreParsedAsBullets() {
        val segments = parseNoteSegments("Groceries\n- Milk\n- Bread")

        assertEquals(3, segments.size)
        assertTrue(segments[0] is NoteSegment.Text)
        assertEquals("Milk", (segments[1] as NoteSegment.Bullet).text)
        assertEquals("Bread", (segments[2] as NoteSegment.Bullet).text)
    }

    @Test
    fun testProseThatOnlyLooksLikeAMarkerStaysText() {
        val segments = parseNoteSegments("-5 degrees outside\n-no space after the dash")

        assertTrue(segments.all { it is NoteSegment.Text })
    }

    @Test
    fun testNumberedLinesKeepTheirNumbers() {
        val segments = parseNoteSegments("1. First\n2. Second")

        assertEquals(1, (segments[0] as NoteSegment.Numbered).number)
        assertEquals("Second", (segments[1] as NoteSegment.Numbered).text)
    }

    @Test
    fun testNoteRoundTripsThroughTheLineModel() {
        val note = "Shopping\n- Milk\n\n1. Pay rent\n[ ] Call mom\n[x] Book tickets\n- Eggs"

        assertEquals(note, serializeNoteSegments(parseNoteSegments(note)))
    }

    @Test
    fun testMixedLinesKeepTheirOwnKind() {
        val segments = parseNoteSegments("[x] Done\n- Bullet\n2. Numbered\nPlain")

        assertTrue(segments[0] is NoteSegment.Task)
        assertTrue(segments[1] is NoteSegment.Bullet)
        assertTrue(segments[2] is NoteSegment.Numbered)
        assertTrue(segments[3] is NoteSegment.Text)
    }

    @Test
    fun testRenumberingKeepsARunInStep() {
        val segments = listOf(
            NoteSegment.Numbered(number = 1, text = "One"),
            NoteSegment.Numbered(number = 1, text = "Inserted"),
            NoteSegment.Numbered(number = 2, text = "Two")
        )

        assertEquals(listOf(1, 2, 3), numbersOf(segments))
    }

    @Test
    fun testRenumberingKeepsTheNumberARunStartsOn() {
        val segments = listOf(
            NoteSegment.Numbered(number = 5, text = "Five"),
            NoteSegment.Numbered(number = 9, text = "Six")
        )

        assertEquals(listOf(5, 6), numbersOf(segments))
    }

    @Test
    fun testRenumberingRestartsAfterAnotherLine() {
        val segments = listOf(
            NoteSegment.Numbered(number = 3, text = "Three"),
            NoteSegment.Text(""),
            NoteSegment.Numbered(number = 7, text = "Seven")
        )

        assertEquals(listOf(3, 7), numbersOf(segments))
    }

    @Test
    fun testANumberPastNineDigitsStillReadsBackAsANumberedLine() {
        val note = "999999999. Almost\n1000000000. Past the old cap"

        val segments = parseNoteSegments(note)

        assertEquals(999_999_999, (segments[0] as NoteSegment.Numbered).number)
        assertEquals(1_000_000_000, (segments[1] as NoteSegment.Numbered).number)
        assertEquals(note, serializeNoteSegments(segments))
    }

    @Test
    fun testRenumberingStopsAtTheLargestNumberRatherThanWrapping() {
        val segments = listOf(
            NoteSegment.Numbered(number = Int.MAX_VALUE, text = "Last"),
            NoteSegment.Numbered(number = 1, text = "One past it")
        )

        assertEquals(listOf(Int.MAX_VALUE, Int.MAX_VALUE), numbersOf(segments))
    }

    /** The numbers a renumbered list ends up with, in order. */
    private fun numbersOf(segments: List<NoteSegment>): List<Int> =
        renumberNumberedSegments(segments)
            .filterIsInstance<NoteSegment.Numbered>()
            .map { segment -> segment.number }
}
