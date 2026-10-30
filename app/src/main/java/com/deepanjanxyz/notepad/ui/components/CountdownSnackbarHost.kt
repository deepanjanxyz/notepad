package com.deepanjanxyz.notepad.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** How long a snackbar with a dismiss action stays before it closes itself. */
const val SNACKBAR_COUNTDOWN_SECONDS = 5

private const val TICK_MS = 1_000L
private val HOST_PADDING = 12.dp
private val CONTENT_PADDING = 16.dp
private val CONTENT_PADDING_VERTICAL = 12.dp
private val COUNTDOWN_SIZE = 28.dp
private val COUNTDOWN_GAP = 4.dp

/**
 * A [SnackbarHost] that swaps the dismiss icon for a countdown.
 *
 * A snackbar carrying a dismiss action closes itself after
 * [SNACKBAR_COUNTDOWN_SECONDS], so a counter says more than a cross does: it shows
 * how long is left, and tapping it still dismisses at once.
 */
@Composable
fun CountdownSnackbarHost(
    hostState: SnackbarHostState,
    seconds: Int = SNACKBAR_COUNTDOWN_SECONDS
) {
    SnackbarHost(hostState) { data ->
        if (data.visuals.withDismissAction) {
            CountdownSnackbar(data = data, seconds = seconds)
        } else {
            Snackbar(snackbarData = data)
        }
    }
}

/** A snackbar laid out like the Material one, with a countdown in the last slot. */
@Composable
private fun CountdownSnackbar(data: SnackbarData, seconds: Int) {
    val visuals = data.visuals
    Surface(
        shape = SnackbarDefaults.shape,
        color = SnackbarDefaults.color,
        contentColor = SnackbarDefaults.contentColor,
        modifier = Modifier
            .padding(HOST_PADDING)
            .fillMaxWidth()
            .testTag("countdown_snackbar")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = CONTENT_PADDING,
                vertical = CONTENT_PADDING_VERTICAL
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            val actionLabel = visuals.actionLabel
            if (actionLabel != null) {
                TextButton(onClick = { data.performAction() }) {
                    Text(text = actionLabel, color = SnackbarDefaults.actionColor)
                }
            }
            Spacer(Modifier.width(COUNTDOWN_GAP))
            CountdownDismiss(seconds = seconds, onDismiss = { data.dismiss() })
        }
    }
}

/**
 * The seconds left, one digit at a time: the next number slides down from the top
 * as the one it replaces slides away at the bottom.
 */
@Composable
private fun CountdownDismiss(seconds: Int, onDismiss: () -> Unit) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    LaunchedEffect(seconds) {
        while (remaining > 0) {
            delay(TICK_MS)
            remaining -= 1
        }
        onDismiss()
    }
    Box(
        modifier = Modifier
            .size(COUNTDOWN_SIZE)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("snackbar_countdown"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = remaining,
            transitionSpec = {
                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                    (slideOutVertically { height -> height } + fadeOut())
            },
            label = "snackbar_countdown_digit"
        ) { value ->
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
