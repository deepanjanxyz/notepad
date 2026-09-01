package com.deepanjanxyz.notepad.features.editor

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepanjanxyz.notepad.core.designsystem.component.ConfirmationDialog
import com.deepanjanxyz.notepad.features.editor.components.MarkdownPreview
import com.deepanjanxyz.notepad.features.editor.components.MarkdownToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    BackHandler {
        viewModel.saveNoteLocally()
        onNavigateBack()
    }

    fun shareNoteText() {
        val shareBody = buildString {
            if (uiState.title.isNotBlank()) {
                append(uiState.title)
                append("\n\n")
            }
            append(uiState.content)
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareBody)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Note")
        context.startActivity(shareIntent)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isSaved) "Saved" else "Editing...",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (uiState.isSaved) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveNoteLocally()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleMarkdownPreview() }) {
                        Icon(
                            imageVector = if (uiState.isMarkdownPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (uiState.isMarkdownPreview) "Edit mode" else "Preview markdown"
                        )
                    }
                    IconButton(onClick = { shareNoteText() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share note"
                        )
                    }
                    if (uiState.noteId > 0L) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveNoteLocally()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save and exit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (!uiState.isMarkdownPreview) {
                    MarkdownToolbar(
                        onInsert = { prefix, suffix ->
                            viewModel.insertMarkdown(prefix, suffix)
                        }
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.date.isNotBlank()) "Last saved: ${uiState.date}" else "Draft",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${uiState.wordCount} words • ${uiState.charCount} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isMarkdownPreview) {
                MarkdownPreview(
                    title = uiState.title,
                    content = uiState.content,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    TextField(
                        value = uiState.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        placeholder = {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    TextField(
                        value = uiState.content,
                        onValueChange = { viewModel.onContentChanged(it) },
                        placeholder = {
                            Text(
                                "Type something...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.deleteNote(onDeleted = onNavigateBack)
            },
            title = "Delete Note?",
            text = "Are you sure you want to delete this note? This action cannot be undone.",
            confirmText = "Delete",
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }
}
