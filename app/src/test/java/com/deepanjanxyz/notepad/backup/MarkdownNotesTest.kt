package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownNotesTest {

    @Test
    fun everyNoteComesBackWithItsTitleAndText() {
        val notes = listOf(
            Note(title = "Groceries", content = "milk\neggs\nbread"),
            Note(title = "Ideas", content = "a second note")
        )

        val restored = parseMarkdownNotes(buildMarkdownNotes(notes))

        assertEquals(listOf("Groceries", "Ideas"), restored.map { it.title })
        assertEquals(listOf("milk\neggs\nbread", "a second note"), restored.map { it.content })
    }

    @Test
    fun aNoteWithoutTextKeepsItsTitle() {
        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Only a title"))))

        assertEquals(1, restored.size)
        assertEquals("Only a title", restored.first().title)
        assertEquals("", restored.first().content)
    }

    @Test
    fun aLineThatLooksLikeTheSeparatorStaysInTheNote() {
        val content = "above\n---\nbelow"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Rules", content = content))))

        assertEquals(1, restored.size)
        assertEquals(content, restored.first().content)
    }

    @Test
    fun aLineThatLooksLikeAHeadingStaysInTheNote() {
        val content = "## not a heading\nplain line"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Headings", content = content))))

        assertEquals(1, restored.size)
        assertEquals(content, restored.first().content)
    }

    @Test
    fun checklistLinesSurviveTheRoundTrip() {
        val content = "[ ] milk\n[x] eggs"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "List", content = content))))

        assertEquals(content, restored.first().content)
    }

    @Test
    fun aBreakInTheTitleIsFoldedToOneLine() {
        val note = Note(title = "First line\nsecond line", content = "body")

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(note)))

        assertEquals("First line second line", restored.first().title)
    }

    @Test
    fun anEmptyNoteIsLeftOutOfTheFile() {
        val markdown = buildMarkdownNotes(
            listOf(Note(title = "", content = ""), Note(title = "Kept", content = "text"))
        )

        assertEquals(1, parseMarkdownNotes(markdown).size)
        assertTrue(markdown.contains("## Kept"))
    }

    @Test
    fun aFileThisAppDidNotWriteStillImports() {
        val text = "## Alpha\n\nfirst note\n\n---\n\n## Beta\n\nsecond note\n"

        val restored = parseMarkdownNotes(text)

        assertEquals(listOf("Alpha", "Beta"), restored.map { it.title })
        assertEquals(listOf("first note", "second note"), restored.map { it.content })
    }

    @Test
    fun windowsLineEndingsAreRead() {
        val text = "## Windows\r\n\r\nwritten elsewhere\r\n"

        val restored = parseMarkdownNotes(text)

        assertEquals(1, restored.size)
        assertEquals("written elsewhere", restored.first().content)
    }

    @Test
    fun aFileWithNoNotesYieldsNothing() {
        assertEquals(emptyList<Note>(), parseMarkdownNotes("just some text\nwith no headings"))
    }

    @Test
    fun unfinishedFencedCodeDoesNotAbsorbTheNextNoteOrChangeOnImport() {
        val first = Note(title = "Code", content = "~~~kotlin\nval value = 1")
        val second = Note(title = "Next", content = "second note")

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(first, second)))

        assertEquals(listOf("Code", "Next"), restored.map { it.title })
        assertEquals(listOf(first.content, second.content), restored.map { it.content })
    }

    @Test
    fun headingsAndSeparatorsInsideFencedCodeRemainLiteral() {
        val content = "~~~\n## literal heading\n---\nC:\\Users\\me\n~~~"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Code", content = content))))

        assertEquals(content, restored.single().content)
    }

    @Test
    fun aFourSpaceIndentedFenceDoesNotCloseAnOpenFence() {
        val text = "## Code\n\n~~~\ninside\n    ~~~\n## still inside\n~~~\n"

        val restored = parseMarkdownNotes(text)

        assertEquals(1, restored.size)
        assertEquals("~~~\ninside\n    ~~~\n## still inside\n~~~", restored.single().content)
    }

    @Test
    fun aBackslashInANoteIsKeptAsWritten() {
        val content = "C:\\Users\\olivia\\notes"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Paths", content = content))))

        assertEquals(content, restored.first().content)
    }

    @Test
    fun aLineThatAlreadyLooksEscapedStaysLiteral() {
        val content = "\\---\n\\## not a heading"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Escaped", content = content))))

        assertEquals(1, restored.size)
        assertEquals(content, restored.first().content)
    }

    @Test
    fun anIndentedSeparatorKeepsItsIndentation() {
        val content = "  ---\n  ## indented heading"

        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Indented", content = content))))

        assertEquals(content, restored.first().content)
    }
}
