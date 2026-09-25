package com.deepanjanxyz.notepad.feature.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.core.model.ChecklistItem

@Composable
fun EditorChecklistContent(
    checklistItems: List<ChecklistItem>,
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
    onAddNewItem: () -> Unit,
    onItemCheckChange: (itemId: String, isChecked: Boolean) -> Unit,
    onItemTextChange: (itemId: String, newText: String) -> Unit,
    onItemDelete: (itemId: String) -> Unit,
    textPrimary: Color = Color(0xFFE2E2E6),
    textPlaceholder: Color = Color(0xFF6E7179),
    amberAccent: Color = Color(0xFFFFB300),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val uncompleted = checklistItems.filter { !it.isChecked }
        uncompleted.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.8.dp, Color(0xFF8E9099), RoundedCornerShape(3.dp))
                        .clickable { onItemCheckChange(item.id, true) }
                        .testTag("checkbox_${item.id}")
                )
                BasicTextField(
                    value = item.text,
                    onValueChange = { onItemTextChange(item.id, it) },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = textPrimary,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(amberAccent),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .testTag("checklist_text_${item.id}")
                )
                IconButton(
                    onClick = { onItemDelete(item.id) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("remove_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove item",
                        tint = Color(0xFF6E7179),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // New Item input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF8E9099),
                modifier = Modifier.size(22.dp)
            )
            BasicTextField(
                value = newItemText,
                onValueChange = onNewItemTextChange,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = textPrimary,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(amberAccent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddNewItem() }),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (newItemText.isEmpty()) {
                            Text(
                                text = "List item",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = textPlaceholder
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
                    .testTag("new_checklist_item_input")
            )
            IconButton(
                onClick = onAddNewItem,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("add_checklist_item_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add list item",
                    tint = amberAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Completed Items Section
        val completedItems = checklistItems.filter { it.isChecked }
        if (completedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2C2C30), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${completedItems.size} Completed items",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E9099)
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            completedItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(amberAccent)
                            .clickable { onItemCheckChange(item.id, false) }
                            .testTag("checkbox_completed_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color(0xFF1B1B1F),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    BasicTextField(
                        value = item.text,
                        onValueChange = { onItemTextChange(item.id, it) },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = Color(0xFF6E7179),
                            textDecoration = TextDecoration.LineThrough,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(amberAccent),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .testTag("completed_checklist_text_${item.id}")
                    )
                    IconButton(
                        onClick = { onItemDelete(item.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("remove_completed_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove item",
                            tint = Color(0xFF6E7179),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
