package com.deepanjanxyz.notepad

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.ui.components.AppDrawerContent
import com.deepanjanxyz.notepad.ui.components.EditLabelsDialog
import com.deepanjanxyz.notepad.ui.screens.ArchiveScreen
import com.deepanjanxyz.notepad.ui.screens.DrawingScreen
import com.deepanjanxyz.notepad.ui.screens.HomeScreen
import com.deepanjanxyz.notepad.ui.screens.NoteEditorScreen
import com.deepanjanxyz.notepad.ui.screens.SettingsScreen
import com.deepanjanxyz.notepad.ui.screens.TrashScreen
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme
import com.deepanjanxyz.notepad.ui.viewmodel.FeedbackType
import com.deepanjanxyz.notepad.ui.viewmodel.NotesViewModel
import com.deepanjanxyz.notepad.ui.viewmodel.Screen
import kotlinx.coroutines.launch

/** Extra used by the reminder notification to deep link into a specific note. */
private const val EXTRA_OPEN_NOTE_ID = "open_note_id"

class MainActivity : FragmentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
            val rawActiveNotes by viewModel.rawActiveNotes.collectAsStateWithLifecycle()
            val archiveNotes by viewModel.archiveNotes.collectAsStateWithLifecycle()
            val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()
            val roomLabels by viewModel.roomLabels.collectAsStateWithLifecycle()
            val allTags by viewModel.allTags.collectAsStateWithLifecycle()
            val isContentReady by viewModel.isContentReady.collectAsStateWithLifecycle()

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }

            // A reminder notification must not push the user past the app lock.
            val pendingNoteId = intent?.getLongExtra(EXTRA_OPEN_NOTE_ID, -1L) ?: -1L
            LaunchedEffect(uiState.isLocked, pendingNoteId) {
                if (!uiState.isLocked && pendingNoteId > 0L) {
                    viewModel.navigateTo(Screen.Editor(pendingNoteId))
                    intent?.removeExtra(EXTRA_OPEN_NOTE_ID)
                }
            }

            // Single place that turns a view-model feedback message into a
            // localised snackbar, offering Undo for reversible operations.
            val feedback = uiState.feedback
            LaunchedEffect(feedback) {
                if (feedback == null) return@LaunchedEffect
                val message = when (feedback.type) {
                    FeedbackType.MOVED_TO_TRASH ->
                        context.getString(R.string.snackbar_moved_to_trash, feedback.count)
                    FeedbackType.ARCHIVED ->
                        context.getString(R.string.snackbar_archived, feedback.count)
                    FeedbackType.RESTORED ->
                        context.getString(R.string.snackbar_restored, feedback.count)
                    FeedbackType.DELETED_PERMANENTLY ->
                        context.getString(R.string.snackbar_deleted_permanently, feedback.count)
                    FeedbackType.ACTION_FAILED -> context.getString(R.string.error_title)
                }
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = if (feedback.isUndoable) {
                        context.getString(R.string.action_undo)
                    } else {
                        null
                    },
                    withDismissAction = !feedback.isUndoable,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoLastAction()
                } else {
                    viewModel.dismissFeedback()
                }
            }

            EliteMemoTheme(themeMode = uiState.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.isLocked && uiState.lockEnabled) {
                        LockScreen(
                            onUnlockRequest = { showBiometricPrompt() },
                            onBypass = { viewModel.unlockApp() },
                            showBypassAction = !canAuthenticateWithDeviceCredentials()
                        )

                        LaunchedEffect(Unit) {
                            showBiometricPrompt()
                        }
                    } else {
                        // Navigation Drawer Menu (ModalNavigationDrawer)
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = uiState.currentScreen !is Screen.Editor && !uiState.isSelectionMode,
                            drawerContent = {
                                AppDrawerContent(
                                    currentScreen = uiState.currentScreen,
                                    selectedTagFilter = uiState.selectedTagFilter,
                                    labels = roomLabels,
                                    onSelectNotes = {
                                        viewModel.onTagFilterChange(null)
                                        viewModel.navigateTo(Screen.Home)
                                        scope.launch { drawerState.close() }
                                    },
                                    onSelectTag = { tag ->
                                        viewModel.onTagFilterChange(tag)
                                        viewModel.navigateTo(Screen.Home)
                                        scope.launch { drawerState.close() }
                                    },
                                    onOpenEditLabels = {
                                        viewModel.setEditLabelsDialogVisible(true)
                                        scope.launch { drawerState.close() }
                                    },
                                    onSelectArchive = {
                                        viewModel.navigateTo(Screen.Archive)
                                        scope.launch { drawerState.close() }
                                    },
                                    onSelectTrash = {
                                        viewModel.navigateTo(Screen.Trash)
                                        scope.launch { drawerState.close() }
                                    },
                                    onSelectSettings = {
                                        viewModel.navigateTo(Screen.Settings)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                            ) { innerPadding ->
                                AnimatedContent(
                                    targetState = uiState.currentScreen,
                                    transitionSpec = {
                                        val forward = navigationDepth(targetState) >= navigationDepth(initialState)
                                        val enter = slideInHorizontally(
                                            animationSpec = tween(
                                                durationMillis = 280,
                                                easing = FastOutSlowInEasing
                                            ),
                                            initialOffsetX = { width ->
                                                if (forward) width / 5 else -width / 5
                                            }
                                        ) + fadeIn(animationSpec = tween(durationMillis = 220))
                                        val exit = fadeOut(animationSpec = tween(durationMillis = 160))
                                        enter togetherWith exit
                                    },
                                    label = "screen_transition",
                                    modifier = Modifier.padding(innerPadding)
                                ) { screen ->
                                    when (screen) {
                                        is Screen.Home -> {
                                            HomeScreen(
                                                uiState = uiState,
                                                notes = filteredNotes,
                                                allTags = allTags,
                                                isContentReady = isContentReady,
                                                onOpenDrawer = {
                                                    scope.launch { drawerState.open() }
                                                },
                                                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                                onColorFilterChange = { viewModel.onColorFilterChange(it) },
                                                onTagFilterChange = { viewModel.onTagFilterChange(it) },
                                                onToggleLayout = { viewModel.toggleLayoutView() },
                                                onNoteClick = { note ->
                                                    if (DrawingSerializer.isDrawing(note.content)) {
                                                        viewModel.navigateTo(Screen.Drawing(note.id))
                                                    } else {
                                                        viewModel.navigateTo(Screen.Editor(note.id))
                                                    }
                                                },
                                                onNoteLongClick = { note ->
                                                    viewModel.toggleSelection(note.id)
                                                },
                                                onTogglePin = { note ->
                                                    viewModel.togglePin(note.id, note.isPinned)
                                                },
                                                onTogglePinSelected = { viewModel.togglePinForSelected() },
                                                onAddNewNote = {
                                                    viewModel.navigateTo(Screen.Editor(0L))
                                                },
                                                onAddNewDrawingNote = {
                                                    viewModel.navigateTo(Screen.Drawing(0L))
                                                },
                                                onClearSelection = { viewModel.clearSelection() },
                                                onSelectAll = { viewModel.selectAll(filteredNotes) },
                                                onMoveSelectedToTrash = { viewModel.moveSelectedToTrash() },
                                                onMoveSelectedToArchive = { viewModel.moveSelectedToArchive() }
                                            )
                                        }

                                        is Screen.Editor -> {
                                            NoteEditorScreen(
                                                noteId = screen.noteId,
                                                availableTags = allTags,
                                                onGetNote = { id -> viewModel.getNote(id) },
                                                onSaveNote = { id, title, content, colorIndex, tags, isPinned, inArchive, reminderTime ->
                                                    viewModel.saveNote(id, title, content, colorIndex, tags, isPinned, inArchive, reminderTime)
                                                },
                                                onAddLabel = { label -> viewModel.addLabel(label) },
                                                onMoveToArchive = { id -> viewModel.moveToArchive(id) },
                                                onMoveToTrash = { id -> viewModel.moveToTrash(id) },
                                                onOpenDrawing = { noteId -> viewModel.navigateTo(Screen.Drawing(noteId)) },
                                                onNavigateBack = { viewModel.navigateTo(Screen.Home) }
                                            )
                                        }

                                        is Screen.Drawing -> {
                                            DrawingScreen(
                                                noteId = screen.noteId,
                                                onGetNote = { id -> viewModel.getNote(id) },
                                                onSaveNote = { id, title, content, colorIndex, tags, isPinned, inArchive ->
                                                    viewModel.saveNote(id, title, content, colorIndex, tags, isPinned, inArchive)
                                                },
                                                onNavigateBack = { savedId ->
                                                    if (savedId > 0L) {
                                                        viewModel.navigateTo(Screen.Editor(savedId))
                                                    } else {
                                                        viewModel.navigateTo(Screen.Home)
                                                    }
                                                }
                                            )
                                        }

                                        is Screen.Archive -> {
                                            ArchiveScreen(
                                                notes = archiveNotes,
                                                isGridLayout = uiState.isGridLayout,
                                                isContentReady = isContentReady,
                                                selectedNoteIds = uiState.selectedNoteIds,
                                                onOpenDrawer = {
                                                    scope.launch { drawerState.open() }
                                                },
                                                onRestoreNote = { viewModel.restoreFromArchive(it) },
                                                onMoveToTrash = { viewModel.moveToTrash(it) },
                                                onRestoreSelected = { selectedIds ->
                                                    viewModel.restoreSelectedArchiveNotes(selectedIds)
                                                },
                                                onMoveSelectedToTrash = { selectedIds ->
                                                    viewModel.moveSelectedToTrash(selectedIds)
                                                },
                                                onToggleSelection = { viewModel.toggleSelection(it) },
                                                onSelectAll = { viewModel.selectAll(archiveNotes) },
                                                onClearSelection = { viewModel.clearSelection() },
                                                onNoteClick = { note ->
                                                    if (DrawingSerializer.isDrawing(note.content)) {
                                                        viewModel.navigateTo(Screen.Drawing(note.id))
                                                    } else {
                                                        viewModel.navigateTo(Screen.Editor(note.id))
                                                    }
                                                }
                                            )
                                        }

                                        is Screen.Trash -> {
                                            TrashScreen(
                                                trashNotes = trashNotes,
                                                isGridLayout = uiState.isGridLayout,
                                                isContentReady = isContentReady,
                                                selectedNoteIds = uiState.selectedNoteIds,
                                                onOpenDrawer = {
                                                    scope.launch { drawerState.open() }
                                                },
                                                onRestoreNote = { viewModel.restoreFromTrash(it) },
                                                onPermanentlyDeleteNote = { viewModel.permanentlyDelete(it) },
                                                onEmptyTrash = { viewModel.emptyTrash() },
                                                onRestoreSelected = { selectedIds ->
                                                    viewModel.restoreSelectedTrashNotes(selectedIds)
                                                },
                                                onPermanentlyDeleteSelected = { selectedIds ->
                                                    viewModel.permanentlyDeleteSelectedTrashNotes(selectedIds)
                                                },
                                                onToggleSelection = { viewModel.toggleSelection(it) },
                                                onSelectAll = { viewModel.selectAll(trashNotes) },
                                                onClearSelection = { viewModel.clearSelection() }
                                            )
                                        }

                                        is Screen.Settings -> {
                                            SettingsScreen(
                                                uiState = uiState,
                                                notes = rawActiveNotes,
                                                onThemeChange = { viewModel.setTheme(it) },
                                                onLockToggle = { enabled ->
                                                    viewModel.setLockEnabled(enabled)
                                                },
                                                onNavigateBack = { viewModel.navigateTo(Screen.Home) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Dynamic "Edit Labels" Dialog (Tag Management)
                        if (uiState.showEditLabelsDialog) {
                            EditLabelsDialog(
                                labels = roomLabels,
                                onAddLabel = { viewModel.addLabel(it) },
                                onRenameLabel = { old, new -> viewModel.renameLabel(old, new) },
                                onDeleteLabel = { viewModel.deleteLabel(it) },
                                onDismiss = { viewModel.setEditLabelsDialogVisible(false) }
                            )
                        }
                    }
                }
            }
        }
    }

    /** True when the device can authenticate at all, so the lock screen knows
     * whether offering a fallback exit is meaningful. */
    private fun canAuthenticateWithDeviceCredentials(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(this).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_title))
            .setSubtitle(getString(R.string.lock_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockApp()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            // Fallback for emulators or environments without enrolled security
            viewModel.unlockApp()
        }
    }
}

/**
 * Navigation depth drives the transition direction so going deeper slides in
 * from the leading edge and going back slides out to it, instead of the flat
 * cross-fade that made every navigation feel identical.
 */
private fun navigationDepth(screen: Screen): Int = when (screen) {
    is Screen.Home -> 0
    is Screen.Archive, is Screen.Trash -> 1
    is Screen.Settings -> 2
    is Screen.Editor -> 3
    is Screen.Drawing -> 4
}

@Composable
fun LockScreen(
    onUnlockRequest: () -> Unit,
    onBypass: () -> Unit,
    showBypassAction: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlockRequest,
                modifier = Modifier.testTag("unlock_biometric_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.lock_unlock_action))
            }

            // Only offered when the device has no enrolled credential at all -
            // previously this path silently unlocked the app with no explanation.
            if (showBypassAction) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onBypass,
                    modifier = Modifier.testTag("lock_bypass_button")
                ) {
                    Text(stringResource(R.string.lock_fallback_action))
                }
            }
        }
    }
}
