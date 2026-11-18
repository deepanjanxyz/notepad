@file:Suppress("MagicNumber")

package com.deepanjanxyz.notepad.ui.feature_drawing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.CanvasGridType
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.theme.Radii
import com.deepanjanxyz.notepad.ui.theme.Spacing

// Spoken name of the drawing's title field.
private const val TITLE_CONTENT_DESCRIPTION = "Drawing title"

/**
 * The drawing screen's top bar.
 *
 * Only the actions used while drawing stay on the bar: back, the title, undo and
 * redo. Everything that changes the canvas itself - its background, its grid and
 * clearing it - lives in the overflow menu, so the bar stays quiet and the
 * destructive action is never one stray tap away.
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
    currentGridType: CanvasGridType,
    onGridTypeChange: (CanvasGridType) -> Unit,
    onOpenBackgroundDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val enabledTint = MaterialTheme.colorScheme.onSurface
    val disabledTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
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
                    .padding(horizontal = Spacing.sm)
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
                onOpenBackgroundDialog = onOpenBackgroundDialog,
                onClearCanvas = onClearCanvas,
                currentGridType = currentGridType,
                onGridTypeChange = onGridTypeChange
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                text = "Untitled drawing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = TITLE_CONTENT_DESCRIPTION }
                .testTag("drawing_title_input")
        )
    }
}

/**
 * The drawing top bar's overflow menu.
 *
 * The canvas settings live here rather than on the bar, so the bar keeps only
 * the actions that are used while drawing.
 */
@Composable
private fun DrawingOverflowMenu(
    onOpenBackgroundDialog: () -> Unit,
    onClearCanvas: () -> Unit,
    currentGridType: CanvasGridType,
    onGridTypeChange: (CanvasGridType) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        DrawingTopBarIcon(
            tooltip = "More options",
            icon = Icons.Default.MoreVert,
            tint = MaterialTheme.colorScheme.onSurface,
            testTag = "drawing_more_button",
            onClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(Radii.md)
        ) {
            MenuAction(
                label = "Canvas background",
                icon = Icons.Default.ColorLens,
                tint = MaterialTheme.colorScheme.primary,
                testTag = "menu_canvas_background"
            ) {
                showMenu = false
                onOpenBackgroundDialog()
            }

            GridTypeMenuItems(
                currentGridType = currentGridType,
                onGridTypeSelected = { gridType ->
                    onGridTypeChange(gridType)
                    showMenu = false
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.xs),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            MenuAction(
                label = "Clear canvas",
                icon = Icons.Default.DeleteOutline,
                tint = MaterialTheme.colorScheme.error,
                testTag = "menu_clear_canvas"
            ) {
                showMenu = false
                onClearCanvas()
            }
        }
    }
}

/** One row of the drawing top bar's overflow menu. */
@Composable
private fun MenuAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = tint) },
        onClick = onClick,
        modifier = Modifier.testTag(testTag)
    )
}

/** The "Background grid" heading and the grid choices under it. */
@Composable
private fun GridTypeMenuItems(
    currentGridType: CanvasGridType,
    onGridTypeSelected: (CanvasGridType) -> Unit
) {
    Text(
        text = "Background grid",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm)
    )

    CanvasGridType.entries.forEach { gridType ->
        DropdownMenuItem(
            text = { Text(gridType.label()) },
            trailingIcon = {
                if (currentGridType == gridType) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            onClick = { onGridTypeSelected(gridType) },
            modifier = Modifier.testTag("menu_grid_${gridType.name.lowercase()}")
        )
    }

    Spacer(modifier = Modifier.height(Spacing.xs))
}

/** The name shown for a grid pattern. */
internal fun CanvasGridType.label(): String = when (this) {
    CanvasGridType.NONE -> "Blank"
    CanvasGridType.RULED -> "Ruled"
    CanvasGridType.GRID -> "Grid"
    CanvasGridType.DOTS -> "Dots"
}
