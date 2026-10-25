package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.feature_drawing.model.KeepColorGrid
import com.deepanjanxyz.notepad.ui.theme.AppColors

// The swatch size and the gap between swatches in the recolor row, in dp.
private const val RECOLOR_SWATCH_DP = 30
private const val RECOLOR_SWATCH_GAP_DP = 8

// Base of the hexadecimal colour written into the swatch test tag.
private const val HEX_RADIX = 16

fun DrawScope.drawSelectionBoundingBox(b: Rect) {
    val blueColor = AppColors.Accent
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

/**
 * The bar shown while strokes are selected: how many are selected, and what can be
 * done with them. Tapping the palette action reveals the colours the selection can
 * be repainted with.
 */
@Composable
fun FloatingSelectionBar(
    selectedCount: Int,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onRecolor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRecolorRow by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AppColors.SelectionSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, AppColors.SelectionEdge),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectionActions(
                    selectedCount = selectedCount,
                    onToggleRecolor = { showRecolorRow = !showRecolorRow },
                    onSelectAll = onSelectAll,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    onClearSelection = onClearSelection
                )
            }

            if (showRecolorRow) {
                RecolorRow(
                    onRecolor = { color ->
                        onRecolor(color)
                        showRecolorRow = false
                    }
                )
            }
        }
    }
}

/** The selection count and the five actions shown along the bar. */
@Composable
private fun SelectionActions(
    selectedCount: Int,
    onToggleRecolor: () -> Unit,
    onSelectAll: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit
) {
    Text(
        text = "$selectedCount selected",
        style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    )
    SelectionAction(
        tooltip = "Select all",
        icon = Icons.Default.SelectAll,
        tint = AppColors.TextIdle,
        testTag = "drawing_select_all_button",
        onClick = onSelectAll
    )
    SelectionAction(
        tooltip = "Recolor selected",
        icon = Icons.Default.Palette,
        tint = AppColors.Accent,
        testTag = "drawing_recolor_selected_button",
        onClick = onToggleRecolor
    )
    SelectionAction(
        tooltip = "Duplicate selected",
        icon = Icons.Default.ContentCopy,
        tint = AppColors.Accent,
        testTag = "drawing_duplicate_selected_button",
        onClick = onDuplicate
    )
    SelectionAction(
        tooltip = "Delete selected",
        icon = Icons.Default.Delete,
        tint = AppColors.Danger,
        testTag = "drawing_delete_selected_button",
        onClick = onDelete
    )
    SelectionAction(
        tooltip = "Deselect",
        icon = Icons.Default.Close,
        tint = AppColors.TextMuted,
        testTag = "drawing_deselect_button",
        onClick = onClearSelection
    )
}

/** One action in the selection bar, at the 48dp touch target the guidelines ask for. */
@Composable
private fun SelectionAction(
    tooltip: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    ActionTooltip(tooltip) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** The colours the selected strokes can be repainted with. */
@Composable
private fun RecolorRow(onRecolor: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(RECOLOR_SWATCH_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeepColorGrid.flatten().forEach { color ->
            Box(
                modifier = Modifier
                    .size(RECOLOR_SWATCH_DP.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = 1.dp,
                        color = if (color == AppColors.OPAQUE_WHITE) {
                            AppColors.SwatchIdle
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .clickable { onRecolor(color) }
                    .testTag("recolor_swatch_${color.toString(HEX_RADIX)}")
            )
        }
    }
}
