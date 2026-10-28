package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import com.deepanjanxyz.notepad.ui.theme.AppColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DrawingSelectionOverlayTest(private val density: Float) {

    @Test
    fun testOutlineUsesSelectionBoundsAndThinAccentStroke() {
        val scope = drawBounds()

        val outline = scope.rectangles.single()
        assertEquals(AppColors.Accent, outline.color)
        assertEquals(bounds.topLeft, outline.topLeft)
        assertEquals(bounds.size, outline.size)
        assertTrue(outline.style is Stroke)
        assertEquals(1.5f * density, (outline.style as Stroke).width, tolerance)
    }

    @Test
    fun testRotationKnobHasPanelFillAndAccentRingAtEndOfStalk() {
        val scope = drawBounds()
        val center = Offset(bounds.center.x, bounds.top - 24f * density)

        val stalk = scope.lines.single()
        assertEquals(AppColors.Accent, stalk.color)
        assertEquals(Offset(bounds.center.x, bounds.top), stalk.start)
        assertEquals(center, stalk.end)
        assertEquals(1.5f * density, stalk.width, tolerance)
        assertGrip(scope, center, 5f * density)
    }

    @Test
    fun testAllFourCornerGripsHavePanelFillAndAccentRing() {
        val scope = drawBounds()
        val corners = listOf(bounds.topLeft, bounds.topRight, bounds.bottomLeft, bounds.bottomRight)

        // Four corner grips and one rotation knob, each painted as a fill followed by a ring.
        assertEquals(10, scope.circles.size)
        for (corner in corners) {
            assertGrip(scope, corner, 4.5f * density)
        }
    }

    @Test
    fun testTopAndBottomBarsAreHorizontalAndCenteredOnEdges() {
        val scope = drawBounds()

        assertEquals(4, scope.roundRectangles.size)
        for (center in listOf(Offset(bounds.center.x, bounds.top), Offset(bounds.center.x, bounds.bottom))) {
            assertBar(scope, center, Size(10f * density, 2.5f * density))
        }
    }

    @Test
    fun testLeftAndRightBarsAreVerticalAndCenteredOnEdges() {
        val scope = drawBounds()

        for (center in listOf(Offset(bounds.left, bounds.center.y), Offset(bounds.right, bounds.center.y))) {
            assertBar(scope, center, Size(2.5f * density, 10f * density))
        }
    }

    @Test
    fun testHandleSizesDoNotGrowWithSelectionBounds() {
        val small = drawBounds(Rect(0f, 0f, 40f, 60f))
        val large = drawBounds(Rect(-300f, -200f, 900f, 800f))

        assertEquals(small.circles.map { it.radius }, large.circles.map { it.radius })
        assertEquals(small.roundRectangles.map { it.size }, large.roundRectangles.map { it.size })
        assertEquals(small.lines.single().width, large.lines.single().width, tolerance)
        assertEquals(Offset(-300f, -200f), large.rectangles.single().topLeft)
        assertEquals(Size(1200f, 1000f), large.rectangles.single().size)
    }

    @Test
    fun testFontScalingDoesNotChangeGripGeometry() {
        val normal = drawBounds()
        val largeFont = RecordingDrawScope(density, fontScale = 2f).apply {
            drawSelectionBoundingBox(bounds)
        }

        assertEquals(normal.rectangles, largeFont.rectangles)
        assertEquals(normal.lines, largeFont.lines)
        assertEquals(normal.circles, largeFont.circles)
        assertEquals(normal.roundRectangles, largeFont.roundRectangles)
    }

    private fun drawBounds(rect: Rect = bounds): RecordingDrawScope = RecordingDrawScope(density).apply {
        drawSelectionBoundingBox(rect)
    }

    private fun assertGrip(scope: RecordingDrawScope, center: Offset, radius: Float) {
        val layers = scope.circles.filter { it.center == center }
        assertEquals(2, layers.size)
        assertEquals(AppColors.PanelDeep, layers[0].color)
        assertEquals(Fill, layers[0].style)
        assertEquals(AppColors.Accent, layers[1].color)
        assertTrue(layers[1].style is Stroke)
        assertEquals(1.5f * density, (layers[1].style as Stroke).width, tolerance)
        layers.forEach { assertEquals(radius, it.radius, tolerance) }
    }

    private fun assertBar(scope: RecordingDrawScope, center: Offset, size: Size) {
        val bar = scope.roundRectangles.single {
            it.topLeft + Offset(it.size.width / 2f, it.size.height / 2f) == center
        }
        assertEquals(size, bar.size)
        assertEquals(AppColors.Accent, bar.color)
        assertEquals(Fill, bar.style)
        assertEquals(CornerRadius(1.25f * density), bar.cornerRadius)
    }

    companion object {
        private val bounds = Rect(100f, 150f, 400f, 350f)
        private const val tolerance = 0.001f

        @JvmStatic
        @Parameterized.Parameters(name = "density={0}")
        fun densities(): List<Array<Float>> = listOf(arrayOf(1f), arrayOf(1.5f), arrayOf(3f))
    }
}

/** Records the public drawing operations without allocating Android paints or a bitmap. */
private class RecordingDrawScope(density: Float, fontScale: Float = 1f) : DrawScope by (CanvasDrawScope().also {
    it.drawContext.density = Density(density, fontScale)
}) {
    val rectangles = mutableListOf<Rectangle>()
    val lines = mutableListOf<Line>()
    val circles = mutableListOf<Circle>()
    val roundRectangles = mutableListOf<RoundRectangle>()

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        rectangles += Rectangle(color, topLeft, size, style)
    }

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        lines += Line(color, start, end, strokeWidth)
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        circles += Circle(color, radius, center, style)
    }

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        roundRectangles += RoundRectangle(color, topLeft, size, cornerRadius, style)
    }

    data class Rectangle(val color: Color, val topLeft: Offset, val size: Size, val style: DrawStyle)
    data class Line(val color: Color, val start: Offset, val end: Offset, val width: Float)
    data class Circle(val color: Color, val radius: Float, val center: Offset, val style: DrawStyle)
    data class RoundRectangle(
        val color: Color,
        val topLeft: Offset,
        val size: Size,
        val cornerRadius: CornerRadius,
        val style: DrawStyle
    )
}
