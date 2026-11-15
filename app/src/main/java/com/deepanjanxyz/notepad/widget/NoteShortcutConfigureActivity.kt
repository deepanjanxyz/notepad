package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme
import kotlinx.coroutines.launch

/**
 * Chooses which note a shortcut widget shows.
 *
 * The home screen starts this activity when a widget is placed, and it must return
 * a result: the host drops the widget again unless it hears [RESULT_OK]. It is
 * also reopened by a widget whose note has since been deleted, so the same screen
 * both sets and replaces a link.
 */
class NoteShortcutConfigureActivity : ComponentActivity() {

    private val viewModel: NoteShortcutConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appWidgetId = intent
            ?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // The host keeps the widget only when this activity reports success, so
        // backing out of the picker leaves the home screen as it was.
        setResult(RESULT_CANCELED)

        setContent {
            EliteMemoTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                NoteShortcutConfigScreen(
                    uiState = uiState,
                    onNoteSelected = { noteId -> link(appWidgetId, noteId) },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun link(appWidgetId: Int, noteId: Long) {
        lifecycleScope.launch {
            viewModel.link(appWidgetId, noteId)
            // The link is stored before the host is told it may place the widget,
            // and every placed shortcut is redrawn from it straight away, so the
            // home screen fills in with the note rather than a placeholder.
            NoteShortcutWidgetProvider.refresh(this@NoteShortcutConfigureActivity)
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            finish()
        }
    }
}
