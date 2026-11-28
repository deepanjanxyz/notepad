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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.components.ActionTooltip

@Composable
fun EditorTopBar(
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onSaveAndClose: () -> Unit,
    onNavigateBack: () -> Unit,
    textPrimary: Color = MaterialTheme.colorScheme.onBackground,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarAction(
            tooltip = stringResource(R.string.editor_back),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            tint = textPrimary,
            testTag = "editor_back_button",
            onClick = onNavigateBack
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TopBarAction(
                tooltip = if (isPinned) {
                    stringResource(R.string.editor_unpin)
                } else {
                    stringResource(R.string.editor_pin)
                },
                icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                tint = if (isPinned) accentColor else textPrimary,
                testTag = "toggle_pin_button",
                onClick = onTogglePin
            )
            TopBarAction(
                tooltip = stringResource(R.string.editor_archive_note),
                icon = Icons.Outlined.Archive,
                tint = textPrimary,
                testTag = "archive_note_button",
                onClick = onArchive
            )

            Spacer(modifier = Modifier.width(6.dp))

            EditorSaveButton(
                accentColor = accentColor,
                accentContainer = accentContainer,
                onSaveAndClose = onSaveAndClose
            )
        }
    }
}

@Composable
private fun EditorSaveButton(
    accentColor: Color,
    accentContainer: Color,
    onSaveAndClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(accentContainer)
            .border(1.5.dp, accentColor, CircleShape)
            .clickable(onClick = onSaveAndClose)
            .testTag("save_note_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.editor_save_close),
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

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
