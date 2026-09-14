package com.deepanjanxyz.notepad

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androoidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androoidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androoidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.ui.theme.AccentYellow
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme

class SettingsActivity : ComponentActivity() {

    private val settings by lazy { AppSettings(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        val themeMode by remember { mutableStateOf(settings.themeMode) }
        var dynamicColors by remember { mutableStateOf(settings.dynamicColors) }
        var lockOnLaunch by remember { mutableStateOf(settings.lockOnLaunch) }

        val lockAvailability = remember { BiometricLockManager.lockAvailability(context) }
        val dynamicColorsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val versionName = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "?"
        }
        val noteCount = remember { DatabaseHelper(context).getAllNotes().size }

        EliteMemoTheme(
            themeMode = themeMode,
            dynamicColor = dynamicColors && dynamicColorsSupported,
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.settings_title),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            },
                            navigationIcon = {
                                 IconButton(onClick = { finish() }) {
                                   Icon(
                                        Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.action_back),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            },
                                 colors = TopAppBarDefaults.topAppBarColors(
                                   containerColor = MaterialTheme.colorScheme.background,
                            ),
                       )
                    },
                ) {
                    innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // --- Theme & Display ---
                        SectionLabel(stringResource(R.string.section_theme_display))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                ThemeOption(
                                    icon = Icons.Default.LightMode,
                                    label = stringResource(R.string.theme_light),
                                    selected = themeMode == ThemeMode.LIGHT,
                                    onSelect = {
                                        themeMode = ThemeMode.LIGHT
                                        settings.themeMode = ThemeMode.LIGHT
                                    },
                                )
                                ThemeOption(
                                    icon = Icons.Default.DarkMode,
                                    label = stringResource(R.string.theme_dark),
                                    selected = themeMode == ThemeMode.DARK,
                                    onSelect = {
                                        themeMode = ThemeMode.DARK
                                        settings.themeMode = ThemeMode.DARK
                                    },
                                 )
                                 ThemeOption(
                                    icon = Icons.Default.Info,
                                     label = stringResource(R.string.theme_system),
                                     selected = themeMode == ThemeMode.SYSTEM,
                                    onSelect = {
                                        themeMode = ThemeMode.SYSTEM
                                        settings.themeMode = ThemeMode.SYSTEM
                                    },
                                  )
                                 Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDividerThin()
                                SettingRow(
                                     title = stringResource(R.string.dynamic_colors),
                                    subtitle = if (dynamicColorsSupported) {
                                        stringResource(R.string.dynamic_colors_desc)
                                   } else {
                                        stringResource(R.string.dynamic_colors_unsupported)
                                    },
                                     trailing = {
                                        Switch(
                                           checked = dynamicColors && dynamicColorsSupported,
                                             onCheckedChange = { enabled ->
                                                 dynamicColors = enabled
                                                settings.dynamicColors = enabled
                                           },
                                             enabled = dynamicColorsSupported,
                                        )
                                    },
                                )
                            }
                        }

                        // --- Security ---
                        SectionLabel(stringResource(R.string.section_security))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                SettingRow(
                                    title = stringResource(R.string.lock_app),
                                    subtitle = when (lockAvailability) {
                                        BiometricLockManager.LockAvailability.READY ->
                                            stringResource(R.string.lock_app_desc)
                                        BiometricLockManager.LockAvailability.NONE_ENROLLED ->
                                            stringResource(R.string.lock_app_none_enrolled)
                                        BiometricLockManager.LockAvailability.UNAVAILABLE ->
                                            stringResource(R.string.lock_app_unavailable)
                                    },
                                    trailing = {
                                        Switch(
                                            checked = lockOnLaunch,
                                             onCheckedChange = { enabled ->
                                                  lockOnLaunch = enabled
                                                settings.lockOnLaunch = enabled
                                            },
                                             enabled = lockAvailability == BiometricLockManager.LockAvailability.READY,
                                        )
                                     },
                                 )
                            }
                        }

                        // --- Note Statistics ---
                        SectionLabel(stringResource(R.string.section_note_stats))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                StatRow(stringResource(R.string.stat_active_notes), noteCount)
                                StatRow(stringResource(R.string.stat_archived_notes), 0)
                                StatRow(stringResource(R.string.stat_trash_notes), 0)
                            }
                        }

                        // --- About ---
                        SectionLabel(stringResource(R.string.section_about))
                       Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                       ) {
                            Row(
                               modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                       .clip(CircleShape)
                                        .background(AccentYellow.copy(alpha = 0.15f)),
                                     contentAlignment = Alignment.Center,
                                ) {
                                   Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                       tint = AccentYellow,
                                       modifier = Modifier.size(20.dp),
                                   )
                                 }
                                Spacer(modifier = Modifier.size(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.app_version, versionName),
                                        style = MaterialTheme.typography.bodySmall,
                                       color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     )
                                 }
                                TextButton(onClick = {
                                     runCatching {
                                        context.startActivity(
                                           Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)),
                                       )
                                    }
                                }) {
                                    Text(
                                        stringResource(R.string.github_link_open),
                                       color = AccentYellow,
                                     )
                                }
                            }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                    }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
    }

    @Composable
    private fun HorizontalDividerThin() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
            )
    }

    @Composable
    private fun ThemeOption(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        selected: Boolean,
        onSelect: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(lorizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                 modifier = Modifier.weight(1f),
            )
            RadioButton(
                selected = selected,
                onClick = { onSelect() },
                colors = RadioButtonDefaults.colors(
                    selectedColor = AccentYellow,
                     unselectedColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }

    @Composable
    private fun SettingRow(
        title: String,
        subtitle: String,
        trailing: @Composable () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing()
        }
    }

    @Composable
    private fun StatRow(label: String, value: Int) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = AccentYellow,
            )
        }
    }

    companion object {
        private const val GITHUB_URL = "https://github.com/deepanjanxyz/notepad"
    }
}
