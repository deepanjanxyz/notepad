package com.deepanjanxyz.notepad.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// Palette from the reference design (dark editor)
private val EditorBackgroundDark = Color(0xFF212121)
private val EditorBackgroundLight = Color(0xFFFAFAFA)
private val TitleTextDark = Color(0xFFBDBDBD)
private val TitleTextLight = Color(0xFF202124)
private val BodyTextDark = Color(0xFF757575)
private val BodyTextLight = Color(0xFF5F6368)
private val CheckButtonColor = Color(0xFFC68B2D)
private val LabelChipColor = Color(0xFF4A3366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (dark) EditorBackgroundDark else EditorBackgroundLight
    val titleColor = if (dark) TitleTextDark else TitleTextLight
    val bodyColor = if (dark) BodyTextDark else BodyTextLight

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val leave: () -> Unit = {
        scope.launch {
            viewModel.flush()
            onBack()
        }
    }

    BackHandler(onBack = leave)

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = titleColor,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* pin: not implemented yet */ }) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Pin note", tint = titleColor)
                    }
                    IconButton(onClick = { /* view options: not implemented yet */ }) {
                        Icon(
                            Icons.Filled.FormatListBulleted,
                            contentDescription = "Bullets",
                            tint = titleColor,
                        )
                    }
                    IconButton(onClick = { /* background: not implemented yet */ }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Background color", tint = titleColor)
                    }
                    IconButton(onClick = { /* archive: not implemented yet */ }) {
                        Icon(Icons.Filled.Archive, contentDescription = "Archive", tint = titleColor)
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CheckButtonColor)
                            .clickable(onClick = leave),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
        ) {
            TextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text("Title", color = titleColor, style = MaterialTheme.typography.titleLarge)
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = titleColor),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = titleColor,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                placeholder = {
                    Text("Note", color = bodyColor, style = MaterialTheme.typography.bodyLarge)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = bodyColor),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = titleColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .clip(CircleShape)
                    .background(LabelChipColor)
                    .clickable(onClick = { /* labels: not implemented yet */ }),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Add label",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
