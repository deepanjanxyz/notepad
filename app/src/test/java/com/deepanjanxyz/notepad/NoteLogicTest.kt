package com.deepanjanxyz.notepad

import com.deepanjanxyz.notepad.data.local.entity.NoteEntity
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.theme.NoteColorNames
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLogicTest {

    @Test
    fun testNoteEntityToDomainMapping() {
        val entity = NoteEntity(
            id = 42L,
            title = "Project Blueprint",
            content = "Architecture notes and requirements",
            date = "May 20, 2026",
            colorIndex = 3,
            isPinned = true,
            tags = "work,ideas,urgent",
            inTrash = false
        )

        val domain = entity.toDomain()
        assertEquals(42L, domain.id)
        assertEquals("Project Blueprint", domain.title)
        assertEquals(3, domain.tags.size)
        assertTrue(domain.tags.contains("work"))
        assertTrue(domain.tags.contains("ideas"))
        assertTrue(domain.tags.contains("urgent"))
        assertFalse(domain.inTrash)
        assertTrue(domain.isPinned)
    }

    @Test
    fun testDomainToEntityMapping() {
        val note = Note(
            id = 99L,
            title = "Shopping List",
            content = "Apples, Milk, Bread",
            date = "May 21, 2026",
            colorIndex = 5,
            isPinned = false,
            tags = listOf("Groceries", "Personal"),
            inTrash = true
        )

        val entity = NoteEntity.fromDomain(note)
        assertEquals("Groceries,Personal", entity.tags)
        assertTrue(entity.inTrash)
        assertEquals(5, entity.colorIndex)
    }

    @Test
    fun testColorPaletteExpansion() {
        // Must have more than the old 6-7 modern tint options
        assertTrue("Color options should be expanded to more than 7", NoteColorOptions.size >= 12)
        assertEquals(NoteColorOptions.size, NoteColorNames.size)
    }

    @Test
    fun testTagFilteringLogic() {
        val note1 = Note(id = 1, title = "Note 1", content = "Alpha", tags = listOf("work", "meeting"))
        val note2 = Note(id = 2, title = "Note 2", content = "Beta", tags = listOf("personal"))
        val note3 = Note(id = 3, title = "Note 3", content = "Gamma", tags = listOf("work"))

        val allNotes = listOf(note1, note2, note3)
        val workNotes = allNotes.filter { it.tags.any { tag -> tag.equals("work", ignoreCase = true) } }
        val personalNotes = allNotes.filter { it.tags.any { tag -> tag.equals("personal", ignoreCase = true) } }

        assertEquals(2, workNotes.size)
        assertEquals(1, personalNotes.size)
        assertEquals(1L, workNotes[0].id)
        assertEquals(3L, workNotes[1].id)
    }

    @Test
    fun testPinSelectionLogic() {
        val note1 = Note(id = 1, title = "Note 1", content = "A", isPinned = false)
        val note2 = Note(id = 2, title = "Note 2", content = "B", isPinned = true)

        val selected = listOf(note1, note2)
        val shouldPin = selected.any { !it.isPinned }
        assertTrue(shouldPin)

        val allPinned = listOf(note2, note2.copy(id = 4))
        val shouldUnpin = !allPinned.any { !it.isPinned }
        assertTrue(shouldUnpin)
    }

    @Test
    fun testTagSanitizationAndMultipleTags() {
        val input = "#Work, #Ideas, Project Alpha, #Android"
        val parsedTags = input.split(",")
            .map { it.trim().replace("#", "") }
            .filter { it.isNotBlank() }

        assertEquals(4, parsedTags.size)
        assertEquals("Work", parsedTags[0])
        assertEquals("Ideas", parsedTags[1])
        assertEquals("Project Alpha", parsedTags[2])
        assertEquals("Android", parsedTags[3])
        // Verify none contains hash
        assertTrue(parsedTags.none { it.contains("#") })
    }

    @Test
    fun testDrawingSerializationAndParsing() {
        val stroke1 = com.deepanjanxyz.notepad.domain.model.DrawingStroke(
            points = listOf(
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(10f, 20f),
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(30f, 40f)
            ),
            color = 0xFF1E88E5L,
            strokeWidth = 8f,
            alpha = 1f,
            isHighlighter = false
        )
        val stroke2 = com.deepanjanxyz.notepad.domain.model.DrawingStroke(
            points = listOf(
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(50f, 60f),
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(70f, 80f)
            ),
            color = 0xFFFDD835L,
            strokeWidth = 24f,
            alpha = 0.35f,
            isHighlighter = true
        )

        val serialized = com.deepanjanxyz.notepad.domain.model.DrawingSerializer.serialize(
            strokes = listOf(stroke1, stroke2),
            backgroundColor = 0xFFFFFFFFL
        )

        assertTrue(com.deepanjanxyz.notepad.domain.model.DrawingSerializer.isDrawing(serialized))
        assertFalse(com.deepanjanxyz.notepad.domain.model.DrawingSerializer.isDrawing("Just regular text"))

        val parsed = com.deepanjanxyz.notepad.domain.model.DrawingSerializer.parse(serialized)
        assertEquals(0xFFFFFFFFL, parsed.backgroundColor)
        assertEquals(2, parsed.strokes.size)
        assertEquals(2, parsed.strokes[0].points.size)
        assertEquals(10f, parsed.strokes[0].points[0].x, 0.2f)
        assertEquals(20f, parsed.strokes[0].points[0].y, 0.2f)
        assertEquals(false, parsed.strokes[0].isHighlighter)
        assertEquals(true, parsed.strokes[1].isHighlighter)
    }

    @Test
    fun testDrawingAndTextExtractionAndCombine() {
        val serializedDrawing = com.deepanjanxyz.notepad.domain.model.DrawingSerializer.serialize(
            strokes = emptyList(),
            backgroundColor = 0xFF131314L
        )
        val textBody = "Here are my notes underneath the drawing."
        val combined = com.deepanjanxyz.notepad.domain.model.DrawingSerializer.combine(serializedDrawing, textBody)

        assertTrue(com.deepanjanxyz.notepad.domain.model.DrawingSerializer.isDrawing(combined))
        assertEquals(serializedDrawing.trim(), com.deepanjanxyz.notepad.domain.model.DrawingSerializer.extractDrawingPart(combined))
        assertEquals(textBody, com.deepanjanxyz.notepad.domain.model.DrawingSerializer.extractTextPart(combined))

        // When content is pure drawing with no text
        assertEquals("", com.deepanjanxyz.notepad.domain.model.DrawingSerializer.extractTextPart(serializedDrawing))

        // When content is pure text with no drawing
        assertFalse(com.deepanjanxyz.notepad.domain.model.DrawingSerializer.isDrawing(textBody))
        assertEquals("", com.deepanjanxyz.notepad.domain.model.DrawingSerializer.extractDrawingPart(textBody))
        assertEquals(textBody, com.deepanjanxyz.notepad.domain.model.DrawingSerializer.extractTextPart(textBody))
    }

    @Test
    fun testSelectionBoundsCalculation() {
        val stroke = com.deepanjanxyz.notepad.domain.model.DrawingStroke(
            points = listOf(
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(100f, 150f),
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(200f, 250f)
            ),
            color = 0xFF00B0FFL,
            strokeWidth = 6f
        )
        val bounds = com.deepanjanxyz.notepad.ui.screens.getSelectionBounds(listOf(stroke), setOf(0))
        assertNotNull(bounds)
        assertTrue(bounds!!.left < 100f)
        assertTrue(bounds.right > 200f)
        assertTrue(bounds.top < 150f)
        assertTrue(bounds.bottom > 250f)
    }

    @Test
    fun testNoteFilterTitleAndContentKeywords() {
        val note1 = Note(
            id = 1L,
            title = "Weekly Team Sync",
            content = "Discuss Q4 roadmap, marketing budget, and product deadlines",
            date = "Today",
            colorIndex = 1,
            isPinned = false,
            tags = listOf("work")
        )
        val note2 = Note(
            id = 2L,
            title = "Grocery Shopping",
            content = "Milk, eggs, sourdough bread, organic apples",
            date = "Today",
            colorIndex = 2,
            isPinned = false,
            tags = listOf("personal")
        )
        val note3 = Note(
            id = 3L,
            title = "Weekend Hike Plan",
            content = "Pack trail mix, hydration pack, sunscreen",
            date = "Yesterday",
            colorIndex = 1,
            isPinned = false,
            tags = listOf("fitness")
        )
        val allNotes = listOf(note1, note2, note3)

        // 1. Single keyword in title
        val titleMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "Grocery")
        assertEquals(1, titleMatch.size)
        assertEquals(2L, titleMatch[0].id)

        // 2. Single keyword in content
        val contentMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "budget")
        assertEquals(1, contentMatch.size)
        assertEquals(1L, contentMatch[0].id)

        // 3. Multi-keyword matching across title and content
        val multiKeywordMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "sync marketing")
        assertEquals(1, multiKeywordMatch.size)
        assertEquals(1L, multiKeywordMatch[0].id)

        // 4. Case-insensitivity & whitespace trimming
        val caseWhitespaceMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "   TEAM   ROADMAP   ")
        assertEquals(1, caseWhitespaceMatch.size)
        assertEquals(1L, caseWhitespaceMatch[0].id)

        // 5. Keyword in tags
        val tagMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "fitness")
        assertEquals(1, tagMatch.size)
        assertEquals(3L, tagMatch[0].id)

        // 6. Blank query returns all notes
        val emptyMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "   ")
        assertEquals(3, emptyMatch.size)

        // 7. No matches
        val noMatch = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(allNotes, "cryptocurrency")
        assertEquals(0, noMatch.size)
    }

    @Test
    fun testNoteFilterWithDrawingNote() {
        val stroke = com.deepanjanxyz.notepad.domain.model.DrawingStroke(
            points = listOf(com.deepanjanxyz.notepad.domain.model.DrawingPoint(10f, 20f)),
            color = 0xFFFFFFFFL,
            strokeWidth = 4f
        )
        val drawingContent = com.deepanjanxyz.notepad.domain.model.DrawingSerializer.serialize(
            strokes = listOf(stroke),
            backgroundColor = 0xFF000000L
        )
        val drawingNote = Note(
            id = 10L,
            title = "Mountain Sketch",
            content = drawingContent,
            date = "Today",
            colorIndex = 0,
            isPinned = false
        )
        val notes = listOf(drawingNote)

        // Title matches
        val matchTitle = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(notes, "mountain")
        assertEquals(1, matchTitle.size)

        // Internal JSON keywords (like "strokes" or "points") should NOT match
        val matchJson = com.deepanjanxyz.notepad.domain.util.NoteFilter.filterNotes(notes, "strokes")
        assertEquals(0, matchJson.size)
    }

    @Test
    fun testNoteReminderEntityAndDomainMapping() {
        val targetTime = System.currentTimeMillis() + 3600_000L
        val note = Note(
            id = 55L,
            title = "Doctor Appointment",
            content = "Bring prescription records",
            reminderTime = targetTime
        )
        val entity = NoteEntity.fromDomain(note)
        assertEquals(targetTime, entity.reminderTime)

        val mappedDomain = entity.toDomain()
        assertEquals(targetTime, mappedDomain.reminderTime)
        assertEquals(55L, mappedDomain.id)
        assertEquals("Doctor Appointment", mappedDomain.title)
    }

    @Test
    fun testNoteReminderSchedulerWorkName() {
        val workName = com.deepanjanxyz.notepad.worker.NoteReminderScheduler.getWorkName(42L)
        assertEquals("note_reminder_42", workName)
    }

    @Test
    fun testNoteReminderDateTimeFormatting() {
        val now = System.currentTimeMillis()
        val formattedNow = com.deepanjanxyz.notepad.worker.NoteReminderScheduler.formatReminderDateTime(now)
        assertTrue(formattedNow.startsWith("Today, "))

        val tomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val formattedTomorrow = com.deepanjanxyz.notepad.worker.NoteReminderScheduler.formatReminderDateTime(tomorrow)
        assertTrue(formattedTomorrow.startsWith("Tomorrow, "))
    }

    @Test
    fun testDrawingStateAndBoundsIntegrity() {
        val stroke1 = com.deepanjanxyz.notepad.domain.model.DrawingStroke(
            points = listOf(
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(10f, 10f),
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(50f, 50f)
            ),
            color = 0xFF00B0FFL,
            strokeWidth = 6f
        )
        val stroke2 = com.deepanjanxyz.notepad.domain.model.DrawingStroke(
            points = listOf(
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(100f, 100f),
                com.deepanjanxyz.notepad.domain.model.DrawingPoint(200f, 200f)
            ),
            color = 0xFF34A853L,
            strokeWidth = 12f
        )
        val strokes = listOf(stroke1, stroke2)
        val bounds = com.deepanjanxyz.notepad.ui.screens.getSelectionBounds(strokes, setOf(0, 1))
        assertNotNull(bounds)
        assertEquals(10f - 12f, bounds!!.left, 0.01f)
        assertEquals(10f - 12f, bounds.top, 0.01f)
        assertEquals(200f + 12f, bounds.right, 0.01f)
        assertEquals(200f + 12f, bounds.bottom, 0.01f)
    }
}
