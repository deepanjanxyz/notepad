package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputScope
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingStroke
import com.deepanjanxyz.notepad.ui.feature_drawing.util.getHandleHit
import com.deepanjanxyz.notepad.ui.feature_drawing.util.isMoveTargetHit
import com.deepanjanxyz.notepad.ui.feature_drawing.util.isStrokeHit
import com.deepanjanxyz.notepad.ui.feature_drawing.util.isStrokeInRect
import com.deepanjanxyz.notepad.ui.feature_drawing.util.updateBoundsWithHandle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// How close a touch has to be to a selection handle before it counts, in pixels.
private const val HANDLE_HIT_RADIUS_PX = 28f
// How far out a handle can be grabbed from, in pixels.
private const val HANDLE_GRAB_RADIUS_PX = 34f
// How close a touch has to be to a stroke to select it, in pixels.
private const val STROKE_HIT_RADIUS_PX = 28f
// The smallest marquee that counts as a selection, in pixels.
private const val MARQUEE_MIN_SIZE_PX = 8f
// The handle that rotates the selection rather than resizing it.
private const val ROTATE_HANDLE_INDEX = 8

/** What a drag on the selection is currently doing. */
internal class SelectionDragState {
    val isDragging: MutableState<Boolean> = mutableStateOf(false)
    val activeHandleIndex: MutableState<Int?> = mutableStateOf(null)
    val initialBounds: MutableState<Rect?> = mutableStateOf(null)
    val initialStrokes: MutableState<Map<Int, DrawingStroke>> = mutableStateOf(emptyMap())
    val startPos: MutableState<Offset?> = mutableStateOf(null)

    fun reset() {
        isDragging.value = false
        activeHandleIndex.value = null
        initialBounds.value = null
        initialStrokes.value = emptyMap()
        startPos.value = null
    }
}

/**
 * The drag gestures of the select tool: grabbing a handle to resize or rotate the
 * selection, dragging inside it to move it, or dragging on empty canvas to marquee.
 */
internal suspend fun PointerInputScope.selectionGestures(
    drag: SelectionDragState,
    strokes: () -> List<DrawingStroke>,
    selectedIndices: () -> Set<Int>,
    bounds: () -> Rect?,
    marquee: () -> Rect?,
    stalkLengthPx: Float,
    onTouchDown: () -> Unit,
    onStrokesChange: (List<DrawingStroke>) -> Unit,
    onSelectedIndicesChange: (Set<Int>) -> Unit,
    onMarqueeChange: (Rect?) -> Unit,
    onPushHistory: () -> Unit
) {
    detectDragGestures(
        onDragStart = { startPos ->
            handleSelectionDragStart(
                startPos = startPos,
                drag = drag,
                strokes = strokes,
                selectedIndices = selectedIndices,
                bounds = bounds,
                stalkLengthPx = stalkLengthPx,
                onTouchDown = onTouchDown,
                onSelectedIndicesChange = onSelectedIndicesChange,
                onMarqueeChange = onMarqueeChange,
                onPushHistory = onPushHistory
            )
        },
        onDrag = { change, dragAmount ->
            change.consume()
            applySelectionDrag(
                pos = change.position,
                dragAmount = dragAmount,
                drag = drag,
                strokes = strokes,
                selectedIndices = selectedIndices,
                marquee = marquee,
                onStrokesChange = onStrokesChange,
                onMarqueeChange = onMarqueeChange
            )
        },
        onDragEnd = {
            finishSelectionDrag(
                marquee = marquee(),
                strokes = strokes,
                drag = drag,
                onSelectedIndicesChange = onSelectedIndicesChange,
                onMarqueeChange = onMarqueeChange
            )
        },
        onDragCancel = { cancelSelectionDrag(drag = drag, onMarqueeChange = onMarqueeChange) }
    )
}

