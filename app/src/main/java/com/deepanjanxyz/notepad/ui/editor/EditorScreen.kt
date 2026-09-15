package com.deepanjanxyz.notepad.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val leave: () -> Unit = {
        scope.launch {
            viewModel.flush()
            onBack()
        }
    }

    BackHandler(onBack = leave)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == -1L) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .imePadding(),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = { Text("Title") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                placeholder = { Text("Type something...") },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}
