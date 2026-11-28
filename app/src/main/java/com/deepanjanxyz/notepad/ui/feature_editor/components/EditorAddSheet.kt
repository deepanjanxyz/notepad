package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorAddSheet(
    currentFormat: EditorFormat,
    onOpenDrawing: () -> Unit,
    onSelectFormat: (EditorFormat) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { AddSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag("editor_add_sheet")
        ) {
            AddSheetRow(
                icon = Icons.Outlined.Brush,
                label = stringResource(R.string.editor_drawing),
                selected = false,
                testTag = "open_drawing_button",
                onClick = {
                    onDismiss()
                    onOpenDrawing()
                }
            )
            AddSheetRow(
                icon = Icons.Outlined.CheckBox,
                label = stringResource(R.string.editor_checkboxes),
                selected = currentFormat == EditorFormat.Checklist,
                testTag = EditorFormat.Checklist.testTag,
                onClick = {
                    onDismiss()
                    val next = if (currentFormat == EditorFormat.Checklist) {
                        EditorFormat.PlainText
                    } else {
                        EditorFormat.Checklist
                    }
                    onSelectFormat(next)
                }
            )
            AddSheetRow(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = stringResource(R.string.editor_bulleted_list),
                selected = currentFormat == EditorFormat.BulletedList,
                testTag = EditorFormat.BulletedList.testTag,
                onClick = {
                    onDismiss()
                    val next = if (currentFormat == EditorFormat.BulletedList) {
                        EditorFormat.PlainText
                    } else {
                        EditorFormat.BulletedList
                    }
                    onSelectFormat(next)
                }
            )
            AddSheetRow(
                icon = Icons.Default.FormatListNumbered,
                label = stringResource(R.string.editor_numbered_list),
                selected = currentFormat == EditorFormat.NumberedList,
                testTag = EditorFormat.NumberedList.testTag,
                onClick = {
                    onDismiss()
                    val next = if (currentFormat == EditorFormat.NumberedList) {
                        EditorFormat.PlainText
                    } else {
                        EditorFormat.NumberedList
                    }
                    onSelectFormat(next)
                }
            )
        }
    }
}

@Composable
private fun AddSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            shape = CircleShape,
            modifier = Modifier.size(width = 36.dp, height = 4.dp)
        ) {}
    }
}

@Composable
private fun AddSheetRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.editor_selected_format),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
