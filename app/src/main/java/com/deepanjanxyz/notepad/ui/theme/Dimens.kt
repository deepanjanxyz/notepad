package com.deepanjanxyz.notepad.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Centralised layout tokens.
 *
 * Every screen used to hard-code its own dp values (16.dp here, 12.dp there,
 * a 28.dp corner somewhere else), which is why the same component rendered
 * slightly differently on each screen. Collecting the values here keeps the
 * visual rhythm consistent and turns future density/dark-mode work into a
 * single-file change.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /**
     * Minimum interactive size from the Material accessibility guidance and
     * WCAG 2.5.5 (Target Size). Additive row icon buttons used to override this
     * with 28.dp, which made them hard to hit for touch and motor-impaired users.
     */
    val minTouchTarget = 48.dp

    val screenHorizontal = 16.dp
    val listItemSpacing = 12.dp
    val listBottomInset = 88.dp

    val colorBadge = 30.dp
    val avatar = 40.dp
    val emptyIcon = 80.dp

    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val iconLarge = 24.dp
}

/** Consistent shape language shared by every surface in the app. */
val EliteMemoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
