package com.deepanjanxyz.notepad.ui.components

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.ui.theme.Spacing

/** Optional call to action rendered under an empty state. */
data class StateAction(
    val label: String,
    val onClick: () -> Unit,
    val testTag: String = "state_action"
)

/**
 * Shared loading placeholder.
 *
 * Previously every list screen showed its "nothing here yet" illustration while
 * the Room query was still running, so users saw an empty state flash before
 * their notes appeared. Screens now render this view until the first real
 * database emission arrives.
 */
@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
    testTag: String = "loading_state"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            CircularProgressIndicator(
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Shared empty state. Keeps icon/title/body/action rhythm identical everywhere.
 * The description block is merged for screen readers, while the action stays a
 * separate focus stop so it can still be activated with TalkBack and with a
 * keyboard/D-pad.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: StateAction? = null,
    testTag: String = "empty_state"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxxl)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(Spacing.emptyIcon)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (action != null) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                TextButton(
                    onClick = action.onClick,
                    modifier = Modifier.testTag(action.testTag)
                ) {
                    Text(action.label)
                }
            }
        }
    }
}

/**
 * Shared recoverable error view. Used when a database operation fails; before
 * this the app failed silently and the user had no idea a save had been lost.
 */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
    testTag: String = "error_state"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxxl)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(Spacing.xl)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }

                if (onRetry != null && retryLabel != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    TextButton(onClick = onRetry) {
                        Text(retryLabel)
                    }
                }
            }
        }
    }
}
