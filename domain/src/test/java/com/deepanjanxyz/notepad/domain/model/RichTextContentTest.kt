package com.deepanjanxyz.notepad.domain.model

import com.deepanjanxyz.notepad.domain.util.NoteFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextContentTest {

    @Test
    fun `unformatted text encodes verbatim without separator`() {
        val doc = RichTextDocument(text = "Plain note body")
        val encoded = RichTextCodec.encode(doc)
        assertEquals("Plain note body", encoded)
        assertEquals(doc, RichTextCodec.decode(encoded))
    }

    @Test
    fun `inline and block spans round trip cleanly`() {
        val doc = RichTextDocument(
            text = "Heading\nHello world",
            inlineSpans = listOf(
                RichInlineSpan(8, 13, RichInlineKind.BOLD),
                RichInlineSpan(14, 19, RichInlineKind.ITALIC),
                RichInlineSpan(14, 19, RichInlineKind.UNDERLINE)
            ),
            blockSpans = listOf(
                RichBlockSpan(0, RichBlockKind.H1)
            )
        )
        val encoded = RichTextCodec.encode(doc)
        assertFalse(encoded.startsWith("*"))
        assertEquals("Heading\nHello world", RichTextCodec.extractPlainText(encoded))
        assertEquals(doc, RichTextCodec.decode(encoded))
    }

    @Test
    fun `typing with active styles adds spans without literal markers`() {
        val initial = RichTextDocument(text = "Hi ")
        val afterTyping = RichTextCodec.adjustForTextEdit(
            oldDoc = initial,
            newText = "Hi there",
            cursorAfterEdit = 8,
            activeTypingKinds = setOf(RichInlineKind.BOLD, RichInlineKind.ITALIC)
        )
        assertEquals("Hi there", afterTyping.text)
        assertEquals(
            listOf(
                RichInlineSpan(3, 8, RichInlineKind.BOLD),
                RichInlineSpan(3, 8, RichInlineKind.ITALIC)
            ),
            afterTyping.inlineSpans
        )
    }

    @Test
    fun `inserting plain text inside bold span splits span cleanly`() {
        val initial = RichTextDocument(
            text = "abcdef",
            inlineSpans = listOf(RichInlineSpan(0, 6, RichInlineKind.BOLD))
        )
        val edited = RichTextCodec.adjustForTextEdit(
            oldDoc = initial,
            newText = "abcXYdef",
            cursorAfterEdit = 5,
            activeTypingKinds = emptySet()
        )
        assertEquals(
            listOf(
                RichInlineSpan(0, 3, RichInlineKind.BOLD),
                RichInlineSpan(5, 8, RichInlineKind.BOLD)
            ),
            edited.inlineSpans
        )
    }

    @Test
    fun `toggle and clear formatting update spans accurately`() {
        val doc = RichTextDocument(text = "Hello world")
        val bolded = RichTextCodec.toggleInlineSpan(doc, 0, 5, RichInlineKind.BOLD)
        assertEquals(listOf(RichInlineSpan(0, 5, RichInlineKind.BOLD)), bolded.inlineSpans)

        val unbolded = RichTextCodec.toggleInlineSpan(bolded, 0, 5, RichInlineKind.BOLD)
        assertTrue(unbolded.inlineSpans.isEmpty())

        val h1 = RichTextCodec.applyBlockKind(bolded, 2, RichBlockKind.H1)
        assertEquals(listOf(RichBlockSpan(0, RichBlockKind.H1)), h1.blockSpans)

        val cleared = RichTextCodec.clearFormatting(h1, 0, 0)
        assertTrue(cleared.inlineSpans.isEmpty())
        assertTrue(cleared.blockSpans.isEmpty())
    }

    @Test
    fun `checklist and numbered list item spans survive renumbering and extraction`() {
        val segments = listOf(
            NoteSegment.Numbered(1, "Buy milk"),
            NoteSegment.Task(ChecklistItem(text = "Call Alice", isChecked = true))
        )
        val perSegmentSpans = listOf(
            listOf(RichInlineSpan(4, 8, RichInlineKind.BOLD)),
            listOf(RichInlineSpan(0, 4, RichInlineKind.UNDERLINE))
        )
        val doc = RichTextCodec.assembleFromSegments(segments, perSegmentSpans)
        val encoded = RichTextCodec.encode(doc)
        val decoded = RichTextCodec.decode(encoded)
        val extracted = RichTextCodec.extractSegmentInlineSpans(decoded)

        assertEquals(2, extracted.size)
        assertEquals(listOf(RichInlineSpan(4, 8, RichInlineKind.BOLD)), extracted[0])
        assertEquals(listOf(RichInlineSpan(0, 4, RichInlineKind.UNDERLINE)), extracted[1])
    }

    @Test
    fun `NoteFilter and NoteShortcut strip encoded rich-text metadata`() {
        val encoded = RichTextCodec.encode(
            RichTextDocument(
                text = "Important meeting",
                inlineSpans = listOf(RichInlineSpan(0, 9, RichInlineKind.BOLD)),
                blockSpans = listOf(RichBlockSpan(0, RichBlockKind.H1))
            )
        )
        val note = Note(id = 1L, title = "Plan", content = encoded)

        // Internal style marker token must NOT match search
        assertTrue(NoteFilter.filterNotes(listOf(note), "RICH_STYLES_V1").isEmpty())
        assertEquals(1, NoteFilter.filterNotes(listOf(note), "meeting").size)

        val shortcut = NoteShortcutBuilder.build(note)
        assertEquals("Important meeting", shortcut.text)
    }

    @Test
    fun `sanitizeLinkUrl accepts safe schemes and bare hosts while rejecting unsafe schemes`() {
        assertEquals("https://example.com/docs", RichTextCodec.sanitizeLinkUrl("  https://example.com/docs  "))
        assertEquals("http://localhost:8080", RichTextCodec.sanitizeLinkUrl("http://localhost:8080"))
        assertEquals("mailto:team@example.com", RichTextCodec.sanitizeLinkUrl("mailto:team@example.com"))
        assertEquals("tel:+15550199", RichTextCodec.sanitizeLinkUrl("tel:+15550199"))
        assertEquals("https://kotlinlang.org/docs", RichTextCodec.sanitizeLinkUrl("kotlinlang.org/docs"))

        assertEquals(null, RichTextCodec.sanitizeLinkUrl(""))
        assertEquals(null, RichTextCodec.sanitizeLinkUrl("javascript:alert(1)"))
        assertEquals(null, RichTextCodec.sanitizeLinkUrl("data:text/html,<script>"))
        assertEquals(null, RichTextCodec.sanitizeLinkUrl("file:///etc/passwd"))
        assertEquals(null, RichTextCodec.sanitizeLinkUrl("intent://foo#Intent;end"))
        assertEquals(null, RichTextCodec.sanitizeLinkUrl("https://bad host.com"))
        assertEquals(null, RichTextCodec.sanitizeLinkUrl("not a domain"))
    }

    @Test
    fun `link spans round trip and support apply, edit, remove, and clear formatting`() {
        val base = RichTextDocument(text = "Check docs today")
        val (withLink, cursorAfter) = RichTextCodec.applyLink(
            doc = base,
            selStart = 6,
            selEnd = 10,
            rawUrl = "kotlinlang.org",
            displayText = "Kotlin Docs"
        )
        assertEquals("Check Kotlin Docs today", withLink.text)
        assertEquals(17, cursorAfter)
        assertEquals(
            listOf(RichLinkSpan(6, 17, "https://kotlinlang.org")),
            withLink.linkSpans
        )

        val encoded = RichTextCodec.encode(withLink)
        val decoded = RichTextCodec.decode(encoded)
        assertEquals(withLink, decoded)
        assertEquals("Check Kotlin Docs today", RichTextCodec.extractPlainText(encoded))

        // Cursor inside link finds it
        assertEquals(
            RichLinkSpan(6, 17, "https://kotlinlang.org"),
            RichTextCodec.linkAt(decoded, 10, 10)
        )

        // Removing link leaves text untouched
        val removed = RichTextCodec.removeLink(decoded, 10, 10)
        assertEquals("Check Kotlin Docs today", removed.text)
        assertTrue(removed.linkSpans.isEmpty())

        // Clear formatting also removes links
        val cleared = RichTextCodec.clearFormatting(decoded, 6, 17)
        assertTrue(cleared.linkSpans.isEmpty())
    }

    @Test
    fun `editing text before and inside a link updates link bounds accurately`() {
        val initial = RichTextDocument(
            text = "Read docs",
            linkSpans = listOf(RichLinkSpan(5, 9, "https://example.com"))
        )
        val afterPrefixEdit = RichTextCodec.adjustForTextEdit(
            oldDoc = initial,
            newText = "Please read docs",
            cursorAfterEdit = 7,
            activeTypingKinds = emptySet()
        )
        assertEquals(
            listOf(RichLinkSpan(12, 16, "https://example.com")),
            afterPrefixEdit.linkSpans
        )
    }

    @Test
    fun `checklist and list item links survive assembly and extraction`() {
        val segments = listOf(
            NoteSegment.Task(ChecklistItem(text = "Review PR", isChecked = false)),
            NoteSegment.Bullet("Read guide")
        )
        val perSegmentLinks = listOf(
            listOf(RichLinkSpan(7, 9, "https://github.com/org/repo/pull/1")),
            listOf(RichLinkSpan(5, 10, "https://example.com/guide"))
        )
        val doc = RichTextCodec.assembleFromSegments(
            segments = segments,
            perSegmentSpans = emptyList(),
            perSegmentLinkSpans = perSegmentLinks
        )
        val decoded = RichTextCodec.decode(RichTextCodec.encode(doc))
        val extracted = RichTextCodec.extractSegmentLinkSpans(decoded)

        assertEquals(2, extracted.size)
        assertEquals(listOf(RichLinkSpan(7, 9, "https://github.com/org/repo/pull/1")), extracted[0])
        assertEquals(listOf(RichLinkSpan(5, 10, "https://example.com/guide")), extracted[1])
    }

    @Test
    fun `NoteFilter searches link URLs without matching internal metadata prefix`() {
        val doc = RichTextDocument(
            text = "Check documentation",
            linkSpans = listOf(RichLinkSpan(6, 19, "https://developer.android.com"))
        )
        val note = Note(id = 9L, title = "Links", content = RichTextCodec.encode(doc))

        assertEquals(1, NoteFilter.filterNotes(listOf(note), "developer.android.com").size)
        assertTrue(NoteFilter.filterNotes(listOf(note), "RICH_STYLES_V1").isEmpty())
    }
}
