package com.deepanjanxyz.notepad.ui.feature_drawing.components

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.ui.feature_drawing.model.ToolColorGrid
import com.deepanjanxyz.notepad.ui.feature_drawing.model.ThicknessLevels
import com.deepanjanxyz.notepad.ui.theme.AppColors
import kotlin.math.roundToInt

// Base of the hexadecimal colour written into the swatch test tag.
private const val HEX_RADIX = 16
// The range the selected stroke thickness is kept inside, in dp.
private const val THICKNESS_MIN_DP = 2f
private const val THICKNESS_MAX_DP = 22f
// The range the thickness slider covers, in dp.
private const val THICKNESS_SLIDER_MIN_DP = 2f
private const val THICKNESS_SLIDER_MAX_DP = 54f

@Composable
fun ColorCircleItem(
    color: Long,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val size = if (isSelected) 36.dp else 28.dp
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onSelect)
            .semantics {
                contentDescription = "Colour ${color.toString(HEX_RADIX)}"
                selected = isSelected
                role = Role.RadioButton
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
                    width = if (color == AppColors.OPAQUE_WHITE) 1.dp else if (isSelected) 2.5.dp else 0.dp,
                    color = when {
                        color == AppColors.OPAQUE_WHITE -> AppColors.SwatchIdle
                        isSelected -> AppColors.Accent
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                )
        )
    }
}

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
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tool header with size indicator and preview
        ToolHeaderRow(
            toolTitle = toolTitleOf(activeTool),
            selectedThickness = selectedThickness,
            selectedColor = selectedColor,
            activeTool = activeTool
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Interactive Pencil Size Slider
        ThicknessSliderRow(
            selectedThickness = selectedThickness,
            onThicknessSelected = onThicknessSelected
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 7 Quick Preset Dots
        ThicknessPresetRow(
            selectedThickness = selectedThickness,
            onThicknessSelected = onThicknessSelected
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = AppColors.DividerLine, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // 4 rows of 7 colors
        ColorGrid(selectedColor = selectedColor, onColorSelected = onColorSelected)
    }
}

/** The name shown for the tool whose colours are being picked. */
private fun toolTitleOf(activeTool: DrawingTool): String = when (activeTool) {
    DrawingTool.PEN -> "Pencil / Pen"
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = toolTitle,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Text(
                text = "${selectedThickness.roundToInt()} px",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.Accent
                ),
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
            .width(60.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.FieldSurface),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 44.dp,
                    height = (selectedThickness.coerceIn(THICKNESS_MIN_DP, THICKNESS_MAX_DP)).dp
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

/** The slider that sets the stroke thickness. */
@Composable
private fun ThicknessSliderRow(
    selectedThickness: Float,
    onThicknessSelected: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Size",
            style = TextStyle(fontSize = 12.sp, color = AppColors.TextFaint),
            modifier = Modifier.width(32.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThicknessLevels.forEach { (thick, _) ->
            val isSelected = (selectedThickness - thick).let { it >= -2f && it <= 2f }
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
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onThicknessSelected(thick) }
            .semantics {
                contentDescription = "Stroke width ${thick.roundToInt()} pixels"
                selected = isSelected
                role = Role.RadioButton
            }
            .testTag("thickness_option_${thick.toInt()}"),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
    ToolColorGrid.forEach { colorRow ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
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
