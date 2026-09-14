package com.deepanjanxyz.notepad

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        var themeMode by remember { mutableStateOf(settings.themeMode) }
        var dynamicColors by remember { mutableStateOf(settings.dynamicColors) }
        var lockOnLaunch by remember { mutableStateOf(settings.lockOnLaunch) }

        val lockAvailability = remember { BiometricLockManager.lockAvailability(context) }
        val dynamicColorsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val versionName = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "?"
        }

        EliteMemoTheme(
            themeMode = themeMode,
            dynamicColor = dynamicColors && dynamicColorsSupported,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SectionHeader(stringResource(R.string.section_appearance))

                        Text(
                            text = stringResource(R.string.theme_label),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        themeMode = mode
                                        settings.themeMode = mode
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = {
                                        themeMode = mode
                                        settings.themeMode = mode
                                    },
                                )
                                Text(text = stringResource(mode.labelRes))
                            }
                        }

                        SettingSwitch(
                            title = stringResource(R.string.dynamic_colors),
                            subtitle = if (dynamicColorsSupported) {
                                stringResource(R.string.dynamic_colors_desc)
                            } else {
                                stringResource(R.string.dynamic_colors_unsupported)
                            },
                            checked = dynamicColors && dynamicColorsSupported,
                            onCheckedChange = { enabled ->
                                dynamicColors = enabled
                                settings.dynamicColors = enabled
                            },
                            enabled = dynamicColorsSupported,
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SectionHeader(stringResource(R.string.section_security))
                        SettingSwitch(
                            title = stringResource(R.string.lock_app),
                            subtitle = when (lockAvailability) {
                                BiometricLockManager.LockAvailability.READY ->
                                    stringResource(R.string.lock_app_desc)
                                BiometricLockManager.LockAvailability.NONE_ENROLLED ->
                                    stringResource(R.string.lock_app_none_enrolled)
                                BiometricLockManager.LockAvailability.UNAVAILABLE ->
                                    stringResource(R.string.lock_app_unavailable)
                            },
                            checked = lockOnLaunch,
                            onCheckedChange = { enabled ->
                                lockOnLaunch = enabled
                                settings.lockOnLaunch = enabled
                            },
                            enabled =
                                lockAvailability == BiometricLockManager.LockAvailability.READY,
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SectionHeader(stringResource(R.string.section_about))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.github_link_title))
                                Text(
                                    text = stringResource(R.string.github_link_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)),
                                    )
                                },
                            ) {
                                Text(stringResource(R.string.github_link_open))
                            }
                        }
                        Text(
                            text = stringResource(R.string.app_version, versionName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @Composable
    private fun SettingSwitch(
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        enabled: Boolean = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }

    companion object {
        private const val GITHUB_URL = "https://github.com/deepanjanxyz/notepad"
    }
}
