package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.deepanjanxyz.notepad.LockScreen
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
class NoteShortcutConfigureActivity : FragmentActivity() {

    private val viewModel: NoteShortcutConfigViewModel by viewModels()
    private var authenticated by mutableStateOf(false)

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
        // Tell the ViewModel which widget is being configured so it can mark the
        // currently linked note in the picker list.
        viewModel.setAppWidgetId(appWidgetId)
        // The host keeps the widget only when this activity reports success, so
        // backing out of the picker leaves the home screen as it was.
        setResult(RESULT_CANCELED)

        setContent {
            EliteMemoTheme {
                val lockEnabled by viewModel.lockEnabled.collectAsStateWithLifecycle()
                if (lockEnabled == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@EliteMemoTheme
                }
                val isLocked = lockEnabled == true && !authenticated
                LaunchedEffect(isLocked) {
                    if (isLocked) showBiometricPrompt()
                }
                if (isLocked) {
                    LockScreen(onUnlockRequest = ::showBiometricPrompt)
                    return@EliteMemoTheme
                }
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                NoteShortcutConfigScreen(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onNoteSelected = { noteId -> link(appWidgetId, noteId) },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun showBiometricPrompt() {
        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Elite Memo Pro")
            .setSubtitle("Confirm your biometric or device credentials")
        promptBuilder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        val promptInfo = promptBuilder.build()
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticated = true
                }
            }
        ).authenticate(promptInfo)
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
