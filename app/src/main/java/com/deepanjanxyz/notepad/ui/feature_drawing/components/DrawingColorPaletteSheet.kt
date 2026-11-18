@file:Suppress("MagicNumber")

package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.ui.feature_drawing.model.KeepColorGrid
import com.deepanjanxyz.notepad.ui.feature_drawing.model.KeepThicknessLevels
import com.deepanjanxyz.notepad.ui.theme.AppColors
import com.deepanjanxyz.notepad.ui.theme.Motion
import com.deepanjanxyz.notepad.ui.theme.Radii
import com.deepanjanxyz.notepad.ui.theme.Spacing
import kotlin.math.roundToInt

// Base of the hexadecimal colour written into the swatch test tag.
private const val HEX_RADIX = 16
// The shortest and longest the stroke preview is drawn, in dp.
private const val THICKNESS_MIN_DP = 2f
private const val THICKNESS_MAX_DP = 22f
// The range the thickness slider covers, in dp.
private const val THICKNESS_SLIDER_MIN_DP = 2f
private const val THICKNESS_SLIDER_MAX_DP = 54f
// How close a thickness has to be to a preset to count as that preset.
private const val PRESET_TOLERANCE = 2f

/**
 * One colour in the palette.
 *
 * The selected swatch grows and gains a ring rather than only changing colour,
 * so the current choice is visible even for a colour close to its neighbours.
 */
@Composable
fun ColorCircleItem(
    color: Long,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val size by animateDpAsState(
        targetValue = if (isSelected) 34.dp else 26.dp,
        animationSpec = tween(Motion.short),
        label = "swatch_size"
    )
    val ring by animateColorAsState(
        targetValue = when {
            isSelected -> AppColors.Accent
            color == AppColors.OPAQUE_WHITE -> AppColors.SwatchIdle
            else -> Color.Transparent
        },
        animationSpec = tween(Motion.short),
        label = "swatch_ring"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onSelect)
            .semantics {
                contentDescription = "Colour ${color.toString(HEX_RADIX)}"
                selected = isSelected
            }
            .testTag("color_item_${color.toString(HEX_RADIX)}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = ring,
                    shape = CircleShape
                )
        )
    }
}

/**
 * The palette for the active brush: its size, its quick presets and its colours.
 *
 * It opens above the dock as a panel rather than a modal sheet, so the canvas
 * stays visible and a colour can be judged against the drawing while it is
 * being picked.
 */
@Composable
fun DrawingColorPaletteSheet(
    activeTool: DrawingTool = DrawingTool.PEN,
    selectedColor: Long,
    selectedThickness: Float,
    onColorSelected: (Long) -> Unit,
    onThicknessSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SheetSurfaceAlt)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        ToolHeaderRow(
            toolTitle = toolTitleOf(activeTool),
            selectedThickness = selectedThickness,
            selectedColor = selectedColor,
            activeTool = activeTool
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        ThicknessSliderRow(
            selectedThickness = selectedThickness,
            onThicknessSelected = onThicknessSelected
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        ThicknessPresetRow(
            selectedThickness = selectedThickness,
            onThicknessSelected = onThicknessSelected
        )

        Spacer(modifier = Modifier.height(Spacing.sm))
        HorizontalDivider(color = AppColors.DividerLine, thickness = 1.dp)
        Spacer(modifier = Modifier.height(Spacing.sm))

        ColorGrid(selectedColor = selectedColor, onColorSelected = onColorSelected)
    }
}

/** The name shown for the tool whose colours are being picked. */
private fun toolTitleOf(activeTool: DrawingTool): String = when (activeTool) {
    DrawingTool.PEN -> "Pen"
    DrawingTool.MARKER -> "Marker"
    DrawingTool.HIGHLIGHTER -> "Highlighter"
    else -> "Brush"
}

