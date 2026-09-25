package com.deepanjanxyz.notepad.core.designsystem.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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

    val cleanedNew = newLabelText.trim().replace("#", "")
    val isDuplicate = cleanedNew.isNotBlank() && labels.any { it.equals(cleanedNew, ignoreCase = true) }

    // Rename sub-dialog
    if (renamingLabel != null) {
        val targetLabel = renamingLabel!!
        val renameCleaned = renameInputText.trim().replace("#", "")
        val isRenameDuplicate = renameCleaned.isNotBlank() &&
            !renameCleaned.equals(targetLabel, ignoreCase = true) &&
            labels.any { it.equals(renameCleaned, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { renamingLabel = null },
            title = { Text("Rename Label") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = {
                            renameInputText = it.take(30)
                        },
                        label = { Text("Label Name") },
                        isError = isRenameDuplicate,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isRenameDuplicate) {
                                    Text(
                                        text = "Label already exists",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                Text(
                                    text = "${renameInputText.length}/30",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRenameDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                )
                            }
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
                        if (renameCleaned.isNotBlank() && !isRenameDuplicate) {
                            onRenameLabel(targetLabel, renameCleaned)
                        }
                        renamingLabel = null
                    },
                    enabled = renameInputText.isNotBlank() && !isRenameDuplicate,
                    modifier = Modifier.testTag("confirm_rename_label_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingLabel = null }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                // Dialog Header
                Text(
                    text = "Edit Labels",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top Input Field: "Create new label" with max 30 char limit and trailing '+' button
                OutlinedTextField(
                    value = newLabelText,
                    onValueChange = {
                        newLabelText = it.take(30)
                    },
                    placeholder = { Text("Create new label") },
                    isError = isDuplicate,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (cleanedNew.isNotBlank() && !isDuplicate) {
                                    onAddLabel(cleanedNew)
                                    newLabelText = ""
                                }
                            },
                            enabled = newLabelText.isNotBlank() && !isDuplicate,
                            modifier = Modifier.testTag("add_label_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add label",
                                tint = if (newLabelText.isNotBlank() && !isDuplicate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isDuplicate) {
                                Text(
                                    text = "Label already exists",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text(
                                text = "${newLabelText.length}/30",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_label_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Existing Labels List
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
                                onRename = {
                                    renamingLabel = label
                                    renameInputText = label
                                },
                                onDelete = {
                                    onDeleteLabel(label)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action: "Done" button to dismiss dialog
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
    var menuExpanded by remember { mutableStateOf(false) }

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
                imageVector = Icons.Default.Label,
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
            Box {
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

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
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
    }
}
