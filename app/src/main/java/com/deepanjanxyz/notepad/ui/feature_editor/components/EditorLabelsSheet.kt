package com.deepanjanxyz.notepad.ui.feature_editor.components

import com.deepanjanxyz.notepad.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip

private const val MAX_LABEL_LENGTH = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorLabelsSheet(
    allUniqueLabels: List<String>,
    currentTags: List<String>,
    onToggleLabel: (String) -> Unit,
    onCreateLabel: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    var newLabelInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.labels_select),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            LabelCheckList(
                allUniqueLabels = allUniqueLabels,
                currentTags = currentTags,
                onToggleLabel = onToggleLabel
            )

            CreateLabelField(
                newLabelInput = newLabelInput,
                onInputChange = { newLabelInput = it },
                allUniqueLabels = allUniqueLabels,
                onCreateLabel = onCreateLabel
            )
        }
    }
}

/** The little grab bar at the top of a bottom sheet. */
@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            shape = CircleShape,
            modifier = Modifier.size(width = 36.dp, height = 4.dp)
        ) {}
    }
}

/** Every label the user can pick, with a checkbox on each. */
@Composable
private fun LabelCheckList(
    allUniqueLabels: List<String>,
    currentTags: List<String>,
    onToggleLabel: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
    ) {
        items(allUniqueLabels) { label ->
            val isChecked = currentTags.any { it.equals(label, ignoreCase = true) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLabel(label) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("label_item_$label")
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggleLabel(label) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

/** The field a new label is typed into, with its create button. */
@Composable
private fun CreateLabelField(
    newLabelInput: String,
    onInputChange: (String) -> Unit,
    allUniqueLabels: List<String>,
    onCreateLabel: (String) -> Unit
) {
    val cleanInput = newLabelInput.replace("#", "").trim()
    val isDuplicate = cleanInput.isNotBlank() &&
        allUniqueLabels.any { it.equals(cleanInput, ignoreCase = true) }
    val createIfValid = {
        if (cleanInput.isNotBlank() && !isDuplicate) {
            onCreateLabel(cleanInput)
            onInputChange("")
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = newLabelInput,
            onValueChange = { onInputChange(it.take(MAX_LABEL_LENGTH)) },
            placeholder = {
                Text(
                    stringResource(R.string.labels_create_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            isError = isDuplicate,
            supportingText = {
                LabelSupportingText(newLabelInput = newLabelInput, isDuplicate = isDuplicate)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { createIfValid() }),
            trailingIcon = {
                if (newLabelInput.isNotBlank()) {
                    ActionTooltip(stringResource(R.string.editor_create_label)) {
                        IconButton(
                            onClick = createIfValid,
                            enabled = !isDuplicate,
                            modifier = Modifier.testTag("add_new_label_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.editor_create_label),
                                tint = if (!isDuplicate) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_label_input")
        )
    }
}

/** The "already exists" note and the character count under the label field. */
@Composable
private fun LabelSupportingText(newLabelInput: String, isDuplicate: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isDuplicate) {
            Text(
                text = stringResource(R.string.labels_duplicate),
                color = MaterialTheme.colorScheme.error,
                style = TextStyle(fontSize = 12.sp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = "${newLabelInput.length}/30",
            style = TextStyle(fontSize = 12.sp),
            color = if (isDuplicate) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
