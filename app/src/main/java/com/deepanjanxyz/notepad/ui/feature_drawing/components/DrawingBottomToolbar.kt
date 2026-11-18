@file:Suppress("MagicNumber")

package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.ui.theme.AppColors
import com.deepanjanxyz.notepad.ui.theme.Motion
import com.deepanjanxyz.notepad.ui.theme.Radii
import com.deepanjanxyz.notepad.ui.theme.Spacing

// Dash pattern of the guide line drawn across the canvas, in pixels.
private const val GUIDE_DASH_LENGTH_PX = 4f
private const val GUIDE_DASH_GAP_PX = 3f

// Size of one tool target in the dock.
private val TOOL_TARGET = 48.dp

/**
 * One tool in the floating dock.
 *
 * The selected tool sits on a filled pill and the others stay flat, so the
 * current tool is readable without reading the icons. The pill animates between
 * tools instead of appearing, which makes the change of tool feel like one
 * control moving rather than two redrawing.
 */
@Composable
fun DockToolButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    label: String,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (isSelected) AppColors.DockSelected else Color.Transparent,
        animationSpec = tween(Motion.short),
        label = "dock_tool_background"
    )
    val size by animateDpAsState(
        targetValue = if (isSelected) TOOL_TARGET else TOOL_TARGET - 4.dp,
        animationSpec = tween(Motion.short),
        label = "dock_tool_size"
    )

    Box(
        modifier = Modifier
            .size(TOOL_TARGET)
            .clip(RoundedCornerShape(Radii.sm))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(Radii.sm))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun SelectionToolIcon(isSelected: Boolean) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val color = if (isSelected) AppColors.Accent else AppColors.TextMuted
        val strokeW = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val pad = 3.dp.toPx()

        drawRect(
            color = color,
            topLeft = Offset(pad, pad),
            size = Size(w - pad * 2, h - pad * 2),
            style = Stroke(
                width = strokeW,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(GUIDE_DASH_LENGTH_PX, GUIDE_DASH_GAP_PX)
                )
            )
        )

        val arrowPath = Path().apply {
            moveTo(pad + 1f, h - pad - 6.dp.toPx())
            lineTo(pad + 1f, h - pad - 1f)
            lineTo(pad + 6.dp.toPx(), h - pad - 1f)
            moveTo(pad + 1f, h - pad - 1f)
            lineTo(pad + 5.dp.toPx(), h - pad - 5.dp.toPx())
        }
        drawPath(arrowPath, color, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}

@Composable
fun EraserToolIcon(isSelected: Boolean) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val bodyColor = if (isSelected) AppColors.Accent else AppColors.TextMuted
        val capColor = if (isSelected) Color.White else AppColors.IconIdleOnTool

        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.78f)
            lineTo(size.width * 0.38f, size.height * 0.90f)
            lineTo(size.width * 0.88f, size.height * 0.40f)
            lineTo(size.width * 0.65f, size.height * 0.20f)
            close()
        }
        drawPath(path, bodyColor)

        val divider = Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.84f)
            lineTo(size.width * 0.54f, size.height * 0.52f)
        }
        drawPath(divider, capColor, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun PenToolIcon(tipColor: Color, isSelected: Boolean) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val bodyColor = if (isSelected) Color.White else AppColors.TextMuted

        val body = Path().apply {
            moveTo(size.width * 0.45f, size.height * 0.42f)
            lineTo(size.width * 0.85f, size.height * 0.05f)
            lineTo(size.width * 0.95f, size.height * 0.15f)
            lineTo(size.width * 0.55f, size.height * 0.52f)
            close()
        }
        drawPath(body, bodyColor)

        val tip = Path().apply {
            moveTo(size.width * 0.45f, size.height * 0.42f)
            lineTo(size.width * 0.12f, size.height * 0.78f)
            lineTo(size.width * 0.08f, size.height * 0.92f)
            lineTo(size.width * 0.22f, size.height * 0.88f)
            lineTo(size.width * 0.55f, size.height * 0.52f)
            close()
        }
        drawPath(tip, tipColor)
    }
}

