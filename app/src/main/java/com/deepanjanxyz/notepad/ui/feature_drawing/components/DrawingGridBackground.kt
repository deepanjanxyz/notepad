package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.CanvasGridType

fun DrawScope.drawCanvasGrid(gridType: CanvasGridType, isDarkBg: Boolean) {
    val lineColor = if (isDarkBg) Color(0x22FFFFFF) else Color(0x1F000000)
    val dotColor = if (isDarkBg) Color(0x33FFFFFF) else Color(0x33000000)
    when (gridType) {
        CanvasGridType.NONE -> {}
        CanvasGridType.RULED -> {
            val spacing = 36.dp.toPx()
            var y = spacing
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += spacing
            }
        }
        CanvasGridType.GRID -> {
            val spacing = 32.dp.toPx()
            var x = spacing
            while (x < size.width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += spacing
            }
            var y = spacing
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += spacing
            }
        }
        CanvasGridType.DOTS -> {
            val spacing = 28.dp.toPx()
            val dotRadius = 1.5.dp.toPx()
            var x = spacing
            while (x < size.width) {
                var y = spacing
                while (y < size.height) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += spacing
                }
                x += spacing
            }
        }
    }
}
