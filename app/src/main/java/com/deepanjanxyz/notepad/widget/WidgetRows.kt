package com.deepanjanxyz.notepad.widget

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.checklistLineText
import com.deepanjanxyz.notepad.domain.model.isChecklistLine
import com.deepanjanxyz.notepad.domain.model.isChecklistLineChecked
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.withAppLocale

/** One checklist line of a note, remembering the line it came from. */
internal data class WidgetTask(val lineIndex: Int, val text: String, val isChecked: Boolean)

/** A checklist note together with the lines it contributes to the widget. */
internal data class WidgetTaskGroup(val note: Note, val tasks: List<WidgetTask>)

/** One row of the widget list: either a note header or one of its tasks. */
internal sealed interface WidgetRow {
    data class Group(val note: Note) : WidgetRow
    data class Item(val note: Note, val task: WidgetTask) : WidgetRow
}


/**
 * The checklist lines of a note, each remembering the line it came from.
 *
 * Only lines that already carry a checkbox are listed, and a tap flips just that
 * line's checkbox - so ordinary text, blank lines and the rest of the note are
 * never rewritten by the widget.
 */
private fun checklistTasks(content: String): List<WidgetTask> =
    content.split("\n").mapIndexedNotNull { index, line ->
        if (line.isChecklistLine()) {
            WidgetTask(index, line.checklistLineText(), line.isChecklistLineChecked())
        } else {
            null
        }
    }

/**
 * The pinned checklist notes, newest first, each with the lines it contributes.
 *
 * The repository hands back only the rows that can match, so a widget refresh
 * reads a handful of notes instead of the whole database.
 */
internal suspend fun loadTaskGroups(repository: NoteRepository): List<WidgetTaskGroup> =
    repository.getPinnedChecklistNotes()
        .map { note -> WidgetTaskGroup(note, checklistTasks(note.content)) }
        .filter { it.tasks.isNotEmpty() }

/** Flattens the groups into the flat row list the widget's ListView renders. */
internal fun flattenRows(groups: List<WidgetTaskGroup>): List<WidgetRow> = buildList {
    for (group in groups) {
        add(WidgetRow.Group(group.note))
        group.tasks.forEach { task -> add(WidgetRow.Item(group.note, task)) }
    }
}

/**
 * Builds one row.
 *
 * Headers and tasks share a single layout, because a remote collection reports
 * one view type and the host therefore caches one view: a row can only ever
 * inflate this layout, and the unused half is hidden instead.
 *
 * Every row sets a click target on its root. A header gets an empty one, which
 * resolves to no item, so a header can never inherit the fill-in intent of the
 * task whose view it was recycled from and flip that item by accident.
 */
internal fun buildRowViews(context: Context, row: WidgetRow): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_task_row)
    when (row) {
        is WidgetRow.Group -> {
            views.setViewVisibility(R.id.widget_row_header, View.VISIBLE)
            views.setViewVisibility(R.id.widget_row_task, View.GONE)
            views.setTextViewText(
                R.id.widget_group_title,
                row.note.title.ifBlank { context.withAppLocale().getString(R.string.widget_untitled) }
            )
            views.setInt(R.id.widget_group_dot, "setColorFilter", noteAccentColor(context, row.note.colorIndex))
            views.setOnClickFillInIntent(R.id.widget_row_root, Intent())
        }

        is WidgetRow.Item -> {
            views.setViewVisibility(R.id.widget_row_header, View.GONE)
            views.setViewVisibility(R.id.widget_row_task, View.VISIBLE)
            views.setImageViewResource(
                R.id.widget_item_check,
                if (row.task.isChecked) {
                    R.drawable.ic_widget_check_done
                } else {
                    R.drawable.ic_widget_check_todo
                }
            )
            views.setTextViewText(R.id.widget_item_text, itemText(row.task))
            views.setTextColor(
                R.id.widget_item_text,
                context.getColor(
                    if (row.task.isChecked) {
                        R.color.widget_on_surface_variant
                    } else {
                        R.color.widget_on_surface
                    }
                )
            )
            views.setOnClickFillInIntent(
                R.id.widget_row_root,
                TaskWidgetProvider.toggleFillInIntent(row.note.id, row.task.lineIndex)
            )
        }
    }
    return views
}

/** Completed lines are struck through, the way a checklist reads at a glance. */
private fun itemText(task: WidgetTask): CharSequence =
    if (task.isChecked) {
        SpannableString(task.text).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    } else {
        task.text
    }

/**
 * The dot mirrors the note's own colour so a group is recognisable at a glance,
 * exactly as the note card is in the app. Notes left on the default colour fall
 * back to the widget accent.
 *
 * Shared with the note shortcut widget, which shows the same dot for the note it
 * opens.
 */
internal fun noteAccentColor(context: Context, colorIndex: Int): Int =
    if (colorIndex in 1 until NoteColorOptions.size) {
        NoteColorOptions[colorIndex].toArgb()
    } else {
        context.getColor(R.color.widget_accent)
    }
