package com.deepanjanxyz.notepad.domain.util

import com.deepanjanxyz.notepad.domain.model.DrawingData
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingStroke
import org.junit.Assert.assertEquals
import org.junit.Test

private const val DELTA = 0.01f

class DrawingFitTest {

    @Test
    fun `a square drawing is centred in a wider box`() {
        val fit = DrawingFit.of(squareDrawing(), boxWidth = 200f, boxHeight = 100f)

        assertEquals(1f, fit.scale, DELTA)
        assertEquals(50f, fit.x(DrawingPoint(0f, 0f)), DELTA)
        assertEquals(0f, fit.y(DrawingPoint(0f, 0f)), DELTA)
        assertEquals(150f, fit.x(DrawingPoint(100f, 100f)), DELTA)
        assertEquals(100f, fit.y(DrawingPoint(100f, 100f)), DELTA)
    }

    @Test
    fun `padding shrinks the drawing into the box`() {
        val fit = DrawingFit.of(squareDrawing(), boxWidth = 200f, boxHeight = 100f, padding = 10f)

        assertEquals(0.8f, fit.scale, DELTA)
        assertEquals(60f, fit.x(DrawingPoint(0f, 0f)), DELTA)
        assertEquals(10f, fit.y(DrawingPoint(0f, 0f)), DELTA)
        assertEquals(140f, fit.x(DrawingPoint(100f, 100f)), DELTA)
        assertEquals(90f, fit.y(DrawingPoint(100f, 100f)), DELTA)
    }

    @Test
    fun `a drawing of one point stays where it is drawn`() {
        val drawing = DrawingData(
            strokes = listOf(
                DrawingStroke(
                    points = listOf(DrawingPoint(50f, 50f)),
                    color = 0xFF000000L,
                    strokeWidth = 0f
                )
            )
        )

        val fit = DrawingFit.of(drawing, boxWidth = 200f, boxHeight = 100f)

        assertEquals(1f, fit.scale, DELTA)
        assertEquals(100f, fit.x(DrawingPoint(50f, 50f)), DELTA)
        assertEquals(50f, fit.y(DrawingPoint(50f, 50f)), DELTA)
    }

    @Test
    fun `a drawing with no strokes is left unscaled`() {
        val fit = DrawingFit.of(DrawingData(), boxWidth = 200f, boxHeight = 100f)

        assertEquals(1f, fit.scale, DELTA)
        assertEquals(0f, fit.offsetX, DELTA)
        assertEquals(0f, fit.offsetY, DELTA)
    }

    @Test
    fun `a horizontal line is scaled by its width alone`() {
        val drawing = DrawingData(
            strokes = listOf(
                DrawingStroke(
                    points = listOf(DrawingPoint(0f, 10f), DrawingPoint(100f, 10f)),
                    color = 0xFF000000L,
                    strokeWidth = 0f
                )
            )
        )

        val fit = DrawingFit.of(drawing, boxWidth = 200f, boxHeight = 100f)

        assertEquals(2f, fit.scale, DELTA)
        assertEquals(0f, fit.x(DrawingPoint(0f, 0f)), DELTA)
    }

    @Test
    fun `stroke width is included when fitting a short mark`() {
        val drawing = DrawingData(
            strokes = listOf(
                DrawingStroke(
                    points = listOf(DrawingPoint(0f, 0f), DrawingPoint(1f, 0f)),
                    color = 0xFF000000L,
                    strokeWidth = 3f
                )
            )
        )

        val fit = DrawingFit.of(drawing, boxWidth = 100f, boxHeight = 100f, padding = 4f)
        val radius = 1.5f * fit.scale

        assertEquals(23f, fit.scale, DELTA)
        assertEquals(4f, fit.x(DrawingPoint(0f, 0f)) - radius, DELTA)
        assertEquals(96f, fit.x(DrawingPoint(1f, 0f)) + radius, DELTA)
    }

    private fun squareDrawing(): DrawingData = DrawingData(
        strokes = listOf(
            DrawingStroke(
                points = listOf(DrawingPoint(0f, 0f), DrawingPoint(100f, 100f)),
                color = 0xFF000000L,
                strokeWidth = 0f
            )
        )
    )
}
