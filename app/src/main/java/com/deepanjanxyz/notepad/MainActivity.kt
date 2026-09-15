package com.deepanjanxyz.notepad

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.deepanjanxyz.notepad.data.SettingsRepository
import com.deepanjanxyz.notepad.ui.EliteMemoApp
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme

class MainActivity : FragmentActivity() {

    // Biometric gate: the user authenticates once per foreground session and
    // stays unlocked while using the app. It re-locks only when the app goes
    // to the background — never while navigating between screens.
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository.getInstance(this)

        setContent {
            val settings by settingsRepository.settings.collectAsState()
            val darkTheme = when (settings.theme) {
                SettingsRepository.THEME_DARK -> true
                SettingsRepository.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            EliteMemoTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LockScreen(
                        lockEnabled = settings.lockEnabled,
                        unlocked = unlocked,
                        onUnlocked = { unlocked = true },
                    ) {
                        EliteMemoApp()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // The app went to the background — require authentication again on
        // the next visit.
        unlocked = false
    }
}

/**
 * Shows [content] once the biometric lock is satisfied. The prompt appears on
 * first launch and after every return from the background; after one
 * successful unlock the app stays open until it is backgrounded again.
 */
@Composable
private fun LockScreen(
    lockEnabled: Boolean,
    unlocked: Boolean,
    onUnlocked: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!lockEnabled || unlocked) {
        content()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(false) }
    var promptShown by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Trigger the prompt only while the app is actually resumed (never from
    // the background) and only once per lock session.
    LaunchedEffect(unlocked, resumed) {
        if (unlocked || !resumed || promptShown) return@LaunchedEffect
        promptShown = true
        showBiometricPrompt(context, onUnlocked)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(text = "Elite Memo Security", fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Unlock to access your notes",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun showBiometricPrompt(context: Context, onSuccess: () -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(activity)

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                activity.finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Elite Memo Security")
        .setSubtitle("Unlock to access your notes")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        .build()

    prompt.authenticate(promptInfo)
}
