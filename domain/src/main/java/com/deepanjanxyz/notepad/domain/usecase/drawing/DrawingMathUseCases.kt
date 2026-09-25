package com.deepanjanxyz.notepad.domain.usecase.drawing

import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingStroke

data class DrawingOffset(val x: Float, val y: Float)

data class DrawingRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(point: DrawingOffset): Boolean =
        point.x >= left && point.x < right && point.y >= top && point.y < bottom
}

object DrawingMathUseCases {

    fun calculateSelectionBounds(strokes: List<DrawingStroke>, selectedIndices: Set<Int>): DrawingRect? {
        if (selectedIndices.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var hasPoints = false

        for (idx in selectedIndices) {
            val s = strokes.getOrNull(idx) ?: continue
            val pad = (s.strokeWidth / 2f).coerceAtLeast(12f)
            for (p in s.points) {
                hasPoints = true
                if (p.x - pad < minX) minX = p.x - pad
                if (p.y - pad < minY) minY = p.y - pad
                if (p.x + pad > maxX) maxX = p.x + pad
                if (p.y + pad > maxY) maxY = p.y + pad
            }
        }
        if (!hasPoints) return null
        val minDimension = 48f
        if (maxX - minX < minDimension) {
            val cx = (minX + maxX) / 2f
            minX = cx - minDimension / 2f
            maxX = cx + minDimension / 2f
        }
        if (maxY - minY < minDimension) {
            val cy = (minY + maxY) / 2f
            minY = cy - minDimension / 2f
            maxY = cy + minDimension / 2f
        }
        return DrawingRect(minX, minY, maxX, maxY)
    }

    fun getHandleHit(
        b: DrawingRect,
        touch: DrawingOffset,
        radius: Float = 34f,
        stalkLengthPx: Float = 60f
    ): Int? {
        val rSq = radius * radius
        val handles = listOf(
            DrawingOffset(b.left, b.top),
            DrawingOffset(b.centerX, b.top),
            DrawingOffset(b.right, b.top),
            DrawingOffset(b.left, b.centerY),
            DrawingOffset(b.right, b.centerY),
            DrawingOffset(b.left, b.bottom),
            DrawingOffset(b.centerX, b.bottom),
            DrawingOffset(b.right, b.bottom),
            DrawingOffset(b.centerX, b.top - stalkLengthPx)
        )
        for (i in handles.indices) {
            val h = handles[i]
            val distSq = (h.x - touch.x) * (h.x - touch.x) + (h.y - touch.y) * (h.y - touch.y)
            if (distSq <= rSq) return i
        }
        return null
    }

    fun isMoveTargetHit(b: DrawingRect, touch: DrawingOffset, padding: Float = 28f): Boolean {
        val expanded = DrawingRect(b.left - padding, b.top - padding, b.right + padding, b.bottom + padding)
        return expanded.contains(touch)
    }

    fun updateBoundsWithHandle(oldB: DrawingRect, handleIdx: Int, newPos: DrawingOffset): DrawingRect {
        val minSize = 20f
        return when (handleIdx) {
            0 -> {
                val left = minOf(newPos.x, oldB.right - minSize)
                val top = minOf(newPos.y, oldB.bottom - minSize)
                DrawingRect(left, top, oldB.right, oldB.bottom)
            }
            1 -> {
                val top = minOf(newPos.y, oldB.bottom - minSize)
                DrawingRect(oldB.left, top, oldB.right, oldB.bottom)
            }
            2 -> {
                val right = maxOf(newPos.x, oldB.left + minSize)
                val top = minOf(newPos.y, oldB.bottom - minSize)
                DrawingRect(oldB.left, top, right, oldB.bottom)
            }
            3 -> {
                val left = minOf(newPos.x, oldB.right - minSize)
                DrawingRect(left, oldB.top, oldB.right, oldB.bottom)
            }
            4 -> {
                val right = maxOf(newPos.x, oldB.left + minSize)
                DrawingRect(oldB.left, oldB.top, right, oldB.bottom)
            }
            5 -> {
                val left = minOf(newPos.x, oldB.right - minSize)
                val bottom = maxOf(newPos.y, oldB.top + minSize)
                DrawingRect(left, oldB.top, oldB.right, bottom)
            }
            6 -> {
                val bottom = maxOf(newPos.y, oldB.top + minSize)
                DrawingRect(oldB.left, oldB.top, oldB.right, bottom)
            }
            7 -> {
                val right = maxOf(newPos.x, oldB.left + minSize)
                val bottom = maxOf(newPos.y, oldB.top + minSize)
                DrawingRect(oldB.left, oldB.top, right, bottom)
            }
            else -> oldB
        }
    }

    fun isStrokeInRect(stroke: DrawingStroke, rect: DrawingRect): Boolean {
        return stroke.points.any { rect.contains(DrawingOffset(it.x, it.y)) }
    }

    fun isPointNearStrokeSegment(stroke: DrawingStroke, pt: DrawingOffset, threshold: Float = 24f): Boolean {
        if (stroke.points.isEmpty()) return false
        val tSq = (threshold + stroke.strokeWidth / 2f) * (threshold + stroke.strokeWidth / 2f)
        for (i in 0 until stroke.points.size - 1) {
            val p1 = stroke.points[i]
            val p2 = stroke.points[i + 1]
            val distSq = distToSegmentSquared(pt.x, pt.y, p1.x, p1.y, p2.x, p2.y)
            if (distSq <= tSq) return true
        }
        if (stroke.points.size == 1) {
            val p = stroke.points[0]
            val dSq = (p.x - pt.x) * (p.x - pt.x) + (p.y - pt.y) * (p.y - pt.y)
            if (dSq <= tSq) return true
        }
        return false
    }

    private fun distToSegmentSquared(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
        if (l2 == 0f) return (px - x1) * (px - x1) + (py - y1) * (py - y1)
        var t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2
        t = t.coerceIn(0f, 1f)
        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)
        return (px - projX) * (px - projX) + (py - projY) * (py - projY)
    }
}
