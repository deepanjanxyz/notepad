package com.deepanjanxyz.notepad.features.notes.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    isGridLayout: Boolean,
    onToggleLayout: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isSelectionMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "TopBarTransition"
    ) { selectionActive ->
        if (selectionActive) {
            TopAppBar(
                title = {
                    Text(
                        text = "$selectedCount Selected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear selection"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Select all"
                        )
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = modifier
            )
        } else {
            TopAppBar(
                title = {
                    Text(
                        text = "Elite Memo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            imageVector = if (isGridLayout) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridLayout) "List view" else "Grid view"
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search notes"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = modifier
            )
        }
    }
}
