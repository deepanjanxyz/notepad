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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val onNotes = currentScreen is Screen.Home && selectedTagFilter == null

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
            DrawerHeader()

            DrawerNavItem(
                "Notes",
                if (onNotes) Icons.Filled.Description else Icons.Outlined.Description,
                onNotes, "drawer_nav_notes", onSelectNotes
            )

            DrawerLabelsSection(
                labels, currentScreen, selectedTagFilter, onSelectTag, onOpenEditLabels
            )

            DrawerNavItem(
                "Archive",
                if (currentScreen is Screen.Archive) Icons.Filled.Archive else Icons.Outlined.Archive,
                currentScreen is Screen.Archive, "drawer_nav_archive", onSelectArchive
            )

            DrawerNavItem(
                "Trash",
                if (currentScreen is Screen.Trash) Icons.Filled.Delete else Icons.Outlined.Delete,
                currentScreen is Screen.Trash, "drawer_nav_trash", onSelectTrash
            )

            Spacer(modifier = Modifier.weight(1f))

            DrawerDivider(horizontalPadding = 16.dp, alpha = 0.5f, verticalPadding = 8.dp)

            DrawerNavItem(
                "Settings",
                if (currentScreen is Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                currentScreen is Screen.Settings, "drawer_nav_settings", onSelectSettings
            )

            Spacer(modifier = Modifier.height(4.dp))

            DrawerGitHubFooter()
        }
    }
}

/** The app's name and tagline at the top of the drawer. */
@Composable
private fun DrawerHeader() {
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
    DrawerDivider(horizontalPadding = 16.dp, alpha = 0.5f)
    Spacer(modifier = Modifier.height(12.dp))
}

/** The labels heading, every label, and the row that opens the label editor. */
@Composable
private fun DrawerLabelsSection(
    labels: List<String>,
    currentScreen: Screen,
    selectedTagFilter: String?,
    onSelectTag: (String) -> Unit,
    onOpenEditLabels: () -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    DrawerDivider(horizontalPadding = 24.dp, alpha = 0.3f)
    Spacer(modifier = Modifier.height(8.dp))

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

    labels.forEach { label ->
        val isSelected =
            currentScreen is Screen.Home && selectedTagFilter.equals(label, ignoreCase = true)
        DrawerLabelItem(label = label, isSelected = isSelected, onClick = { onSelectTag(label) })
    }

    DrawerCreateLabelItem(onOpenEditLabels = onOpenEditLabels)

    Spacer(modifier = Modifier.height(8.dp))
    DrawerDivider(horizontalPadding = 24.dp, alpha = 0.3f)
    Spacer(modifier = Modifier.height(8.dp))
}

/** One entry in the drawer: its label, its icon and where it goes. */
@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = FontWeight.Medium) },
        icon = {
            Icon(imageVector = icon, contentDescription = label)
        },
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag(testTag)
    )
}

/** One label in the drawer's label list. */
@Composable
private fun DrawerLabelItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = {
            Icon(
                imageVector = if (isSelected) {
                    Icons.AutoMirrored.Filled.Label
                } else {
                    Icons.AutoMirrored.Outlined.Label
                },
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        },
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("drawer_tag_item_$label")
    )
}

/** The drawer row that opens the label editor. */
@Composable
private fun DrawerCreateLabelItem(onOpenEditLabels: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Text("+ Create new label / Edit Labels", style = MaterialTheme.typography.bodyMedium)
        },
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
}

/** The link to the project's source, at the foot of the drawer. */
@Composable
private fun DrawerGitHubFooter() {
    val context = LocalContext.current
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

/** A hairline between two groups of drawer entries. */
@Composable
private fun DrawerDivider(horizontalPadding: Dp, alpha: Float, verticalPadding: Dp = 0.dp) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha),
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
    )
}
