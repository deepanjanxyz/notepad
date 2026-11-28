package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Keeps the existing String-based editor state contract while using Compose's
 * state-based BasicTextField API. External changes are synchronized without
 * resetting the selection when the text already matches.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StatefulBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    cursorBrush: Brush = SolidColor(Color.Black),
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    onKeyboardAction: KeyboardActionHandler? = null,
    decorator: TextFieldDecorator? = null,
    externalState: TextFieldState? = null
) {
    val state = externalState ?: rememberTextFieldState(initialText = value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { currentOnValueChange(it) }
    }

    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    val keyboardAction = onKeyboardAction?.let { actionHandler ->
        KeyboardActionHandler { performDefaultAction ->
            // snapshotFlow forwards edits asynchronously. Flush the current editor
            // value before a Done handler can commit the draft from external state.
            currentOnValueChange(state.text.toString())
            actionHandler.onKeyboardAction(performDefaultAction)
        }
    }

    BasicTextField(
        state = state,
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = cursorBrush,
        lineLimits = lineLimits,
        keyboardOptions = keyboardOptions,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        onKeyboardAction = keyboardAction,
        decorator = decorator
    )
}
