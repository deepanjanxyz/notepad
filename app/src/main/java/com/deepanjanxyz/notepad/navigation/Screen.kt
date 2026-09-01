package com.deepanjanxyz.notepad.navigation

sealed class Screen(val route: String) {
    data object Notes : Screen("notes")
    data object Editor : Screen("editor/{noteId}") {
        fun createRoute(noteId: Long): String = "editor/$noteId"
    }
    data object Settings : Screen("settings")
}