/** The tool's name, its current size, and a preview of the stroke. */
@Composable
private fun ToolHeaderRow(
    toolTitle: String,
    selectedThickness: Float,
    selectedColor: Long,
    activeTool: DrawingTool
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = toolTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Text(
                text = "${selectedThickness.roundToInt()} px",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Accent,
                modifier = Modifier.testTag("pencil_size_label")
            )
        }

        StrokePreview(
            selectedThickness = selectedThickness,
            selectedColor = selectedColor,
            activeTool = activeTool
        )
    }
}

/** A small box showing what the current stroke looks like. */
@Composable
private fun StrokePreview(
    selectedThickness: Float,
    selectedColor: Long,
    activeTool: DrawingTool
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(Radii.xs))
            .background(AppColors.FieldSurface),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 46.dp,
                    height = previewStrokeHeightDp(selectedThickness).dp
                )
                .clip(CircleShape)
                .background(
                    Color(selectedColor).copy(
                        alpha = if (activeTool == DrawingTool.HIGHLIGHTER) 0.5f else 1f
                    )
                )
        )
    }
}

/**
 * The preview's stroke height for [thickness], scaled across the slider's whole
 * range so it keeps growing right up to the maximum instead of flattening out
 * once it passes the preview's own limit.
 */
private fun previewStrokeHeightDp(thickness: Float): Float {
    val ratio = ((thickness - THICKNESS_SLIDER_MIN_DP) /
        (THICKNESS_SLIDER_MAX_DP - THICKNESS_SLIDER_MIN_DP)).coerceIn(0f, 1f)
    return THICKNESS_MIN_DP + ratio * (THICKNESS_MAX_DP - THICKNESS_MIN_DP)
}

/** The slider that sets the stroke thickness. */
@Composable
private fun ThicknessSliderRow(
    selectedThickness: Float,
    onThicknessSelected: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "Size",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextLabel,
            modifier = Modifier.width(30.dp)
        )
        Slider(
            value = selectedThickness,
            onValueChange = { onThicknessSelected(it.roundToInt().toFloat()) },
            valueRange = THICKNESS_SLIDER_MIN_DP..THICKNESS_SLIDER_MAX_DP,
            steps = 25,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Accent,
                activeTrackColor = AppColors.Accent,
                inactiveTrackColor = AppColors.TrackIdle
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("pencil_size_slider")
        )
    }
}

/** The seven quick thickness presets. */
@Composable
private fun ThicknessPresetRow(
    selectedThickness: Float,
    onThicknessSelected: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeepThicknessLevels.forEach { (thick, _) ->
            val isSelected = (selectedThickness - thick) in -PRESET_TOLERANCE..PRESET_TOLERANCE
            ThicknessPresetDot(
                thick = thick,
                isSelected = isSelected,
                onThicknessSelected = onThicknessSelected
            )
        }
    }
}

/** One quick thickness preset, ringed when it is the selected one. */
@Composable
private fun ThicknessPresetDot(
    thick: Float,
    isSelected: Boolean,
    onThicknessSelected: (Float) -> Unit
) {
    val circleDp = when (thick) {
        3f -> 5.dp
        6f -> 8.dp
        10f -> 12.dp
        16f -> 16.dp
        24f -> 20.dp
        34f -> 24.dp
        else -> 28.dp
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable { onThicknessSelected(thick) }
            .semantics {
                contentDescription = "Stroke size ${thick.toInt()}"
                selected = isSelected
            }
            .testTag("thickness_option_${thick.toInt()}"),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(2.dp, AppColors.Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(circleDp)
                        .clip(CircleShape)
                        .background(AppColors.Accent)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(circleDp)
                    .clip(CircleShape)
                    .background(AppColors.DotIdleLight)
            )
        }
    }
}

/** The four rows of seven colours. */
@Composable
private fun ColorGrid(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    KeepColorGrid.forEach { colorRow ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorRow.forEach { c ->
                ColorCircleItem(
                    color = c,
                    isSelected = selectedColor == c,
                    onSelect = { onColorSelected(c) }
                )
            }
        }
    }
}
