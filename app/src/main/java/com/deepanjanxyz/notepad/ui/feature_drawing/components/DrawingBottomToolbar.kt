package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.ui.theme.AppColors

// Dash pattern of the guide line drawn across the canvas, in pixels.
private const val GUIDE_DASH_LENGTH_PX = 4f
private const val GUIDE_DASH_GAP_PX = 3f

// The box every tool icon is drawn in, and the single stroke weight they share.
private const val TOOL_ICON_SIZE_DP = 24f
private const val TOOL_ICON_STROKE_DP = 1.8f

/** The nib that tells the pen, the marker and the highlighter apart. */
private enum class NibShape { PEN, MARKER, HIGHLIGHTER }


@Composable
fun DockToolButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(54.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .testTag(testTag)
    ) {
        content()
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) AppColors.Accent else Color.Transparent)
        )
    }
}

@Composable
fun SelectionToolIcon(isSelected: Boolean) {
    Canvas(modifier = Modifier.size(TOOL_ICON_SIZE_DP.dp)) {
        val color = if (isSelected) AppColors.Accent else AppColors.TextMuted
        val strokePx = TOOL_ICON_STROKE_DP.dp.toPx()
        val inset = size.width * 0.14f
        val bracket = size.width * 0.20f

        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius(size.width * 0.12f),
            style = Stroke(
                width = strokePx,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(GUIDE_DASH_LENGTH_PX, GUIDE_DASH_GAP_PX)
                )
            )
        )

        // Solid corner brackets over the dashed frame read as a marquee rather than a box.
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        val brackets = listOf(
            listOf(Offset(left, top + bracket), Offset(left, top), Offset(left + bracket, top)),
            listOf(Offset(right - bracket, top), Offset(right, top), Offset(right, top + bracket)),
            listOf(Offset(left, bottom - bracket), Offset(left, bottom), Offset(left + bracket, bottom)),
            listOf(Offset(right - bracket, bottom), Offset(right, bottom), Offset(right, bottom - bracket))
        )
        for (points in brackets) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                lineTo(points[1].x, points[1].y)
                lineTo(points[2].x, points[2].y)
            }
            drawPath(
                path,
                color,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun EraserToolIcon(isSelected: Boolean) {
    Canvas(modifier = Modifier.size(TOOL_ICON_SIZE_DP.dp)) {
        val color = if (isSelected) AppColors.Accent else AppColors.TextMuted
        val strokePx = TOOL_ICON_STROKE_DP.dp.toPx()

        rotate(degrees = 45f, pivot = center) {
            val bodyWidth = size.width * 0.56f
            val bodyHeight = size.height * 0.34f
            val bodyLeft = center.x - bodyWidth / 2f
            val bodyTop = center.y - bodyHeight / 2f
            val corner = CornerRadius(bodyHeight * 0.32f)

            drawRoundRect(
                color = color,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = corner,
                style = Stroke(width = strokePx)
            )
            // The worn end of the eraser.
            drawRoundRect(
                color = color,
                topLeft = Offset(bodyLeft + strokePx, bodyTop + bodyHeight * 0.60f),
                size = Size(bodyWidth - strokePx * 2f, bodyHeight * 0.24f),
                cornerRadius = CornerRadius(bodyHeight * 0.12f)
            )
        }
    }
}

@Composable
fun PenToolIcon(tipColor: Color, isSelected: Boolean) {
    NibToolIcon(tipColor = tipColor, isSelected = isSelected, shape = NibShape.PEN)
}

@Composable
fun MarkerToolIcon(tipColor: Color, isSelected: Boolean) {
    NibToolIcon(tipColor = tipColor, isSelected = isSelected, shape = NibShape.MARKER)
}

@Composable
fun HighlighterToolIcon(tipColor: Color, isSelected: Boolean) {
    NibToolIcon(tipColor = tipColor, isSelected = isSelected, shape = NibShape.HIGHLIGHTER)
}

/**
 * The pen, marker and highlighter share one barrel and differ only in the nib they
 * draw, so the three read as a family at toolbar size instead of as three drawings.
 */
@Composable
private fun NibToolIcon(tipColor: Color, isSelected: Boolean, shape: NibShape) {
    Canvas(modifier = Modifier.size(TOOL_ICON_SIZE_DP.dp)) {
        val bodyColor = if (isSelected) Color.White else AppColors.TextMuted
        val strokePx = TOOL_ICON_STROKE_DP.dp.toPx()

        rotate(degrees = -45f, pivot = center) {
            val barrelWidth = size.width * 0.38f
            val barrelHeight = size.height * 0.62f
            val barrelLeft = center.x - barrelWidth / 2f
            val barrelTop = center.y - barrelHeight / 2f - size.height * 0.10f

            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(barrelLeft, barrelTop),
                size = Size(barrelWidth, barrelHeight),
                cornerRadius = CornerRadius(barrelWidth * 0.34f),
                style = Stroke(width = strokePx)
            )

            val nibTop = barrelTop + barrelHeight
            val nib = when (shape) {
                NibShape.PEN -> Path().apply {
                    moveTo(barrelLeft + barrelWidth * 0.20f, nibTop)
                    lineTo(barrelLeft + barrelWidth * 0.80f, nibTop)
                    lineTo(center.x, nibTop + size.height * 0.24f)
                    close()
                }
                NibShape.MARKER -> Path().apply {
                    moveTo(barrelLeft + barrelWidth * 0.12f, nibTop)
                    lineTo(barrelLeft + barrelWidth * 0.88f, nibTop)
                    lineTo(barrelLeft + barrelWidth * 0.72f, nibTop + size.height * 0.17f)
                    lineTo(barrelLeft + barrelWidth * 0.28f, nibTop + size.height * 0.17f)
                    close()
                }
                NibShape.HIGHLIGHTER -> Path().apply {
                    moveTo(barrelLeft - barrelWidth * 0.08f, nibTop)
                    lineTo(barrelLeft + barrelWidth * 1.08f, nibTop)
                    lineTo(barrelLeft + barrelWidth * 0.86f, nibTop + size.height * 0.13f)
                    lineTo(barrelLeft + barrelWidth * 0.14f, nibTop + size.height * 0.13f)
                    close()
                }
            }
            drawPath(
                nib,
                if (shape == NibShape.HIGHLIGHTER) tipColor.copy(alpha = 0.85f) else tipColor
            )
        }
    }
}

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
    ToolRow(
        activeTool = activeTool,
        onToolClick = onToolClick,
        penColor = penColor,
        markerColor = markerColor,
        highlighterColor = highlighterColor,
        modifier = modifier
    )
}

/** The row of tool buttons the toolbar draws, one per drawing tool. */
@Composable
private fun ToolRow(
    activeTool: DrawingTool,
    onToolClick: (DrawingTool) -> Unit,
    penColor: Long,
    markerColor: Long,
    highlighterColor: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(AppColors.PanelDeep)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
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
            MarkerToolIcon(tipColor = Color(markerColor), isSelected = activeTool == DrawingTool.MARKER)
        }
        ToolDockButton(DrawingTool.HIGHLIGHTER, activeTool, "tool_highlighter", "Highlighter", onToolClick) {
            HighlighterToolIcon(tipColor = Color(highlighterColor), isSelected = activeTool == DrawingTool.HIGHLIGHTER)
        }
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
