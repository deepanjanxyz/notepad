package com.deepanjanxyz.notepad

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.deepanjanxyz.notepad.ui.navigation.AppRoot
import com.deepanjanxyz.notepad.ui.viewmodel.NotesViewModel
import com.deepanjanxyz.notepad.ui.viewmodel.Screen

private const val SPLASH_ANIMATION_DURATION_MS = 300L


/** How long after the first back press a second one leaves the app. */
internal const val EXIT_BACK_WINDOW_MS = 2000L

class MainActivity : FragmentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    private fun handleNotificationIntent(intent: Intent?) {
        val noteId = intent?.getLongExtra("open_note_id", -1L) ?: -1L
        if (noteId > 0L) {
            viewModel.navigateTo(Screen.Editor(noteId))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashAnimations()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        setContent {
            AppRoot(
                viewModel = viewModel,
                onShowBiometricPrompt = { showBiometricPrompt() },
                onExit = { finish() }
            )
        }
    }

    /**
     * Hands the splash screen over to the app: the logo scales up while the splash
     * fades out, and the splash view is then removed.
     *
     * Called before `super.onCreate` so that the Android 12 splash (with its icon
     * reveal) also shows on Android 11 and below; it applies postSplashTheme too.
     */
    private fun installSplashAnimations() {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { provider ->
            val iconView: View? = provider.iconView
            val splashView: View? = provider.view
            // Not every platform hands back both views, so animate only what is
            // present, and always hand the splash screen back.
            val animators: List<Animator> = buildList {
                if (iconView != null) {
                    add(ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.2f))
                    add(ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.2f))
                }
                if (splashView != null) {
                    add(ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f))
                }
            }
            if (animators.isEmpty()) {
                provider.remove()
                return@setOnExitAnimationListener
            }
            AnimatorSet().apply {
                playTogether(animators)
                duration = SPLASH_ANIMATION_DURATION_MS
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        provider.remove()
                    }
                })
                start()
            }
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Elite Memo Pro")
            .setSubtitle("Confirm your biometric or device credentials")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockApp()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            // Fallback for emulators or environments without enrolled security
            viewModel.unlockApp()
        }
    }
}

@Composable
fun LockScreen(
    onUnlockRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Elite Memo Pro is Locked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authenticate to access your personal notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlockRequest,
                modifier = Modifier.testTag("unlock_biometric_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Unlock with Biometrics")
            }
        }
    }
}

/**
 * Ordering of the destinations, used to decide the direction of the screen
 * transition: moving to a higher rank slides forward (the new screen enters
 * from the right) and moving to a lower rank slides back. Drawer destinations
 * sit on their drawer order; detail screens sit above them.
 */
private enum class DestinationRank { HOME, ARCHIVE, TRASH, SETTINGS, EDITOR, DRAWING }

private fun screenRank(screen: Screen): Int = when (screen) {
    is Screen.Home -> DestinationRank.HOME
    is Screen.Archive -> DestinationRank.ARCHIVE
    is Screen.Trash -> DestinationRank.TRASH
    is Screen.Settings -> DestinationRank.SETTINGS
    is Screen.Editor -> DestinationRank.EDITOR
    is Screen.Drawing -> DestinationRank.DRAWING
}.ordinal

private const val SCREEN_SLIDE_DURATION_MS = 300
private const val SCREEN_SLIDE_DIVISOR = 1
private const val SCREEN_TOP_LEVEL_SLIDE_DIVISOR = 3

/**
 * Directional transition between destinations: every navigation slides and
 * fades, so moving around the app (including into Settings) always animates.
 * Drawer destinations use a subtle shared-axis slide; detail screens push in
 * with a full-width slide and slide back out on the way home.
 */
internal fun screenTransitionSpec(): AnimatedContentTransitionScope<Screen>.() -> ContentTransform = {
    val from = screenRank(initialState)
    val to = screenRank(targetState)
    val lastTopLevel = DestinationRank.SETTINGS.ordinal
    val bothTopLevel = from <= lastTopLevel && to <= lastTopLevel
    val divisor = if (bothTopLevel) SCREEN_TOP_LEVEL_SLIDE_DIVISOR else SCREEN_SLIDE_DIVISOR
    val slideSpec = tween<IntOffset>(SCREEN_SLIDE_DURATION_MS)
    val fadeSpec = tween<Float>(SCREEN_SLIDE_DURATION_MS)
    if (to > from) {
        val enter = slideInHorizontally(animationSpec = slideSpec) { it / divisor } + fadeIn(fadeSpec)
        val exit = slideOutHorizontally(animationSpec = slideSpec) { -it / (divisor * 2) } +
            fadeOut(fadeSpec)
        enter togetherWith exit
    } else {
        val enter = slideInHorizontally(animationSpec = slideSpec) { -it / divisor } + fadeIn(fadeSpec)
        val exit = slideOutHorizontally(animationSpec = slideSpec) { it / (divisor * 2) } +
            fadeOut(fadeSpec)
        enter togetherWith exit
    }
}
