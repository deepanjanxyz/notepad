package com.deepanjanxyz.notepad.widget

import android.appwidget.AppWidgetManager
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

        fun of(options: Bundle, density: Float): NoteShortcutSizing {
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            return NoteShortcutSizing(
                isExpanded = widthDp >= EXPANDED_MIN_WIDTH_DP && heightDp >= EXPANDED_MIN_HEIGHT_DP,
                thumbnailWidthPx = toPixels(widthDp - HORIZONTAL_CHROME_DP, density, MAX_THUMBNAIL_WIDTH_PX),
                thumbnailHeightPx = toPixels(heightDp - CHROME_DP, density, MAX_THUMBNAIL_HEIGHT_PX)
            )
        }

        private fun toPixels(dp: Int, density: Float, maxPx: Int): Int =
            (dp * density).toInt().coerceIn(MIN_THUMBNAIL_PX, maxPx)
    }
}
