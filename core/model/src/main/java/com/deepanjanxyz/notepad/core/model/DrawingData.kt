package com.deepanjanxyz.notepad.core.model

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
    val backgroundColor: Long = 0xFFFFFFFFL
)

object DrawingSerializer {
    const val PREFIX = "[DRAWING_V1]"
    private const val TEXT_SEPARATOR = "\n---KEEP_TEXT_BODY---\n"

    fun isDrawing(content: String): Boolean = content.startsWith(PREFIX)

    fun extractDrawingPart(content: String): String {
        if (!isDrawing(content)) return ""
        val idx = content.indexOf(TEXT_SEPARATOR)
        return if (idx >= 0) content.substring(0, idx).trim() else content.trim()
    }

    fun extractTextPart(content: String): String {
        if (!isDrawing(content)) return content
        val idx = content.indexOf(TEXT_SEPARATOR)
        return if (idx >= 0) content.substring(idx + TEXT_SEPARATOR.length) else ""
    }

    fun combine(drawingPart: String, textPart: String): String {
        val cleanDrawing = drawingPart.trim()
        val cleanText = textPart.trim()
        return when {
            cleanDrawing.isEmpty() -> cleanText
            cleanText.isEmpty() -> cleanDrawing
            else -> "$cleanDrawing$TEXT_SEPARATOR$cleanText"
        }
    }

    fun serialize(strokes: List<DrawingStroke>, backgroundColor: Long): String {
        val sb = StringBuilder()
        sb.append(PREFIX).append("\n")
        sb.append("BG:").append(backgroundColor.toString(16)).append("\n")
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            sb.append("S:")
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

    fun parse(content: String): DrawingData {
        if (!isDrawing(content)) {
            return DrawingData(emptyList(), 0xFFFFFFFFL)
        }
        var bg: Long = 0xFFFFFFFFL
        val strokes = mutableListOf<DrawingStroke>()
        val lines = content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("BG:")) {
                bg = trimmed.substring(3).toLongOrNull(16) ?: 0xFFFFFFFFL
            } else if (trimmed.startsWith("S:")) {
                val parts = trimmed.substring(2).split(":", limit = 2)
                if (parts.size == 2) {
                    val meta = parts[0].split(",")
                    val color = meta.getOrNull(0)?.toLongOrNull(16) ?: 0xFF000000L
                    val width = meta.getOrNull(1)?.toFloatOrNull() ?: 6f
                    val alpha = meta.getOrNull(2)?.toFloatOrNull() ?: 1f
                    val isHighlighter = meta.getOrNull(3) == "1"
                    val ptsStr = parts[1]
                    val pts = ptsStr.split(";").mapNotNull { ptStr ->
                        val coords = ptStr.split(",")
                        if (coords.size == 2) {
                            val x = coords[0].toFloatOrNull()
                            val y = coords[1].toFloatOrNull()
                            if (x != null && y != null) DrawingPoint(x, y) else null
                        } else null
                    }
                    if (pts.isNotEmpty()) {
                        strokes.add(
                            DrawingStroke(
                                points = pts,
                                color = color,
                                strokeWidth = width,
                                alpha = alpha,
                                isHighlighter = isHighlighter
                            )
                        )
                    }
                }
            }
        }
        return DrawingData(strokes = strokes, backgroundColor = bg)
    }
}
