package com.deepanjanxyz.notepad.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.deepanjanxyz.notepad.di.AppContainer
import com.deepanjanxyz.notepad.features.editor.NoteEditorScreen
import com.deepanjanxyz.notepad.features.editor.NoteEditorViewModel
import com.deepanjanxyz.notepad.features.notes.NotesScreen
import com.deepanjanxyz.notepad.features.notes.NotesViewModel
import com.deepanjanxyz.notepad.features.settings.SettingsScreen
import com.deepanjanxyz.notepad.features.settings.SettingsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Notes.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Notes.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val notesViewModel = remember {
                NotesViewModel(
                    noteRepository = appContainer.noteRepository,
                    preferenceRepository = appContainer.preferenceRepository
                )
            }

            NotesScreen(
                viewModel = notesViewModel,
                onNavigateToEditor = { noteId ->
                    navController.navigate(Screen.Editor.createRoute(noteId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val editorViewModel = remember(noteId) {
                NoteEditorViewModel(noteRepository = appContainer.noteRepository)
            }

            NoteEditorScreen(
                noteId = noteId,
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            val settingsViewModel = remember {
                SettingsViewModel(
                    context = context,
                    preferenceRepository = appContainer.preferenceRepository,
                    noteRepository = appContainer.noteRepository
                )
            }

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
