package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.CanvasGridType
import com.deepanjanxyz.notepad.ui.theme.AppColors

// The canvas background colours offered in the dialog, with their spoken names.
val CanvasBackgroundOptions = listOf(
    AppColors.OPAQUE_WHITE to "Pure white",
    0xFFFDFBF7L to "Warm ivory",
    0xFFF0F4F8L to "Soft slate",
    0xFF1E1E22L to "Dark charcoal",
    0xFF121214L to "Midnight black"
)

/**
 * The canvas appearance dialog: background colour and grid pattern.
 *
 * This is the single place a canvas pattern is chosen. Each option reports its
 * selected state, so the choice is not conveyed by colour alone.
 */
@Composable
fun CanvasBackgroundDialog(
    selectedColor: Long,
    selectedGrid: CanvasGridType,
    onColorSelected: (Long) -> Unit,
    onGridSelected: (CanvasGridType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.DialogSurface,
        title = {
            Text(
                text = "Canvas background & grid",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("Background colour")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CanvasBackgroundOptions.forEach { (colorValue, name) ->
                        BackgroundOptionCircle(
                            colorValue = colorValue,
                            name = name,
                            isSelected = selectedColor == colorValue,
                            onClick = { onColorSelected(colorValue) }
                        )
                    }
                }

                SectionLabel("Grid pattern")
                GridPatternRow(selectedGrid = selectedGrid, onGridSelected = onGridSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AppColors.Accent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/** A heading inside the dialog. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = AppColors.TextHint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    )
}

/** One background colour, ringed when it is the chosen one. */
@Composable
private fun BackgroundOptionCircle(
    colorValue: Long,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = name
                selected = isSelected
                role = Role.RadioButton
            }
            .testTag("canvas_color_${colorValue.toString(16)}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 38.dp else 30.dp)
                .clip(CircleShape)
                .background(Color(colorValue))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) AppColors.Accent else AppColors.SwatchIdle,
                    shape = CircleShape
                )
        )
    }
}

/** The four grid patterns, as a single-select row. */
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
            val label = grid.label()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) AppColors.Accent else AppColors.ChipSurface,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGridSelected(grid) }
                    .semantics {
                        contentDescription = "$label grid"
                        selected = isSelected
                        role = Role.RadioButton
                    }
                    .testTag("grid_option_${grid.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else AppColors.TextIdle,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

/** The user-facing name of a canvas pattern. */
fun CanvasGridType.label(): String = when (this) {
    CanvasGridType.NONE -> "Blank"
    CanvasGridType.RULED -> "Ruled"
    CanvasGridType.GRID -> "Grid"
    CanvasGridType.DOTS -> "Dots"
}
