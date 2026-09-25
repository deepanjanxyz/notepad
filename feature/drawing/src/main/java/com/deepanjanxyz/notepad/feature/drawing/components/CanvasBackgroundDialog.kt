package com.deepanjanxyz.notepad.feature.drawing.components

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.core.model.CanvasGridType

val CanvasBackgroundOptions = listOf(
    0xFFFFFFFFL to "Pure White",
    0xFFFDFBF7L to "Warm Ivory",
    0xFFF0F4F8L to "Soft Slate",
    0xFF1E1E22L to "Dark Charcoal",
    0xFF121214L to "Midnight Black"
)

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
        containerColor = Color(0xFF202124),
        title = {
            Text(
                text = "Canvas Background & Grid",
                style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Background Color",
                    style = TextStyle(color = Color(0xFFB0B3BD), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CanvasBackgroundOptions.forEach { (colorVal, name) ->
                        BackgroundOptionCircle(
                            colorVal = colorVal,
                            isSelected = selectedColor == colorVal,
                            onClick = { onColorSelected(colorVal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grid Pattern",
                    style = TextStyle(color = Color(0xFFB0B3BD), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvasGridType.entries.forEach { grid ->
                        val isSelected = selectedGrid == grid
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2196F3) else Color(0xFF2C2C32),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onGridSelected(grid) }
                                .testTag("grid_option_${grid.name.lowercase()}")
                        ) {
                            Text(
                                text = when (grid) {
                                    CanvasGridType.NONE -> "Blank"
                                    CanvasGridType.RULED -> "Ruled"
                                    CanvasGridType.GRID -> "Grid"
                                    CanvasGridType.DOTS -> "Dots"
                                },
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun BackgroundOptionCircle(
    colorVal: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 36.dp else 28.dp)
                .clip(CircleShape)
                .background(Color(colorVal))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) Color(0xFF2196F3) else Color(0xFF666666),
                    shape = CircleShape
                )
        )
    }
}
