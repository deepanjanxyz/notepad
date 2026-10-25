package com.deepanjanxyz.notepad.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepanjanxyz.notepad.EXIT_BACK_WINDOW_MS
import com.deepanjanxyz.notepad.LockScreen
import com.deepanjanxyz.notepad.screenTransitionSpec
import com.deepanjanxyz.notepad.ui.components.AppDrawerContent
import com.deepanjanxyz.notepad.ui.components.EditLabelsDialog
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme
import com.deepanjanxyz.notepad.ui.viewmodel.NotesUiState
import com.deepanjanxyz.notepad.ui.viewmodel.NotesViewModel
import com.deepanjanxyz.notepad.ui.viewmodel.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The whole app: the lock screen, or the drawer and whichever screen is showing. */
@Composable
internal fun AppRoot(
    viewModel: NotesViewModel,
    onShowBiometricPrompt: () -> Unit,
    onExit: () -> Unit
) {
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
    val snackbarHostState = remember { SnackbarHostState() }

    AppBackHandler(
        drawerState = drawerState,
        currentScreen = uiState.currentScreen,
        scope = scope,
        snackbarHostState = snackbarHostState,
        onExit = onExit
    )

    val routes = ScreenRouteState(
        viewModel = viewModel,
        uiState = uiState,
        filteredNotes = filteredNotes,
        rawActiveNotes = rawActiveNotes,
        archiveNotes = archiveNotes,
        trashNotes = trashNotes,
        allTags = allTags,
        autoBackupStatus = autoBackupStatus,
        importOutcome = importOutcome,
        snackbarHostState = snackbarHostState,
        drawerState = drawerState,
        scope = scope
    )

    EliteMemoTheme(themeMode = uiState.themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isLocked && uiState.lockEnabled) {
                LockScreen(onUnlockRequest = onShowBiometricPrompt)

                LaunchedEffect(Unit) {
                    onShowBiometricPrompt()
                }
            } else {
                AppDrawerLayout(
                    routes = routes,
                    roomLabels = roomLabels,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

/**
 * Back closes the drawer first; on the home screen a second press within
 * [EXIT_BACK_WINDOW_MS] leaves the app, and the first press says so.
 *
 * Active while the drawer is open and on the home screen, which is the one place
 * back can leave from. Every other screen owns its own back, and `enabled` is the
 * documented way to scope a handler: calling BackHandler inside an `if` would
 * reorder the handlers on recomposition instead.
 */
@Composable
private fun AppBackHandler(
    drawerState: DrawerState,
    currentScreen: Screen,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onExit: () -> Unit
) {
    var lastBackAt by remember { mutableStateOf(0L) }

    BackHandler(enabled = drawerState.isOpen || currentScreen is Screen.Home) {
        val now = System.currentTimeMillis()
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            now - lastBackAt < EXIT_BACK_WINDOW_MS -> onExit()
            else -> {
                lastBackAt = now
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Press back again to exit",
                        actionLabel = "Exit",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) onExit()
                }
            }
        }
    }

    // The exit prompt belongs to the screen that raised it. Moving away - a screen
    // change, or the drawer opening - takes the prompt and its two-second window
    // with it, so a press elsewhere can never leave the app on the strength of an
    // earlier one, and the Exit action cannot be tapped once an edit has begun.
    LaunchedEffect(currentScreen, drawerState.isOpen) {
        lastBackAt = 0L
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}

/** The navigation drawer, the current screen inside it, and the labels dialog. */
@Composable
private fun AppDrawerLayout(
    routes: ScreenRouteState,
    roomLabels: List<String>,
    snackbarHostState: SnackbarHostState
) {
    val closeDrawer = { routes.scope.launch { routes.drawerState.close() } }
    val navigateAndClose: (Screen) -> Unit = { screen ->
        routes.viewModel.navigateTo(screen)
        closeDrawer()
    }
    val filterAndClose: (String?) -> Unit = { tag ->
        routes.viewModel.onTagFilterChange(tag)
        routes.viewModel.navigateTo(Screen.Home)
        closeDrawer()
    }

    ModalNavigationDrawer(
        drawerState = routes.drawerState,
        gesturesEnabled = routes.uiState.currentScreen !is Screen.Editor &&
            !routes.uiState.isSelectionMode,
        drawerContent = {
            DrawerHost(
                uiState = routes.uiState,
                roomLabels = roomLabels,
                onFilterAndClose = filterAndClose,
                onNavigateAndClose = navigateAndClose,
                onOpenEditLabels = {
                    routes.viewModel.setEditLabelsDialogVisible(true)
                    closeDrawer()
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) }
        ) { innerPadding ->
            AnimatedContent(
                targetState = routes.uiState.currentScreen,
                transitionSpec = screenTransitionSpec(),
                label = "screen_transition",
                modifier = Modifier.padding(innerPadding)
            ) { screen ->
                ScreenRoutes(routes = routes, screen = screen)
            }
        }
    }

    if (routes.uiState.showEditLabelsDialog) {
        EditLabelsHost(viewModel = routes.viewModel, labels = roomLabels)
    }
}

/** The drawer's entries, wired to the navigation they perform. */
@Composable
private fun DrawerHost(
    uiState: NotesUiState,
    roomLabels: List<String>,
    onFilterAndClose: (String?) -> Unit,
    onNavigateAndClose: (Screen) -> Unit,
    onOpenEditLabels: () -> Unit
) {
    AppDrawerContent(
        currentScreen = uiState.currentScreen,
        selectedTagFilter = uiState.selectedTagFilter,
        labels = roomLabels,
        onSelectNotes = { onFilterAndClose(null) },
        onSelectTag = { onFilterAndClose(it) },
        onOpenEditLabels = onOpenEditLabels,
        onSelectArchive = { onNavigateAndClose(Screen.Archive) },
        onSelectTrash = { onNavigateAndClose(Screen.Trash) },
        onSelectSettings = { onNavigateAndClose(Screen.Settings) }
    )
}

/** The tag-management dialog, shown when the drawer or the editor asks for it. */
@Composable
private fun EditLabelsHost(viewModel: NotesViewModel, labels: List<String>) {
    EditLabelsDialog(
        labels = labels,
        onAddLabel = { viewModel.addLabel(it) },
        onRenameLabel = { old, new -> viewModel.renameLabel(old, new) },
        onDeleteLabel = { viewModel.deleteLabel(it) },
        onDismiss = { viewModel.setEditLabelsDialogVisible(false) }
    )
}
