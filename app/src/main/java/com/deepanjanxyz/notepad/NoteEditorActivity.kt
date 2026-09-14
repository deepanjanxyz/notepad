package com.deepanjanxyz.notepad

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteEditorActivity : ComponentActivity() {

    private val dbHelper by lazy { DatabaseHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        val initialTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val initialContent = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        setContent {
            EditorScreen(initialNoteId, initialTitle, initialContent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditorScreen(initialNoteId: Long, initialTitle: String, initialContent: String) {
        var title by rememberSaveable { mutableStateOf(initialTitle) }
        var content by rememberSaveable { mutableStateOf(initialContent) }
        var noteId by rememberSaveable { mutableStateOf(initialNoteId) }

        // Last values actually written to the database. persist() compares
        // against these (not the launch-time values) so that reverting an
        // auto-saved edit is still detected as a change and gets saved.
        var lastPersistedTitle by rememberSaveable { mutableStateOf(initialTitle) }
        var lastPersistedContent by rememberSaveable { mutableStateOf(initialContent) }

        /**
         * Persists the note if anything actually changed since the last save.
         */
        fun persist() {
            if (title == lastPersistedTitle && content == lastPersistedContent) return
            val trimmedTitle = title.trim()
            val trimmedContent = content.trim()
            // Never persist a fully empty note (parity with the original app).
            if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) return
            val date = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
            if (noteId == -1L) {
                noteId = dbHelper.insertNote(trimmedTitle, trimmedContent, date)
            } else {
                dbHelper.updateNote(noteId, trimmedTitle, trimmedContent, date)
            }
            lastPersistedTitle = title
            lastPersistedContent = content
        }

        // Debounced auto-save on every change.
        LaunchedEffect(title, content) {
            delay(AUTO_SAVE_DELAY_MS)
            persist()
        }

        // Save one last time when the editor leaves the screen
        // (finish, configuration change, or process teardown).
        DisposableEffect(Unit) {
            onDispose { persist() }
        }

        fun saveAndFinish() {
            persist()
            Toast.makeText(this@NoteEditorActivity, R.string.note_saved, Toast.LENGTH_SHORT).show()
            finish()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (noteId == -1L) {
                                stringResource(R.string.new_note)
                            } else {
                                stringResource(R.string.edit_note)
                            },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { saveAndFinish() }) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = stringResource(R.string.action_save),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.note_title_hint)) },
                    singleLine = true,
                    textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    placeholder = { Text(stringResource(R.string.note_content_hint)) },
                )
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
        private const val DATE_FORMAT = "MMM dd, HH:mm"
        private const val AUTO_SAVE_DELAY_MS = 400L
    }
}