/** Works out what the press grabbed, and sets the drag up for it. */
private fun handleSelectionDragStart(
    startPos: Offset,
    drag: SelectionDragState,
    strokes: () -> List<DrawingStroke>,
    selectedIndices: () -> Set<Int>,
    bounds: () -> Rect?,
    stalkLengthPx: Float,
    onTouchDown: () -> Unit,
    onSelectedIndicesChange: (Set<Int>) -> Unit,
    onMarqueeChange: (Rect?) -> Unit,
    onPushHistory: () -> Unit
) {
    onTouchDown()
    val currentBounds = bounds()
    val handleHit = currentBounds?.let {
        getHandleHit(it, startPos, HANDLE_GRAB_RADIUS_PX, stalkLengthPx)
    }

    when {
        handleHit != null -> {
            drag.activeHandleIndex.value = handleHit
            drag.initialBounds.value = currentBounds
            drag.initialStrokes.value = selectedIndices().mapNotNull { idx ->
                strokes().getOrNull(idx)?.let { idx to it }
            }.toMap()
            drag.startPos.value = startPos
            onPushHistory()
        }

        currentBounds != null && isMoveTargetHit(currentBounds, startPos, HANDLE_HIT_RADIUS_PX) -> {
            drag.isDragging.value = true
            onPushHistory()
        }

        else -> {
            val hitSelected = selectedIndices().any { idx ->
                strokes().getOrNull(idx)
                    ?.let { isStrokeHit(it, startPos, STROKE_HIT_RADIUS_PX) } == true
            }
            if (hitSelected) {
                drag.isDragging.value = true
                onPushHistory()
            } else {
                val hitIndex = strokes().indexOfLast { stroke ->
                    isStrokeHit(stroke, startPos, STROKE_HIT_RADIUS_PX)
                }
                if (hitIndex != -1) {
                    onSelectedIndicesChange(setOf(hitIndex))
                    drag.isDragging.value = true
                    onPushHistory()
                } else {
                    onSelectedIndicesChange(emptySet())
                    onMarqueeChange(Rect(startPos, Size(0f, 0f)))
                }
            }
        }
    }
}

/** Applies one step of the drag: move, resize, rotate or extend the marquee. */
private fun applySelectionDrag(
    pos: Offset,
    dragAmount: Offset,
    drag: SelectionDragState,
    strokes: () -> List<DrawingStroke>,
    selectedIndices: () -> Set<Int>,
    marquee: () -> Rect?,
    onStrokesChange: (List<DrawingStroke>) -> Unit,
    onMarqueeChange: (Rect?) -> Unit
) {
    val initialBounds = drag.initialBounds.value
    val activeHandleIndex = drag.activeHandleIndex.value
    val currentMarquee = marquee()
    when {
        drag.isDragging.value -> onStrokesChange(
            moveSelectedStrokes(strokes(), selectedIndices(), dragAmount)
        )

        activeHandleIndex != null && initialBounds != null -> {
            resizeSelectedStrokes(pos, activeHandleIndex, initialBounds, drag, strokes())
                ?.let(onStrokesChange)
        }

        currentMarquee != null -> onMarqueeChange(marqueeFromDrag(currentMarquee, pos, dragAmount))
    }
}

/** Every selected stroke moved by the drag. */
private fun moveSelectedStrokes(
    strokes: List<DrawingStroke>,
    selectedIndices: Set<Int>,
    dragAmount: Offset
): List<DrawingStroke> = strokes.mapIndexed { idx, stroke ->
    if (selectedIndices.contains(idx)) {
        stroke.copy(
            points = stroke.points.map { pt ->
                DrawingPoint(pt.x + dragAmount.x, pt.y + dragAmount.y)
            }
        )
    } else {
        stroke
    }
}

/** Every selected stroke resized or rotated about the grabbed handle. */
private fun resizeSelectedStrokes(
    pos: Offset,
    activeHandleIndex: Int,
    initialBounds: Rect,
    drag: SelectionDragState,
    strokes: List<DrawingStroke>
): List<DrawingStroke>? {
    val initialStrokes = drag.initialStrokes.value
    return if (activeHandleIndex == ROTATE_HANDLE_INDEX) {
        rotateSelectedStrokes(pos, initialBounds, drag.startPos.value, initialStrokes, strokes)
    } else {
        scaleSelectedStrokes(pos, activeHandleIndex, initialBounds, initialStrokes, strokes)
    }
}

