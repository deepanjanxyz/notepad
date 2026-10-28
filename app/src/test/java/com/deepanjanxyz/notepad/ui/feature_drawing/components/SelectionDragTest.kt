package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingStroke
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SelectionDragTest(private val density: Float) {

    @Test
    fun testAllResizeHandlesAndRotationHandleKeepTheirIndices() {
        val handles = listOf(
            bounds.topLeft,
            Offset(bounds.center.x, bounds.top),
            bounds.topRight,
            Offset(bounds.left, bounds.center.y),
            Offset(bounds.right, bounds.center.y),
            bounds.bottomLeft,
            Offset(bounds.center.x, bounds.bottom),
            bounds.bottomRight,
            Offset(bounds.center.x, bounds.top - 24f * density)
        )

        handles.forEachIndexed { index, start ->
            val fixture = GestureFixture()
            fixture.run(start)

            assertEquals(index, fixture.drag.activeHandleIndex.value)
            assertEquals(bounds, fixture.drag.initialBounds.value)
            assertEquals(start, fixture.drag.startPos.value)
            assertEquals(mapOf(0 to fixture.originalStroke), fixture.drag.initialStrokes.value)
            assertFalse(fixture.drag.isDragging.value)
            assertEquals(1, fixture.historyPushes)
            assertEquals(1, fixture.touchDowns)
            assertTrue(fixture.selectionChanges.isEmpty())
            assertNull(fixture.marquee)
        }
    }

    @Test
    fun testHandleCanBeGrabbedExactlyTwelveDpAway() {
        val fixture = GestureFixture()

        fixture.run(Offset(bounds.left - 12f * density, bounds.top))

        assertEquals(0, fixture.drag.activeHandleIndex.value)
        assertEquals(1, fixture.historyPushes)
        assertNull(fixture.marquee)
    }

    @Test
    fun testTouchBeyondHandleGrabRadiusStartsMarquee() {
        val fixture = GestureFixture()
        val start = Offset(bounds.left - 12f * density - 0.5f, bounds.top)

        fixture.run(start)

        assertNull(fixture.drag.activeHandleIndex.value)
        assertFalse(fixture.drag.isDragging.value)
        assertEquals(Rect(start, Size.Zero), fixture.marquee)
        assertEquals(emptySet<Int>(), fixture.selected)
        assertEquals(0, fixture.historyPushes)
    }

    @Test
    fun testDiagonalTouchOutsideCircularGrabTargetDoesNotResize() {
        val fixture = GestureFixture()
        val start = bounds.topLeft - Offset(10f * density, 10f * density)

        fixture.run(start)

        assertNull(fixture.drag.activeHandleIndex.value)
        assertEquals(Rect(start, Size.Zero), fixture.marquee)
        assertEquals(0, fixture.historyPushes)
    }

    @Test
    fun testTouchWithinNineDpMovePaddingMovesSelection() {
        val fixture = GestureFixture()

        // Stay away from the resize handles so this specifically exercises the move padding.
        fixture.run(Offset(300f * density, bounds.top - 9f * density + 0.5f))

        assertTrue(fixture.drag.isDragging.value)
        assertNull(fixture.drag.activeHandleIndex.value)
        assertEquals(setOf(0), fixture.selected)
        assertEquals(1, fixture.historyPushes)
        assertNull(fixture.marquee)
    }

    @Test
    fun testTouchBeyondNineDpMovePaddingStartsMarquee() {
        val fixture = GestureFixture()
        val start = Offset(300f * density, bounds.top - 9f * density - 0.5f)

        fixture.run(start)

        assertFalse(fixture.drag.isDragging.value)
        assertNull(fixture.drag.activeHandleIndex.value)
        assertEquals(Rect(start, Size.Zero), fixture.marquee)
        assertEquals(0, fixture.historyPushes)
    }

    @Test
    fun testStrokeHitRadiusIncludesTenDpBoundaryAndExistingStrokePadding() {
        val fixture = GestureFixture().apply {
            currentBounds = null
            selected = emptySet()
        }
        // DrawingMath also provides a 14 px padding around half the stroke width.
        val radius = maxOf(10f * density, fixture.originalStroke.strokeWidth / 2f + 14f)
        val point = fixture.originalStroke.points.first()

        fixture.run(Offset(point.x, point.y - radius))

        assertEquals(setOf(0), fixture.selected)
        assertTrue(fixture.drag.isDragging.value)
        assertEquals(1, fixture.historyPushes)
        assertNull(fixture.marquee)
    }

    @Test
    fun testTouchBeyondStrokeHitRadiusDoesNotSelectStroke() {
        val fixture = GestureFixture().apply {
            currentBounds = null
            selected = emptySet()
        }
        val radius = maxOf(10f * density, fixture.originalStroke.strokeWidth / 2f + 14f)
        val point = fixture.originalStroke.points.first()
        val start = Offset(point.x, point.y - radius - 0.5f)

        fixture.run(start)

        assertTrue(fixture.selected.isEmpty())
        assertFalse(fixture.drag.isDragging.value)
        assertEquals(Rect(start, Size.Zero), fixture.marquee)
        assertEquals(0, fixture.historyPushes)
    }

    @Test
    fun testOverlappingStrokesSelectTopmostStroke() {
        val fixture = GestureFixture().apply {
            currentBounds = null
            selected = emptySet()
            strokes = listOf(originalStroke, originalStroke.copy(color = 0xFFFF0000L))
        }
        val underlying = fixture.strokes.first()
        val point = fixture.originalStroke.points.first()

        fixture.run(Offset(point.x, point.y))

        assertEquals(setOf(1), fixture.selected)
        assertEquals(underlying, fixture.strokes.first())
        assertEquals(0xFFFF0000L, fixture.strokes.last().color)
        assertEquals(1, fixture.historyPushes)
    }

    @Test
    fun testSelectedStrokeCanStillBeMovedWithoutSelectionBounds() {
        val fixture = GestureFixture().apply { currentBounds = null }
        val point = fixture.originalStroke.points.first()

        fixture.run(Offset(point.x, point.y))

        assertTrue(fixture.drag.isDragging.value)
        assertTrue(fixture.selectionChanges.isEmpty())
        assertEquals(1, fixture.historyPushes)
    }

    @Test
    fun testMarqueeAtMinimumSizeDoesNotApplySelection() {
        val minimum = 3f * density
        for (size in listOf(Size(minimum, 0f), Size(0f, minimum), Size(minimum, minimum))) {
            val fixture = GestureFixture()
            val start = Offset(50f * density, 50f * density)

            fixture.run(start, listOf(start + Offset(size.width, size.height)), Ending.UP)

            // Only the initial clearing callback fires; the undersized marquee is discarded.
            assertEquals(listOf(emptySet<Int>()), fixture.selectionChanges)
            assertNull(fixture.marquee)
            assertDragReset(fixture.drag)
            assertEquals(0, fixture.historyPushes)
        }
    }

    @Test
    fun testMarqueeJustLargerThanMinimumAppliesSelection() {
        val fixture = GestureFixture()
        val start = Offset(50f * density, 50f * density)

        fixture.run(start, listOf(start + Offset(3f * density + 0.5f, 0f)), Ending.UP)

        assertEquals(listOf(emptySet<Int>(), emptySet<Int>()), fixture.selectionChanges)
        assertNull(fixture.marquee)
        assertDragReset(fixture.drag)
    }

    @Test
    fun testWideShortMarqueeSelectsEnclosedStroke() {
        val fixture = GestureFixture().apply {
            currentBounds = null
            selected = emptySet()
        }
        val point = fixture.originalStroke.points.first()
        val start = Offset(point.x - 50f * density, point.y - density)
        val end = Offset(point.x + 50f * density, point.y + density)

        fixture.run(start, listOf(end), Ending.UP)

        assertEquals(listOf(emptySet<Int>(), setOf(0)), fixture.selectionChanges)
        assertNull(fixture.marquee)
        assertDragReset(fixture.drag)
    }

    @Test
    fun testTallNarrowMarqueeSelectsEnclosedStroke() {
        val fixture = GestureFixture().apply {
            currentBounds = null
            selected = emptySet()
        }
        val point = fixture.originalStroke.points.first()
        val start = Offset(point.x - density, point.y - 50f * density)
        val end = Offset(point.x + density, point.y + 50f * density)

        fixture.run(start, listOf(end), Ending.UP)

        assertEquals(listOf(emptySet<Int>(), setOf(0)), fixture.selectionChanges)
        assertNull(fixture.marquee)
        assertDragReset(fixture.drag)
    }

    @Test
    fun testMovingSelectionPreservesUnselectedStrokesAndBrushProperties() {
        val fixture = GestureFixture()
        val unselected = fixture.originalStroke.copy(color = 0xFF00FF00L)
        fixture.strokes = fixture.strokes + unselected
        val start = bounds.center
        val delta = Offset(12f, -8f)

        fixture.run(start, listOf(start + delta), Ending.UP)

        // The 1 px motion that crosses touch slop is also delivered to onDrag.
        val total = delta + Offset(1f, 0f)
        val expected = fixture.originalStroke.copy(points = fixture.originalStroke.points.map {
            DrawingPoint(it.x + total.x, it.y + total.y)
        })
        assertEquals(listOf(expected, unselected), fixture.strokes)
        assertEquals(1, fixture.historyPushes)
        assertDragReset(fixture.drag)
    }

    @Test
    fun testResizingUsesInitialSnapshotAcrossMultipleDragEvents() {
        val fixture = GestureFixture()
        val unselected = fixture.originalStroke.copy(color = 0xFF00FF00L)
        fixture.strokes = fixture.strokes + unselected

        fixture.run(
            bounds.bottomRight,
            listOf(Offset(700f * density, 700f * density), Offset(800f * density, 800f * density)),
            Ending.UP
        )

        val expected = fixture.originalStroke.copy(points = fixture.originalStroke.points.map {
            DrawingPoint(bounds.left + (it.x - bounds.left) * 1.5f, bounds.top + (it.y - bounds.top) * 1.5f)
        })
        assertEquals(listOf(expected, unselected), fixture.strokes)
        assertEquals(1, fixture.historyPushes)
        assertDragReset(fixture.drag)
    }

    @Test
    fun testRotationUsesStalkHandleAndPreservesUnselectedStroke() {
        val fixture = GestureFixture()
        val unselected = fixture.originalStroke.copy(color = 0xFF00FF00L)
        fixture.strokes = fixture.strokes + unselected
        val start = Offset(bounds.center.x, bounds.top - 24f * density)
        val end = Offset(bounds.center.x + 224f * density, bounds.center.y)

        fixture.run(start, listOf(end), Ending.UP)

        val actual = fixture.strokes.first()
        assertEquals(fixture.originalStroke.copy(points = actual.points), actual)
        fixture.originalStroke.points.zip(actual.points).forEach { (before, after) ->
            assertEquals(bounds.center.x - (before.y - bounds.center.y), after.x, 0.001f)
            assertEquals(bounds.center.y + (before.x - bounds.center.x), after.y, 0.001f)
        }
        assertEquals(unselected, fixture.strokes.last())
        assertEquals(1, fixture.historyPushes)
        assertDragReset(fixture.drag)
    }

    @Test
    fun testHandleSnapshotIgnoresStaleSelectionIndices() {
        val fixture = GestureFixture().apply { selected = setOf(-1, 0, 20) }

        fixture.run(bounds.topLeft)

        assertEquals(mapOf(0 to fixture.originalStroke), fixture.drag.initialStrokes.value)
        assertEquals(0, fixture.drag.activeHandleIndex.value)
        assertEquals(1, fixture.historyPushes)
    }

    @Test
    fun testCancellingHandleDragClearsAllTransientState() {
        val fixture = GestureFixture()

        fixture.run(bounds.topLeft, ending = Ending.CANCEL)

        assertDragReset(fixture.drag)
        assertNull(fixture.marquee)
        assertEquals(setOf(0), fixture.selected)
        assertEquals(1, fixture.historyPushes)
    }

    @Test
    fun testCancellingMarqueeDoesNotApplySelection() {
        val fixture = GestureFixture()
        val start = Offset(50f * density, 50f * density)

        fixture.run(start, listOf(Offset(500f * density, 500f * density)), Ending.CANCEL)

        assertEquals(listOf(emptySet<Int>()), fixture.selectionChanges)
        assertNull(fixture.marquee)
        assertEquals(0, fixture.historyPushes)
        assertDragReset(fixture.drag)
    }

    private fun assertDragReset(drag: SelectionDragState) {
        assertFalse(drag.isDragging.value)
        assertNull(drag.activeHandleIndex.value)
        assertNull(drag.initialBounds.value)
        assertTrue(drag.initialStrokes.value.isEmpty())
        assertNull(drag.startPos.value)
    }

    private val bounds: Rect
        get() = Rect(200f * density, 200f * density, 600f * density, 600f * density)

    private inner class GestureFixture {
        val originalStroke = DrawingStroke(
            points = listOf(DrawingPoint(260f * density, 260f * density), DrawingPoint(300f * density, 300f * density)),
            color = 0xFF123456L,
            strokeWidth = 2f,
            alpha = 0.35f,
            isHighlighter = true
        )
        val drag = SelectionDragState()
        var strokes = listOf(originalStroke)
        var selected = setOf(0)
        var currentBounds: Rect? = bounds
        var marquee: Rect? = null
        var historyPushes = 0
        var touchDowns = 0
        val selectionChanges = mutableListOf<Set<Int>>()

        fun run(start: Offset, moves: List<Offset> = emptyList(), ending: Ending = Ending.PRESSED) {
            val events = pointerEvents(start, moves, ending)
            val scope = SelectionPointerScope(density, events)
            runBlocking {
                try {
                    scope.selectionGestures(
                        drag = drag,
                        strokes = { strokes },
                        selectedIndices = { selected },
                        bounds = { currentBounds },
                        marquee = { marquee },
                        stalkLengthPx = 24f * density,
                        onTouchDown = { touchDowns++ },
                        onStrokesChange = { strokes = it },
                        onSelectedIndicesChange = {
                            selected = it
                            selectionChanges += it
                        },
                        onMarqueeChange = { marquee = it },
                        onPushHistory = { historyPushes++ }
                    )
                } catch (_: EndOfPointerEvents) {
                    // Gesture detectors listen forever; exhaustion ends this finite input stream.
                }
            }
            assertEquals("The drag must start exactly once", 1, touchDowns)
            assertTrue("The drag event must be consumed", events[1].changes.single().isConsumed)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "density={0}")
        fun densities(): List<Array<Float>> = listOf(arrayOf(1f), arrayOf(1.5f), arrayOf(3f))
    }
}

