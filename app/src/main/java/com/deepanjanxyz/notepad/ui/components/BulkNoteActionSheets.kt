package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.theme.NoteColorNames
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions

private const val MAX_LABEL_LENGTH = 30

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkColorPickerSheet(
    selectedNotes: List<Note>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val commonColorIndex = remember(selectedNotes) {
        selectedNotes.map { it.colorIndex }.distinct().singleOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("bulk_color_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bulk_color_sheet_title, selectedNotes.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bulk_color_done_button")
                ) {
                    Text(stringResource(R.string.labels_done))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NoteColorOptions.forEachIndexed { index, color ->
                    val isSelected = commonColorIndex == index
                    val colorName = NoteColorNames.getOrElse(index) { "" }
                    val swatchFill = if (index == 0) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        color
                    }
                    val borderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(swatchFill)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = borderColor,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(index) }
                            .semantics {
                                contentDescription = colorName
                            }
                            .testTag("bulk_color_option_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.editor_selected),
                                tint = if (index == 0) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (index == 0) {
                            Icon(
                                imageVector = Icons.Outlined.FormatColorReset,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkLabelsSheet(
    selectedNotes: List<Note>,
    allUniqueLabels: List<String>,
    onSetLabelForSelected: (tag: String, enabled: Boolean) -> Unit,
    onCreateAndApplyLabel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newLabelText by remember { mutableStateOf("") }
    val mergedLabels = remember(allUniqueLabels, selectedNotes) {
        (allUniqueLabels + selectedNotes.flatMap { it.tags })
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    fun submitNewLabel() {
        val cleaned = newLabelText.replace("#", "").trim().take(MAX_LABEL_LENGTH).trim()
        if (cleaned.isEmpty()) return
        val existing = mergedLabels.firstOrNull { it.equals(cleaned, ignoreCase = true) }
        if (existing != null) {
            onSetLabelForSelected(existing, true)
        } else {
            onCreateAndApplyLabel(cleaned)
        }
        newLabelText = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("bulk_labels_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bulk_labels_sheet_title, selectedNotes.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bulk_labels_done_button")
                ) {
                    Text(stringResource(R.string.labels_done))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newLabelText,
                    onValueChange = { input ->
                        newLabelText = input.replace("#", "").take(MAX_LABEL_LENGTH)
                    },
                    placeholder = { Text(stringResource(R.string.labels_create_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNewLabel() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bulk_create_label_input")
                )
                FilledTonalIconButton(
                    onClick = ::submitNewLabel,
                    enabled = newLabelText.replace("#", "").trim().isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("bulk_add_new_label_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.labels_add)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mergedLabels.isEmpty()) {
                Text(
                    text = stringResource(R.string.labels_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(mergedLabels, key = { it.lowercase() }) { label ->
                        val matchCount = selectedNotes.count { note ->
                            note.tags.any { it.equals(label, ignoreCase = true) }
                        }
                        val state = when {
                            selectedNotes.isNotEmpty() && matchCount == selectedNotes.size -> ToggleableState.On
                            matchCount > 0 -> ToggleableState.Indeterminate
                            else -> ToggleableState.Off
                        }
                        val nextEnabled = state != ToggleableState.On

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSetLabelForSelected(label, nextEnabled) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("bulk_label_item_$label"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TriStateCheckbox(
                                state = state,
                                onClick = { onSetLabelForSelected(label, nextEnabled) }
                            )
                        }
                    }
                }
            }
        }
    }
}
