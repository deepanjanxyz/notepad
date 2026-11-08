package com.deepanjanxyz.notepad.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import kotlinx.coroutines.delay
import kotlin.math.max

/** How long a snackbar with a dismiss action stays before it closes itself. */
const val SNACKBAR_COUNTDOWN_SECONDS = 5

private const val TICK_MS = 1_000L
private const val SHORT_SECONDS = 4
private const val LONG_SECONDS = 10
private val HOST_PADDING = 12.dp
private val CONTENT_PADDING = 16.dp
private val CONTENT_PADDING_VERTICAL = 12.dp

/**
 * The smallest the countdown may be. It is a floor, not a size: the digit has to be
 * able to grow with the font scale, or a large system font is cut off inside it.
 */
private val COUNTDOWN_SIZE = 22.dp
private val COUNTDOWN_GAP = 2.dp

/** How long one digit takes to roll away and the next to take its place. */
private const val DIGIT_ROLL_MS = 150

/** The width of the ring that measures the time left. */
private val RING_STROKE = 2.dp

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
            CountdownSnackbar(data = data, seconds = countdownSeconds(data.visuals.duration, seconds))
        } else {
            Snackbar(snackbarData = data)
        }
    }
}

/**
 * The seconds the digit counts from. A timed snackbar is closed by the host, so the
 * digit has to match its duration; only an indefinite one is closed by the countdown,
 * and that is what [indefinite] is for.
 */
private fun countdownSeconds(duration: SnackbarDuration, indefinite: Int): Int = when (duration) {
    SnackbarDuration.Short -> SHORT_SECONDS
    SnackbarDuration.Long -> LONG_SECONDS
    SnackbarDuration.Indefinite -> indefinite
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
            CountdownDismiss(
                snackbar = data,
                seconds = seconds,
                onDismiss = { data.dismiss() }
            )
        }
    }
}

/**
 * The seconds left, one digit at a time: the next number slides down from the top
 * as the one it replaces slides away at the bottom.
 */
@Composable
private fun CountdownDismiss(snackbar: SnackbarData, seconds: Int, onDismiss: () -> Unit) {
    // Someone who needs longer to reach the screen can raise this in the system
    // accessibility settings, so the countdown follows the recommended timeout.
    val accessibility = LocalAccessibilityManager.current
    val timeout = accessibility?.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = seconds * TICK_MS,
        containsText = true,
        containsControls = true
    ) ?: (seconds * TICK_MS)
    // Touch exploration on Android 8 and 9 asks for no timeout at all, which comes
    // back as Long.MAX_VALUE and would wrap around when divided.
    val indefinite = timeout == Long.MAX_VALUE
    val ticks = if (indefinite) 0 else max(1, (timeout / TICK_MS).toInt())
    var remaining by remember(snackbar, ticks) { mutableIntStateOf(ticks) }
    LaunchedEffect(snackbar, ticks) {
        while (remaining > 0) {
            delay(TICK_MS)
            remaining -= 1
        }
        if (!indefinite) onDismiss()
    }
    // The ring drains continuously rather than a second at a time, so the countdown
    // reads as time passing instead of a number changing.
    // Read here: SnackbarDefaults.actionColor is composable, and the draw lambda is not.
    val ringColor = SnackbarDefaults.actionColor
    val drained by animateFloatAsState(
        targetValue = if (indefinite) 1f else remaining.toFloat() / ticks,
        animationSpec = tween(durationMillis = TICK_MS.toInt(), easing = LinearEasing),
        label = "snackbar_countdown_ring"
    )
    Box(
        modifier = Modifier
            // A floor rather than a fixed size, so the digit is not clipped once the
            // font scale is turned up. Compose still expands the touch target to 48dp.
            .defaultMinSize(minWidth = COUNTDOWN_SIZE, minHeight = COUNTDOWN_SIZE)
            .countdownRing(drained = drained, color = ringColor, visible = !indefinite)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                // The digit says how long is left; it does not say what tapping it does.
                onClickLabel = stringResource(R.string.snackbar_countdown_dismiss),
                role = Role.Button,
                onClick = onDismiss
            )
            .testTag("snackbar_countdown"),
        contentAlignment = Alignment.Center
    ) {
        CountdownDigit(value = if (indefinite) "\u221e" else remaining.toString())
    }
}

/**
 * The ring that drains as the time runs out, drawn around the digit.
 *
 * It follows the digit's measured size rather than a fixed one, so a larger font
 * scale widens the ring with it.
 */
private fun Modifier.countdownRing(drained: Float, color: Color, visible: Boolean): Modifier =
    drawBehind {
        if (!visible) return@drawBehind
        val stroke = RING_STROKE.toPx()
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = -360f * drained,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }

/** The digit itself, the next one rolling in from the top as the old one drops away. */
@Composable
private fun CountdownDigit(value: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val rollingIn = slideInVertically(tween(DIGIT_ROLL_MS)) { height -> -height } +
                fadeIn(tween(DIGIT_ROLL_MS))
            val rollingOut = slideOutVertically(tween(DIGIT_ROLL_MS)) { height -> height } +
                fadeOut(tween(DIGIT_ROLL_MS))
            rollingIn togetherWith rollingOut
        },
        label = "snackbar_countdown_digit"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = COUNTDOWN_GAP)
        )
    }
}
