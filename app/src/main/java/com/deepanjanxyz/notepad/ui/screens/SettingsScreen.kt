package com.deepanjanxyz.notepad.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.viewmodel.ImportOutcome
import com.deepanjanxyz.notepad.ui.viewmodel.NotesUiState
import com.deepanjanxyz.notepad.ui.components.ActionTooltip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: NotesUiState,
    notes: List<Note>,
    backupNotes: List<Note>,
    importOutcome: ImportOutcome?,
    onThemeChange: (String) -> Unit,
    onLockToggle: (Boolean) -> Unit,
    onImportNotes: (List<Note>) -> Unit,
    onImportOutcomeShown: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val versionName = getAppVersionName(context)

    var showOverwriteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var availableBackups by remember { mutableStateOf(listOf<File>()) }

    fun performExport(target: File) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { target.writeText(buildBackupJson(backupNotes)) }.isSuccess
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar(
                if (ok) "Backup saved to ${target.name}" else "Couldn't save the backup"
            )
        }
    }

    fun restoreFromText(text: String) {
        val parsed = parseBackupJson(text)
        if (parsed.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("No notes found in that file") }
        } else {
            onImportNotes(parsed)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readBytes().toString(Charsets.UTF_8)
                        }
                    }.getOrNull().orEmpty()
                }
                restoreFromText(text)
            }
        }
    }

    LaunchedEffect(importOutcome) {
        val outcome = importOutcome ?: return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        snackbarHostState.showSnackbar(
            if (outcome.skipped > 0) {
                "Imported ${outcome.imported} note(s), skipped ${outcome.skipped} duplicate(s)"
            } else {
                "Imported ${outcome.imported} note(s)"
            }
        )
        onImportOutcomeShown()
    }

    BackHandler {
        onNavigateBack()
    }

    val totalNotes = notes.size
    val totalPinned = notes.count { it.isPinned }
    val totalWords = notes.sumOf {
        if (it.content.isBlank()) 0 else it.content.trim().split("\\s+".toRegex()).size
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    ActionTooltip("Back") {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Stats Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Workspace Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalNotes",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalPinned",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalWords",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Words",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Theme Settings Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = stringResource(R.string.theme_setting),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("system", "System", Icons.Default.SettingsBrightness),
                            Triple("light", "Light", Icons.Default.LightMode),
                            Triple("dark", "Dark", Icons.Default.DarkMode)
                        ).forEach { (mode, label, icon) ->
                            val isSelected = uiState.themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onThemeChange(mode) },
                                label = { Text(label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_$mode")
                            )
                        }
                    }
                }
            }

            // Security Settings Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.biometric_lock),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.biometric_lock_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = uiState.lockEnabled,
                            onCheckedChange = onLockToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }
                }
            }

            // Backup & Restore Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Backups are stored in the app's own backup folder. Export keeps a single file: if one already exists you can overwrite it or save a separate copy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (defaultBackupFile(context).exists()) {
                                    showOverwriteDialog = true
                                } else {
                                    performExport(defaultBackupFile(context))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_notes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export")
                        }

                        OutlinedButton(
                            onClick = {
                                availableBackups = listBackups(context)
                                showRestoreDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_notes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            // About Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = stringResource(R.string.app_info),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.version_label, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Crafted with Kotlin Coroutines, Jetpack Compose Material 3, and Room local persistence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    GitHubLinkRow(
                        label = "View Source on GitHub",
                        testTag = "github_source_row",
                        url = "https://github.com/deepanjanxyz/notepad"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GitHubLinkRow(
                        label = "Report an Issue",
                        testTag = "github_issue_row",
                        url = "https://github.com/deepanjanxyz/notepad/issues"
                    )
                }
            }
        }
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("Backup already exists") },
            text = { Text("A backup file already exists in the app's backup folder. Overwrite it, or save a separate copy?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverwriteDialog = false
                        performExport(defaultBackupFile(context))
                    },
                    modifier = Modifier.testTag("overwrite_backup_button")
                ) {
                    Text("Overwrite")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showOverwriteDialog = false
                            performExport(newBackupFile(context))
                        },
                        modifier = Modifier.testTag("new_copy_backup_button")
                    ) {
                        Text("New copy")
                    }
                    TextButton(onClick = { showOverwriteDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore notes") },
            text = {
                Column {
                    if (availableBackups.isEmpty()) {
                        Text("No backups found in the app's backup folder.")
                    } else {
                        availableBackups.forEach { file ->
                            TextButton(
                                onClick = {
                                    showRestoreDialog = false
                                    scope.launch {
                                        val text = withContext(Dispatchers.IO) {
                                            runCatching { file.readText() }.getOrDefault("")
                                        }
                                        restoreFromText(text)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(file.name, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    modifier = Modifier.testTag("browse_backup_button")
                ) {
                    Text("Choose file")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GitHubLinkRow(label: String, testTag: String, url: String) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = "GitHub",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Suppress("DEPRECATION")
private fun getAppVersionName(context: Context): String =
    requireNotNull(context.packageManager.getPackageInfo(context.packageName, 0).versionName) {
        "App versionName is missing"
    }

/** The app-managed folder where backups live (app-specific external storage, no permission needed). */
private fun backupDir(context: Context): File {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    return File(base, "backups").apply { if (!exists()) mkdirs() }
}

private fun defaultBackupFile(context: Context): File =
    File(backupDir(context), "elite-memo-backup.json")

private fun newBackupFile(context: Context): File {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return File(backupDir(context), "elite-memo-backup-$stamp.json")
}

private fun listBackups(context: Context): List<File> =
    backupDir(context)
        .listFiles { file -> file.isFile && file.name.endsWith(".json") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

/** Serializes the given notes into a portable JSON backup document. */
private fun buildBackupJson(notes: List<Note>): String {
    val array = JSONArray()
    notes.forEach { note ->
        val obj = JSONObject()
        obj.put("title", note.title)
        obj.put("content", note.content)
        obj.put("date", note.date)
        obj.put("colorIndex", note.colorIndex)
        obj.put("isPinned", note.isPinned)
        obj.put("inArchive", note.inArchive)
        obj.put("reminderTime", note.reminderTime ?: JSONObject.NULL)
        val tagArray = JSONArray()
        note.tags.forEach { tagArray.put(it) }
        obj.put("tags", tagArray)
        array.put(obj)
    }
    val root = JSONObject()
    root.put("app", "Elite Memo Pro")
    root.put("version", 1)
    root.put("notes", array)
    return root.toString(2)
}

/** Parses a JSON backup document produced by [buildBackupJson]. */
private fun parseBackupJson(text: String): List<Note> {
    if (text.isBlank()) return emptyList()
    return runCatching {
        val root = JSONObject(text)
        val array = root.optJSONArray("notes")
        val result = mutableListOf<Note>()
        if (array != null) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val tagArray = obj.optJSONArray("tags")
                val tags = if (tagArray == null) {
                    emptyList()
                } else {
                    (0 until tagArray.length())
                        .mapNotNull { index -> tagArray.optString(index).takeIf { it.isNotBlank() } }
                }
                result.add(
                    Note(
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        date = obj.optString("date"),
                        colorIndex = obj.optInt("colorIndex"),
                        isPinned = obj.optBoolean("isPinned"),
                        inArchive = obj.optBoolean("inArchive"),
                        tags = tags,
                        reminderTime = if (obj.isNull("reminderTime")) null else obj.optLong("reminderTime")
                    )
                )
            }
        }
        result.toList()
    }.getOrDefault(emptyList())
}
