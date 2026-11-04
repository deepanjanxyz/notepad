package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import androidx.compose.ui.platform.testTag
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.theme.AppColors

/**
 * The editor's format control: one button that opens the choice of plain text,
 * checklist, bulleted list or numbered list.
 *
 * A menu rather than four icons, because the top bar already carries the pin,
 * the reminder, the palette, the drawing, share, archive and save actions, and
 * one more icon per format would crowd them out.
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Note format",
                    tint = if (currentFormat.isLinesMode) {
                        AppColors.AccentAmber
                    } else {
                        AppColors.TextPrimary
                    }
                )
            }
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            EditorFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text(text = format.label) },
                    onClick = {
                        isExpanded = false
                        onFormatSelected(format)
                    },
                    modifier = Modifier.testTag(format.testTag)
                )
            }
        }
    }
}
