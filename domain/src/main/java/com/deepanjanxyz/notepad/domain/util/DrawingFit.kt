package com.deepanjanxyz.notepad.domain.util

import com.deepanjanxyz.notepad.domain.model.DrawingData
import com.deepanjanxyz.notepad.domain.model.DrawingPoint

/**
 * The scale and offset that fit a drawing into a box of a given size.
 *
 * A stroke's points are stored in the canvas pixels it was drawn in, which have
 * nothing to do with the box it is shown in later: a full-screen canvas and a
 * widget cell differ by an order of magnitude. [of] derives the single scale that
 * keeps the drawing's proportions and the offset that centres it, so a thumbnail
 * shows the whole sketch instead of a corner of it.
 */
data class DrawingFit(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
) {
    fun x(point: DrawingPoint): Float = point.x * scale + offsetX

    fun y(point: DrawingPoint): Float = point.y * scale + offsetY

    companion object {

        /** The scale of a drawing that already fills its box, or cannot be measured. */
        private const val UNIT_SCALE = 1f

        /** The narrowest box a drawing is still fitted into. */
        private const val MIN_AVAILABLE = 1f

        fun of(
            drawing: DrawingData,
            boxWidth: Float,
            boxHeight: Float,
            padding: Float = 0f
        ): DrawingFit {
            val points = drawing.strokes.flatMap { stroke -> stroke.points }
            if (points.isEmpty()) return DrawingFit(UNIT_SCALE, 0f, 0f)

            val minX = points.minOf { point -> point.x }
            val maxX = points.maxOf { point -> point.x }
            val minY = points.minOf { point -> point.y }
            val maxY = points.maxOf { point -> point.y }
            val spanX = (maxX - minX).takeIf { span -> span > 0f }
            val spanY = (maxY - minY).takeIf { span -> span > 0f }

            val availableWidth = (boxWidth - padding * 2).coerceAtLeast(MIN_AVAILABLE)
            val availableHeight = (boxHeight - padding * 2).coerceAtLeast(MIN_AVAILABLE)
            val scale = scaleFor(spanX, spanY, availableWidth, availableHeight)

            // Centre what is left over after scaling, then move the drawing's own
            // origin into place.
            val drawnWidth = (spanX ?: 0f) * scale
            val drawnHeight = (spanY ?: 0f) * scale
            val offsetX = padding + (availableWidth - drawnWidth) / 2 - minX * scale
            val offsetY = padding + (availableHeight - drawnHeight) / 2 - minY * scale
            return DrawingFit(scale, offsetX, offsetY)
        }

        /**
         * The largest scale that keeps the drawing inside the box, measured on
         * whichever axes it actually spans. A straight horizontal line has no
         * height to scale against, so its width alone decides.
         */
        private fun scaleFor(
            spanX: Float?,
            spanY: Float?,
            availableWidth: Float,
            availableHeight: Float
        ): Float = when {
            spanX != null && spanY != null ->
                minOf(availableWidth / spanX, availableHeight / spanY)

            spanX != null -> availableWidth / spanX
            spanY != null -> availableHeight / spanY
            else -> UNIT_SCALE
        }
    }
}
