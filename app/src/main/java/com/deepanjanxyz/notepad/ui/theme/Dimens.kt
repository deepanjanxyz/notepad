package com.deepanjanxyz.notepad.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The single source of truth for spacing, shape and motion.
 *
 * Screens read these tokens instead of writing their own dp values, so a change
 * here moves the whole app together and no screen drifts from the rest.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Horizontal page gutter used by every scrolling screen. */
    val gutter = 16.dp

    /** Gap between cards in a grid or list. */
    val cardGap = 12.dp
}

/** Corner radii, from a chip to a full sheet. */
object Radii {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 28.dp
    val pill = 100.dp
}

/** The Material shape scale, built from [Radii]. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.xs),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.xl)
)

/** Durations and easing for every animated transition in the app. */
object Motion {
    const val instant = 90
    const val short = 140
    const val medium = 220
    const val long = 320

    /** Decelerating curve for things that enter. */
    val enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Accelerating curve for things that leave. */
    val exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Symmetric curve for a state change in place. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Elevation steps, so a raised surface is raised by the same amount everywhere. */
object Elevation {
    val flat = 0.dp
    val low = 1.dp
    val medium = 3.dp
    val high = 6.dp
}
