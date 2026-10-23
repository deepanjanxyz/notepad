package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip

/**
 * The drawing screen's app bar.
 *
 * Only the actions a drawing session needs constantly stay on the bar: leaving the
 * screen, the title, and undo/redo. Canvas-level settings and the destructive
 * "clear" command sit in the overflow menu, so the bar stays quiet and the canvas
 * keeps the vertical space.
 */
@Composable
fun DrawingTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onNavigateBack: () -> Unit,
    onClearCanvas: () -> Unit,
    onOpenBackgroundDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val enabledTint = MaterialTheme.colorScheme.onSurface
    val disabledTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawingTopBarIcon(
            tooltip = "Back",
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            tint = enabledTint,
            testTag = "drawing_back_button",
            onClick = onNavigateBack
        )

        DrawingTitleField(
            title = title,
            onTitleChange = onTitleChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        DrawingTopBarIcon(
            tooltip = "Undo",
            icon = Icons.AutoMirrored.Filled.Undo,
            tint = if (canUndo) enabledTint else disabledTint,
            testTag = "drawing_undo_button",
            enabled = canUndo,
            onClick = onUndo
        )

        DrawingTopBarIcon(
            tooltip = "Redo",
            icon = Icons.AutoMirrored.Filled.Redo,
            tint = if (canRedo) enabledTint else disabledTint,
            testTag = "drawing_redo_button",
            enabled = canRedo,
            onClick = onRedo
        )

        DrawingOverflowMenu(
            canClearCanvas = canUndo,
            onOpenBackgroundDialog = onOpenBackgroundDialog,
            onClearCanvas = onClearCanvas
        )
    }
}

/** One action icon in the drawing top bar. */
@Composable
private fun DrawingTopBarIcon(
    tooltip: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ActionTooltip(tooltip) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint
            )
        }
    }
}

/** The drawing's title, with its placeholder. */
@Composable
private fun DrawingTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (title.isEmpty()) {
            Text(
                text = "Title",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            )
        }
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("drawing_title_input")
        )
    }
}

/**
 * The drawing top bar's overflow menu.
 *
 * It holds the canvas-level actions that do not belong on every session's path.
 * The grid pattern is chosen inside the canvas dialog rather than repeated here,
 * so each setting has exactly one place to change it.
 */
@Composable
private fun DrawingOverflowMenu(
    canClearCanvas: Boolean,
    onOpenBackgroundDialog: () -> Unit,
    onClearCanvas: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        DrawingTopBarIcon(
            tooltip = "Drawing options",
            icon = Icons.Default.MoreVert,
            tint = MaterialTheme.colorScheme.onSurface,
            testTag = "drawing_more_button",
            onClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Canvas background & grid") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    showMenu = false
                    onOpenBackgroundDialog()
                },
                modifier = Modifier.testTag("menu_canvas_background")
            )

            DropdownMenuItem(
                text = { Text("Clear canvas") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                enabled = canClearCanvas,
                onClick = {
                    showMenu = false
                    onClearCanvas()
                },
                modifier = Modifier.testTag("menu_clear_canvas")
            )
        }
    }
}
