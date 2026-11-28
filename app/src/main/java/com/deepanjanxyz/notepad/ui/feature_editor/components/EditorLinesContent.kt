package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.FIRST_LIST_NUMBER
import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.domain.model.RichInlineSpan
import com.deepanjanxyz.notepad.domain.model.RichLinkSpan
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.components.StatefulBasicTextField
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.feature_editor.model.buildItemOutputTransformation

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
 * keeps its number. Each line also displays its inline rich-text formatting spans
 * visually without showing raw markers.
 */
@Suppress("LongParameterList")
@Composable
fun EditorLinesContent(
    segments: List<NoteSegment>,
    segmentInlineSpans: List<List<RichInlineSpan>>,
    segmentLinkSpans: List<List<RichLinkSpan>>,
    newItemText: String,
    newItemSpans: List<RichInlineSpan>,
    newItemLinkSpans: List<RichLinkSpan>,
    newItemFormat: EditorFormat,
    findQuery: String = "",
    findHighlightColor: Color = Color.Transparent,
    findActiveColor: Color = Color.Transparent,
    activeMatchSegmentIndex: Int? = null,
    activeMatchRange: IntRange? = null,
    activeMatchJumpToken: Int = 0,
    newLineFocusIndex: Int = -1,
    onNewItemTextChange: (String, Int) -> Unit,
    onNewItemSelectionChange: (TextRange) -> Unit,
    onNewItemFocusChange: (TextRange) -> Unit,
    onAddItem: () -> Unit,
    onToggleTask: (segmentIndex: Int) -> Unit,
    onSegmentTextChange: (segmentIndex: Int, text: String, cursorOffset: Int) -> Unit,
    onSegmentSelectionChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSegmentFocusChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit,
    onDeleteSegment: (segmentIndex: Int) -> Unit,
    textPrimary: Color = MaterialTheme.colorScheme.onBackground,
    textPlaceholder: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
    textMuted: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        segments.forEachIndexed { index, segment ->
            val spans = segmentInlineSpans.getOrNull(index).orEmpty()
            val links = segmentLinkSpans.getOrNull(index).orEmpty()
            val rowActiveRange = if (activeMatchSegmentIndex == index) activeMatchRange else null
            EditorLine(
                segment = segment,
                segmentIndex = index,
                itemSpans = spans,
                itemLinks = links,
                findQuery = findQuery,
                findHighlightColor = findHighlightColor,
                activeMatchRange = rowActiveRange,
                findActiveColor = findActiveColor,
                activeMatchJumpToken = activeMatchJumpToken,
                textPrimary = textPrimary,
                textPlaceholder = textPlaceholder,
                textMuted = textMuted,
                accentColor = accentColor,
                onAccentColor = onAccentColor,
                onToggleTask = onToggleTask,
                onSegmentTextChange = onSegmentTextChange,
                onSegmentSelectionChange = onSegmentSelectionChange,
                onSegmentFocusChange = onSegmentFocusChange,
                onSplitSegment = onSplitSegment,
                onDeleteSegment = onDeleteSegment
            )
        }

        NewLineRow(
            newItemText = newItemText,
            newItemSpans = newItemSpans,
            newItemLinkSpans = newItemLinkSpans,
            newItemFormat = newItemFormat,
            newItemNumber = nextLineNumber(segments),
            findQuery = findQuery,
            findHighlightColor = findHighlightColor,
            activeMatchRange = if (activeMatchSegmentIndex == newLineFocusIndex) activeMatchRange else null,
            findActiveColor = findActiveColor,
            activeMatchJumpToken = activeMatchJumpToken,
            onNewItemTextChange = onNewItemTextChange,
            onNewItemSelectionChange = onNewItemSelectionChange,
            onNewItemFocusChange = onNewItemFocusChange,
            onAddItem = onAddItem,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            textMuted = textMuted,
            accentColor = accentColor,
            onAccentColor = onAccentColor
        )
    }
}

