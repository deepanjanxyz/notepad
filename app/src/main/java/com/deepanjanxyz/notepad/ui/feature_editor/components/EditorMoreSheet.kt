package com.deepanjanxyz.notepad.ui.feature_editor.components

import android.text.format.DateUtils
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
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.components.NewFeatureBadge

private const val JUST_NOW_THRESHOLD_MS = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorMoreSheet(
    editedAtMillis: Long,
    hasReminder: Boolean,
    onOpenReminder: () -> Unit,
    onFindInNote: () -> Unit,
    onMakeCopy: () -> Unit,
    onSend: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenLabels: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val editedLabel = formatEditedTimestamp(editedAtMillis)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { MoreSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag("editor_overflow_menu")
        ) {
            Text(
                text = editedLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .testTag("editor_edited_timestamp")
            )

            MoreSheetRow(
                icon = if (hasReminder) Icons.Filled.NotificationsActive else Icons.Outlined.AddAlert,
                label = if (hasReminder) {
                    stringResource(R.string.editor_edit_reminder)
                } else {
                    stringResource(R.string.editor_add_reminder)
                },
                tint = if (hasReminder) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                testTag = "toggle_reminder_button",
                onClick = {
                    onDismiss()
                    onOpenReminder()
                }
            )
            MoreSheetRow(
                icon = Icons.Default.Search,
                label = stringResource(R.string.editor_find_in_note),
                testTag = "find_in_note_button",
                isNew = true,
                onClick = {
                    onDismiss()
                    onFindInNote()
                }
            )
            MoreSheetRow(
                icon = Icons.Outlined.ContentCopy,
                label = stringResource(R.string.editor_make_copy),
                testTag = "make_copy_button",
                isNew = true,
                onClick = {
                    onDismiss()
                    onMakeCopy()
                }
            )
            MoreSheetRow(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.editor_send),
                testTag = "share_note_button",
                onClick = {
                    onDismiss()
                    onSend()
                }
            )
            MoreSheetRow(
                icon = Icons.Outlined.PictureAsPdf,
                label = stringResource(R.string.editor_export_pdf),
                testTag = "export_pdf_button",
                onClick = {
                    onDismiss()
                    onExportPdf()
                }
            )
            MoreSheetRow(
                icon = Icons.AutoMirrored.Outlined.Label,
                label = stringResource(R.string.editor_labels),
                testTag = "add_tag_button",
                onClick = {
                    onDismiss()
                    onOpenLabels()
                }
            )
            MoreSheetRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.editor_delete),
                tint = MaterialTheme.colorScheme.error,
                testTag = "delete_note_button",
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun formatEditedTimestamp(editedAtMillis: Long): String {
    val now = System.currentTimeMillis()
    if (editedAtMillis <= 0L || now - editedAtMillis < JUST_NOW_THRESHOLD_MS) {
        return stringResource(R.string.editor_edited_just_now)
    }
    val relative = DateUtils.getRelativeTimeSpanString(
        editedAtMillis,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
    return stringResource(R.string.editor_edited_time, relative)
}

@Composable
private fun MoreSheetDragHandle() {
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
private fun MoreSheetRow(
    icon: ImageVector,
    label: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    isNew: Boolean = false,
    onClick: () -> Unit
) {
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
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        if (isNew) {
            Spacer(modifier = Modifier.width(12.dp))
            NewFeatureBadge()
        }
    }
}