private enum class Ending { PRESSED, UP, CANCEL }

private fun pointerEvents(start: Offset, moves: List<Offset>, ending: Ending): List<PointerEvent> {
    val positions = listOf(start - Offset(1f, 0f), start) + moves
    val events = positions.mapIndexed { index, position ->
        pointerEvent(position, positions.getOrElse(index - 1) { position }, index, pressed = true)
    }.toMutableList()
    var last = positions.last()
    if (ending == Ending.CANCEL) {
        val cancelled = pointerEvent(last + Offset(1f, 0f), last, events.size, pressed = true)
        cancelled.changes.single().consume()
        events += cancelled
        last += Offset(1f, 0f)
    }
    if (ending != Ending.PRESSED) {
        events += pointerEvent(last, last, events.size, pressed = false)
    }
    return events
}

private fun pointerEvent(position: Offset, previous: Offset, index: Int, pressed: Boolean): PointerEvent = PointerEvent(
    listOf(
        PointerInputChange(
            id = PointerId(0L),
            uptimeMillis = index * 16L,
            position = position,
            pressed = pressed,
            previousUptimeMillis = (index - 1).coerceAtLeast(0) * 16L,
            previousPosition = previous,
            previousPressed = index > 0,
            isInitiallyConsumed = false
        )
    )
)

/** Replays each event through Initial, Main and Final passes, as Compose dispatch does. */
private class SelectionPointerScope(
    override val density: Float,
    private val events: List<PointerEvent>
) : PointerInputScope, AwaitPointerEventScope {
    override val fontScale = 1f
    override val size = IntSize(10_000, 10_000)
    override val extendedTouchPadding = Size.Zero
    override val viewConfiguration = object : ViewConfiguration {
        override val longPressTimeoutMillis = 500L
        override val doubleTapTimeoutMillis = 300L
        override val doubleTapMinTimeMillis = 40L
        // One pixel of motion starts the drag without platform-specific touch slop.
        override val touchSlop = 0f
    }
    override var currentEvent = PointerEvent(emptyList())
        private set
    private var eventIndex = -1
    private var lastPass = PointerEventPass.Final

    override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R = block(this)

    override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent {
        if (pass.ordinal <= lastPass.ordinal) eventIndex++
        lastPass = pass
        currentEvent = events.getOrNull(eventIndex) ?: throw EndOfPointerEvents()
        return currentEvent
    }
}

private class EndOfPointerEvents : RuntimeException()
