package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.NoteShortcutBuilder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        render(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // The host reports a resize here, so the widget redraws at its new size.
        render(context, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appContext = context.applicationContext as NotepadApplication
        appContext.appScope.launch(Dispatchers.IO) {
            val shortcuts = appContext.container.noteShortcutUseCases
            appWidgetIds.forEach { appWidgetId ->
                shortcuts.unlinkNote(appWidgetId)
                renderLocks.remove(appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH -> refresh(context)
            else -> super.onReceive(context, intent)
        }
    }

    companion object {

        private const val ACTION_REFRESH =
            "com.deepanjanxyz.notepad.widget.ACTION_REFRESH_NOTE_SHORTCUTS"

        private val renderLocks = ConcurrentHashMap<Int, Mutex>()

        /** Redraws every placed shortcut from the notes as they are now. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteShortcutWidgetProvider::class.java)
            render(context, manager.getAppWidgetIds(component))
        }

        private fun render(context: Context, appWidgetIds: IntArray) {
            if (appWidgetIds.isEmpty()) return
            val appContext = context.applicationContext as NotepadApplication
            appContext.appScope.launch(Dispatchers.IO) {
                val manager = AppWidgetManager.getInstance(appContext)
                val density = appContext.resources.displayMetrics.density
                val container = appContext.container
                for (appWidgetId in appWidgetIds) {
                    renderLocks.computeIfAbsent(appWidgetId) { Mutex() }.withLock {
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
                    }
                }
            }
        }
    }
}
