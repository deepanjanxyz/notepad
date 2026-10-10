package com.deepanjanxyz.notepad

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.deepanjanxyz.notepad.ui.viewmodel.NotesViewModel
import com.deepanjanxyz.notepad.ui.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    private fun handleNotificationIntent(intent: Intent?) {
        val noteId = intent?.getLongExtra("open_note_id", -1L) ?: -1L
        if (noteId > 0L) {
            viewModel.navigateTo(Screen.Editor(noteId))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the compat splash screen before super.onCreate so that the
        // Android 12 splash (with its icon reveal) also shows on Android 11 and
        // below. It also applies postSplashScreenTheme.
        val splashScreen = installSplashScreen()
        // Hand the splash over to the app smoothly: the logo scales up while the
        // splash fades out, then the splash view is removed.
        splashScreen.setOnExitAnimationListener { provider ->
            val iconView = provider.iconView
            val splashView = provider.view
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.2f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.2f)
            val fadeOut = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, fadeOut)
                duration = 300L
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        provider.remove()
                    }
                })
                start()
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
            val rawActiveNotes by viewModel.rawActiveNotes.collectAsStateWithLifecycle()
            val archiveNotes by viewModel.archiveNotes.collectAsStateWithLifecycle()
            val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()
            val roomLabels by viewModel.roomLabels.collectAsStateWithLifecycle()
            val allTags by viewModel.allTags.collectAsStateWithLifecycle()
            val autoBackupStatus by viewModel.autoBackupStatus.collectAsStateWithLifecycle()
            val importOutcome by viewModel.importOutcome.collectAsStateWithLifecycle()

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            EliteMemoTheme(themeMode = uiState.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (uiState.isLocked && uiState.lockEnabled) {
                        LockScreen(
                            onUnlockRequest = { showBiometricPrompt() },
                            onBypass = { viewModel.unlockApp() }
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
                                contentWindowInsets = WindowInsets(0, 0, 0, 0)
                            ) { innerPadding ->
                                AnimatedContent(
                                    targetState = uiState.currentScreen,
                                    transitionSpec = screenTransitionSpec(),
                                    label = "screen_transition",
                                    modifier = Modifier.padding(innerPadding)
                                ) { screen ->
                                    when (screen) {
                                        is Screen.Home -> {
                                            HomeScreen(
                                                uiState = uiState,
                                                notes = filteredNotes,
                                                allTags = allTags,
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
                                                onMoveSelectedToArchive = { viewModel.moveSelectedToArchive() },
                                                onUndoMoveToTrash = { viewModel.undoMoveToTrash() },
                                                onSortOptionChange = { viewModel.setSortOption(it) }
                                            )
                                        }

                                        is Screen.Editor -> {
                                            NoteEditorScreen(
                                                noteId = screen.noteId,
                                                availableTags = allTags,
                                                onGetNote = { id -> viewModel.getNote(id) },
                                                onSaveNote = { id, title, content, colorIndex, tags, isPinned, inArchive, reminderTime ->
                                                    viewModel.saveNote(id, title, content, colorIndex, tags, isPinned, inArchive, reminderTime).id
                                                },
                                                onAddLabel = { label -> viewModel.addLabel(label) },
                                                onMoveToArchive = { id -> viewModel.moveToArchive(id) },
                                                onMoveToTrash = { id -> viewModel.moveToTrash(id) },
                                                onClearReminder = { id -> viewModel.setNoteReminder(id, null) },
                                                onOpenDrawing = { noteId -> viewModel.navigateTo(Screen.Drawing(noteId, screen.returnTo)) },
                                                onNavigateBack = { viewModel.navigateTo(screen.returnTo) }
                                            )
                                        }

                                        is Screen.Drawing -> {
                                            DrawingScreen(
                                                noteId = screen.noteId,
                                                onGetNote = { id -> viewModel.getNote(id) },
                                                onSaveNote = { id, title, content, colorIndex, tags, isPinned, inArchive ->
                                                    viewModel.saveNote(id, title, content, colorIndex, tags, isPinned, inArchive).id
                                                },
                                                onNavigateBack = { savedId ->
                                                    if (savedId > 0L) {
                                                        viewModel.navigateTo(Screen.Editor(savedId, screen.returnTo))
                                                    } else {
                                                        viewModel.navigateTo(screen.returnTo)
                                                    }
                                                }
                                            )
                                        }

                                        is Screen.Archive -> {
                                            ArchiveScreen(
                                                notes = archiveNotes,
                                                isGridLayout = uiState.isGridLayout,
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
                                                onUndoMoveToTrash = { viewModel.undoMoveToTrash() },
                                                onNoteClick = { note ->
                                                    if (DrawingSerializer.isDrawing(note.content)) {
                                                        viewModel.navigateTo(Screen.Drawing(note.id, Screen.Archive))
                                                    } else {
                                                        viewModel.navigateTo(Screen.Editor(note.id, Screen.Archive))
                                                    }
                                                }
                                            )
                                        }

                                        is Screen.Trash -> {
                                            TrashScreen(
                                                trashNotes = trashNotes,
                                                isGridLayout = uiState.isGridLayout,
                                                onOpenDrawer = {
                                                    scope.launch { drawerState.open() }
                                                },
                                                onRestoreNote = { viewModel.restoreFromTrash(it) },
                                                onPermanentlyDeleteNote = { viewModel.permanentlyDelete(it) },
                                                onEmptyTrash = { viewModel.emptyTrash() },
                                                onRestoreSelected = { viewModel.restoreSelectedTrashNotes() },
                                                onPermanentlyDeleteSelected = { viewModel.permanentlyDeleteSelectedTrashNotes() },
                                                onNoteClick = { note ->
                                                    if (DrawingSerializer.isDrawing(note.content)) {
                                                        viewModel.navigateTo(Screen.Drawing(note.id, Screen.Trash))
                                                    } else {
                                                        viewModel.navigateTo(Screen.Editor(note.id, Screen.Trash))
                                                    }
                                                }
                                            )
                                        }

                                        is Screen.Settings -> {
                                            SettingsScreen(
                                                uiState = uiState,
                                                notes = rawActiveNotes,
                                                autoBackupStatus = autoBackupStatus,
                                                importOutcome = importOutcome,
                                                onThemeChange = { viewModel.setTheme(it) },
                                                onLockToggle = { enabled ->
                                                    viewModel.setLockEnabled(enabled)
                                                },
                                                onAutoBackupChange = { enabled ->
                                                    viewModel.setAutoBackup(enabled)
                                                },
                                                onBackUpNow = { name -> viewModel.backUp(name) },
                                                onImportNotes = { viewModel.importNotes(it) },
                                                onImportOutcomeShown = { viewModel.clearImportOutcome() },
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

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Elite Memo Pro")
            .setSubtitle("Confirm your biometric or device credentials")
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

@Composable
fun LockScreen(
    onUnlockRequest: () -> Unit,
    onBypass: () -> Unit,
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
                text = "Elite Memo Pro is Locked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authenticate to access your personal notes",
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
                Text("Unlock with Biometrics")
            }
        }
    }
}

/**
 * A rough "depth" for each destination, used to decide the direction of the
 * screen transition: higher numbers are pushed on top of lower ones, so moving
 * to a higher depth slides forward and moving to a lower depth slides back.
 */
private fun screenDepth(screen: Screen): Int = when (screen) {
    is Screen.Editor -> 1
    is Screen.Drawing -> 2
    else -> 0
}

private const val SCREEN_SLIDE_DURATION_MS = 300
private const val SCREEN_FADE_IN_DURATION_MS = 220
private const val SCREEN_FADE_OUT_DURATION_MS = 180
private const val SCREEN_SLIDE_DIVISOR = 4

/**
 * Directional transition between destinations: detail screens slide in from the
 * right and back out on the way home, so the back gesture reads as a genuine
 * "back". Same-depth destinations (drawer entries) simply cross-fade.
 */
private fun screenTransitionSpec(): AnimatedContentTransitionScope<Screen>.() -> ContentTransform = {
    val forward = screenDepth(targetState) > screenDepth(initialState)
    val backward = screenDepth(targetState) < screenDepth(initialState)
    val slideSpec = tween<IntOffset>(SCREEN_SLIDE_DURATION_MS)
    val fadeSpec = tween<Float>(SCREEN_SLIDE_DURATION_MS)
    when {
        forward -> {
            val enter = slideInHorizontally(animationSpec = slideSpec) { it } + fadeIn(fadeSpec)
            val exit = slideOutHorizontally(animationSpec = slideSpec) { -it / SCREEN_SLIDE_DIVISOR } +
                fadeOut(fadeSpec)
            enter togetherWith exit
        }
        backward -> {
            val enter = slideInHorizontally(animationSpec = slideSpec) { -it / SCREEN_SLIDE_DIVISOR } +
                fadeIn(fadeSpec)
            val exit = slideOutHorizontally(animationSpec = slideSpec) { it } + fadeOut(fadeSpec)
            enter togetherWith exit
        }
        else -> fadeIn(tween(SCREEN_FADE_IN_DURATION_MS)) togetherWith fadeOut(tween(SCREEN_FADE_OUT_DURATION_MS))
    }
}
