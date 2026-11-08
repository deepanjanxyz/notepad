package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.theme.AppColors

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
    amberAccent: Color,
    onTextChange: (segmentIndex: Int, text: String) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit
) {
    BasicTextField(
        value = text,
        onValueChange = { newText -> onTextChange(segmentIndex, newText.replace('\n', ' ')) },
        textStyle = TextStyle(
            fontSize = LINE_FONT_SIZE,
            fontWeight = FontWeight.Normal,
            color = if (isDone) AppColors.TextPlaceholder else textPrimary,
            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            lineHeight = LINE_HEIGHT
        ),
        cursorBrush = SolidColor(amberAccent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { onSplitSegment(segmentIndex) },
            onDone = { onSplitSegment(segmentIndex) }
        ),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = LINE_TEXT_PADDING)
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
    amberAccent: Color,
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
        TaskCheckboxMark(isChecked = isChecked, amberAccent = amberAccent)
    }
}

@Composable
private fun TaskCheckboxMark(isChecked: Boolean, amberAccent: Color) {
    Box(
        modifier = Modifier
            .size(CHECKBOX_SIZE)
            .clip(RoundedCornerShape(CHECKBOX_CORNER))
            .then(
                if (isChecked) {
                    Modifier.background(amberAccent)
                } else {
                    Modifier.border(CHECKBOX_BORDER, AppColors.TextMuted, RoundedCornerShape(CHECKBOX_CORNER))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = AppColors.TextOnAmber,
                modifier = Modifier.size(CHECKBOX_TICK_SIZE)
            )
        }
    }
}

/** The dot that marks an unordered list line. */
@Composable
internal fun BulletGlyph() {
    LineLeadingSlot {
        Text(
            text = "\u2022",
            style = TextStyle(
                fontSize = LINE_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextMuted
            )
        )
    }
}

/** The number that marks an ordered list line, right-aligned so the words stay flush. */
@Composable
internal fun NumberGlyph(number: Int) {
    LineLeadingSlot {
        Text(
            text = "$number.",
            style = TextStyle(
                fontSize = LINE_FONT_SIZE,
                fontWeight = FontWeight.Normal,
                color = AppColors.TextMuted,
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
    onDeleteSegment: (segmentIndex: Int) -> Unit
) {
    ActionTooltip("Remove line") {
        IconButton(
            onClick = { onDeleteSegment(segmentIndex) },
            modifier = Modifier
                .size(REMOVE_BUTTON_SIZE)
                .testTag("remove_line_$segmentIndex")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove line",
                tint = AppColors.TextPlaceholder,
                modifier = Modifier.size(REMOVE_ICON_SIZE)
            )
        }
    }
}
