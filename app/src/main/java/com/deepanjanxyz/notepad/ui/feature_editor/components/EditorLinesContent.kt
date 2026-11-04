package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.theme.AppColors

// Height a blank line in the note is given, so paragraphs keep their spacing.
private val BLANK_LINE_HEIGHT = 12.dp

// Room above and below the row that adds a new line.
private val ADD_ROW_PADDING = 6.dp

// The add button and the glyph inside it.
private val ADD_BUTTON_SIZE = 36.dp
private val ADD_ICON_SIZE = 22.dp

/**
 * Line-by-line mode for the note editor.
 *
 * Every line is rendered where it was written and as what it is: ordinary text
 * stays text, a task gets a checkbox, a bullet gets its dot and a numbered line
 * keeps its number. Because the model keeps those lines apart, editing one of
 * them never reformats - or converts - the rest of the note.
 */
@Composable
fun EditorLinesContent(
    segments: List<NoteSegment>,
    newItemText: String,
    newItemFormat: EditorFormat,
    onNewItemTextChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onToggleTask: (segmentIndex: Int) -> Unit,
    onSegmentTextChange: (segmentIndex: Int, text: String) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit,
    onDeleteSegment: (segmentIndex: Int) -> Unit,
    textPrimary: Color = AppColors.TextPrimary,
    textPlaceholder: Color = AppColors.TextPlaceholder,
    amberAccent: Color = AppColors.AccentAmber,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is NoteSegment.Text -> EditorTextLine(text = segment.text, textPrimary = textPrimary)

                is NoteSegment.Task -> EditorItemRow(
                    leading = {
                        TaskCheckbox(
                            isChecked = segment.item.isChecked,
                            amberAccent = amberAccent,
                            testTag = "task_checkbox_$index",
                            onToggle = { onToggleTask(index) }
                        )
                    },
                    text = segment.item.text,
                    isDone = segment.item.isChecked,
                    segmentIndex = index,
                    textPrimary = textPrimary,
                    amberAccent = amberAccent,
                    onSegmentTextChange = onSegmentTextChange,
                    onSplitSegment = onSplitSegment,
                    onDeleteSegment = onDeleteSegment
                )

                is NoteSegment.Bullet -> EditorItemRow(
                    leading = { BulletGlyph() },
                    text = segment.text,
                    isDone = false,
                    segmentIndex = index,
                    textPrimary = textPrimary,
                    amberAccent = amberAccent,
                    onSegmentTextChange = onSegmentTextChange,
                    onSplitSegment = onSplitSegment,
                    onDeleteSegment = onDeleteSegment
                )

                is NoteSegment.Numbered -> EditorItemRow(
                    leading = { NumberGlyph(number = segment.number) },
                    text = segment.text,
                    isDone = false,
                    segmentIndex = index,
                    textPrimary = textPrimary,
                    amberAccent = amberAccent,
                    onSegmentTextChange = onSegmentTextChange,
                    onSplitSegment = onSplitSegment,
                    onDeleteSegment = onDeleteSegment
                )
            }
        }

        NewLineRow(
            newItemText = newItemText,
            newItemFormat = newItemFormat,
            onNewItemTextChange = onNewItemTextChange,
            onAddItem = onAddItem,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            amberAccent = amberAccent
        )
    }
}

/** One editable line: its marker, its words and its remove button. */
@Composable
private fun EditorItemRow(
    leading: @Composable () -> Unit,
    text: String,
    isDone: Boolean,
    segmentIndex: Int,
    textPrimary: Color,
    amberAccent: Color,
    onSegmentTextChange: (segmentIndex: Int, text: String) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit,
    onDeleteSegment: (segmentIndex: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LINE_ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        LineItemText(
            text = text,
            isDone = isDone,
            segmentIndex = segmentIndex,
            textPrimary = textPrimary,
            amberAccent = amberAccent,
            onTextChange = onSegmentTextChange,
            onSplitSegment = onSplitSegment
        )
        RemoveLineButton(segmentIndex = segmentIndex, onDeleteSegment = onDeleteSegment)
    }
}

/** One run of plain text between the list lines. */
@Composable
private fun EditorTextLine(text: String, textPrimary: Color) {
    if (text.isBlank()) {
        Spacer(modifier = Modifier.height(BLANK_LINE_HEIGHT))
    } else {
        Text(
            text = text,
            style = TextStyle(
                fontSize = LINE_FONT_SIZE,
                fontWeight = FontWeight.Normal,
                color = textPrimary,
                lineHeight = LINE_HEIGHT
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LINE_ROW_PADDING)
        )
    }
}

/** The row that adds a new line: its marker, the field and its button. */
@Composable
private fun NewLineRow(
    newItemText: String,
    newItemFormat: EditorFormat,
    onNewItemTextChange: (String) -> Unit,
    onAddItem: () -> Unit,
    textPrimary: Color,
    textPlaceholder: Color,
    amberAccent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ADD_ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NewLineMarker(newItemFormat = newItemFormat, amberAccent = amberAccent)
        NewLineField(
            newItemText = newItemText,
            newItemFormat = newItemFormat,
            onNewItemTextChange = onNewItemTextChange,
            onAddItem = onAddItem,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            amberAccent = amberAccent
        )
        ActionTooltip("Add line") {
            IconButton(
                onClick = onAddItem,
                modifier = Modifier
                    .size(ADD_BUTTON_SIZE)
                    .testTag("add_list_item_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add line",
                    tint = amberAccent,
                    modifier = Modifier.size(ADD_ICON_SIZE)
                )
            }
        }
    }
}

/** The marker the new line will get, so the add row shows what it is about to add. */
@Composable
private fun NewLineMarker(newItemFormat: EditorFormat, amberAccent: Color) {
    when (newItemFormat) {
        EditorFormat.Checklist -> LineLeadingSlot {
            TaskCheckbox(
                isChecked = false,
                amberAccent = amberAccent,
                testTag = "new_line_marker",
                onToggle = {}
            )
        }

        EditorFormat.NumberedList -> NumberGlyph(number = 1)
        else -> BulletGlyph()
    }
}

/** The field a new line is typed into; its action key adds the line. */
@Composable
private fun RowScope.NewLineField(
    newItemText: String,
    newItemFormat: EditorFormat,
    onNewItemTextChange: (String) -> Unit,
    onAddItem: () -> Unit,
    textPrimary: Color,
    textPlaceholder: Color,
    amberAccent: Color
) {
    BasicTextField(
        value = newItemText,
        onValueChange = onNewItemTextChange,
        textStyle = TextStyle(
            fontSize = LINE_FONT_SIZE,
            fontWeight = FontWeight.Normal,
            color = textPrimary,
            lineHeight = LINE_HEIGHT
        ),
        cursorBrush = SolidColor(amberAccent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAddItem() }),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (newItemText.isEmpty()) {
                    Text(
                        text = newItemFormat.placeholder,
                        style = TextStyle(
                            fontSize = LINE_FONT_SIZE,
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
            .padding(horizontal = LINE_TEXT_PADDING)
            .testTag("new_list_item_input")
    )
}

/** What the add row's field says before anything is typed into it. */
private val EditorFormat.placeholder: String
    get() = if (this == EditorFormat.Checklist) "Checklist item" else "List item"
