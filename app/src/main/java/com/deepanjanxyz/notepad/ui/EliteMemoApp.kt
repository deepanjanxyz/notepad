package com.deepanjanxyz.notepad.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepanjanxyz.notepad.ui.editor.EditorScreen
import com.deepanjanxyz.notepad.ui.notes.NotesScreen
import com.deepanjanxyz.notepad.ui.settings.SettingsScreen

object Routes {
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{noteId}"

    fun editor(noteId: Long) = "editor/$noteId"
}

@Composable
fun EliteMemoApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.NOTES) {
        composable(Routes.NOTES) {
            NotesScreen(
                onOpenNote = { noteId -> navController.navigate(Routes.editor(noteId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            EditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
