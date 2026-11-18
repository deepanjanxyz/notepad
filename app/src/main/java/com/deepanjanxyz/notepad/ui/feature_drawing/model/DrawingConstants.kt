package com.deepanjanxyz.notepad.ui.feature_drawing.model

// Four rows of seven colours, ordered from the most used to the softest tints.
val KeepColorGrid = listOf(
    // Row 1: the everyday colours.
    listOf(
        0xFF1B1B1FL, // Black
        0xFFEA4335L, // Coral Red
        0xFFFBBC04L, // Amber Yellow
        0xFF34A853L, // Emerald Green
        0xFF00B0FFL, // Sky Blue
        0xFFAB47BCL, // Magenta Purple
        0xFF8D6E63L  // Warm Brown
    ),
    // Row 2: the deeper shades.
    listOf(
        0xFFFFFFFFL, // Pure White
        0xFF880E4FL, // Burgundy
        0xFFFF6D00L, // Deep Orange
        0xFF558B2FL, // Olive Green
        0xFF1565C0L, // Navy Blue
        0xFF6A1B9AL, // Deep Violet
        0xFF3E2723L  // Dark Brown
    ),
    // Row 3: the mid tints.
    listOf(
        0xFFB0BEC5L, // Cool Grey
        0xFFC2185BL, // Medium Pink
        0xFFFFB74DL, // Warm Peach
        0xFFAED581L, // Light Sage
        0xFF4DD0E1L, // Aqua
        0xFFBA68C8L, // Soft Orchid
        0xFFBCAAA4L  // Pale Taupe
    ),
    // Row 4: the pastels.
    listOf(
        0xFF37474FL, // Charcoal
        0xFFF06292L, // Flamingo Pink
        0xFFFFE082L, // Cream Yellow
        0xFFDCEDC8L, // Mint
        0xFF80DEEAL, // Ice Blue
        0xFFE1BEE7L, // Lavender
        0xFFD7CCC8L  // Sandstone
    )
)

// Seven stroke sizes, from a fine line to a heavy marker.
val KeepThicknessLevels = listOf(
    Pair(3f, "Fine"),
    Pair(6f, "Thin"),
    Pair(10f, "Light"),
    Pair(16f, "Regular"),
    Pair(24f, "Medium"),
    Pair(34f, "Thick"),
    Pair(48f, "Heavy")
)
