package com.deepanjanxyz.notepad.ui.feature_editor.components

import com.deepanjanxyz.notepad.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.components.StatefulBasicTextField

// Width of the leading slot every line shares, so their words all start together.
internal val LINE_LEADING_WIDTH = 40.dp

// Width the numbered line's number is right-aligned in.
private val LINE_NUMBER_WIDTH = 26.dp

// Gap between a line's marker and its words.
internal val LINE_TEXT_PADDING = 14.dp

// Room above and below each line, so a list does not read as one solid block.
internal val LINE_ROW_PADDING = 4.dp

// Font size and line height every line of the list is written in.
internal val LINE_FONT_SIZE = 18.sp
internal val LINE_HEIGHT = 24.sp

// The checkbox a task line draws, and the tap target around it.
private val CHECKBOX_SIZE = 20.dp
private val CHECKBOX_CORNER = 3.dp
private val CHECKBOX_BORDER = 1.8.dp
private val CHECKBOX_TICK_SIZE = 15.dp

// The remove button on each line, and the glyph inside it.
private val REMOVE_BUTTON_SIZE = 36.dp
private val REMOVE_ICON_SIZE = 18.dp

/**
 * The words of one line.
 *
 * A line is a single line of a note: a newline typed or pasted into the field
 * would split the note into an extra line on the next save, so it becomes a space
 * instead. The keyboard's action key asks for the next line rather than a newline.
 */
@Composable
internal fun RowScope.LineItemText(
    text: String,
    isDone: Boolean,
    segmentIndex: Int,
    textPrimary: Color,
    textPlaceholder: Color,
    accentColor: Color,
    outputTransformation: OutputTransformation?,
    onTextChange: (segmentIndex: Int, text: String, cursorOffset: Int) -> Unit,
    onSelectionChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onFocusChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit
) {
    val itemState = rememberTextFieldState(initialText = text)
    val currentIndex by rememberUpdatedState(segmentIndex)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)

    LaunchedEffect(itemState) {
        snapshotFlow { itemState.selection }.collect { sel ->
            currentOnSelectionChange(currentIndex, sel)
        }
    }

    StatefulBasicTextField(
        value = text,
        externalState = itemState,
        onValueChange = { newText ->
            onTextChange(segmentIndex, newText, itemState.selection.end)
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformation.byValue { _, proposed ->
            proposed.toString().replace('\n', ' ')
        },
        outputTransformation = outputTransformation,
        textStyle = TextStyle(
            fontSize = LINE_FONT_SIZE,
            fontWeight = FontWeight.Normal,
            color = if (isDone) textPlaceholder else textPrimary,
            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            lineHeight = LINE_HEIGHT
        ),
        cursorBrush = SolidColor(accentColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        onKeyboardAction = {
            onSplitSegment(segmentIndex)
        },
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = LINE_TEXT_PADDING)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocusChange(segmentIndex, itemState.selection)
                }
            }
            .testTag("line_text_$segmentIndex")
    )
}

/**
 * The checkbox of a task line.
 *
 * The tap target is larger than the box it draws: a 20dp hit area is easy to
 * miss, which made ticking an item feel unreliable.
 */
@Composable
internal fun TaskCheckbox(
    isChecked: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    uncheckedColor: Color,
    testTag: String,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(LINE_LEADING_WIDTH)
            .clickable(onClick = onToggle)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        TaskCheckboxMark(
            isChecked = isChecked,
            accentColor = accentColor,
            onAccentColor = onAccentColor,
            uncheckedColor = uncheckedColor
        )
    }
}

@Composable
private fun TaskCheckboxMark(
    isChecked: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    uncheckedColor: Color
) {
    Box(
        modifier = Modifier
            .size(CHECKBOX_SIZE)
            .clip(RoundedCornerShape(CHECKBOX_CORNER))
            .then(
                if (isChecked) {
                    Modifier.background(accentColor)
                } else {
                    Modifier.border(CHECKBOX_BORDER, uncheckedColor, RoundedCornerShape(CHECKBOX_CORNER))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.editor_completed),
                tint = onAccentColor,
                modifier = Modifier.size(CHECKBOX_TICK_SIZE)
            )
        }
    }
}

/** The dot that marks an unordered list line. */
@Composable
internal fun BulletGlyph(color: Color) {
    LineLeadingSlot {
        Text(
            text = "\u2022",
            style = TextStyle(
                fontSize = LINE_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

/** The number that marks an ordered list line, right-aligned so the words stay flush. */
@Composable
internal fun NumberGlyph(number: Int, color: Color) {
    LineLeadingSlot {
        Text(
            text = "$number.",
            style = TextStyle(
                fontSize = LINE_FONT_SIZE,
                fontWeight = FontWeight.Normal,
                color = color,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.width(LINE_NUMBER_WIDTH)
        )
    }
}

/** The fixed-width leading slot a line's marker sits in. */
@Composable
internal fun LineLeadingSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(LINE_LEADING_WIDTH),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Removes one line, leaving the lines around it in place. */
@Composable
internal fun RemoveLineButton(
    segmentIndex: Int,
    tint: Color,
    onDeleteSegment: (segmentIndex: Int) -> Unit
) {
    ActionTooltip(stringResource(R.string.editor_remove_line)) {
        IconButton(
            onClick = { onDeleteSegment(segmentIndex) },
            modifier = Modifier
                .size(REMOVE_BUTTON_SIZE)
                .testTag("remove_line_$segmentIndex")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.editor_remove_line),
                tint = tint,
                modifier = Modifier.size(REMOVE_ICON_SIZE)
            )
        }
    }
}
