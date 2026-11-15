package com.deepanjanxyz.notepad.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R

private val ROW_VERTICAL_PADDING = 14.dp
private val DOT_SIZE = 10.dp
private val SCREEN_HORIZONTAL_PADDING = 20.dp

/**
 * Picks the note one shortcut widget opens.
 *
 * The list is the app's own notes, so choosing a shortcut is the same choice as
 * choosing a note: no second copy of the notes exists for the home screen.
 */
@Composable
internal fun NoteShortcutConfigScreen(
    uiState: NoteShortcutConfigUiState,
    onNoteSelected: (Long) -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
        ) {
            Spacer(modifier = Modifier.height(SCREEN_HORIZONTAL_PADDING))
            Text(
                text = stringResource(R.string.widget_note_choose_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.widget_note_choose_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            NoteShortcutChoices(uiState = uiState, onNoteSelected = onNoteSelected)
            TextButton(onClick = onCancel, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(text = stringResource(R.string.widget_note_choose_cancel))
            }
        }
    }
}

@Composable
private fun ColumnScope.NoteShortcutChoices(
    uiState: NoteShortcutConfigUiState,
    onNoteSelected: (Long) -> Unit
) {
    when {
        uiState.isLoading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        uiState.choices.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.widget_note_choose_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = uiState.choices, key = { choice -> choice.noteId }) { choice ->
                NoteShortcutChoiceRow(choice = choice, onClick = { onNoteSelected(choice.noteId) })
            }
        }
    }
}

@Composable
private fun NoteShortcutChoiceRow(choice: NoteShortcutChoice, onClick: () -> Unit) {
    val dotColor = Color(noteAccentColor(LocalContext.current, choice.colorIndex))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(DOT_SIZE)
                .background(color = dotColor, shape = CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = choice.title.ifBlank { stringResource(R.string.widget_note_untitled) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (choice.preview.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = choice.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (choice.isDrawing) {
            Text(
                text = stringResource(R.string.widget_note_choose_drawing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(1.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
