package com.deepanjanxyz.notepad.ui.feature_drawing.components

import com.deepanjanxyz.notepad.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.CanvasGridType
import com.deepanjanxyz.notepad.ui.theme.AppColors

// Base of the hexadecimal colour written into the swatch test tag.
private const val HEX_RADIX = 16

@Composable
fun CanvasBackgroundDialog(
    selectedColor: Long,
    selectedGrid: CanvasGridType,
    onColorSelected: (Long) -> Unit,
    onGridSelected: (CanvasGridType) -> Unit,
    onDismiss: () -> Unit
) {
    val canvasBackgroundOptions = listOf(
        AppColors.OPAQUE_WHITE to stringResource(R.string.canvas_pure_white),
        0xFFFDFBF7L to stringResource(R.string.canvas_warm_ivory),
        0xFFF0F4F8L to stringResource(R.string.canvas_soft_slate),
        0xFF1E1E22L to stringResource(R.string.canvas_dark_charcoal),
        0xFF121214L to stringResource(R.string.canvas_midnight_black)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.DialogSurface,
        title = {
            Text(
                text = stringResource(R.string.drawing_bg_grid),
                style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.canvas_bg_color),
                    style = TextStyle(color = AppColors.TextHint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    canvasBackgroundOptions.forEach { (colorVal, name) ->
                        BackgroundOptionCircle(
                            colorVal = colorVal,
                            name = name,
                            isSelected = selectedColor == colorVal,
                            onClick = { onColorSelected(colorVal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.canvas_grid_pattern),
                    style = TextStyle(color = AppColors.TextHint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
                GridPatternRow(selectedGrid = selectedGrid, onGridSelected = onGridSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.drawing_done), color = AppColors.Accent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun BackgroundOptionCircle(
    colorVal: Long,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = name
                selected = isSelected
            }
            .testTag("background_option_${colorVal.toString(HEX_RADIX)}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 36.dp else 28.dp)
                .clip(CircleShape)
                .background(Color(colorVal))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) AppColors.Accent else AppColors.SwatchIdle,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun GridPatternRow(
    selectedGrid: CanvasGridType,
    onGridSelected: (CanvasGridType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CanvasGridType.entries.forEach { grid ->
            val isSelected = selectedGrid == grid
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) AppColors.Accent else AppColors.ChipSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onGridSelected(grid) }
                    .semantics { selected = isSelected }
                    .testTag("grid_option_${grid.name.lowercase()}")
            ) {
                Text(
                    text = when (grid) {
                        CanvasGridType.NONE -> stringResource(R.string.drawing_blank)
                        CanvasGridType.RULED -> stringResource(R.string.drawing_ruled)
                        CanvasGridType.GRID -> stringResource(R.string.drawing_grid)
                        CanvasGridType.DOTS -> stringResource(R.string.drawing_dots)
                    },
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else AppColors.TextIdle,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}
