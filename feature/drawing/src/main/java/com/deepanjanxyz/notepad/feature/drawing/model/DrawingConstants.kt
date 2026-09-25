package com.deepanjanxyz.notepad.feature.drawing.model

// 4-row x 7-color matrix matching Google Keep Drawing palette
val KeepColorGrid = listOf(
    // Row 1 (Primary / Quick Row)
    listOf(
        0xFF1B1B1FL, // Black
        0xFFEA4335L, // Coral Red
        0xFFFBBC04L, // Amber Yellow
        0xFF34A853L, // Emerald Green
        0xFF00B0FFL, // Electric Cyan / Sky Blue (Default active)
        0xFFAB47BCL, // Magenta Purple
        0xFF8D6E63L  // Warm Brown
    ),
    // Row 2 (7 colors)
    listOf(
        0xFFFFFFFFL, // Pure White
        0xFF880E4FL, // Dark Wine / Burgundy
        0xFFFF6D00L, // Deep Orange
        0xFF558B2FL, // Olive Green
        0xFF1565C0L, // Royal Navy Blue
        0xFF6A1B9AL, // Deep Violet
        0xFF3E2723L  // Dark Roast Brown
    ),
    // Row 3 (7 colors)
    listOf(
        0xFFB0BEC5L, // Cool Grey
        0xFFC2185BL, // Medium Pink
        0xFFFFB74DL, // Warm Peach
        0xFFAED581L, // Light Sage Green
        0xFF4DD0E1L, // Aqua Cyan
        0xFFBA68C8L, // Soft Orchid
        0xFFBCAAA4L  // Pale Taupe
    ),
    // Row 4 (7 colors)
    listOf(
        0xFF37474FL, // Charcoal Slate
        0xFFF06292L, // Flamingo Pink
        0xFFFFE082L, // Cream Pastel Yellow
        0xFFDCEDC8L, // Mint Foam
        0xFF80DEEAL, // Ice Light Blue
        0xFFE1BEE7L, // Lavender Cloud
        0xFFD7CCC8L  // Sandstone Beige
    )
)

// Google Keep 7-step thickness dot scale
val KeepThicknessLevels = listOf(
    Pair(3f, "Fine"),
    Pair(6f, "Thin"),
    Pair(10f, "Medium-Light"),
    Pair(16f, "Regular"),
    Pair(24f, "Medium-Thick"),
    Pair(34f, "Thick"),
    Pair(48f, "Heavy")
)
