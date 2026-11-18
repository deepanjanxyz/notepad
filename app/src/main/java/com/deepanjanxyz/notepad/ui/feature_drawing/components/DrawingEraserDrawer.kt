@file:Suppress("MagicNumber")

package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.EraserMode
import com.deepanjanxyz.notepad.ui.theme.AppColors
import com.deepanjanxyz.notepad.ui.theme.Motion
import com.deepanjanxyz.notepad.ui.theme.Radii
import com.deepanjanxyz.notepad.ui.theme.Spacing
import kotlin.math.abs

// The three eraser sizes offered as presets, in pixels.
private const val ERASER_SMALL_RADIUS_PX = 18f
private const val ERASER_MEDIUM_RADIUS_PX = 32f
private const val ERASER_LARGE_RADIUS_PX = 48f
// How close a radius has to be to a preset to count as that preset.
private const val ERASER_PRESET_TOLERANCE = 5f

/**
 * The eraser's options: how it erases and how wide it is.
 *
 * Clearing the whole canvas used to sit here as a red button next to the size
 * presets, which put a destructive action in the middle of a frequently used
 * panel. It now lives in the top bar's overflow menu, so this panel only holds
 * the two settings that belong to the eraser.
 */
@Composable
fun DrawingEraserDrawer(
    currentMode: EraserMode,
    onModeSelected: (EraserMode) -> Unit,
    currentRadiusPx: Float,
    onRadiusChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.SheetSurfaceAlt,
        contentColor = AppColors.TextPrimary,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Eraser",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )

            EraserModeRow(currentMode = currentMode, onModeSelected = onModeSelected)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Size",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextLabel
                )
                EraserSizeRow(currentRadiusPx = currentRadiusPx, onRadiusChanged = onRadiusChanged)
            }
        }
    }
}

/** The Precision / Whole stroke pair of mode buttons. */
@Composable
private fun EraserModeRow(currentMode: EraserMode, onModeSelected: (EraserMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EraserModeChip(
            label = "Precision",
            isSelected = currentMode == EraserMode.SEGMENT,
            onClick = { onModeSelected(EraserMode.SEGMENT) }
        )
        EraserModeChip(
            label = "Whole stroke",
            isSelected = currentMode == EraserMode.STROKE,
            onClick = { onModeSelected(EraserMode.STROKE) }
        )
    }
}

@Composable
private fun RowScope.EraserModeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (isSelected) AppColors.Accent else AppColors.ControlIdle,
        animationSpec = tween(Motion.short),
        label = "eraser_mode_container"
    )

    Surface(
        shape = RoundedCornerShape(Radii.pill),
        color = container,
        border = BorderStroke(
            1.dp,
            if (isSelected) AppColors.AccentEdge else AppColors.ControlEdge
        ),
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
    ) {
        Box(
            modifier = Modifier.padding(vertical = Spacing.sm, horizontal = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else AppColors.TextIdle
            )
        }
    }
}

/** The three eraser size presets. */
@Composable
private fun EraserSizeRow(currentRadiusPx: Float, onRadiusChanged: (Float) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple(ERASER_SMALL_RADIUS_PX, "S", "Small eraser"),
            Triple(ERASER_MEDIUM_RADIUS_PX, "M", "Medium eraser"),
            Triple(ERASER_LARGE_RADIUS_PX, "L", "Large eraser")
        ).forEach { (rad, label, description) ->
            val isSelected = abs(currentRadiusPx - rad) < ERASER_PRESET_TOLERANCE
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
