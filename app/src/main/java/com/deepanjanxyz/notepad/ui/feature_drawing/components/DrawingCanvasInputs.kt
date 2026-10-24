package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.deepanjanxyz.notepad.domain.model.DrawingStroke

/**
 * The callbacks a drawing canvas raises. Grouped so the canvas can be handed one
 * value rather than seven.
 */
internal class DrawingCanvasCallbacks(
    val onStrokesChange: (List<DrawingStroke>) -> Unit,
    val onCurrentStrokeChange: (DrawingStroke?) -> Unit,
    val onSelectedIndicesChange: (Set<Int>) -> Unit,
    val onSelectionMarqueeChange: (Rect?) -> Unit,
    val onPushHistory: () -> Unit,
    val onEraseNear: (Offset, Offset?) -> Unit,
    val onCanvasTouchDown: () -> Unit = {},
    val onEraserCursorChange: (Offset?) -> Unit = {}
)

/**
 * What the canvas's gestures read: the strokes and selection as they stand, and
 * the callbacks. Held as one value so a gesture always sees the latest of each.
 */
internal class DrawingCanvasInputs(
    val strokes: List<DrawingStroke>,
    val selectedIndices: Set<Int>,
    val bounds: Rect?,
    val alpha: Float,
    val isHighlighter: Boolean,
    val eraserRadiusPx: Float,
    val eraserCursor: Offset?,
    val callbacks: DrawingCanvasCallbacks
)
