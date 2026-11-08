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
import com.deepanjanxyz.notepad.ui.theme.AppColors
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.deepanjanxyz.notepad.domain.model.DrawingData
import com.deepanjanxyz.notepad.domain.model.DrawingStroke

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
            DrawingPreviewCanvas(drawingData = drawingData)

            // Controls on top-right of drawing preview
            DrawingPreviewControls(onClick = onClick, onDelete = onDelete)
        }
    }
}

/** The drawing itself, scaled to sit inside the card. */
@Composable
private fun DrawingPreviewCanvas(drawingData: DrawingData) {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        if (drawingData.strokes.isEmpty()) {
            return@Canvas
        }
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
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
            drawStrokePreview(stroke, scale, offsetX, offsetY)
        }
    }
}

/** Draws one stroke of a preview, as a dot when it has a single point. */
private fun DrawScope.drawStrokePreview(
    stroke: DrawingStroke,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
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

/** The edit and delete buttons that sit on top of the preview. */
@Composable
private fun BoxScope.DrawingPreviewControls(onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PreviewControlButton(
            icon = Icons.Default.Edit,
            contentDescription = "Edit drawing",
            onClick = onClick
        )
        PreviewControlButton(
            icon = Icons.Default.Close,
            contentDescription = "Delete drawing",
            onClick = onDelete
        )
    }
}

@Composable
private fun PreviewControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = AppColors.Scrim,
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
