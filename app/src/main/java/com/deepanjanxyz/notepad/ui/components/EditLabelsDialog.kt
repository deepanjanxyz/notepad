package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private const val MAX_LABEL_LENGTH = 30

@Composable
fun EditLabelsDialog(
    labels: List<String>,
    onAddLabel: (String) -> Unit,
    onRenameLabel: (String, String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newLabelText by remember { mutableStateOf("") }
    var renamingLabel by remember { mutableStateOf<String?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val pendingLabel = newLabelText.trim().replace("#", "")
    val pendingIsDuplicate = pendingLabel.isNotBlank() &&
        labels.any { it.equals(pendingLabel, ignoreCase = true) }

    // Typing a name and then tapping Done - or dismissing the dialog - used to
    // discard it unless "+" had been pressed. Commit it the same way "+" does.
    val commitPendingLabel = {
        if (pendingLabel.isNotBlank() && !pendingIsDuplicate) {
            onAddLabel(pendingLabel)
            newLabelText = ""
        }
    }

    renamingLabel?.let { targetLabel ->
        RenameLabelDialog(
            targetLabel = targetLabel,
            labels = labels,
            renameInputText = renameInputText,
            onInputChange = { renameInputText = it },
            onConfirm = { newName ->
                onRenameLabel(targetLabel, newName)
                renamingLabel = null
            },
            onDismiss = { renamingLabel = null }
        )
    }

    EditLabelsDialogBody(
        labels = labels,
        newLabelText = newLabelText,
        onNewLabelChange = { newLabelText = it },
        onAddLabel = onAddLabel,
        onStartRename = { label ->
            renamingLabel = label
            renameInputText = label
        },
        onDeleteLabel = onDeleteLabel,
        onDismiss = {
            commitPendingLabel()
            onDismiss()
        },
        modifier = modifier
    )
}

/** The dialog itself: its title, the new-label field, the list and the done row. */
@Composable
private fun EditLabelsDialogBody(
    labels: List<String>,
    newLabelText: String,
    onNewLabelChange: (String) -> Unit,
    onAddLabel: (String) -> Unit,
    onStartRename: (String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("edit_labels_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Edit Labels",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                NewLabelField(
                    newLabelText = newLabelText,
                    labels = labels,
                    onInputChange = onNewLabelChange,
                    onAddLabel = onAddLabel
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LabelList(
                    labels = labels,
                    onStartRename = onStartRename,
                    onDeleteLabel = onDeleteLabel
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditLabelsDoneRow(onDismiss = onDismiss)
            }
        }
    }
}

/** The field a new label is typed into, with its add button. */
@Composable
private fun NewLabelField(
    newLabelText: String,
    labels: List<String>,
    onInputChange: (String) -> Unit,
    onAddLabel: (String) -> Unit
) {
    val cleaned = newLabelText.trim().replace("#", "")
    val isDuplicate = cleaned.isNotBlank() && labels.any { it.equals(cleaned, ignoreCase = true) }
    val canAdd = newLabelText.isNotBlank() && !isDuplicate
    val atLimit = newLabelText.length >= MAX_LABEL_LENGTH
    val addIfValid = {
        if (cleaned.isNotBlank() && !isDuplicate) {
            onAddLabel(cleaned)
            onInputChange("")
        }
    }

    val haptics = LocalHapticFeedback.current

    OutlinedTextField(
        value = newLabelText,
        onValueChange = { raw ->
            // take() drops anything past the limit; buzz and redden the field.
            if (raw.length > MAX_LABEL_LENGTH) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onInputChange(raw.take(MAX_LABEL_LENGTH))
        },
        placeholder = { Text("Create new label") },
        isError = isDuplicate || atLimit,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { addIfValid() }),
        trailingIcon = {
            NewLabelAddButton(canAdd = canAdd, onClick = addIfValid)
        },
        supportingText = {
            LabelFieldSupportingText(length = newLabelText.length, isDuplicate = isDuplicate)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("create_label_input")
    )
}

/**
 * The add button beside the new-label field.
 *
 * It is kept clickable even while the name is not valid yet - the guard lives in
 * the action - so a tap that lands near the icon is swallowed here instead of
 * falling through to the text field or the list underneath.
 */
@Composable
private fun NewLabelAddButton(canAdd: Boolean, onClick: () -> Unit) {
    ActionTooltip("Add label") {
        // A fixed 48dp box gives the button a hit area of its own, so a tap that
        // lands beside the icon cannot reach the field or the list behind it.
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.testTag("add_label_plus_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add label",
                    tint = if (canAdd) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

/** The "already exists" note and the character count under a label field. */
@Composable
private fun LabelFieldSupportingText(length: Int, isDuplicate: Boolean) {
    val atLimit = length >= MAX_LABEL_LENGTH
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when {
            isDuplicate -> Text(
                text = "Label already exists",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
            atLimit -> Text(
                text = "Character limit reached",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
            else -> Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = "$length/$MAX_LABEL_LENGTH",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (atLimit) FontWeight.Bold else FontWeight.Normal,
            color = if (isDuplicate || atLimit) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    }
}

/** The sub-dialog that renames one label. */
@Composable
private fun RenameLabelDialog(
    targetLabel: String,
    labels: List<String>,
    renameInputText: String,
    onInputChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val cleaned = renameInputText.trim().replace("#", "")
    val isDuplicate = cleaned.isNotBlank() &&
        !cleaned.equals(targetLabel, ignoreCase = true) &&
        labels.any { it.equals(cleaned, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Label") },
        text = {
            Column {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { raw ->
                        if (raw.length > MAX_LABEL_LENGTH) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onInputChange(raw.take(MAX_LABEL_LENGTH))
                    },
                    label = { Text("Label Name") },
                    isError = isDuplicate,
                    supportingText = {
                        LabelFieldSupportingText(
                            length = renameInputText.length,
                            isDuplicate = isDuplicate
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_label_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (cleaned.isNotBlank() && !isDuplicate) {
                        onConfirm(cleaned)
                    }
                    onDismiss()
                },
                enabled = renameInputText.isNotBlank() && !isDuplicate,
                modifier = Modifier.testTag("confirm_rename_label_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** Every existing label, or a note that there are none yet. */
@Composable
private fun LabelList(
    labels: List<String>,
    onStartRename: (String) -> Unit,
    onDeleteLabel: (String) -> Unit
) {
    if (labels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No labels yet. Create one above!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
        ) {
            items(labels, key = { it }) { label ->
                LabelListItem(
                    label = label,
                    onRename = { onStartRename(label) },
                    onDelete = { onDeleteLabel(label) }
                )
            }
        }
    }
}

/** The done button that closes the dialog. */
@Composable
private fun EditLabelsDoneRow(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("done_edit_labels_button")
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LabelListItem(
    label: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("label_row_$label")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Trailing 3-dot overflow menu with Rename and Delete
            LabelOverflowMenu(label = label, onRename = onRename, onDelete = onDelete)
        }
    }
}

@Composable
private fun LabelOverflowMenu(
    label: String,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        ActionTooltip("Label options") {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("label_overflow_menu_$label")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Label options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                onClick = {
                    menuExpanded = false
                    onRename()
                },
                modifier = Modifier.testTag("rename_menu_item_$label")
            )

            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
                modifier = Modifier.testTag("delete_menu_item_$label")
            )
        }
    }
}
