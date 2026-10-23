package com.deepanjanxyz.notepad.domain.model

import java.util.Locale

data class DrawingPoint(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Long,
    val strokeWidth: Float,
    val alpha: Float = 1f,
    val isHighlighter: Boolean = false
)

data class DrawingData(
    val strokes: List<DrawingStroke> = emptyList(),
    val backgroundColor: Long = 0xFFFFFFFFL,
    val gridType: CanvasGridType = CanvasGridType.NONE
)

object DrawingSerializer {
    const val PREFIX = "[DRAWING_V1]"

    // The marker between the drawing payload and the note body stored with it.
    // It is part of the on-disk format, so it must never change: drawings saved
    // by earlier versions are read back with exactly this separator.
    private const val TEXT_SEPARATOR = "\n---KEEP_TEXT_BODY---\n"

    private const val BG_KEY = "BG:"
    private const val GRID_KEY = "GRID:"
    private const val STROKE_KEY = "S:"

    /** Whether [content] holds a drawing payload rather than plain text. */
    fun isDrawing(content: String): Boolean = content.startsWith(PREFIX)

    /** The drawing payload of [content], without the note body stored with it. */
    fun extractDrawingPart(content: String): String {
        if (!isDrawing(content)) return ""
        val idx = content.indexOf(TEXT_SEPARATOR)
        return if (idx >= 0) content.substring(0, idx).trim() else content.trim()
    }

    /** The note body stored alongside the drawing in [content]. */
    fun extractTextPart(content: String): String {
        if (!isDrawing(content)) return content
        val idx = content.indexOf(TEXT_SEPARATOR)
        return if (idx >= 0) content.substring(idx + TEXT_SEPARATOR.length) else ""
    }

    /** The drawing payload and the note body joined into one stored string. */
    fun combine(drawingPart: String, textPart: String): String {
        val cleanDrawing = drawingPart.trim()
        val cleanText = textPart.trim()
        return when {
            cleanDrawing.isEmpty() -> cleanText
            cleanText.isEmpty() -> cleanDrawing
            else -> "$cleanDrawing$TEXT_SEPARATOR$cleanText"
        }
    }

    /**
     * Encodes [strokes], the canvas background and its grid pattern.
     *
     * The grid is written as its own line so a canvas comes back looking the way
     * it was left. Older payloads without that line parse as [CanvasGridType.NONE].
     */
    fun serialize(
        strokes: List<DrawingStroke>,
        backgroundColor: Long,
        gridType: CanvasGridType = CanvasGridType.NONE
    ): String {
        val sb = StringBuilder()
        sb.append(PREFIX).append("\n")
        sb.append(BG_KEY).append(backgroundColor.toString(16)).append("\n")
        sb.append(GRID_KEY).append(gridType.name).append("\n")
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            sb.append(STROKE_KEY)
                .append(stroke.color.toString(16)).append(",")
                .append(String.format(Locale.US, "%.1f", stroke.strokeWidth)).append(",")
                .append(String.format(Locale.US, "%.2f", stroke.alpha)).append(",")
                .append(if (stroke.isHighlighter) "1" else "0").append(":")
            val pts = stroke.points.joinToString(";") {
                String.format(Locale.US, "%.1f,%.1f", it.x, it.y)
            }
            sb.append(pts).append("\n")
        }
        return sb.toString()
    }

    /** Decodes a stored payload, falling back to a blank page for unreadable input. */
    fun parse(content: String): DrawingData {
        if (!isDrawing(content)) {
            return DrawingData(emptyList(), 0xFFFFFFFFL, CanvasGridType.NONE)
        }
        var bg: Long = 0xFFFFFFFFL
        var grid = CanvasGridType.NONE
        val strokes = mutableListOf<DrawingStroke>()
        val lines = content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith(BG_KEY)) {
                bg = trimmed.substring(BG_KEY.length).toLongOrNull(16) ?: 0xFFFFFFFFL
            } else if (trimmed.startsWith(GRID_KEY)) {
                val stored = trimmed.substring(GRID_KEY.length).trim()
                grid = CanvasGridType.entries.firstOrNull { it.name == stored }
                    ?: CanvasGridType.NONE
            } else if (trimmed.startsWith(STROKE_KEY)) {
                parseStroke(trimmed)?.let(strokes::add)
            }
        }
        return DrawingData(strokes = strokes, backgroundColor = bg, gridType = grid)
    }

    /** One `S:` line turned into a stroke, or null when the line is malformed. */
    private fun parseStroke(line: String): DrawingStroke? {
        val parts = line.substring(STROKE_KEY.length).split(":", limit = 2)
        val meta = parts.getOrNull(0).orEmpty().split(",")
        val points = parts.getOrNull(1).orEmpty().split(";").mapNotNull { pointString ->
            val coords = pointString.split(",")
            if (coords.size != 2) return@mapNotNull null
            val x = coords[0].toFloatOrNull() ?: return@mapNotNull null
            val y = coords[1].toFloatOrNull() ?: return@mapNotNull null
            DrawingPoint(x, y)
        }
        // A line with no metadata/points separator, or with no readable points, is skipped.
        return if (parts.size != 2 || points.isEmpty()) {
            null
        } else {
            DrawingStroke(
                points = points,
                color = meta.getOrNull(0)?.toLongOrNull(16) ?: 0xFF000000L,
                strokeWidth = meta.getOrNull(1)?.toFloatOrNull() ?: 6f,
                alpha = meta.getOrNull(2)?.toFloatOrNull() ?: 1f,
                isHighlighter = meta.getOrNull(3) == "1"
            )
        }
    }
}
