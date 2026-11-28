package com.deepanjanxyz.notepad.backup

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.RichInlineKind
import com.deepanjanxyz.notepad.domain.model.RichInlineSpan
import com.deepanjanxyz.notepad.domain.model.RichLinkSpan
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.domain.model.RichTextDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun aLiteralFenceClosureMarkerSurvivesMarkdownRoundTrip() {
        val content = "<!-- elite-memo-pro-fence-closure -->"
        val restored = parseMarkdownNotes(buildMarkdownNotes(listOf(Note(title = "Marker", content = content))))
        assertEquals(content, restored.single().content)
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

    @Test
    fun richTextMetadataIsStrippedFromMarkdownExport() {
        val encoded = RichTextCodec.encode(
            RichTextDocument(
                text = "Bold intro\n[ ] Buy milk",
                inlineSpans = listOf(RichInlineSpan(0, 4, RichInlineKind.BOLD))
            )
        )
        val markdown = buildMarkdownNotes(listOf(Note(title = "Styled", content = encoded)))

        assertFalse(markdown.contains("RICH_STYLES_V1"))
        val restored = parseMarkdownNotes(markdown)
        assertEquals("Bold intro\n[ ] Buy milk", restored.single().content)
    }

    @Test
    fun inNoteLinksExportAsMarkdownAndRoundTripBackIntoLinkSpans() {
        val doc = RichTextDocument(
            text = "Visit Kotlin and Android [v2] today\n[ ] Read Guide",
            linkSpans = listOf(
                RichLinkSpan(6, 12, "https://kotlinlang.org"),
                RichLinkSpan(17, 29, "https://developer.android.com/(v2)"),
                RichLinkSpan(45, 50, "https://example.com/guide")
            )
        )
        val markdown = buildMarkdownNotes(listOf(Note(title = "Links", content = RichTextCodec.encode(doc))))

        assertTrue(markdown.contains("[Kotlin](https://kotlinlang.org)"))
        assertTrue(markdown.contains("[Android \\[v2\\]](https://developer.android.com/%28v2%29)"))
        assertTrue(markdown.contains("[ ] Read [Guide](https://example.com/guide)"))
        assertFalse(markdown.contains("RICH_STYLES_V1"))

        val restored = parseMarkdownNotes(markdown)
        val restoredDoc = RichTextCodec.decode(restored.single().content)
        assertEquals(doc.text, restoredDoc.text)
        assertEquals(
            listOf(
                RichLinkSpan(6, 12, "https://kotlinlang.org"),
                RichLinkSpan(17, 29, "https://developer.android.com/%28v2%29"),
                RichLinkSpan(45, 50, "https://example.com/guide")
            ),
            restoredDoc.linkSpans
        )
    }

    @Test
    fun linksInsideFencedCodeAndUnsafeSchemesAreNotImportedAsLinkSpans() {
        val text = "## Mixed\n\n~~~\n[literal](https://example.com)\n~~~\n[bad](javascript:alert(1))\n"
        val restored = parseMarkdownNotes(text)
        val restoredDoc = RichTextCodec.decode(restored.single().content)

        assertTrue(restoredDoc.linkSpans.isEmpty())
        assertTrue(restoredDoc.text.contains("[literal](https://example.com)"))
        assertTrue(restoredDoc.text.contains("[bad](javascript:alert(1))"))
    }

    @Test
    fun linkLabelsEndingInBackslashOrWithEscapedBracketsRoundTrip() {
        val doc = RichTextDocument(
            text = "Open foo\\ and C:\\docs\\[v1]\\ here",
            linkSpans = listOf(
                RichLinkSpan(5, 9, "https://example.com/slash"),
                RichLinkSpan(14, 27, "https://example.com/docs")
            )
        )
        val markdown = buildMarkdownNotes(listOf(Note(title = "Slashes", content = RichTextCodec.encode(doc))))

        assertTrue(markdown.contains("[foo\\\\](https://example.com/slash)"))
        assertTrue(markdown.contains("[C:\\\\docs\\\\\\[v1\\]\\\\](https://example.com/docs)"))

        val restored = parseMarkdownNotes(markdown)
        val restoredDoc = RichTextCodec.decode(restored.single().content)
        assertEquals(doc.text, restoredDoc.text)
        assertEquals(doc.linkSpans, restoredDoc.linkSpans)
    }

    @Test
    fun escapedBracketsAreNotParsedAsMarkdownLinks() {
        val text = "## Literal\n\n\\[not a link\\](https://example.com) and C:\\\\[link\\\\](https://example.com)\n"
        val restored = parseMarkdownNotes(text)
        val restoredDoc = RichTextCodec.decode(restored.single().content)

        assertEquals(
            "[not a link](https://example.com) and C:\\link\\",
            restoredDoc.text
        )
        assertEquals(
            listOf(RichLinkSpan(41, 46, "https://example.com")),
            restoredDoc.linkSpans
        )
    }

    @Test
    fun literalMarkdownLinkSyntaxInPlainNoteRoundTripsWithoutBecomingRichLink() {
        val content = "Read [docs](https://example.com) and \\[raw\\](https://example.com)\n[ ] Task [sub](https://x.com)"
        val markdown = buildMarkdownNotes(listOf(Note(title = "PlainLinks", content = content)))

        assertTrue(markdown.contains("\\[docs\\](https://example.com)"))
        assertTrue(markdown.contains("[ ] Task \\[sub\\](https://x.com)"))

        val restored = parseMarkdownNotes(markdown)
        val restoredDoc = RichTextCodec.decode(restored.single().content)
        assertEquals(content, restoredDoc.text)
        assertTrue(restoredDoc.linkSpans.isEmpty())
    }

    @Test
    fun mixedPlainLiteralLinkAndRichLinkPreserveOnlyRichLink() {
        val doc = RichTextDocument(
            text = "Literal [docs](https://example.com) and Kotlin",
            linkSpans = listOf(RichLinkSpan(40, 46, "https://kotlinlang.org"))
        )
        val markdown = buildMarkdownNotes(listOf(Note(title = "MixedLinks", content = RichTextCodec.encode(doc))))
        val restored = parseMarkdownNotes(markdown)
        val restoredDoc = RichTextCodec.decode(restored.single().content)

        assertEquals(doc.text, restoredDoc.text)
        assertEquals(doc.linkSpans, restoredDoc.linkSpans)
    }

    @Test
    fun balancedParenthesesInImportedLinkUrlArePreserved() {
        val text = "## Wiki\n\nSee [docs](https://example.com/a(b)c) for details\n"
        val restored = parseMarkdownNotes(text)
        val restoredDoc = RichTextCodec.decode(restored.single().content)

        assertEquals("See docs for details", restoredDoc.text)
        assertEquals(
            listOf(RichLinkSpan(4, 8, "https://example.com/a(b)c")),
            restoredDoc.linkSpans
        )
    }

    @Test
    fun crlfNormalizationRemapsRichLinkOffsetsAccurately() {
        val rawText = "x\r\nDocs!"
        val doc = RichTextDocument(
            text = rawText,
            linkSpans = listOf(RichLinkSpan(3, 7, "https://example.com/docs"))
        )
        val markdown = buildMarkdownNotes(listOf(Note(title = "CRLF", content = RichTextCodec.encode(doc))))

        assertTrue(markdown.contains("x\n[Docs](https://example.com/docs)!"))
        val restored = parseMarkdownNotes(markdown)
        val restoredDoc = RichTextCodec.decode(restored.single().content)
        assertEquals("x\nDocs!", restoredDoc.text)
        assertEquals(
            listOf(RichLinkSpan(2, 6, "https://example.com/docs")),
            restoredDoc.linkSpans
        )
    }

    @Test
    fun fenceOpenerRetainsFullInfoStringWithoutLinkParsing() {
        val content = "~~~ [docs](https://example.com)\ncode\n~~~"
        val markdown = buildMarkdownNotes(listOf(Note(title = "FenceInfo", content = content)))
        val restored = parseMarkdownNotes(markdown)
        val restoredDoc = RichTextCodec.decode(restored.single().content)

        assertEquals(content, restoredDoc.text)
        assertTrue(restoredDoc.linkSpans.isEmpty())
    }
}
