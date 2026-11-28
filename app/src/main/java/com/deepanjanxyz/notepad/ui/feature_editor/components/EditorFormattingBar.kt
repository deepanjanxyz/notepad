package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.feature_editor.model.BlockStyle
import com.deepanjanxyz.notepad.ui.feature_editor.model.InlineFormatState

@Composable
internal fun EditorFormattingBar(
    blockStyle: BlockStyle,
    inlineFormats: InlineFormatState,
    hasCustomNoteColor: Boolean,
    textPrimary: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    onSelectBlockStyle: (BlockStyle) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onInsertOrEditLink: () -> Unit,
    onClearFormatting: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = if (hasCustomNoteColor) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        color = barColor,
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_formatting_bar")
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatTextPill(
                    label = "H1",
                    tooltip = stringResource(R.string.editor_format_h1),
                    selected = blockStyle == BlockStyle.H1,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_h1_button",
                    onClick = { onSelectBlockStyle(BlockStyle.H1) }
                )
                FormatTextPill(
                    label = "H2",
                    tooltip = stringResource(R.string.editor_format_h2),
                    selected = blockStyle == BlockStyle.H2,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_h2_button",
                    onClick = { onSelectBlockStyle(BlockStyle.H2) }
                )
                FormatTextPill(
                    label = "Aa",
                    tooltip = stringResource(R.string.editor_format_normal),
                    selected = blockStyle == BlockStyle.Normal,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_normal_button",
                    onClick = { onSelectBlockStyle(BlockStyle.Normal) }
                )

                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .height(22.dp)
                        .width(1.dp)
                )

                FormatIconButton(
                    icon = Icons.Default.FormatBold,
                    tooltip = stringResource(R.string.editor_format_bold),
                    selected = inlineFormats.isBold,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_bold_button",
                    onClick = onToggleBold
                )
                FormatIconButton(
                    icon = Icons.Default.FormatItalic,
                    tooltip = stringResource(R.string.editor_format_italic),
                    selected = inlineFormats.isItalic,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_italic_button",
                    onClick = onToggleItalic
                )
                FormatIconButton(
                    icon = Icons.Default.FormatUnderlined,
                    tooltip = stringResource(R.string.editor_format_underline),
                    selected = inlineFormats.isUnderline,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_underline_button",
                    onClick = onToggleUnderline
                )
                FormatIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    tooltip = stringResource(R.string.editor_format_strikethrough),
                    selected = inlineFormats.isStrikethrough,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_strikethrough_button",
                    onClick = onToggleStrikethrough
                )
                FormatIconButton(
                    icon = Icons.Default.Link,
                    tooltip = stringResource(R.string.editor_format_link),
                    selected = inlineFormats.hasLink,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_link_button",
                    onClick = onInsertOrEditLink
                )
                FormatIconButton(
                    icon = Icons.Default.FormatClear,
                    tooltip = stringResource(R.string.editor_format_clear),
                    selected = false,
                    textPrimary = textPrimary,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    testTag = "format_clear_button",
                    onClick = onClearFormatting
                )
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(22.dp)
                    .width(1.dp)
            )

            ActionTooltip(stringResource(R.string.editor_close_formatting)) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("format_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.editor_close_formatting),
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatTextPill(
    label: String,
    tooltip: String,
    selected: Boolean,
    textPrimary: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    testTag: String,
    onClick: () -> Unit
) {
    ActionTooltip(tooltip) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (selected) accentContainer else Color.Transparent,
            modifier = Modifier
                .height(40.dp)
                .testTag(testTag)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) onAccentContainer else textPrimary
                )
            }
        }
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    tooltip: String,
    selected: Boolean,
    textPrimary: Color,
    accentContainer: Color,
    onAccentContainer: Color,
    testTag: String,
    onClick: () -> Unit
) {
    ActionTooltip(tooltip) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (selected) accentContainer else Color.Transparent,
            modifier = Modifier
                .size(40.dp)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltip,
                    tint = if (selected) onAccentContainer else textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