/** One line of the note, rendered as the kind of line it is. */
@Suppress("LongParameterList")
@Composable
private fun EditorLine(
    segment: NoteSegment,
    segmentIndex: Int,
    itemSpans: List<RichInlineSpan>,
    itemLinks: List<RichLinkSpan>,
    findQuery: String,
    findHighlightColor: Color,
    activeMatchRange: IntRange?,
    findActiveColor: Color,
    activeMatchJumpToken: Int,
    textPrimary: Color,
    textPlaceholder: Color,
    textMuted: Color,
    accentColor: Color,
    onAccentColor: Color,
    onToggleTask: (segmentIndex: Int) -> Unit,
    onSegmentTextChange: (segmentIndex: Int, text: String, cursorOffset: Int) -> Unit,
    onSegmentSelectionChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSegmentFocusChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit,
    onDeleteSegment: (segmentIndex: Int) -> Unit
) {
    val isDone = segment is NoteSegment.Task && segment.item.isChecked
    val outputTransformation = remember(
        itemSpans,
        itemLinks,
        accentColor,
        isDone,
        findQuery,
        findHighlightColor,
        activeMatchRange,
        findActiveColor
    ) {
        buildItemOutputTransformation(
            itemSpans = itemSpans,
            itemLinks = itemLinks,
            linkColor = accentColor,
            forceStrikethrough = isDone,
            findQuery = findQuery,
            findHighlightColor = findHighlightColor,
            activeMatchRange = activeMatchRange,
            findActiveColor = findActiveColor
        )
    }

    when (segment) {
        is NoteSegment.Text -> EditorItemRow(
            leading = { LineLeadingSlot {} },
            text = segment.text,
            isDone = false,
            segmentIndex = segmentIndex,
            outputTransformation = outputTransformation,
            isActiveMatch = activeMatchRange != null,
            activeMatchRange = activeMatchRange,
            activeMatchJumpToken = activeMatchJumpToken,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            accentColor = accentColor,
            onSegmentTextChange = onSegmentTextChange,
            onSegmentSelectionChange = onSegmentSelectionChange,
            onSegmentFocusChange = onSegmentFocusChange,
            onSplitSegment = onSplitSegment,
            onDeleteSegment = null
        )

        is NoteSegment.Task -> EditorItemRow(
            leading = {
                TaskCheckbox(
                    isChecked = segment.item.isChecked,
                    accentColor = accentColor,
                    onAccentColor = onAccentColor,
                    uncheckedColor = textMuted,
                    testTag = "task_checkbox_$segmentIndex",
                    onToggle = { onToggleTask(segmentIndex) }
                )
            },
            text = segment.item.text,
            isDone = segment.item.isChecked,
            segmentIndex = segmentIndex,
            outputTransformation = outputTransformation,
            isActiveMatch = activeMatchRange != null,
            activeMatchRange = activeMatchRange,
            activeMatchJumpToken = activeMatchJumpToken,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            accentColor = accentColor,
            onSegmentTextChange = onSegmentTextChange,
            onSegmentSelectionChange = onSegmentSelectionChange,
            onSegmentFocusChange = onSegmentFocusChange,
            onSplitSegment = onSplitSegment,
            onDeleteSegment = onDeleteSegment
        )

        is NoteSegment.Bullet -> EditorItemRow(
            leading = { BulletGlyph(color = textMuted) },
            text = segment.text,
            isDone = false,
            segmentIndex = segmentIndex,
            outputTransformation = outputTransformation,
            isActiveMatch = activeMatchRange != null,
            activeMatchRange = activeMatchRange,
            activeMatchJumpToken = activeMatchJumpToken,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            accentColor = accentColor,
            onSegmentTextChange = onSegmentTextChange,
            onSegmentSelectionChange = onSegmentSelectionChange,
            onSegmentFocusChange = onSegmentFocusChange,
            onSplitSegment = onSplitSegment,
            onDeleteSegment = onDeleteSegment
        )

        is NoteSegment.Numbered -> EditorItemRow(
            leading = { NumberGlyph(number = segment.number, color = textMuted) },
            text = segment.text,
            isDone = false,
            segmentIndex = segmentIndex,
            outputTransformation = outputTransformation,
            isActiveMatch = activeMatchRange != null,
            activeMatchRange = activeMatchRange,
            activeMatchJumpToken = activeMatchJumpToken,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            accentColor = accentColor,
            onSegmentTextChange = onSegmentTextChange,
            onSegmentSelectionChange = onSegmentSelectionChange,
            onSegmentFocusChange = onSegmentFocusChange,
            onSplitSegment = onSplitSegment,
            onDeleteSegment = onDeleteSegment
        )
    }
}

