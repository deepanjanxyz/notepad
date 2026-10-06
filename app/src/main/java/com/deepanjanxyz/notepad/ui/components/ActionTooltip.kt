package com.deepanjanxyz.notepad.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(actionName) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        content()
    }
}
