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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.ui.feature_drawing.model.KeepColorGrid
import com.deepanjanxyz.notepad.ui.feature_drawing.model.KeepThicknessLevels
import kotlin.math.roundToInt

@Composable
fun ColorCircleItem(
    color: Long,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val size = if (isSelected) 36.dp else 26.dp
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onSelect)
            .testTag("color_item_${color.toString(16)}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(color))
                .border(
                    width = if (color == 0xFFFFFFFFL) 1.dp else if (isSelected) 2.5.dp else 0.dp,
                    color = if (color == 0xFFFFFFFFL) Color(0xFF666666) else if (isSelected) Color(0xFF2196F3) else Color.Transparent,
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
    val toolTitle = when (activeTool) {
        DrawingTool.PEN -> "Pencil / Pen"
        DrawingTool.MARKER -> "Marker"
        DrawingTool.HIGHLIGHTER -> "Highlighter"
        else -> "Brush"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1F22))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tool header with size indicator and preview
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
                        color = Color(0xFF2196F3)
                    ),
                    modifier = Modifier.testTag("pencil_size_label")
                )
            }

            // Live stroke preview
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2B2C30)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(
                            width = 44.dp,
                            height = (selectedThickness.coerceIn(2f, 22f)).dp
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

        Spacer(modifier = Modifier.height(4.dp))

        // Interactive Pencil Size Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Size",
                style = TextStyle(fontSize = 12.sp, color = Color(0xFF9E9E9E)),
                modifier = Modifier.width(32.dp)
            )
            Slider(
                value = selectedThickness,
                onValueChange = { onThicknessSelected(it.roundToInt().toFloat()) },
                valueRange = 2f..54f,
                steps = 25,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2196F3),
                    activeTrackColor = Color(0xFF2196F3),
                    inactiveTrackColor = Color(0xFF424248)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pencil_size_slider")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 7 Quick Preset Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeepThicknessLevels.forEach { (thick, label) ->
                val isSelected = (selectedThickness - thick).let { it >= -2f && it <= 2f }
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
                        .testTag("thickness_option_${thick.toInt()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF2196F3), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(circleDp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2196F3))
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(circleDp)
                                .clip(CircleShape)
                                .background(Color(0xFFB0B0B0))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFF2C2D31), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // 4 rows of 7 colors
        KeepColorGrid.forEach { colorRow ->
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
}
