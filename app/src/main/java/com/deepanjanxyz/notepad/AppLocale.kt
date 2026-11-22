package com.deepanjanxyz.notepad

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

internal fun Context.savedAppLocale(): Locale {
    val language = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        .getString("app_language", "en") ?: "en"
    return Locale.forLanguageTag(language)
}

internal fun Context.withAppLocale(locale: Locale = savedAppLocale()): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}
