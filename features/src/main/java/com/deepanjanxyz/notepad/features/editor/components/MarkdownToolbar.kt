package com.deepanjanxyz.notepad.features.editor.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownToolbar(
    onInsert: (prefix: String, suffix: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                icon = Icons.Default.Title,
                contentDescription = "Heading",
                onClick = { onInsert("# ", "") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                onClick = { onInsert("**", "**") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                onClick = { onInsert("*", "*") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                contentDescription = "Bullet List",
                onClick = { onInsert("- ", "") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                contentDescription = "Numbered List",
                onClick = { onInsert("1. ", "") }
            )
            ToolbarButton(
                icon = Icons.Default.CheckCircleOutline,
                contentDescription = "Checklist",
                onClick = { onInsert("- [ ] ", "") }
            )
            ToolbarButton(
                icon = Icons.Default.FormatQuote,
                contentDescription = "Quote",
                onClick = { onInsert("> ", "") }
            )
            ToolbarButton(
                icon = Icons.Default.Code,
                contentDescription = "Code",
                onClick = { onInsert("```\n", "\n```") }
            )
            ToolbarButton(
                icon = Icons.Default.HorizontalRule,
                contentDescription = "Divider",
                onClick = { onInsert("\n---\n", "") }
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
