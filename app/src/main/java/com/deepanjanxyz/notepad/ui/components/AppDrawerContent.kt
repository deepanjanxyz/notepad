package com.deepanjanxyz.notepad.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.viewmodel.Screen

@Composable
fun AppDrawerContent(
    currentScreen: Screen,
    selectedTagFilter: String?,
    labels: List<String>,
    onSelectNotes: () -> Unit,
    onSelectTag: (String) -> Unit,
    onOpenEditLabels: () -> Unit,
    onSelectArchive: () -> Unit,
    onSelectTrash: () -> Unit,
    onSelectSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight()
            .testTag("modal_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: App Title ("Elite Memo Pro")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Elite Memo Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Smart Notes & Organization",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Item 1: Notes (Main View)
            NavigationDrawerItem(
                label = { Text("Notes", fontWeight = FontWeight.Medium) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Home && selectedTagFilter == null) Icons.Filled.Description else Icons.Outlined.Description,
                        contentDescription = "Notes"
                    )
                },
                selected = currentScreen is Screen.Home && selectedTagFilter == null,
                onClick = onSelectNotes,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_notes")
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Item 2: Labels / Tags Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Labels",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onOpenEditLabels,
                    modifier = Modifier.testTag("drawer_edit_labels_header_button")
                ) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sub-items: each label
            labels.forEach { label ->
                val isSelected = currentScreen is Screen.Home && selectedTagFilter.equals(label, ignoreCase = true)
                NavigationDrawerItem(
                    label = { Text(label) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Label else Icons.Outlined.Label,
                            contentDescription = label,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = isSelected,
                    onClick = { onSelectTag(label) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_tag_item_$label")
                )
            }

            // Sub-item: "+ Create new label / Edit Labels"
            NavigationDrawerItem(
                label = { Text("+ Create new label / Edit Labels", style = MaterialTheme.typography.bodyMedium) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create or edit labels",
                        modifier = Modifier.size(20.dp)
                    )
                },
                selected = false,
                onClick = onOpenEditLabels,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_create_or_edit_labels_item")
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Item 3: Archive
            NavigationDrawerItem(
                label = { Text("Archive", fontWeight = FontWeight.Medium) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Archive) Icons.Filled.Archive else Icons.Outlined.Archive,
                        contentDescription = "Archive"
                    )
                },
                selected = currentScreen is Screen.Archive,
                onClick = onSelectArchive,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_archive")
            )

            // Item 4: Trash (Recycler view for deleted notes)
            NavigationDrawerItem(
                label = { Text("Trash", fontWeight = FontWeight.Medium) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Trash) Icons.Filled.Delete else Icons.Outlined.Delete,
                        contentDescription = "Trash"
                    )
                },
                selected = currentScreen is Screen.Trash,
                onClick = onSelectTrash,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_trash")
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Item 5: Settings (Inside the Navigation Drawer at the bottom)
            NavigationDrawerItem(
                label = { Text("Settings", fontWeight = FontWeight.Medium) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                },
                selected = currentScreen is Screen.Settings,
                onClick = onSelectSettings,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_settings")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // GitHub Source in Drawer Footer - Neatly sized 24.dp Octocat vector in a single Material 3 Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/deepanjanxyz/notepad")
                        )
                        context.startActivity(browserIntent)
                    }
                    .testTag("drawer_github_row")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Source on GitHub",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
