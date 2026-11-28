package com.deepanjanxyz.notepad.ui.feature_editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import com.deepanjanxyz.notepad.ui.components.StatefulBasicTextField

@Composable
internal fun EditorFindBar(
    query: String,
    matchCount: Int,
    activeMatchIndex: Int,
    hasCustomNoteColor: Boolean,
    textPrimary: Color,
    textMuted: Color,
    accentColor: Color,
    onQueryChange: (String) -> Unit,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = if (hasCustomNoteColor) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = barColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("editor_find_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                StatefulBasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = textPrimary
                    ),
                    cursorBrush = SolidColor(accentColor),
                    decorator = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_find_hint),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = textMuted.copy(alpha = 0.7f)
                                )
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_find_input")
                )
            }
            if (query.isNotEmpty()) {
                val displayedIndex = if (matchCount > 0) activeMatchIndex + 1 else 0
                Text(
                    text = stringResource(R.string.editor_find_count, displayedIndex, matchCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            ActionTooltip(stringResource(R.string.editor_find_prev)) {
                IconButton(
                    onClick = onPreviousMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.testTag("editor_find_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.editor_find_prev),
                        tint = if (matchCount > 0) textPrimary else textMuted.copy(alpha = 0.4f)
                    )
                }
            }
            ActionTooltip(stringResource(R.string.editor_find_next)) {
                IconButton(
                    onClick = onNextMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.testTag("editor_find_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.editor_find_next),
                        tint = if (matchCount > 0) textPrimary else textMuted.copy(alpha = 0.4f)
                    )
                }
            }
            ActionTooltip(stringResource(R.string.editor_find_close)) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("editor_find_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.editor_find_close),
                        tint = textPrimary
                    )
                }
            }
        }
    }
}
