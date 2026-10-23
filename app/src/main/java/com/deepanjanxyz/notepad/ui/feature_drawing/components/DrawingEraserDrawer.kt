package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.EraserMode
import com.deepanjanxyz.notepad.ui.theme.AppColors
import kotlin.math.abs

// The three eraser sizes offered as presets, in pixels.
private const val ERASER_SMALL_RADIUS_PX = 18f
private const val ERASER_MEDIUM_RADIUS_PX = 32f
private const val ERASER_LARGE_RADIUS_PX = 48f
// How close a preset has to be to the current radius to count as the selected one.
private const val ERASER_RADIUS_MATCH_TOLERANCE_PX = 5f

/**
 * The eraser's own options: how it removes ink and how wide its tip is.
 *
 * Clearing the whole canvas is not offered here - it is a canvas-wide command that
 * lives in the top bar's overflow menu, so the eraser panel only holds eraser settings.
 */
@Composable
fun DrawingEraserDrawer(
    currentMode: EraserMode,
    onModeSelected: (EraserMode) -> Unit,
    currentRadiusPx: Float,
    onRadiusChanged: (Float) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EraserModeRow(currentMode = currentMode, onModeSelected = onModeSelected)
            EraserSizeRow(currentRadiusPx = currentRadiusPx, onRadiusChanged = onRadiusChanged)
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
            label = "Precision",
            contentDescription = "Precision eraser, erases where you touch",
            isSelected = currentMode == EraserMode.SEGMENT,
            testTag = "eraser_mode_precision",
            onClick = { onModeSelected(EraserMode.SEGMENT) }
        )
        EraserModeChip(
            label = "Whole stroke",
            contentDescription = "Whole stroke eraser, removes the entire line it touches",
            isSelected = currentMode == EraserMode.STROKE,
            testTag = "eraser_mode_stroke",
            onClick = { onModeSelected(EraserMode.STROKE) }
        )
    }
}

@Composable
private fun RowScope.EraserModeChip(
    label: String,
    contentDescription: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) AppColors.Accent else AppColors.ControlIdle,
        border = BorderStroke(
            1.dp,
            if (isSelected) AppColors.AccentEdge else AppColors.ControlEdge
        ),
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.selected = isSelected
                role = Role.RadioButton
            }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else AppColors.TextIdle
                ),
                modifier = Modifier.semantics { contentDescription = contentDescription }
            )
        }
    }
}

/** The three eraser size presets. */
@Composable
private fun EraserSizeRow(currentRadiusPx: Float, onRadiusChanged: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Size",
            style = TextStyle(fontSize = 12.sp, color = AppColors.TextLabel)
        )
        listOf(
            Triple(ERASER_SMALL_RADIUS_PX, "S", "Small eraser"),
            Triple(ERASER_MEDIUM_RADIUS_PX, "M", "Medium eraser"),
            Triple(ERASER_LARGE_RADIUS_PX, "L", "Large eraser")
        ).forEach { (radius, label, description) ->
            val isSelected =
                abs(currentRadiusPx - radius) < ERASER_RADIUS_MATCH_TOLERANCE_PX
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onRadiusChanged(radius) }
                    .semantics {
                        contentDescription = description
                        selected = isSelected
                        role = Role.RadioButton
                    }
                    .testTag("eraser_size_${label.lowercase()}"),
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
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
