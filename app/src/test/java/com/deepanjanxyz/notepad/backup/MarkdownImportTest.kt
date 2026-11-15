package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MarkdownImportTest {

    @Test
    fun aMarkdownFileIsReadIntoNotes() {
        val bytes = buildMarkdownNotes(listOf(Note(title = "Groceries", content = "milk")))

        val notes = MarkdownImport.notesFromMarkdown(bytes.toByteArray())

        assertEquals(1, notes.size)
        assertEquals("Groceries", notes.first().title)
        assertEquals("milk", notes.first().content)
    }

    @Test
    fun aZipIsRecognisedByItsSignature() {
        val zip = zipOf("a.md" to "# Elite Memo Pro notes\n\n## A\n\ntext\n")

        assertTrue(MarkdownImport.isZip(zip))
        assertTrue(!MarkdownImport.isZip(buildMarkdownNotes(listOf(Note(title = "A", content = "b"))).toByteArray()))
    }

    @Test
    fun everyMarkdownEntryOfAZipBecomesANote() {
        val zip = zipOf(
            "Groceries.md" to buildMarkdownNotes(listOf(Note(title = "Groceries", content = "milk"))),
            "Ideas.md" to buildMarkdownNotes(listOf(Note(title = "Ideas", content = "a second note")))
        )

        val notes = MarkdownImport.notesFromZip(zip)

        assertEquals(listOf("Groceries", "Ideas"), notes.map { it.title })
        assertEquals(listOf("milk", "a second note"), notes.map { it.content })
    }

    @Test
    fun foldersAndOtherFilesInAZipAreSkipped() {
        val zip = zipOf(
            "notes/" to "",
            "notes/Groceries.md" to buildMarkdownNotes(listOf(Note(title = "Groceries", content = "milk"))),
            "readme.txt" to "not a note"
        )

        val notes = MarkdownImport.notesFromZip(zip)

        assertEquals(listOf("Groceries"), notes.map { it.title })
    }

    @Test
    fun anEmptyZipHoldsNoNotes() {
        assertEquals(emptyList<Note>(), MarkdownImport.notesFromZip(zipOf()))
    }

    @Test
    fun aZipWithTooManyEntriesIsRefusedRatherThanCutShort() {
        val entries = (1..MAX_ZIP_ENTRIES + 1).map { index ->
            "note$index.md" to "# Elite Memo Pro notes\n\n## Note $index\n\nbody\n"
        }

        val refused = runCatching {
            MarkdownImport.notesFromZip(zipOf(*entries.toTypedArray()))
        }

        assertTrue(refused.isFailure)
    }

    /** A zip built the way the export builds one, entry by entry. */
    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
}
