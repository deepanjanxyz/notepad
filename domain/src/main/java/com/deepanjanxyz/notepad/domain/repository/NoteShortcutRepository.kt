package com.deepanjanxyz.notepad.domain.repository

/**
 * Remembers which note each home-screen shortcut widget shows.
 *
 * The link is keyed by the widget's own id, because the home screen can hold
 * several shortcuts at once and each one is configured on its own. A widget with
 * no stored link has never been configured, or its note has since been removed.
 */
interface NoteShortcutRepository {

    /** The note [appWidgetId] shows, or null when it shows none. */
    suspend fun noteIdFor(appWidgetId: Int): Long?

    /** Points [appWidgetId] at [noteId], replacing any earlier choice. */
    suspend fun link(appWidgetId: Int, noteId: Long)

    /** Forgets [appWidgetId], called when the widget is removed from the home screen. */
    suspend fun unlink(appWidgetId: Int)
}
