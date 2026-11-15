package com.deepanjanxyz.notepad.ui.navigation

/**
 * The extra that carries the id of the note an incoming intent should open.
 *
 * A reminder notification and a note shortcut widget both arrive with it, so the
 * name is written once here rather than at each place that sends or reads it.
 */
const val EXTRA_OPEN_NOTE_ID = "open_note_id"