private fun rotateSelectedStrokes(
    pos: Offset,
    initialBounds: Rect,
    startPos: Offset?,
    initialStrokes: Map<Int, DrawingStroke>,
    strokes: List<DrawingStroke>
): List<DrawingStroke> {
    val center = initialBounds.center
    val startAngle = atan2(startPos!!.y - center.y, startPos.x - center.x)
    val currentAngle = atan2(pos.y - center.y, pos.x - center.x)
    val angleDelta = currentAngle - startAngle
    val cosA = cos(angleDelta)
    val sinA = sin(angleDelta)
    return strokes.mapIndexed { idx, stroke ->
        initialStrokes[idx]?.let { initialStroke ->
            initialStroke.copy(
                points = initialStroke.points.map { pt ->
                    val dx = pt.x - center.x
                    val dy = pt.y - center.y
                    val rx = center.x + (dx * cosA - dy * sinA)
                    val ry = center.y + (dx * sinA + dy * cosA)
                    DrawingPoint(rx, ry)
                }
            )
        } ?: stroke
    }
}

private fun scaleSelectedStrokes(
    pos: Offset,
    activeHandleIndex: Int,
    initialBounds: Rect,
    initialStrokes: Map<Int, DrawingStroke>,
    strokes: List<DrawingStroke>
): List<DrawingStroke>? {
    val newBounds = updateBoundsWithHandle(initialBounds, activeHandleIndex, pos)
    val hadRoomToResize = initialBounds.width > 2f && initialBounds.height > 2f
    if (!hadRoomToResize || newBounds.width <= 2f || newBounds.height <= 2f) {
        return null
    }
    val scaleX = newBounds.width / initialBounds.width
    val scaleY = newBounds.height / initialBounds.height
    return strokes.mapIndexed { idx, stroke ->
        initialStrokes[idx]?.let { initialStroke ->
            initialStroke.copy(
                points = initialStroke.points.map { pt ->
                    val nx = newBounds.left + (pt.x - initialBounds.left) * scaleX
                    val ny = newBounds.top + (pt.y - initialBounds.top) * scaleY
                    DrawingPoint(nx, ny)
                }
            )
        } ?: stroke
    }
}

/** Applies the marquee that was drawn, and ends the drag. */
private fun finishSelectionDrag(
    marquee: Rect?,
    strokes: () -> List<DrawingStroke>,
    drag: SelectionDragState,
    onSelectedIndicesChange: (Set<Int>) -> Unit,
    onMarqueeChange: (Rect?) -> Unit
) {
    marquee?.let { current ->
        if (current.width > MARQUEE_MIN_SIZE_PX || current.height > MARQUEE_MIN_SIZE_PX) {
            val currentStrokes = strokes()
            val matched = currentStrokes.indices.filter { idx ->
                isStrokeInRect(currentStrokes[idx], current)
            }.toSet()
            onSelectedIndicesChange(matched)
        }
        onMarqueeChange(null)
    }
    drag.reset()
}

/** Drops the marquee and ends the drag. */
private fun cancelSelectionDrag(drag: SelectionDragState, onMarqueeChange: (Rect?) -> Unit) {
    onMarqueeChange(null)
    drag.reset()
}

/** The marquee extended from the corner the drag started at to [pos]. */
private fun marqueeFromDrag(marquee: Rect, pos: Offset, dragAmount: Offset): Rect {
    val origStart = Offset(
        if (dragAmount.x < 0) marquee.right else marquee.left,
        if (dragAmount.y < 0) marquee.bottom else marquee.top
    )
    return Rect(
        minOf(origStart.x, pos.x),
        minOf(origStart.y, pos.y),
        maxOf(origStart.x, pos.x),
        maxOf(origStart.y, pos.y)
    )
}
