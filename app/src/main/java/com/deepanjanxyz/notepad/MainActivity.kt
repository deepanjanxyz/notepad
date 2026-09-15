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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.deepanjanxyz.notepad.data.SettingsRepository
import com.deepanjanxyz.notepad.ui.EliteMemoApp
import com.deepanjanxyz.notepad.ui.theme.EliteMemoTheme

class MainActivity : FragmentActivity() {

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
                    ) {
                        EliteMemoApp()
                    }
                }
            }
        }
    }
}

/**
 * Shows [content] once the biometric lock is satisfied. When [lockEnabled]
 * the system prompt appears immediately (same behaviour as the legacy gate).
 */
@Composable
private fun LockScreen(lockEnabled: Boolean, content: @Composable () -> Unit) {
    if (!lockEnabled) {
        content()
        return
    }

    var unlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        showBiometricPrompt(context) { unlocked = true }
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