@Composable
fun MarkerToolIcon(tipColor: Color, isSelected: Boolean) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val bodyColor = if (isSelected) Color.White else AppColors.TextMuted

        val barrel = Path().apply {
            moveTo(size.width * 0.40f, size.height * 0.45f)
            lineTo(size.width * 0.80f, size.height * 0.05f)
            lineTo(size.width * 0.95f, size.height * 0.20f)
            lineTo(size.width * 0.55f, size.height * 0.60f)
            close()
        }
        drawPath(barrel, bodyColor)

        val tip = Path().apply {
            moveTo(size.width * 0.40f, size.height * 0.45f)
            lineTo(size.width * 0.22f, size.height * 0.65f)
            lineTo(size.width * 0.12f, size.height * 0.90f)
            lineTo(size.width * 0.35f, size.height * 0.78f)
            lineTo(size.width * 0.55f, size.height * 0.60f)
            close()
        }
        drawPath(tip, tipColor)
    }
}

@Composable
fun HighlighterToolIcon(tipColor: Color, isSelected: Boolean) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val bodyColor = if (isSelected) Color.White else AppColors.TextMuted

        val barrel = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.42f)
            lineTo(size.width * 0.78f, size.height * 0.02f)
            lineTo(size.width * 0.98f, size.height * 0.22f)
            lineTo(size.width * 0.58f, size.height * 0.62f)
            close()
        }
        drawPath(barrel, bodyColor)

        val tip = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.42f)
            lineTo(size.width * 0.15f, size.height * 0.65f)
            lineTo(size.width * 0.10f, size.height * 0.92f)
            lineTo(size.width * 0.38f, size.height * 0.82f)
            lineTo(size.width * 0.58f, size.height * 0.62f)
            close()
        }
        drawPath(tip, tipColor.copy(alpha = 0.85f))
    }
}

/**
 * The floating tool dock.
 *
 * It sits over the canvas rather than in a bar of its own, so the drawing area
 * keeps the full height of the screen. The five tools are always visible; the
 * colour swatch on the right shows the active brush's colour and opens its
 * palette, which is the one control that changes constantly while drawing.
 */
@Composable
fun DrawingBottomToolbar(
    activeTool: DrawingTool,
    onToolClick: (DrawingTool) -> Unit,
    selectedColor: Long,
    penColor: Long = selectedColor,
    markerColor: Long = selectedColor,
    highlighterColor: Long = selectedColor,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        shape = RoundedCornerShape(Radii.lg),
        color = AppColors.DockSurface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ToolDockButton(DrawingTool.SELECT, activeTool, "tool_select", "Select", onToolClick) {
                SelectionToolIcon(isSelected = activeTool == DrawingTool.SELECT)
            }
            ToolDockButton(DrawingTool.ERASER, activeTool, "tool_eraser", "Eraser", onToolClick) {
                EraserToolIcon(isSelected = activeTool == DrawingTool.ERASER)
            }
            ToolDockButton(DrawingTool.PEN, activeTool, "tool_pen", "Pen", onToolClick) {
                PenToolIcon(tipColor = Color(penColor), isSelected = activeTool == DrawingTool.PEN)
            }
            ToolDockButton(DrawingTool.MARKER, activeTool, "tool_marker", "Marker", onToolClick) {
                MarkerToolIcon(
                    tipColor = Color(markerColor),
                    isSelected = activeTool == DrawingTool.MARKER
                )
            }
            ToolDockButton(
                DrawingTool.HIGHLIGHTER, activeTool, "tool_highlighter", "Highlighter", onToolClick
            ) {
                HighlighterToolIcon(
                    tipColor = Color(highlighterColor),
                    isSelected = activeTool == DrawingTool.HIGHLIGHTER
                )
            }

            Spacer(modifier = Modifier.width(Spacing.xs))

            ColorSwatchButton(
                color = selectedColor,
                onClick = { onToolClick(activeTool) }
            )
        }
    }
}

/**
 * The current colour, as a round swatch that opens the palette.
 *
 * It shows the colour the active brush will draw with, so the palette does not
 * have to be open for the current colour to be known. Tapping it re-selects the
 * active tool, which is what opens that tool's options.
 */
@Composable
private fun ColorSwatchButton(color: Long, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(TOOL_TARGET)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "Colour and size" }
            .testTag("tool_color_swatch"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(2.dp, AppColors.DockEdge, CircleShape)
        )
    }
}

/** One dock button, wired to the tool it selects. */
@Composable
private fun ToolDockButton(
    tool: DrawingTool,
    activeTool: DrawingTool,
    testTag: String,
    label: String,
    onToolClick: (DrawingTool) -> Unit,
    icon: @Composable () -> Unit
) {
    DockToolButton(
        isSelected = activeTool == tool,
        onClick = { onToolClick(tool) },
        testTag = testTag,
        label = label,
        content = icon
    )
}
