package com.deepanjanxyz.notepad

import android.content.Context
import android.content.SharedPreferences

/** App-wide theme preference. Keys match the values used by the original preferences screen. */
enum class ThemeMode(val key: String, val labelRes: Int) {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark);

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Immutable view of every user-facing setting, used to drive recomposition. */
data class SettingsSnapshot(
    val themeMode: ThemeMode,
    val dynamicColors: Boolean,
    val lockOnLaunch: Boolean,
)

/**
 * Typed access to the app's SharedPreferences-backed settings.
 *
 * The preferences file is the same "<applicationId>_preferences" store that
 * PreferenceManager.getDefaultSharedPreferences used, so existing users keep
 * their theme and lock choices after the migration.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.key).apply()

    var dynamicColors: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, value).apply()

    var lockOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK, value).apply()

    fun snapshot(): SettingsSnapshot =
        SettingsSnapshot(
            themeMode = themeMode,
            dynamicColors = dynamicColors,
            lockOnLaunch = lockOnLaunch,
        )

    /**
     * Observes every preference change for as long as the returned
     * unregister function has not been invoked.
     */
    fun observe(onChange: (SettingsSnapshot) -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            onChange(snapshot())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        const val KEY_THEME = "pref_theme"
        const val KEY_DYNAMIC_COLORS = "pref_dynamic_colors"
        const val KEY_LOCK = "pref_biometric"
    }
}
