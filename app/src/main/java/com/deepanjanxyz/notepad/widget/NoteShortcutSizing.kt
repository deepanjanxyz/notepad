package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import android.os.Bundle

/**
 * How much room one shortcut widget has been given.
 *
 * The host reports a widget's size in dp, so the thresholds below are written in
 * dp as well and stay the same on every screen density. A widget smaller than
 * them shows only the note's title, which is what a single home-screen cell has
 * room for; anything larger also shows the note's body.
 */
internal data class NoteShortcutSizing(
    val isExpanded: Boolean,
    val thumbnailWidthPx: Int,
    val thumbnailHeightPx: Int
) {

    companion object {

        /** The narrowest widget that also shows the note's body. */
        private const val EXPANDED_MIN_WIDTH_DP = 180

        /** The shortest widget that also shows the note's body. */
        private const val EXPANDED_MIN_HEIGHT_DP = 110

        /** The widget's own padding plus the title, taken off the body's height. */
        private const val CHROME_DP = 60

        /** The widget's own horizontal padding, taken off the body's width. */
        private const val HORIZONTAL_CHROME_DP = 28

        /** A drawing thumbnail is capped so the widget update stays inside Binder's limit. */
        private const val MAX_THUMBNAIL_WIDTH_PX = 512
        private const val MAX_THUMBNAIL_HEIGHT_PX = 320

        /** The smallest bitmap still worth drawing into. */
        private const val MIN_THUMBNAIL_PX = 1

        /**
         * Reads the size the widget has in [orientation].
         *
         * The host reports a range on each axis, not one size: the minimum width
         * is the widget's width in portrait and the maximum width is its width in
         * landscape, and the heights are paired the same way. Which pair applies
         * therefore depends on the orientation on screen right now, not on which
         * of the two numbers happens to be larger - a 200x160dp portrait widget
         * whose landscape size is 240x90dp must still read as expanded while the
         * device is held upright.
         */
        fun of(
            options: Bundle,
            density: Float,
            orientation: Int = Configuration.ORIENTATION_UNDEFINED
        ): NoteShortcutSizing {
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            val (widthDp, heightDp) = when (orientation) {
                Configuration.ORIENTATION_PORTRAIT -> minWidth to maxHeight
                Configuration.ORIENTATION_LANDSCAPE -> maxWidth to minHeight
                // No orientation to go on: take the pair that gives the widget the
                // most room, so a body is shown rather than hidden by a guess.
                else -> if (maxWidth * maxHeight >= minWidth * minHeight) {
                    maxWidth to maxHeight
                } else {
                    minWidth to minHeight
                }
            }
            return NoteShortcutSizing(
                isExpanded = widthDp >= EXPANDED_MIN_WIDTH_DP && heightDp >= EXPANDED_MIN_HEIGHT_DP,
                thumbnailWidthPx = toPixels(
                    widthDp - HORIZONTAL_CHROME_DP,
                    density,
                    MAX_THUMBNAIL_WIDTH_PX
                ),
                thumbnailHeightPx = toPixels(heightDp - CHROME_DP, density, MAX_THUMBNAIL_HEIGHT_PX)
            )
        }

        private fun toPixels(dp: Int, density: Float, maxPx: Int): Int =
            (dp * density).toInt().coerceIn(MIN_THUMBNAIL_PX, maxPx)
    }
}
