package com.deepanjanxyz.notepad.ui.feature_drawing.model

/**
 * The colour and size last chosen for each drawing tool.
 *
 * Held outside the drawing screen so the choice survives leaving and reopening
 * it, instead of falling back to the defaults every time.
 */
data class DrawingToolPrefs(
    val penColor: Long,
    val penSize: Float,
    val markerColor: Long,
    val markerSize: Float,
    val highlighterColor: Long,
    val highlighterSize: Float
)
