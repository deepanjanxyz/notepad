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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.ui.theme.Spacing
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
    val appTitle = stringResource(R.string.app_name)

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight()
            // Announced as the drawer's pane title by TalkBack, and the drawer
            // column is a single scroll container a keyboard user can traverse.
            .semantics { paneTitle = appTitle }
            .testTag("modal_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxl, vertical = Spacing.md)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(Spacing.avatar)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(Spacing.sm)
                            .size(Spacing.iconLarge)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = appTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.drawer_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // Notes (main view). The icon is decorative: NavigationDrawerItem
            // already exposes the row label, so a content description here only
            // produced duplicated announcements.
            NavigationDrawerItem(
                label = {
                    Text(
                        text = stringResource(R.string.title_notes),
                        fontWeight = FontWeight.Medium
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Home && selectedTagFilter == null) {
                            Icons.Filled.Description
                        } else {
                            Icons.Outlined.Description
                        },
                        contentDescription = null
                    )
                },
                selected = currentScreen is Screen.Home && selectedTagFilter == null,
                onClick = onSelectNotes,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_notes")
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = Spacing.xxl)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Labels section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.xxl,
                        end = Spacing.lg,
                        top = Spacing.xs,
                        bottom = Spacing.xs
                    )
            ) {
                Text(
                    text = stringResource(R.string.drawer_labels),
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
                        text = stringResource(R.string.action_edit),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            labels.forEach { label ->
                val isSelected = currentScreen is Screen.Home &&
                    selectedTagFilter.equals(label, ignoreCase = true)
                NavigationDrawerItem(
                    label = { Text(label) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Label else Icons.Outlined.Label,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.iconMedium)
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

            NavigationDrawerItem(
                label = {
                    Text(
                        text = stringResource(R.string.drawer_create_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.iconMedium)
                    )
                },
                selected = false,
                onClick = onOpenEditLabels,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_create_or_edit_labels_item")
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = Spacing.xxl)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            NavigationDrawerItem(
                label = {
                    Text(
                        text = stringResource(R.string.title_archive),
                        fontWeight = FontWeight.Medium
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Archive) {
                            Icons.Filled.Archive
                        } else {
                            Icons.Outlined.Archive
                        },
                        contentDescription = null
                    )
                },
                selected = currentScreen is Screen.Archive,
                onClick = onSelectArchive,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_archive")
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        text = stringResource(R.string.title_trash),
                        fontWeight = FontWeight.Medium
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Trash) {
                            Icons.Filled.Delete
                        } else {
                            Icons.Outlined.Delete
                        },
                        contentDescription = null
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
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Medium
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Settings) {
                            Icons.Filled.Settings
                        } else {
                            Icons.Outlined.Settings
                        },
                        contentDescription = null
                    )
                },
                selected = currentScreen is Screen.Settings,
                onClick = onSelectSettings,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_nav_settings")
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
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
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(Spacing.iconLarge)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(
                        text = stringResource(R.string.link_view_source),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Spacing.iconSmall)
                    )
                }
            }
        }
    }
}
