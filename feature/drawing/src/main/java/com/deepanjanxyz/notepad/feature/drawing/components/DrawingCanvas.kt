package com.deepanjanxyz.notepad.feature.drawing.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.core.model.CanvasGridType
import com.deepanjanxyz.notepad.core.model.DrawingPoint
import com.deepanjanxyz.notepad.core.model.DrawingStroke
import com.deepanjanxyz.notepad.core.model.DrawingTool
import com.deepanjanxyz.notepad.core.model.EraserMode
import com.deepanjanxyz.notepad.core.designsystem.drawing.drawDrawingStroke
import com.deepanjanxyz.notepad.core.designsystem.drawing.getHandleHit
import com.deepanjanxyz.notepad.core.designsystem.drawing.isMoveTargetHit
import com.deepanjanxyz.notepad.core.designsystem.drawing.isStrokeHit
import com.deepanjanxyz.notepad.core.designsystem.drawing.isStrokeInRect
import com.deepanjanxyz.notepad.core.designsystem.drawing.updateBoundsWithHandle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    currentStroke: DrawingStroke?,
    canvasBackgroundColor: Long,
    canvasGridType: CanvasGridType,
    activeTool: DrawingTool,
    eraserMode: EraserMode,
    eraserRadiusPx: Float,
    selectedStrokeIndices: Set<Int>,
    selectionBounds: Rect?,
    selectionMarquee: Rect?,
    currentStrokeWidth: Float,
    selectedColor: Long,
    currentAlpha: Float,
    isCurrentHighlighter: Boolean,
    onStrokesChange: (List<DrawingStroke>) -> Unit,
    onCurrentStrokeChange: (DrawingStroke?) -> Unit,
    onSelectedIndicesChange: (Set<Int>) -> Unit,
    onSelectionMarqueeChange: (Rect?) -> Unit,
    onPushHistory: () -> Unit,
    onEraseNear: (curPos: Offset, prevPos: Offset?) -> Unit,
    onCanvasTouchDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val stalkLengthPx = with(density) { 24.dp.toPx() }

    val currentOnCanvasTouchDown by rememberUpdatedState(onCanvasTouchDown)
    val currentStrokesState by rememberUpdatedState(strokes)
    val currentSelectedIndicesState by rememberUpdatedState(selectedStrokeIndices)
    val currentBoundsState by rememberUpdatedState(selectionBounds)
    val currentStrokeWidthState by rememberUpdatedState(currentStrokeWidth)
    val currentColorState by rememberUpdatedState(selectedColor)
    val currentAlphaState by rememberUpdatedState(currentAlpha)
    val isCurrentHighlighterState by rememberUpdatedState(isCurrentHighlighter)

    var isDraggingSelection by remember { mutableStateOf(false) }
    var activeHandleIndex by remember { mutableStateOf<Int?>(null) }
    var resizeInitialBounds by remember { mutableStateOf<Rect?>(null) }
    var resizeInitialStrokes by remember { mutableStateOf<Map<Int, DrawingStroke>>(emptyMap()) }
    var resizeStartPos by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    currentOnCanvasTouchDown()
                }
            }
            .pointerInput(activeTool, currentStrokeWidth, selectedColor) {
                if (activeTool == DrawingTool.SELECT) {
                    detectDragGestures(
                        onDragStart = { startPos ->
                            currentOnCanvasTouchDown()
                            val b = currentBoundsState
                            if (b != null) {
                                val handleHit = getHandleHit(b, startPos, 34f, stalkLengthPx)
                                if (handleHit != null) {
                                    activeHandleIndex = handleHit
                                    resizeInitialBounds = b
                                    resizeInitialStrokes = currentSelectedIndicesState.mapNotNull { idx ->
                                        currentStrokesState.getOrNull(idx)?.let { idx to it }
                                    }.toMap()
                                    resizeStartPos = startPos
                                    onPushHistory()
                                    return@detectDragGestures
                                }

                                if (isMoveTargetHit(b, startPos, 28f)) {
                                    isDraggingSelection = true
                                    onPushHistory()
                                    return@detectDragGestures
                                }
                            }

                            val hitSelected = currentSelectedIndicesState.any { idx ->
                                currentStrokesState.getOrNull(idx)?.let { isStrokeHit(it, startPos, 28f) } == true
                            }
                            if (hitSelected) {
                                isDraggingSelection = true
                                onPushHistory()
                                return@detectDragGestures
                            }

                            val hitIndex = currentStrokesState.indexOfLast { stroke ->
                                isStrokeHit(stroke, startPos, 28f)
                            }
                            if (hitIndex != -1) {
                                onSelectedIndicesChange(setOf(hitIndex))
                                isDraggingSelection = true
                                onPushHistory()
                            } else {
                                onSelectedIndicesChange(emptySet())
                                onSelectionMarqueeChange(Rect(startPos, Size(0f, 0f)))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val pos = change.position

                            if (isDraggingSelection) {
                                val updated = currentStrokesState.mapIndexed { idx, s ->
                                    if (currentSelectedIndicesState.contains(idx)) {
                                        s.copy(
                                            points = s.points.map { pt ->
                                                DrawingPoint(pt.x + dragAmount.x, pt.y + dragAmount.y)
                                            }
                                        )
                                    } else s
                                }
                                onStrokesChange(updated)
                            } else if (activeHandleIndex != null && resizeInitialBounds != null) {
                                val initB = resizeInitialBounds!!
                                val initStrokes = resizeInitialStrokes
                                if (activeHandleIndex == 8) {
                                    val center = initB.center
                                    val startAngle = atan2(resizeStartPos!!.y - center.y, resizeStartPos!!.x - center.x)
                                    val currentAngle = atan2(pos.y - center.y, pos.x - center.x)
                                    val angleDelta = currentAngle - startAngle
                                    val cosA = cos(angleDelta)
                                    val sinA = sin(angleDelta)
                                    val updated = currentStrokesState.mapIndexed { idx, s ->
                                        initStrokes[idx]?.let { initStroke ->
                                            initStroke.copy(
                                                points = initStroke.points.map { pt ->
                                                    val dx = pt.x - center.x
                                                    val dy = pt.y - center.y
                                                    val rx = center.x + (dx * cosA - dy * sinA)
                                                    val ry = center.y + (dx * sinA + dy * cosA)
                                                    DrawingPoint(rx, ry)
                                                }
                                            )
                                        } ?: s
                                    }
                                    onStrokesChange(updated)
                                } else {
                                    val newB = updateBoundsWithHandle(initB, activeHandleIndex!!, pos)
                                    if (initB.width > 2f && initB.height > 2f && newB.width > 2f && newB.height > 2f) {
                                        val scaleX = newB.width / initB.width
                                        val scaleY = newB.height / initB.height
                                        val updated = currentStrokesState.mapIndexed { idx, s ->
                                            initStrokes[idx]?.let { initStroke ->
                                                initStroke.copy(
                                                    points = initStroke.points.map { pt ->
                                                        val nx = newB.left + (pt.x - initB.left) * scaleX
                                                        val ny = newB.top + (pt.y - initB.top) * scaleY
                                                        DrawingPoint(nx, ny)
                                                    }
                                                )
                                            } ?: s
                                        }
                                        onStrokesChange(updated)
                                    }
                                }
                            } else if (selectionMarquee != null) {
                                val origStart = Offset(
                                    if (dragAmount.x < 0) selectionMarquee.right else selectionMarquee.left,
                                    if (dragAmount.y < 0) selectionMarquee.bottom else selectionMarquee.top
                                )
                                val left = minOf(origStart.x, pos.x)
                                val top = minOf(origStart.y, pos.y)
                                val right = maxOf(origStart.x, pos.x)
                                val bottom = maxOf(origStart.y, pos.y)
                                onSelectionMarqueeChange(Rect(left, top, right, bottom))
                            }
                        },
                        onDragEnd = {
                            selectionMarquee?.let { marquee ->
                                if (marquee.width > 8f || marquee.height > 8f) {
                                    val matched = currentStrokesState.indices.filter { idx ->
                                        isStrokeInRect(currentStrokesState[idx], marquee)
                                    }.toSet()
                                    onSelectedIndicesChange(matched)
                                }
                                onSelectionMarqueeChange(null)
                            }
                            isDraggingSelection = false
                            activeHandleIndex = null
                            resizeInitialBounds = null
                            resizeInitialStrokes = emptyMap()
                            resizeStartPos = null
                        },
                        onDragCancel = {
                            onSelectionMarqueeChange(null)
                            isDraggingSelection = false
                            activeHandleIndex = null
                            resizeInitialBounds = null
                            resizeInitialStrokes = emptyMap()
                            resizeStartPos = null
                        }
                    )
                } else {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        currentOnCanvasTouchDown()
                        down.consume()
                        val startOffset = down.position

                        if (activeTool == DrawingTool.ERASER) {
                            onPushHistory()
                            var lastEraserPos: Offset? = startOffset
                            onEraseNear(startOffset, null)
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()
                                val curPos = change.position
                                onEraseNear(curPos, lastEraserPos)
                                lastEraserPos = curPos
                            }
                        } else {
                            onPushHistory()
                            val strokeWidthToUse = currentStrokeWidthState
                            val colorToUse = currentColorState
                            val alphaToUse = currentAlphaState
                            val isHighlighterToUse = isCurrentHighlighterState

                            var activeStroke = DrawingStroke(
                                points = listOf(DrawingPoint(startOffset.x, startOffset.y)),
                                color = colorToUse,
                                strokeWidth = strokeWidthToUse,
                                alpha = alphaToUse,
                                isHighlighter = isHighlighterToUse
                            )
                            onCurrentStrokeChange(activeStroke)

                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()
                                val pos = change.position
                                val lastPt = activeStroke.points.lastOrNull()
                                if (lastPt == null || hypot(lastPt.x - pos.x, lastPt.y - pos.y) >= 1.8f) {
                                    activeStroke = activeStroke.copy(points = activeStroke.points + DrawingPoint(pos.x, pos.y))
                                    onCurrentStrokeChange(activeStroke)
                                }
                            }

                            if (activeStroke.points.isNotEmpty()) {
                                onStrokesChange(currentStrokesState + activeStroke)
                            }
                            onCurrentStrokeChange(null)
                        }
                    }
                }
            }
            .testTag("drawing_canvas")
    ) {
        val isDark = canvasBackgroundColor == 0xFF1E1E22L || canvasBackgroundColor == 0xFF121214L
        drawCanvasGrid(canvasGridType, isDark)

        for (stroke in strokes) {
            drawDrawingStroke(stroke)
        }

        currentStroke?.let { stroke ->
            drawDrawingStroke(stroke)
        }

        selectionMarquee?.let { marquee ->
            drawRect(
                color = Color(0x222196F3),
                topLeft = marquee.topLeft,
                size = marquee.size
            )
            drawRect(
                color = Color(0xFF2196F3),
                topLeft = marquee.topLeft,
                size = marquee.size,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )
        }

        selectionBounds?.let { b ->
            drawSelectionBoundingBox(b)
        }
    }
}
