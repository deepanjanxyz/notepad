package com.deepanjanxyz.notepad.core.model

enum class ThemeMode(val storageKey: String, val title: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}