private fun nextLineNumber(segments: List<NoteSegment>): Int {
    val last = segments.lastOrNull() as? NoteSegment.Numbered ?: return FIRST_LIST_NUMBER
    return if (last.number == Int.MAX_VALUE) Int.MAX_VALUE else last.number + 1
}

@Suppress("LongParameterList")
@Composable
private fun EditorItemRow(
    leading: @Composable () -> Unit,
    text: String,
    isDone: Boolean,
    segmentIndex: Int,
    outputTransformation: OutputTransformation?,
    isActiveMatch: Boolean,
    activeMatchRange: IntRange?,
    activeMatchJumpToken: Int,
    textPrimary: Color,
    textPlaceholder: Color,
    accentColor: Color,
    onSegmentTextChange: (segmentIndex: Int, text: String, cursorOffset: Int) -> Unit,
    onSegmentSelectionChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSegmentFocusChange: (segmentIndex: Int, selection: TextRange) -> Unit,
    onSplitSegment: (segmentIndex: Int) -> Unit,
    onDeleteSegment: ((segmentIndex: Int) -> Unit)?
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(isActiveMatch, activeMatchRange, activeMatchJumpToken) {
        if (isActiveMatch) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(vertical = LINE_ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        LineItemText(
            text = text,
            isDone = isDone,
            segmentIndex = segmentIndex,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            accentColor = accentColor,
            outputTransformation = outputTransformation,
            onTextChange = onSegmentTextChange,
            onSelectionChange = onSegmentSelectionChange,
            onFocusChange = onSegmentFocusChange,
            onSplitSegment = onSplitSegment
        )
        if (onDeleteSegment != null) {
            RemoveLineButton(
                segmentIndex = segmentIndex,
                tint = textPlaceholder,
                onDeleteSegment = onDeleteSegment
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun NewLineRow(
    newItemText: String,
    newItemSpans: List<RichInlineSpan>,
    newItemLinkSpans: List<RichLinkSpan>,
    newItemFormat: EditorFormat,
    newItemNumber: Int,
    findQuery: String,
    findHighlightColor: Color,
    activeMatchRange: IntRange?,
    findActiveColor: Color,
    activeMatchJumpToken: Int,
    onNewItemTextChange: (String, Int) -> Unit,
    onNewItemSelectionChange: (TextRange) -> Unit,
    onNewItemFocusChange: (TextRange) -> Unit,
    onAddItem: () -> Unit,
    textPrimary: Color,
    textPlaceholder: Color,
    textMuted: Color,
    accentColor: Color,
    onAccentColor: Color
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(activeMatchRange, activeMatchJumpToken) {
        if (activeMatchRange != null) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(vertical = ADD_ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NewLineMarker(
            newItemFormat = newItemFormat,
            newItemNumber = newItemNumber,
            accentColor = accentColor,
            onAccentColor = onAccentColor,
            textMuted = textMuted
        )
        NewLineField(
            newItemText = newItemText,
            newItemSpans = newItemSpans,
            newItemLinkSpans = newItemLinkSpans,
            newItemFormat = newItemFormat,
            findQuery = findQuery,
            findHighlightColor = findHighlightColor,
            activeMatchRange = activeMatchRange,
            findActiveColor = findActiveColor,
            onNewItemTextChange = onNewItemTextChange,
            onNewItemSelectionChange = onNewItemSelectionChange,
            onNewItemFocusChange = onNewItemFocusChange,
            onAddItem = onAddItem,
            textPrimary = textPrimary,
            textPlaceholder = textPlaceholder,
            accentColor = accentColor
        )
        ActionTooltip(stringResource(R.string.editor_add_line)) {
            IconButton(
                onClick = onAddItem,
                modifier = Modifier
                    .size(ADD_BUTTON_SIZE)
                    .testTag("add_list_item_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.editor_add_line),
                    tint = accentColor,
                    modifier = Modifier.size(ADD_ICON_SIZE)
                )
            }
        }
    }
}

@Composable
private fun NewLineMarker(
    newItemFormat: EditorFormat,
    newItemNumber: Int,
    accentColor: Color,
    onAccentColor: Color,
    textMuted: Color
) {
    when (newItemFormat) {
        EditorFormat.Checklist -> LineLeadingSlot {
            TaskCheckbox(
                isChecked = false,
                accentColor = accentColor,
                onAccentColor = onAccentColor,
                uncheckedColor = textMuted,
                testTag = "new_line_marker",
                onToggle = {}
            )
        }

        EditorFormat.NumberedList -> NumberGlyph(number = newItemNumber, color = textMuted)
        else -> BulletGlyph(color = textMuted)
    }
}

@Suppress("LongParameterList")
@Composable
private fun RowScope.NewLineField(
    newItemText: String,
    newItemSpans: List<RichInlineSpan>,
    newItemLinkSpans: List<RichLinkSpan>,
    newItemFormat: EditorFormat,
    findQuery: String,
    findHighlightColor: Color,
    activeMatchRange: IntRange?,
    findActiveColor: Color,
    onNewItemTextChange: (String, Int) -> Unit,
    onNewItemSelectionChange: (TextRange) -> Unit,
    onNewItemFocusChange: (TextRange) -> Unit,
    onAddItem: () -> Unit,
    textPrimary: Color,
    textPlaceholder: Color,
    accentColor: Color
) {
    val newItemState = rememberTextFieldState(initialText = newItemText)
    val currentOnSelectionChange by rememberUpdatedState(onNewItemSelectionChange)

    LaunchedEffect(newItemState) {
        snapshotFlow { newItemState.selection }.collect { sel ->
            currentOnSelectionChange(sel)
        }
    }

    val outputTransformation = remember(
        newItemSpans,
        newItemLinkSpans,
        accentColor,
        findQuery,
        findHighlightColor,
        activeMatchRange,
        findActiveColor
    ) {
        buildItemOutputTransformation(
            itemSpans = newItemSpans,
            itemLinks = newItemLinkSpans,
            linkColor = accentColor,
            forceStrikethrough = false,
            findQuery = findQuery,
            findHighlightColor = findHighlightColor,
            activeMatchRange = activeMatchRange,
            findActiveColor = findActiveColor
        )
    }

    StatefulBasicTextField(
        value = newItemText,
        externalState = newItemState,
        onValueChange = { text -> onNewItemTextChange(text, newItemState.selection.end) },
        lineLimits = TextFieldLineLimits.SingleLine,
        // Keep pasted multi-line text on one item so lines and span indices stay aligned.
        inputTransformation = InputTransformation.byValue { _, proposed ->
            proposed.toString().replace('\n', ' ')
        },
        outputTransformation = outputTransformation,
        textStyle = TextStyle(
            fontSize = LINE_FONT_SIZE,
            fontWeight = FontWeight.Normal,
            color = textPrimary,
            lineHeight = LINE_HEIGHT
        ),
        cursorBrush = SolidColor(accentColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { onAddItem() },
        decorator = { innerTextField ->
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
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onNewItemFocusChange(newItemState.selection)
                }
            }
            .testTag("new_list_item_input")
    )
}

private val EditorFormat.placeholder: String
    get() = if (this == EditorFormat.Checklist) "Checklist item" else "List item"
