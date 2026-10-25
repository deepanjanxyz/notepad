package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.EraserMode
import com.deepanjanxyz.notepad.ui.theme.AppColors
import kotlin.math.abs
import androidx.compose.foundation.layout.RowScope

// The three eraser sizes offered as presets, in pixels.
private const val ERASER_SMALL_RADIUS_PX = 18f
private const val ERASER_MEDIUM_RADIUS_PX = 32f
private const val ERASER_LARGE_RADIUS_PX = 48f

@Composable
fun DrawingEraserDrawer(
    currentMode: EraserMode,
    onModeSelected: (EraserMode) -> Unit,
    currentRadiusPx: Float,
    onRadiusChanged: (Float) -> Unit,
    onClearCanvas: () -> Unit
) = EraserOptionsDrawer(currentMode, onModeSelected, currentRadiusPx, onRadiusChanged, onClearCanvas)

@Composable
fun EraserOptionsDrawer(
    currentMode: EraserMode,
    onModeSelected: (EraserMode) -> Unit,
    currentRadiusPx: Float,
    onRadiusChanged: (Float) -> Unit,
    onClearCanvas: () -> Unit
) {
    Surface(
        color = AppColors.Panel,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Eraser Mode selection: Precision (touch only) vs Whole Stroke
            EraserModeRow(currentMode = currentMode, onModeSelected = onModeSelected)

            // Eraser size presets & Clear Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EraserSizeRow(currentRadiusPx = currentRadiusPx, onRadiusChanged = onRadiusChanged)

                // Clear Canvas button
                ClearCanvasButton(onClearCanvas = onClearCanvas)
            }
        }
    }
}

/** The Precision / Whole Stroke pair of mode buttons. */
@Composable
private fun EraserModeRow(currentMode: EraserMode, onModeSelected: (EraserMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EraserModeChip(
            label = "Precision (Touch only)",
            isSelected = currentMode == EraserMode.SEGMENT,
            onClick = { onModeSelected(EraserMode.SEGMENT) }
        )
        EraserModeChip(
            label = "Stroke (Whole line)",
            isSelected = currentMode == EraserMode.STROKE,
            onClick = { onModeSelected(EraserMode.STROKE) }
        )
    }
}

@Composable
private fun RowScope.EraserModeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) AppColors.Accent else AppColors.ControlIdle,
        border = BorderStroke(
            1.dp,
            if (isSelected) AppColors.AccentEdge else AppColors.ControlEdge
        ),
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else AppColors.TextIdle
                )
            )
        }
    }
}

/** The three eraser size presets. */
@Composable
private fun EraserSizeRow(currentRadiusPx: Float, onRadiusChanged: (Float) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Size:",
            style = TextStyle(fontSize = 12.sp, color = AppColors.TextLabel)
        )
        listOf(
            Triple(ERASER_SMALL_RADIUS_PX, "S", "Small eraser"),
            Triple(ERASER_MEDIUM_RADIUS_PX, "M", "Medium eraser"),
            Triple(ERASER_LARGE_RADIUS_PX, "L", "Large eraser")
        ).forEach { (rad, label, description) ->
            val isSelected = abs(currentRadiusPx - rad) < 5f
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onRadiusChanged(rad) }
                    .semantics {
                        contentDescription = description
                        selected = isSelected
                    }
                    .testTag("eraser_size_$label"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AppColors.Accent else AppColors.DotIdle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }
}

/** The destructive "clear the whole canvas" button. */
@Composable
private fun ClearCanvasButton(onClearCanvas: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppColors.DangerSurface,
        border = BorderStroke(1.dp, AppColors.Danger.copy(alpha = 0.4f)),
        modifier = Modifier
            .clickable { onClearCanvas() }
            .testTag("eraser_clear_all_button")
    ) {
        Text(
            text = "Clear All",
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Danger),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
