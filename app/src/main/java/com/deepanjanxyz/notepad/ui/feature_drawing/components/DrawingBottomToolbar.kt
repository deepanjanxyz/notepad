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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.DrawingTool

@Composable
fun DockToolButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
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
            .testTag(testTag)
    ) {
        content()
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) Color(0xFF2196F3) else Color.Transparent)
        )
    }
}

@Composable
fun SelectionToolIcon(isSelected: Boolean) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val color = if (isSelected) Color(0xFF2196F3) else Color(0xFF8E9099)
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
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
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
    Canvas(modifier = Modifier.size(24.dp)) {
        val bodyColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF8E9099)
        val capColor = if (isSelected) Color.White else Color(0xFFC0C2CC)

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
    Canvas(modifier = Modifier.size(24.dp)) {
        val bodyColor = if (isSelected) Color.White else Color(0xFF8E9099)

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
    Canvas(modifier = Modifier.size(24.dp)) {
        val bodyColor = if (isSelected) Color.White else Color(0xFF8E9099)

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
    Canvas(modifier = Modifier.size(24.dp)) {
        val bodyColor = if (isSelected) Color.White else Color(0xFF8E9099)

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF131314))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        DockToolButton(
            isSelected = activeTool == DrawingTool.SELECT,
            onClick = { onToolClick(DrawingTool.SELECT) },
            testTag = "tool_select"
        ) {
            SelectionToolIcon(isSelected = activeTool == DrawingTool.SELECT)
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.ERASER,
            onClick = { onToolClick(DrawingTool.ERASER) },
            testTag = "tool_eraser"
        ) {
            EraserToolIcon(isSelected = activeTool == DrawingTool.ERASER)
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.PEN,
            onClick = { onToolClick(DrawingTool.PEN) },
            testTag = "tool_pen"
        ) {
            PenToolIcon(
                tipColor = Color(penColor),
                isSelected = activeTool == DrawingTool.PEN
            )
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.MARKER,
            onClick = { onToolClick(DrawingTool.MARKER) },
            testTag = "tool_marker"
        ) {
            MarkerToolIcon(
                tipColor = Color(markerColor),
                isSelected = activeTool == DrawingTool.MARKER
            )
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.HIGHLIGHTER,
            onClick = { onToolClick(DrawingTool.HIGHLIGHTER) },
            testTag = "tool_highlighter"
        ) {
            HighlighterToolIcon(
                tipColor = Color(highlighterColor),
                isSelected = activeTool == DrawingTool.HIGHLIGHTER
            )
        }
    }
}
