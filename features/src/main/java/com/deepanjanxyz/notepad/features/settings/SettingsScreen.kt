package com.deepanjanxyz.notepad.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepanjanxyz.notepad.core.designsystem.component.EliteCenterAlignedTopAppBar
import com.deepanjanxyz.notepad.core.security.BiometricHelper
import com.deepanjanxyz.notepad.features.settings.components.PreferenceItem
import com.deepanjanxyz.notepad.features.settings.components.PreferenceSection
import com.deepanjanxyz.notepad.features.settings.components.SwitchPreferenceItem
import com.deepanjanxyz.notepad.features.settings.components.ThemeSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            EliteCenterAlignedTopAppBar(
                title = "Settings",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceSection(title = "Appearance") {
                PreferenceItem(
                    title = "Theme",
                    summary = uiState.themeMode.title,
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            PreferenceSection(title = "Security") {
                SwitchPreferenceItem(
                    title = "Device Lock",
                    summary = if (uiState.isBiometricAvailable) {
                        "Unlock using Fingerprint or PIN"
                    } else {
                        "Biometric authentication not supported on this device"
                    },
                    icon = Icons.Default.Fingerprint,
                    checked = uiState.isBiometricLockEnabled,
                    enabled = uiState.isBiometricAvailable,
                    onCheckedChange = { targetState ->
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricHelper.showBiometricPrompt(
                                activity = activity,
                                title = "Confirm Security",
                                subtitle = if (targetState) "Authenticate to enable lock" else "Authenticate to disable lock",
                                onSuccess = { viewModel.setBiometricLockEnabled(targetState) },
                                onError = {},
                                onCancel = {}
                            )
                        } else {
                            viewModel.setBiometricLockEnabled(targetState)
                        }
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            PreferenceSection(title = "Statistics") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Note Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(title = "Notes", value = uiState.stats.totalNotes.toString())
                            StatItem(title = "Words", value = uiState.stats.totalWords.toString())
                            StatItem(title = "Chars", value = uiState.stats.totalCharacters.toString())
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            PreferenceSection(title = "About") {
                PreferenceItem(
                    title = "GitHub Repository",
                    summary = "View source code & contribute",
                    icon = Icons.Default.Code,
                    onClick = { openUrl("https://github.com/deepanjanxyz/notepad") }
                )

                PreferenceItem(
                    title = "Version",
                    summary = uiState.appVersion,
                    icon = Icons.Default.Info,
                    onClick = null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onThemeSelected = { viewModel.setThemeMode(it) },
            onDismissRequest = { showThemeDialog = false }
        )
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
