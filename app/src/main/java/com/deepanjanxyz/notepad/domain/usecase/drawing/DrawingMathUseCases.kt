package com.deepanjanxyz.notepad.domain.usecase.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingStroke
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object DrawingMathUseCases {

    fun calculateSelectionBounds(strokes: List<DrawingStroke>, selectedIndices: Set<Int>): Rect? {
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
        return Rect(minX, minY, maxX, maxY)
    }

    fun getHandleHit(b: Rect, touch: Offset, radius: Float = 34f, stalkLengthPx: Float = 60f): Int? {
        val rSq = radius * radius
        val handles = listOf(
            Offset(b.left, b.top),                   // 0: Top-Left
            Offset(b.center.x, b.top),               // 1: Top-Mid
            Offset(b.right, b.top),                  // 2: Top-Right
            Offset(b.left, b.center.y),              // 3: Mid-Left
            Offset(b.right, b.center.y),             // 4: Mid-Right
            Offset(b.left, b.bottom),                // 5: Bottom-Left
            Offset(b.center.x, b.bottom),            // 6: Bottom-Mid
            Offset(b.right, b.bottom),               // 7: Bottom-Right
            Offset(b.center.x, b.top - stalkLengthPx) // 8: Top rotation handle
        )
        for (i in handles.indices) {
            val h = handles[i]
            val distSq = (h.x - touch.x) * (h.x - touch.x) + (h.y - touch.y) * (h.y - touch.y)
            if (distSq <= rSq) return i
        }
        return null
    }

    fun isMoveTargetHit(b: Rect, touch: Offset, padding: Float = 28f): Boolean {
        val expanded = Rect(b.left - padding, b.top - padding, b.right + padding, b.bottom + padding)
        return expanded.contains(touch)
    }

    fun updateBoundsWithHandle(oldB: Rect, handleIdx: Int, newPos: Offset): Rect {
        val minSize = 20f
        return when (handleIdx) {
            0 -> {
                val left = minOf(newPos.x, oldB.right - minSize)
                val top = minOf(newPos.y, oldB.bottom - minSize)
                Rect(left, top, oldB.right, oldB.bottom)
            }
            1 -> {
                val top = minOf(newPos.y, oldB.bottom - minSize)
                Rect(oldB.left, top, oldB.right, oldB.bottom)
            }
            2 -> {
                val right = maxOf(newPos.x, oldB.left + minSize)
                val top = minOf(newPos.y, oldB.bottom - minSize)
                Rect(oldB.left, top, right, oldB.bottom)
            }
            3 -> {
                val left = minOf(newPos.x, oldB.right - minSize)
                Rect(left, oldB.top, oldB.right, oldB.bottom)
            }
            4 -> {
                val right = maxOf(newPos.x, oldB.left + minSize)
                Rect(oldB.left, oldB.top, right, oldB.bottom)
            }
            5 -> {
                val left = minOf(newPos.x, oldB.right - minSize)
                val bottom = maxOf(newPos.y, oldB.top + minSize)
                Rect(left, oldB.top, oldB.right, bottom)
            }
            6 -> {
                val bottom = maxOf(newPos.y, oldB.top + minSize)
                Rect(oldB.left, oldB.top, oldB.right, bottom)
            }
            7 -> {
                val right = maxOf(newPos.x, oldB.left + minSize)
                val bottom = maxOf(newPos.y, oldB.top + minSize)
                Rect(oldB.left, oldB.top, right, bottom)
            }
            else -> oldB
        }
    }

    fun isStrokeInRect(stroke: DrawingStroke, rect: Rect): Boolean {
        return stroke.points.any { rect.contains(Offset(it.x, it.y)) }
    }

    fun isPointNearStrokeSegment(stroke: DrawingStroke, pt: Offset, threshold: Float = 24f): Boolean {
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
