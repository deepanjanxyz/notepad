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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.EraserMode
import kotlin.math.abs

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
        color = Color(0xFF1A1A1E),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Precision (Touch only)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (currentMode == EraserMode.SEGMENT) Color(0xFF2196F3) else Color(0xFF2A2A30),
                    border = BorderStroke(1.dp, if (currentMode == EraserMode.SEGMENT) Color(0xFF64B5F6) else Color(0xFF404048)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeSelected(EraserMode.SEGMENT) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Precision (Touch only)",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = if (currentMode == EraserMode.SEGMENT) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentMode == EraserMode.SEGMENT) Color.White else Color(0xFFCCCCCC)
                            )
                        )
                    }
                }

                // Whole Stroke
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (currentMode == EraserMode.STROKE) Color(0xFF2196F3) else Color(0xFF2A2A30),
                    border = BorderStroke(1.dp, if (currentMode == EraserMode.STROKE) Color(0xFF64B5F6) else Color(0xFF404048)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeSelected(EraserMode.STROKE) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Stroke (Whole line)",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = if (currentMode == EraserMode.STROKE) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentMode == EraserMode.STROKE) Color.White else Color(0xFFCCCCCC)
                            )
                        )
                    }
                }
            }

            // Eraser size presets & Clear Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size:",
                        style = TextStyle(fontSize = 12.sp, color = Color(0xFF9093A0))
                    )
                    listOf(18f to "S", 32f to "M", 48f to "L").forEach { (rad, label) ->
                        val isSel = abs(currentRadiusPx - rad) < 5f
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (isSel) Color(0xFF2196F3) else Color(0xFF2E2E36))
                                .clickable { onRadiusChanged(rad) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                }

                // Clear Canvas button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF352024),
                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onClearCanvas() }
                ) {
                    Text(
                        text = "Clear All",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350)),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
