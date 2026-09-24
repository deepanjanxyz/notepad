package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun DrawScope.drawSelectionBoundingBox(b: Rect) {
    val blueColor = Color(0xFF2196F3)
    val handleSizePx = 9.dp.toPx()
    val halfH = handleSizePx / 2f

    // 1. Blue bounding outline
    drawRect(
        color = blueColor,
        topLeft = Offset(b.left, b.top),
        size = Size(b.width, b.height),
        style = Stroke(width = 2.dp.toPx())
    )

    // 2. Vertical top stalk line
    val stalkLength = 24.dp.toPx()
    val topMidX = b.center.x
    val stalkStartY = b.top
    val stalkEndY = b.top - stalkLength

    drawLine(
        color = blueColor,
        start = Offset(topMidX, stalkStartY),
        end = Offset(topMidX, stalkEndY),
        strokeWidth = 2.dp.toPx()
    )

    // 3. Top rotation circle handle
    drawCircle(
        color = blueColor,
        radius = 6.dp.toPx(),
        center = Offset(topMidX, stalkEndY)
    )

    // 4. 8 solid square handles (4 corners + 4 midpoints)
    val handles = listOf(
        Offset(b.left, b.top),           // 0: Top-Left
        Offset(b.center.x, b.top),       // 1: Top-Mid
        Offset(b.right, b.top),          // 2: Top-Right
        Offset(b.left, b.center.y),      // 3: Mid-Left
        Offset(b.right, b.center.y),     // 4: Mid-Right
        Offset(b.left, b.bottom),        // 5: Bottom-Left
        Offset(b.center.x, b.bottom),    // 6: Bottom-Mid
        Offset(b.right, b.bottom)        // 7: Bottom-Right
    )

    for (handle in handles) {
        drawRect(
            color = blueColor,
            topLeft = Offset(handle.x - halfH, handle.y - halfH),
            size = Size(handleSizePx, handleSizePx)
        )
    }
}

@Composable
fun FloatingSelectionBar(
    selectedCount: Int,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF26272E),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFF3E404C)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "$selectedCount selected",
                style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            IconButton(
                onClick = onDuplicate,
                modifier = Modifier.size(36.dp).testTag("drawing_duplicate_selected_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate selected",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp).testTag("drawing_delete_selected_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Deselect",
                    tint = Color(0xFF8E9099),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
