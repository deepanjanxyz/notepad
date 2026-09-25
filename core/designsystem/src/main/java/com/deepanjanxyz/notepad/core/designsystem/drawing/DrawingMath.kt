package com.deepanjanxyz.notepad.core.designsystem.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.deepanjanxyz.notepad.core.model.DrawingPoint
import com.deepanjanxyz.notepad.core.model.DrawingStroke
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

fun getSelectionBounds(strokes: List<DrawingStroke>, selectedIndices: Set<Int>): Rect? {
    if (selectedIndices.isEmpty()) return null
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    var count = 0
    for (idx in selectedIndices) {
        val s = strokes.getOrNull(idx) ?: continue
        for (p in s.points) {
            count++
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
    }
    if (count == 0) return null
    val pad = 12f
    return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
}

fun isStrokeHit(stroke: DrawingStroke, point: Offset, threshold: Float = 26f): Boolean {
    val r = max(stroke.strokeWidth / 2f + 14f, threshold)
    val rSq = r * r
    for (i in stroke.points.indices) {
        val p = stroke.points[i]
        val dx = p.x - point.x
        val dy = p.y - point.y
        if (dx * dx + dy * dy <= rSq) return true
        if (i > 0) {
            val pPrev = stroke.points[i - 1]
            val segDx = p.x - pPrev.x
            val segDy = p.y - pPrev.y
            val segLenSq = segDx * segDx + segDy * segDy
            if (segLenSq > 0f) {
                val t = (((point.x - pPrev.x) * segDx + (point.y - pPrev.y) * segDy) / segLenSq).coerceIn(0f, 1f)
                val projX = pPrev.x + t * segDx
                val projY = pPrev.y + t * segDy
                val dPx = point.x - projX
                val dPy = point.y - projY
                if (dPx * dPx + dPy * dPy <= rSq) return true
            }
        }
    }
    return false
}

fun isStrokeInRect(stroke: DrawingStroke, rect: Rect): Boolean {
    return stroke.points.any { rect.contains(Offset(it.x, it.y)) }
}

fun getHandleHit(b: Rect, touch: Offset, radius: Float = 34f, stalkLengthPx: Float = 60f): Int? {
    val rSq = radius * radius
    val handles = listOf(
        Offset(b.left, b.top),           // 0: Top-Left
        Offset(b.center.x, b.top),       // 1: Top-Mid
        Offset(b.right, b.top),          // 2: Top-Right
        Offset(b.left, b.center.y),      // 3: Mid-Left
        Offset(b.right, b.center.y),     // 4: Mid-Right
        Offset(b.left, b.bottom),        // 5: Bottom-Left
        Offset(b.center.x, b.bottom),    // 6: Bottom-Mid
        Offset(b.right, b.bottom),       // 7: Bottom-Right
        Offset(b.center.x, b.top - stalkLengthPx) // 8: Top rotation handle
    )
    for (i in handles.indices) {
        val h = handles[i]
        val dx = h.x - touch.x
        val dy = h.y - touch.y
        if (dx * dx + dy * dy <= rSq) return i
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
        0 -> { // Top-Left
            val left = min(newPos.x, oldB.right - minSize)
            val top = min(newPos.y, oldB.bottom - minSize)
            Rect(left, top, oldB.right, oldB.bottom)
        }
        1 -> { // Top-Mid
            val top = min(newPos.y, oldB.bottom - minSize)
            Rect(oldB.left, top, oldB.right, oldB.bottom)
        }
        2 -> { // Top-Right
            val right = max(newPos.x, oldB.left + minSize)
            val top = min(newPos.y, oldB.bottom - minSize)
            Rect(oldB.left, top, right, oldB.bottom)
        }
        3 -> { // Mid-Left
            val left = min(newPos.x, oldB.right - minSize)
            Rect(left, oldB.top, oldB.right, oldB.bottom)
        }
        4 -> { // Mid-Right
            val right = max(newPos.x, oldB.left + minSize)
            Rect(oldB.left, oldB.top, right, oldB.bottom)
        }
        5 -> { // Bottom-Left
            val left = min(newPos.x, oldB.right - minSize)
            val bottom = max(newPos.y, oldB.top + minSize)
            Rect(left, oldB.top, oldB.right, bottom)
        }
        6 -> { // Bottom-Mid
            val bottom = max(newPos.y, oldB.top + minSize)
            Rect(oldB.left, oldB.top, oldB.right, bottom)
        }
        7 -> { // Bottom-Right
            val right = max(newPos.x, oldB.left + minSize)
            val bottom = max(newPos.y, oldB.top + minSize)
            Rect(oldB.left, oldB.top, right, bottom)
        }
        else -> oldB
    }
}

/**
 * Precision eraser that only removes the specific touched points / segments along the stroke
 * rather than removing the whole line.
 */
fun eraseSegmentAt(center: Offset, radius: Float, inputStrokes: List<DrawingStroke>): List<DrawingStroke> {
    val result = mutableListOf<DrawingStroke>()
    for (stroke in inputStrokes) {
        val effectiveR = radius + (stroke.strokeWidth / 2f)
        val effRSq = effectiveR * effectiveR

        var hasHit = false
        for (pt in stroke.points) {
            val dx = pt.x - center.x
            val dy = pt.y - center.y
            if (dx * dx + dy * dy <= effRSq) {
                hasHit = true
                break
            }
        }

        if (!hasHit) {
            result.add(stroke)
            continue
        }

        // Split stroke into surviving continuous segments outside the eraser radius
        val currentSubPoints = mutableListOf<DrawingPoint>()
        for (pt in stroke.points) {
            val dx = pt.x - center.x
            val dy = pt.y - center.y
            val isInside = (dx * dx + dy * dy) <= effRSq
            if (!isInside) {
                currentSubPoints.add(pt)
            } else {
                if (currentSubPoints.isNotEmpty()) {
                    result.add(stroke.copy(points = ArrayList(currentSubPoints)))
                    currentSubPoints.clear()
                }
            }
        }
        if (currentSubPoints.isNotEmpty()) {
            result.add(stroke.copy(points = ArrayList(currentSubPoints)))
        }
    }
    return result
}

// Smooth Bézier stroke rendering
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawingStroke(stroke: DrawingStroke) {
    if (stroke.points.size < 2) {
        if (stroke.points.size == 1) {
            val pt = stroke.points[0]
            drawCircle(
                color = androidx.compose.ui.graphics.Color(stroke.color).copy(alpha = stroke.alpha),
                radius = stroke.strokeWidth / 2,
                center = Offset(pt.x, pt.y)
            )
        }
        return
    }

    val path = androidx.compose.ui.graphics.Path().apply {
        val first = stroke.points.first()
        moveTo(first.x, first.y)
        for (i in 1 until stroke.points.size) {
            val p0 = stroke.points[i - 1]
            val p1 = stroke.points[i]
            val midX = (p0.x + p1.x) / 2
            val midY = (p0.y + p1.y) / 2
            quadraticTo(p0.x, p0.y, midX, midY)
        }
        val last = stroke.points.last()
        lineTo(last.x, last.y)
    }

    drawPath(
        path = path,
        color = androidx.compose.ui.graphics.Color(stroke.color).copy(alpha = stroke.alpha),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke.strokeWidth,
            cap = if (stroke.isHighlighter) androidx.compose.ui.graphics.StrokeCap.Square else androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}

