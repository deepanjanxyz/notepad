package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.ui.components.ActionTooltip

@Composable
internal fun ActiveLinkPill(
    url: String,
    accentColor: Color,
    textPrimary: Color,
    hasCustomNoteColor: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (hasCustomNoteColor) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val borderColor = if (hasCustomNoteColor) {
        Color.White.copy(alpha = 0.24f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("active_link_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            ActionTooltip(stringResource(R.string.active_link_open)) {
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.testTag("active_link_open_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.active_link_open),
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ActionTooltip(stringResource(R.string.active_link_edit)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("active_link_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.active_link_edit),
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ActionTooltip(stringResource(R.string.active_link_remove)) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("active_link_remove_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = stringResource(R.string.active_link_remove),
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditorLinkDialog(
    initialText: String,
    initialUrl: String,
    isEditing: Boolean,
    onSave: (displayText: String, safeUrl: String) -> Unit,
    onOpen: ((safeUrl: String) -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var linkText by remember(initialText, initialUrl) { mutableStateOf(initialText) }
    var urlInput by remember(initialText, initialUrl) { mutableStateOf(initialUrl) }
    var showValidationError by remember { mutableStateOf(false) }

    val validatedUrl = remember(urlInput) { RichTextCodec.sanitizeLinkUrl(urlInput) }

    fun submit() {
        val safeUrl = RichTextCodec.sanitizeLinkUrl(urlInput)
        if (safeUrl == null) {
            showValidationError = true
            return
        }
        onSave(linkText, safeUrl)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isEditing) R.string.link_dialog_edit_title else R.string.link_dialog_add_title
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it.replace('\n', ' ') },
                    label = { Text(stringResource(R.string.link_dialog_text_label)) },
                    placeholder = { Text(stringResource(R.string.link_dialog_text_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("link_dialog_text_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it.replace('\n', ' ')
                        if (showValidationError) showValidationError = false
                    },
                    label = { Text(stringResource(R.string.link_dialog_url_label)) },
                    placeholder = { Text(stringResource(R.string.link_dialog_url_placeholder)) },
                    singleLine = true,
                    isError = showValidationError && validatedUrl == null,
                    supportingText = if (showValidationError && validatedUrl == null) {
                        { Text(stringResource(R.string.link_dialog_url_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("link_dialog_url_input")
                )

                if (isEditing && (onOpen != null || onRemove != null)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onOpen != null) {
                            TextButton(
                                onClick = {
                                    val safeUrl = RichTextCodec.sanitizeLinkUrl(urlInput)
                                    if (safeUrl == null) {
                                        showValidationError = true
                                    } else {
                                        onOpen(safeUrl)
                                    }
                                },
                                modifier = Modifier.testTag("link_dialog_open_button")
                            ) {
                                Text(stringResource(R.string.link_dialog_open))
                            }
                        }
                        if (onRemove != null) {
                            TextButton(
                                onClick = onRemove,
                                modifier = Modifier.testTag("link_dialog_remove_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.link_dialog_remove),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                modifier = Modifier.testTag("link_dialog_save_button")
            ) {
                Text(stringResource(R.string.link_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.trash_cancel))
            }
        }
    )
}
