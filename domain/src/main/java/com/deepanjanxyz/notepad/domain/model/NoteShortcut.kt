package com.deepanjanxyz.notepad.domain.model

/** What a note shortcut widget shows under its title. */
enum class NoteShortcutKind {
    /** Ordinary text, checklist or list content, shown as text. */
    TEXT,

    /** A drawing, shown as a thumbnail. */
    DRAWING,

    /** Nothing to show: the note holds neither a title nor any content. */
    EMPTY
}

/**
 * One note resolved into exactly what a home-screen shortcut renders: the title to
 * print, the kind of body it has, and that body.
 *
 * The title is passed through as the note stores it, so an untitled note keeps an
 * empty title here and the presentation layer supplies its own fallback wording.
 * No user-facing copy lives in the domain.
 */
data class NoteShortcut(
    val noteId: Long,
    val title: String,
    val kind: NoteShortcutKind,
    val colorIndex: Int = 0,
    val text: String = "",
    val drawing: DrawingData? = null
)

/**
 * Builds the [NoteShortcut] a widget draws.
 *
 * A note's content is one stored string, and a drawing note keeps its strokes
 * serialised inside that same string. The kind is therefore read from the content
 * rather than from a second flag that could drift out of step with it.
 */
object NoteShortcutBuilder {

    /** How many body lines a shortcut shows before it cuts the rest. */
    const val PREVIEW_LINES = 6

    /** How many body characters a shortcut shows before it cuts the rest. */
    const val PREVIEW_CHARS = 240

    private const val ELLIPSIS = "\u2026"

    fun build(note: Note): NoteShortcut {
        val title = note.title.trim()
        return if (DrawingSerializer.isDrawing(note.content)) {
            buildDrawingShortcut(note, title)
        } else {
            buildTextShortcut(note, title)
        }
    }

    private fun buildTextShortcut(note: Note, title: String): NoteShortcut {
        val text = previewText(note.content)
        return NoteShortcut(
            noteId = note.id,
            title = title,
            kind = if (title.isEmpty() && text.isEmpty()) NoteShortcutKind.EMPTY else NoteShortcutKind.TEXT,
            colorIndex = note.colorIndex,
            text = text
        )
    }

    private fun buildDrawingShortcut(note: Note, title: String): NoteShortcut {
        val drawing = DrawingSerializer.parse(note.content)
        // A drawing note can also carry a typed caption, which is all it has to
        // show while no stroke has been drawn yet.
        val caption = previewText(DrawingSerializer.extractTextPart(note.content))
        return when {
            drawing.strokes.isNotEmpty() -> NoteShortcut(
                noteId = note.id,
                title = title,
                kind = NoteShortcutKind.DRAWING,
                colorIndex = note.colorIndex,
                drawing = drawing
            )

            caption.isEmpty() && title.isEmpty() -> NoteShortcut(
                noteId = note.id,
                title = title,
                kind = NoteShortcutKind.EMPTY,
                colorIndex = note.colorIndex
            )

            else -> NoteShortcut(
                noteId = note.id,
                title = title,
                kind = NoteShortcutKind.TEXT,
                colorIndex = note.colorIndex,
                text = caption
            )
        }
    }

    /**
     * The opening of [content]: blank lines dropped, cut to [PREVIEW_LINES] lines
     * and then to [PREVIEW_CHARS] characters, so a widget cell never has to render
     * a whole note.
     */
    private fun previewText(content: String): String {
        val preview = content.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .take(PREVIEW_LINES)
            .joinToString("\n")
        return if (preview.length <= PREVIEW_CHARS) {
            preview
        } else {
            preview.take(PREVIEW_CHARS - 1) + ELLIPSIS
        }
    }
}
