package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.NoteShortcutBuilder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Home-screen shortcut to a single note.
 *
 * Each placed widget is pointed at one note by [NoteShortcutConfigureActivity] and
 * then opens it in one tap. A widget in a single cell shows the note's title; one
 * resized larger also shows the note's text, or a thumbnail of its drawing.
 *
 * Unlike the tasks widget there is nothing to pin: the widget itself is the link,
 * so the note a user puts on the home screen is the one they chose there.
 */
class NoteShortcutWidgetProvider : AppWidgetProvider() {

    /**
     * Every widget broadcast goes through here rather than through the separate
     * callbacks, so one [goAsync] result covers the work each of them starts: the
     * process is kept alive until the redraw or the unlink has finished.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in HANDLED_ACTIONS) {
            super.onReceive(context, intent)
            return
        }
        val pending = goAsync()
        val appContext = context.applicationContext as NotepadApplication
        appContext.appScope.launch(Dispatchers.IO) {
            try {
                when (action) {
                    ACTION_REFRESH -> renderNow(context, placedWidgetIds(context))
                    AppWidgetManager.ACTION_APPWIDGET_UPDATE ->
                        renderNow(context, intent.widgetIds())

                    AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED ->
                        renderNow(context, intArrayOf(intent.widgetId()))

                    // A deletion arrives with the singular extra, so the plural one is
                    // empty and the cleanup would be skipped.
                    AppWidgetManager.ACTION_APPWIDGET_DELETED ->
                        unlinkNow(context, intArrayOf(intent.widgetId()))

                    // A restored widget comes back under a new id, so its link is
                    // moved across before it is drawn.
                    AppWidgetManager.ACTION_APPWIDGET_RESTORED -> restoreNow(context, intent)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        private const val TAG = "NoteShortcutWidget"

        private const val ACTION_REFRESH =
            "com.deepanjanxyz.notepad.widget.ACTION_REFRESH_NOTE_SHORTCUTS"

        private val HANDLED_ACTIONS = setOf(
            ACTION_REFRESH,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_DELETED,
            AppWidgetManager.ACTION_APPWIDGET_RESTORED
        )

        private val renderLocks = ConcurrentHashMap<Int, Mutex>()

        /** Redraws every placed shortcut from the notes as they are now. */
        fun refresh(context: Context) {
            val appContext = context.applicationContext as NotepadApplication
            appContext.appScope.launch(Dispatchers.IO) {
                renderNow(context, placedWidgetIds(context))
            }
        }

        private fun placedWidgetIds(context: Context): IntArray {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteShortcutWidgetProvider::class.java)
            return manager.getAppWidgetIds(component)
        }

        /**
         * Moves the stored links of a restored widget onto the ids it came back
         * with, then draws them.
         *
         * The host hands over the old and the new ids in the same order, so each
         * old link has exactly one new home.
         */
        private suspend fun restoreNow(context: Context, intent: Intent) {
            val oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS)
            val newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (oldIds == null || newIds == null) return
            val appContext = context.applicationContext as NotepadApplication
            val shortcuts = appContext.container.noteShortcutUseCases
            oldIds.forEachIndexed { index, oldId ->
                val newId = newIds.getOrNull(index) ?: return@forEachIndexed
                val noteId = shortcuts.getLinkedNoteId(oldId) ?: return@forEachIndexed
                shortcuts.linkNote(newId, noteId)
                shortcuts.unlinkNote(oldId)
            }
            renderNow(context, newIds)
        }

        private suspend fun renderNow(context: Context, appWidgetIds: IntArray) {
            if (appWidgetIds.isEmpty()) return
            val appContext = context.applicationContext as NotepadApplication
            val manager = AppWidgetManager.getInstance(appContext)
            val density = appContext.resources.displayMetrics.density
            val container = appContext.container
            for (appWidgetId in appWidgetIds) {
                // One render per widget at a time, so a compact redraw cannot land
                // after a larger one and leave the old layout showing.
                renderLocks.computeIfAbsent(appWidgetId) { Mutex() }.withLock {
                    // One note the widget cannot draw - a drawing that will not
                    // parse, a bitmap the binder refuses - must not take the whole
                    // app down with it, so each widget is drawn on its own and a
                    // failure is logged instead of thrown.
                    runCatching {
                        val noteId = container.noteShortcutUseCases.getLinkedNoteId(appWidgetId)
                        // The picker offers active notes only, so a note moved to the
                        // trash or the archive reads as gone here too.
                        val shortcut = noteId
                            ?.let { id -> container.noteUseCases.getNoteById(id) }
                            ?.takeUnless { it.inTrash || it.inArchive }
                            ?.let(NoteShortcutBuilder::build)
                        val sizing = NoteShortcutSizing.of(
                            options = manager.getAppWidgetOptions(appWidgetId),
                            density = density
                        )
                        manager.updateAppWidget(
                            appWidgetId,
                            buildShortcutViews(appContext, appWidgetId, shortcut, sizing)
                        )
                    }.onFailure { error ->
                        Log.w(TAG, "Shortcut widget $appWidgetId could not be drawn", error)
                    }
                }
            }
        }

        private suspend fun unlinkNow(context: Context, appWidgetIds: IntArray) {
            val appContext = context.applicationContext as NotepadApplication
            val shortcuts = appContext.container.noteShortcutUseCases
            appWidgetIds.forEach { appWidgetId ->
                shortcuts.unlinkNote(appWidgetId)
                renderLocks.remove(appWidgetId)
            }
        }

        private fun Intent.widgetIds(): IntArray =
            getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: IntArray(0)

        private fun Intent.widgetId(): Int =
            getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }
}
