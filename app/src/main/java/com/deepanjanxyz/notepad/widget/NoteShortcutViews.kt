package com.deepanjanxyz.notepad.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.deepanjanxyz.notepad.MainActivity
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.NoteShortcut
import com.deepanjanxyz.notepad.ui.navigation.EXTRA_OPEN_NOTE_ID

/** The colour a note without its own falls back to, as the app's notes list does. */
private const val DEFAULT_COLOR_INDEX = 0

private val pendingIntentFlags =
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

/**
 * Builds one shortcut widget.
 *
 * A widget smaller than [NoteShortcutSizing.isExpanded] shows the note's title
 * alone; a larger one adds the note's text, or a thumbnail of its drawing. A
 * widget whose note has gone shows that, and tapping it offers another note
 * instead of opening nothing.
 */
internal fun buildShortcutViews(
    context: Context,
    appWidgetId: Int,
    shortcut: NoteShortcut?,
    sizing: NoteShortcutSizing
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_note_shortcut)
    views.setOnClickPendingIntent(
        R.id.widget_shortcut_root,
        openIntent(context, appWidgetId, shortcut)
    )
    views.setTextViewText(R.id.widget_shortcut_title, titleOf(context, shortcut))
    views.setInt(
        R.id.widget_shortcut_dot,
        "setColorFilter",
        noteAccentColor(context, shortcut?.colorIndex ?: DEFAULT_COLOR_INDEX)
    )
    renderBody(context, views, shortcut, sizing)
    return views
}

private fun titleOf(context: Context, shortcut: NoteShortcut?): String =
    when {
        shortcut == null -> context.getString(R.string.widget_note_missing)
        shortcut.title.isNotBlank() -> shortcut.title
        else -> context.getString(R.string.widget_note_untitled)
    }

private fun renderBody(
    context: Context,
    views: RemoteViews,
    shortcut: NoteShortcut?,
    sizing: NoteShortcutSizing
) {
    if (!sizing.isExpanded) {
        views.setViewVisibility(R.id.widget_shortcut_image, View.GONE)
        views.setViewVisibility(R.id.widget_shortcut_body, View.GONE)
        return
    }
    // Only a drawing note carries a drawing, so its presence is the test.
    val drawing = shortcut?.drawing
    if (drawing != null) {
        views.setViewVisibility(R.id.widget_shortcut_body, View.GONE)
        views.setViewVisibility(R.id.widget_shortcut_image, View.VISIBLE)
        views.setImageViewBitmap(
            R.id.widget_shortcut_image,
            drawingThumbnail(drawing, sizing.thumbnailWidthPx, sizing.thumbnailHeightPx)
        )
        return
    }
    views.setViewVisibility(R.id.widget_shortcut_image, View.GONE)
    views.setViewVisibility(R.id.widget_shortcut_body, View.VISIBLE)
    views.setTextViewText(R.id.widget_shortcut_body, bodyText(context, shortcut))
}

private fun bodyText(context: Context, shortcut: NoteShortcut?): String = when {
    shortcut == null -> context.getString(R.string.widget_note_missing)
    shortcut.text.isBlank() -> context.getString(R.string.widget_note_empty)
    else -> shortcut.text
}

/**
 * Opens the note the widget shows, or asks for a replacement when that note is
 * gone. The widget's id is the request code, so several shortcuts never share one
 * pending intent.
 */
private fun openIntent(
    context: Context,
    appWidgetId: Int,
    shortcut: NoteShortcut?
): PendingIntent {
    val intent = if (shortcut == null) {
        Intent(context, NoteShortcutConfigureActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("notepad://note/${shortcut.noteId}")
            putExtra(EXTRA_OPEN_NOTE_ID, shortcut.noteId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
    }
    return PendingIntent.getActivity(context, appWidgetId, intent, pendingIntentFlags)
}
