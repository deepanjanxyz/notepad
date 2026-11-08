package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Reveals [actionName] on a long press.
 *
 * Many controls in the app are icon-only, so their purpose is not spelled out
 * on screen. Wrapping such a control in [ActionTooltip] keeps the existing
 * layout untouched and simply surfaces the action's name when the user presses
 * and holds the control, making the icon-only controls easier to discover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTooltip(
    actionName: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TooltipBox(
        // PlainTooltip has always anchored above its target; the positioning-aware
        // provider keeps that placement after the plain-tooltip API was deprecated.
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(actionName) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * An icon button with [tooltip] shown on long press, at the size the note cards use.
 */
@Composable
internal fun TooltipIconButton(
    tooltip: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    ActionTooltip(tooltip) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
