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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        TopBarAction(
            "Back", Icons.AutoMirrored.Filled.ArrowBack,
            textPrimary, "editor_back_button", onNavigateBack
        )

        EditorActionsRow(
            isPinned = isPinned,
            hasReminder = hasReminder,
            editorFormat = editorFormat,
            hasColor = hasColor,
            hasDrawing = hasDrawing,
            textPrimary = textPrimary,
            amberAccent = amberAccent,
            onTogglePin = onTogglePin,
            onOpenReminder = onOpenReminder,
            onFormatSelected = onFormatSelected,
            onOpenColorPicker = onOpenColorPicker,
            onOpenDrawing = onOpenDrawing,
            onShare = onShare,
            onArchive = onArchive,
            onSaveAndClose = onSaveAndClose
        )
    }
}

/** The action icons on the right of the editor's top bar, and the done button. */
@Composable
private fun EditorActionsRow(
    isPinned: Boolean,
    hasReminder: Boolean,
    editorFormat: EditorFormat,
    hasColor: Boolean,
    hasDrawing: Boolean,
    textPrimary: Color,
    amberAccent: Color,
    onTogglePin: () -> Unit,
    onOpenReminder: () -> Unit,
    onFormatSelected: (EditorFormat) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenDrawing: () -> Unit,
    onShare: () -> Unit,
    onArchive: () -> Unit,
    onSaveAndClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Pin / Unpin
        TopBarAction(
            if (isPinned) "Unpin Note" else "Pin Note",
            if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            if (isPinned) amberAccent else textPrimary, "toggle_pin_button", onTogglePin
        )
        // Reminder Button
        TopBarAction(
            if (hasReminder) "Edit reminder" else "Add reminder",
            if (hasReminder) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
            if (hasReminder) amberAccent else textPrimary, "toggle_reminder_button", onOpenReminder
        )
        // Format menu: plain text, checklist, bulleted list or numbered list
        EditorFormatMenu(
            currentFormat = editorFormat,
            onFormatSelected = onFormatSelected
        )
        // Color Tint Palette
        TopBarAction(
            "Color palette", Icons.Default.Palette,
            if (hasColor) amberAccent else textPrimary, "toggle_color_picker_button", onOpenColorPicker
        )
        // Drawing Action
        TopBarAction(
            "Drawing", Icons.Filled.Brush,
            if (hasDrawing) amberAccent else textPrimary, "open_drawing_button", onOpenDrawing
        )
        // Share Action
        TopBarAction("Share Note", Icons.Default.Share, textPrimary, "share_note_button", onShare)
        // Archive Action
        TopBarAction("Archive Note", Icons.Default.Archive, textPrimary, "archive_note_button", onArchive)

        Spacer(modifier = Modifier.width(4.dp))

        EditorSaveButton(amberAccent = amberAccent, onSaveAndClose = onSaveAndClose)
    }
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
            contentDescription = "Save and Close",
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
