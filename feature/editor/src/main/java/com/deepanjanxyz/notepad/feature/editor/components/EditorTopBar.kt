package com.deepanjanxyz.notepad.feature.editor.components

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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun EditorTopBar(
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    hasReminder: Boolean,
    onOpenReminder: () -> Unit,
    isChecklistMode: Boolean,
    onToggleChecklist: () -> Unit,
    hasColor: Boolean,
    onOpenColorPicker: () -> Unit,
    hasDrawing: Boolean,
    onOpenDrawing: () -> Unit,
    onArchive: () -> Unit,
    onSaveAndClose: () -> Unit,
    onNavigateBack: () -> Unit,
    textPrimary: Color = Color(0xFFE2E2E6),
    amberAccent: Color = Color(0xFFFFB300),
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
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("editor_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textPrimary
            )
        }

        // Right Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Pin / Unpin
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.testTag("toggle_pin_button")
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (isPinned) "Unpin Note" else "Pin Note",
                    tint = if (isPinned) amberAccent else textPrimary
                )
            }

            // Reminder Button
            IconButton(
                onClick = onOpenReminder,
                modifier = Modifier.testTag("toggle_reminder_button")
            ) {
                Icon(
                    imageVector = if (hasReminder) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                    contentDescription = if (hasReminder) "Edit reminder" else "Add reminder",
                    tint = if (hasReminder) amberAccent else textPrimary
                )
            }

            // Checklist / Bullet list toggle
            IconButton(
                onClick = onToggleChecklist,
                modifier = Modifier.testTag("toggle_checklist_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Checklist format",
                    tint = if (isChecklistMode) amberAccent else textPrimary
                )
            }

            // Color Tint Palette
            IconButton(
                onClick = onOpenColorPicker,
                modifier = Modifier.testTag("toggle_color_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Color palette",
                    tint = if (hasColor) amberAccent else textPrimary
                )
            }

            // Drawing Action
            IconButton(
                onClick = onOpenDrawing,
                modifier = Modifier.testTag("open_drawing_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Brush,
                    contentDescription = "Drawing",
                    tint = if (hasDrawing) amberAccent else textPrimary
                )
            }

            // Archive Action
            IconButton(
                onClick = onArchive,
                modifier = Modifier.testTag("archive_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive Note",
                    tint = textPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Circular Done / Check Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF282419))
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
    }
}
