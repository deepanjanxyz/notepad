package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.theme.AppColors

/** How long the format button takes to swap one format's icon for another. */
private const val ICON_SWAP_MS = 140

/** How long the selected marker takes to appear or go. */
private const val MARKER_MS = 160

/** The icon that stands for [this] format, in the menu and on the button. */
private val EditorFormat.icon: ImageVector
    get() = when (this) {
        EditorFormat.PlainText -> Icons.Default.Notes
        EditorFormat.Checklist -> Icons.Default.Checklist
        EditorFormat.BulletedList -> Icons.AutoMirrored.Filled.FormatListBulleted
        // Material icons ships no automirrored FormatListNumbered, so this one stands in.
        EditorFormat.NumberedList -> Icons.Default.FormatListNumbered
    }

/**
 * The editor's format control: one button that opens the choice of plain text,
 * checklist, bulleted list or numbered list.
 *
 * A menu rather than four icons, because the top bar already carries the pin,
 * the reminder, the palette, the drawing, share, archive and save actions, and
 * one more icon per format would crowd them out.
 *
 * The button shows the icon of the format in use, so the current one is legible
 * without opening the menu, and the menu marks the same format with a green tick
 * and a brightened label. Both changes animate, because a tick that appears in
 * place reads as a state change while one that fades in over a redrawn row reads
 * as a glitch.
 */
@Composable
fun EditorFormatMenu(
    currentFormat: EditorFormat,
    onFormatSelected: (EditorFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ActionTooltip("Note format") {
            IconButton(
                onClick = { isExpanded = true },
                modifier = Modifier.testTag("editor_format_button")
            ) {
                AnimatedContent(
                    targetState = currentFormat,
                    transitionSpec = {
                        fadeIn(tween(ICON_SWAP_MS)) togetherWith fadeOut(tween(ICON_SWAP_MS))
                    },
                    label = "format_icon"
                ) { format ->
                    Icon(
                        imageVector = format.icon,
                        contentDescription = "Note format",
                        tint = if (format.isLinesMode) {
                            AppColors.AccentAmber
                        } else {
                            AppColors.TextPrimary
                        }
                    )
                }
            }
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = AppColors.SheetSurface,
            offset = DpOffset(0.dp, 4.dp),
            modifier = Modifier
                .width(232.dp)
                .testTag("editor_format_menu")
        ) {
            EditorFormat.entries.forEach { format ->
                FormatMenuItem(
                    format = format,
                    isSelected = format == currentFormat,
                    onClick = {
                        isExpanded = false
                        onFormatSelected(format)
                    }
                )
            }
        }
    }
}

/**
 * One row of the format menu: the icon and name of [format], a green tick when it
 * is the format in use, and nothing at all when it is not.
 */
@Composable
private fun FormatMenuItem(
    format: EditorFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val labelColour by animateColorAsState(
        targetValue = if (isSelected) AppColors.TextPrimary else AppColors.TextMuted,
        animationSpec = tween(MARKER_MS),
        label = "format_label"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) AppColors.AccentAmber else AppColors.TextMuted,
        animationSpec = tween(MARKER_MS),
        label = "format_icon_tint"
    )

    DropdownMenuItem(
        onClick = onClick,
        modifier = Modifier.testTag(format.testTag),
        leadingIcon = {
            Icon(
                imageVector = format.icon,
                contentDescription = null,
                tint = iconTint
            )
        },
        text = {
            Text(
                text = format.label,
                color = labelColour,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(MARKER_MS)) + scaleIn(tween(MARKER_MS), initialScale = 0.6f),
                exit = fadeOut(tween(MARKER_MS)) + scaleOut(tween(MARKER_MS), targetScale = 0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected format",
                    tint = AppColors.AccentGreen
                )
            }
        }
    )
}
