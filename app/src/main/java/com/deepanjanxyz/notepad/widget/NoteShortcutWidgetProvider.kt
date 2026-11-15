package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.deepanjanxyz.notepad.NotepadApplication
import com.deepanjanxyz.notepad.domain.model.NoteShortcutBuilder
import kotlinx.coroutines.CoroutineScope
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
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val shortcuts = appContext.shortcutUseCases()
            appWidgetIds.forEach { appWidgetId -> shortcuts.unlinkNote(appWidgetId) }
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

        /** Redraws every placed shortcut from the notes as they are now. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteShortcutWidgetProvider::class.java)
            render(context, manager.getAppWidgetIds(component))
        }

        private fun render(context: Context, appWidgetIds: IntArray) {
            if (appWidgetIds.isEmpty()) return
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val manager = AppWidgetManager.getInstance(appContext)
                val density = appContext.resources.displayMetrics.density
                val container = (appContext as NotepadApplication).container
                for (appWidgetId in appWidgetIds) {
                    val noteId = container.noteShortcutUseCases.getLinkedNoteId(appWidgetId)
                    val shortcut = noteId
                        ?.let { id -> container.noteUseCases.getNoteById(id) }
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

/** The shortcut use cases, reached through the app's single dependency container. */
internal fun Context.shortcutUseCases() =
    (applicationContext as NotepadApplication).container.noteShortcutUseCases
