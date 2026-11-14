package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class MarkdownExportTest {

    @Test
    fun theTitleBecomesTheFileName() {
        assertEquals("Trip to Goa.md", MarkdownExport.fileNameFor("Trip to Goa"))
    }

    @Test
    fun aNoteWithoutATitleUsesTheAppName() {
        assertEquals("Elite Memo Pro.md", MarkdownExport.fileNameFor(""))
        assertEquals("Elite Memo Pro.md", MarkdownExport.fileNameFor("   "))
    }

    @Test
    fun charactersAFileNameCannotHoldAreRemoved() {
        val name = MarkdownExport.fileNameFor("a/b\\c:d*e?f\"g<h>i|j")

        assertEquals("a b c d e f g h i j.md", name)
    }

    @Test
    fun aVeryLongTitleIsShortened() {
        val name = MarkdownExport.fileNameFor("t".repeat(200))

        assertEquals(60 + ".md".length, name.length)
        assertTrue(name.endsWith(".md"))
    }

    @Test
    fun twoNotesWithTheSameTitleAreToldApart() {
        val names = MarkdownExport.fileNamesFor(
            listOf(
                Note(title = "Trip", content = "one"),
                Note(title = "Trip", content = "two"),
                Note(title = "trip", content = "three")
            )
        )

        assertEquals(listOf("Trip.md", "Trip (1).md", "trip (2).md"), names)
    }

    @Test
    fun untitledNotesAreNumberedToo() {
        val names = MarkdownExport.fileNamesFor(
            listOf(Note(title = "", content = "a"), Note(title = "", content = "b"))
        )

        assertEquals(listOf("Elite Memo Pro.md", "Elite Memo Pro (1).md"), names)
    }

    @Test
    fun theZipHoldsOneMarkdownFilePerNote() {
        val notes = listOf(
            Note(title = "Groceries", content = "milk\neggs"),
            Note(title = "Ideas", content = "a second note")
        )

        val entries = unzip(MarkdownExport.buildZip(notes))

        assertEquals(listOf("Groceries.md", "Ideas.md"), entries.map { it.first })
        assertTrue(entries[0].second.contains("milk"))
        assertTrue(entries[1].second.contains("a second note"))
    }

    @Test
    fun anEmptyNoteIsLeftOutOfTheZip() {
        val notes = listOf(Note(title = "", content = ""), Note(title = "Kept", content = "text"))

        val entries = unzip(MarkdownExport.buildZip(notes))

        assertEquals(listOf("Kept.md"), entries.map { it.first })
    }

    /** The name and the text of every entry, read back the way a user would open it. */
    private fun unzip(bytes: ByteArray): List<Pair<String, String>> {
        val entries = mutableListOf<Pair<String, String>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += entry.name to zip.readBytes().decodeToString()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }
}
