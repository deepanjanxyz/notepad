package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.ui.theme.AppColors

/**
 * The tool dock along the bottom of the drawing screen.
 *
 * Every drawing tool stays visible in the dock: switching tools is the most
 * frequent action in a session, so none of them is pushed behind a menu. Each
 * button carries its name and its selected state, since the tool icons are drawn
 * as shapes rather than sourced from an icon font.
 */
@Composable
fun DrawingBottomToolbar(
    activeTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.DockSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockToolButton(
            isSelected = activeTool == DrawingTool.SELECT,
            onClick = { onToolSelected(DrawingTool.SELECT) },
            testTag = "tool_select",
            label = "Select"
        ) {
            SelectionToolIcon(isSelected = activeTool == DrawingTool.SELECT)
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.ERASER,
            onClick = { onToolSelected(DrawingTool.ERASER) },
            testTag = "tool_eraser",
            label = "Eraser"
        ) {
            EraserToolIcon(isSelected = activeTool == DrawingTool.ERASER)
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.PEN,
            onClick = { onToolSelected(DrawingTool.PEN) },
            testTag = "tool_pen",
            label = "Pen"
        ) {
            PenToolIcon(isSelected = activeTool == DrawingTool.PEN)
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.MARKER,
            onClick = { onToolSelected(DrawingTool.MARKER) },
            testTag = "tool_marker",
            label = "Marker"
        ) {
            MarkerToolIcon(isSelected = activeTool == DrawingTool.MARKER)
        }

        DockToolButton(
            isSelected = activeTool == DrawingTool.HIGHLIGHTER,
            onClick = { onToolSelected(DrawingTool.HIGHLIGHTER) },
            testTag = "tool_highlighter",
            label = "Highlighter"
        ) {
            HighlighterToolIcon(isSelected = activeTool == DrawingTool.HIGHLIGHTER)
        }
    }
}

/** One tool in the dock: its icon, plus the bar that marks the active tool. */
@Composable
fun DockToolButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(56.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                selected = isSelected
                role = Role.RadioButton
            }
            .testTag(testTag)
    ) {
        content()
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) AppColors.Accent else Color.Transparent)
        )
    }
}

/** Selection tool: a dashed marquee. */
@Composable
private fun SelectionToolIcon(isSelected: Boolean) {
    val tint = if (isSelected) AppColors.Accent else AppColors.DockIconIdle
    Text(
        text = "\u2b1a",
        style = TextStyle(fontSize = 20.sp, color = tint)
    )
}

/** Eraser tool: a filled rounded block. */
@Composable
private fun EraserToolIcon(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) AppColors.Accent else AppColors.DockIconIdle)
    )
}

/** Pen tool: a slim vertical stroke. */
@Composable
private fun PenToolIcon(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 4.dp, height = 22.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (isSelected) AppColors.Accent else AppColors.DockIconIdle)
    )
}

/** Marker tool: a broad block. */
@Composable
private fun MarkerToolIcon(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 14.dp, height = 22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) AppColors.Accent else AppColors.DockIconIdle)
    )
}

/** Highlighter tool: a wide, softly tinted block. */
@Composable
private fun HighlighterToolIcon(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (isSelected) AppColors.Accent else AppColors.DockIconIdle.copy(alpha = 0.55f)
            )
    )
}
