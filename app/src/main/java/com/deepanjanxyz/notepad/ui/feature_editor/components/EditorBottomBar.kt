package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.components.ActionTooltip

private val BUTTON_SIZE = 44.dp
private val ICON_SIZE = 22.dp

@Composable
internal fun EditorBottomBar(
    hasColor: Boolean,
    isFormattingVisible: Boolean,
    hasCustomNoteColor: Boolean,
    textPrimary: Color,
    accentColor: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    onOpenAddMenu: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onToggleFormatting: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val idleContainer = if (hasCustomNoteColor) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomToolbarCircleButton(
                tooltip = stringResource(R.string.editor_add),
                icon = Icons.Outlined.AddBox,
                containerColor = idleContainer,
                iconTint = textPrimary,
                testTag = "editor_add_button",
                onClick = onOpenAddMenu
            )
            BottomToolbarCircleButton(
                tooltip = stringResource(R.string.editor_color_palette),
                icon = Icons.Outlined.Palette,
                containerColor = if (hasColor) accentContainer else idleContainer,
                iconTint = if (hasColor) onAccentContainer else textPrimary,
                testTag = "toggle_color_picker_button",
                onClick = onOpenColorPicker
            )
            BottomToolbarCircleButton(
                tooltip = stringResource(R.string.editor_text_formatting),
                icon = Icons.Default.FormatColorText,
                containerColor = if (isFormattingVisible) accentContainer else idleContainer,
                iconTint = if (isFormattingVisible) accentColor else textPrimary,
                testTag = "editor_formatting_toggle_button",
                onClick = onToggleFormatting
            )
        }

        BottomToolbarCircleButton(
            tooltip = stringResource(R.string.editor_more),
            icon = Icons.Default.MoreVert,
            containerColor = idleContainer,
            iconTint = textPrimary,
            testTag = "editor_more_button",
            onClick = onOpenMoreMenu
        )
    }
}

@Composable
private fun BottomToolbarCircleButton(
    tooltip: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    ActionTooltip(tooltip) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(BUTTON_SIZE)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = iconTint,
                modifier = Modifier
                    .padding(11.dp)
                    .size(ICON_SIZE)
            )
        }
    }
}
