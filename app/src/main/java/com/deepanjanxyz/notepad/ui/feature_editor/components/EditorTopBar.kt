package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.theme.AppColors
import com.deepanjanxyz.notepad.ui.theme.Radii
import com.deepanjanxyz.notepad.ui.theme.Spacing

/**
 * The editor's top bar.
 *
 * The bar keeps the actions that are used while writing - back, pin, reminder,
 * the format menu, the drawing and the done button. The three that are set once
 * per note - the colour tint, sharing and archiving - sit in the overflow menu,
 * which leaves the bar readable on a narrow screen instead of squeezing eight
 * icons into one row.
 */
@Composable
fun EditorTopBar(
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    hasReminder: Boolean,
    onOpenReminder: () -> Unit,
    editorFormat: EditorFormat,
    onFormatSelected: (EditorFormat) -> Unit,
    hasColor: Boolean,
    onOpenColorPicker: () -> Unit,
    hasDrawing: Boolean,
    onOpenDrawing: () -> Unit,
    onArchive: () -> Unit,
    onShare: () -> Unit,
    onSaveAndClose: () -> Unit,
    onNavigateBack: () -> Unit,
    textPrimary: Color = AppColors.TextPrimary,
    amberAccent: Color = AppColors.AccentAmber,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarAction(
            "Back", Icons.AutoMirrored.Filled.ArrowBack,
            textPrimary, "editor_back_button", onNavigateBack
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            TopBarAction(
                if (isPinned) "Unpin note" else "Pin note",
                if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                if (isPinned) amberAccent else textPrimary, "toggle_pin_button", onTogglePin
            )
            TopBarAction(
                if (hasReminder) "Edit reminder" else "Add reminder",
                if (hasReminder) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                if (hasReminder) amberAccent else textPrimary, "toggle_reminder_button", onOpenReminder
            )
            EditorFormatMenu(
                currentFormat = editorFormat,
                onFormatSelected = onFormatSelected
            )
            TopBarAction(
                "Drawing", Icons.Filled.Brush,
                if (hasDrawing) amberAccent else textPrimary, "open_drawing_button", onOpenDrawing
            )

            EditorOverflowMenu(
                hasColor = hasColor,
                amberAccent = amberAccent,
                textPrimary = textPrimary,
                onOpenColorPicker = onOpenColorPicker,
                onShare = onShare,
                onArchive = onArchive
            )

            Spacer(modifier = Modifier.width(Spacing.xs))

            EditorSaveButton(amberAccent = amberAccent, onSaveAndClose = onSaveAndClose)
        }
    }
}

/**
 * The editor's overflow menu: the actions that are set once per note.
 *
 * Each row carries its icon and its name, so the menu explains itself without
 * the user having to recognise an icon first.
 */
@Composable
private fun EditorOverflowMenu(
    hasColor: Boolean,
    amberAccent: Color,
    textPrimary: Color,
    onOpenColorPicker: () -> Unit,
    onShare: () -> Unit,
    onArchive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        TopBarAction(
            "More options", Icons.Default.MoreVert,
            textPrimary, "editor_more_button", { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(Radii.md)
        ) {
            EditorMenuItem(
                label = "Note colour",
                icon = Icons.Default.Palette,
                tint = if (hasColor) amberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "toggle_color_picker_button"
            ) {
                showMenu = false
                onOpenColorPicker()
            }

            EditorMenuItem(
                label = "Share",
                icon = Icons.Default.Share,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "share_note_button"
            ) {
                showMenu = false
                onShare()
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.xs),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            EditorMenuItem(
                label = "Archive",
                icon = Icons.Default.Archive,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                testTag = "archive_note_button"
            ) {
                showMenu = false
                onArchive()
            }
        }
    }
}

/** One row of the editor's overflow menu. */
@Composable
private fun EditorMenuItem(
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

/** The circular done button that saves and closes the editor. */
@Composable
private fun EditorSaveButton(amberAccent: Color, onSaveAndClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(AppColors.AmberSurface)
            .border(1.5.dp, amberAccent, CircleShape)
            .clickable(onClick = onSaveAndClose)
            .testTag("save_note_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Save and close",
            tint = amberAccent,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** One action icon in the editor's top bar. */
@Composable
private fun TopBarAction(
    tooltip: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    ActionTooltip(tooltip) {
        IconButton(
            onClick = onClick,
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
