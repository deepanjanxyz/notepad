package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer

@Composable
fun EmbeddedDrawingCard(
    drawingPart: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawingData = remember(drawingPart) { DrawingSerializer.parse(drawingPart) }
    Surface(
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        color = Color(drawingData.backgroundColor),
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
            .clickable(onClick = onClick)
            .testTag("embedded_drawing_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (drawingData.strokes.isNotEmpty()) {
                    var minX = Float.MAX_VALUE
                    var minY = Float.MAX_VALUE
                    var maxX = Float.MIN_VALUE
                    var maxY = Float.MIN_VALUE
                    drawingData.strokes.forEach { s ->
                        s.points.forEach { pt ->
                            if (pt.x < minX) minX = pt.x
                            if (pt.y < minY) minY = pt.y
                            if (pt.x > maxX) maxX = pt.x
                            if (pt.y > maxY) maxY = pt.y
                        }
                    }
                    val strokeW = maxOf(1f, maxX - minX)
                    val strokeH = maxOf(1f, maxY - minY)
                    val scaleX = size.width / (strokeW + 40f)
                    val scaleY = size.height / (strokeH + 40f)
                    val scale = minOf(scaleX, scaleY, 1f)

                    val offsetX = (size.width - strokeW * scale) / 2f - minX * scale
                    val offsetY = (size.height - strokeH * scale) / 2f - minY * scale

                    drawingData.strokes.forEach { stroke ->
                        if (stroke.points.size == 1) {
                            val pt = stroke.points[0]
                            drawCircle(
                                color = Color(stroke.color).copy(alpha = stroke.alpha),
                                radius = (stroke.strokeWidth * scale) / 2f,
                                center = Offset(pt.x * scale + offsetX, pt.y * scale + offsetY)
                            )
                        } else if (stroke.points.size > 1) {
                            val path = Path()
                            val first = stroke.points[0]
                            path.moveTo(first.x * scale + offsetX, first.y * scale + offsetY)
                            for (i in 1 until stroke.points.size) {
                                val prev = stroke.points[i - 1]
                                val cur = stroke.points[i]
                                val midX = (prev.x + cur.x) / 2f * scale + offsetX
                                val midY = (prev.y + cur.y) / 2f * scale + offsetY
                                path.quadraticTo(prev.x * scale + offsetX, prev.y * scale + offsetY, midX, midY)
                            }
                            val last = stroke.points.last()
                            path.lineTo(last.x * scale + offsetX, last.y * scale + offsetY)
                            drawPath(
                                path = path,
                                color = Color(stroke.color).copy(alpha = stroke.alpha),
                                style = Stroke(
                                    width = maxOf(1f, stroke.strokeWidth * scale),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            // Controls on top-right of drawing preview
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit drawing",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onDelete)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete drawing",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
