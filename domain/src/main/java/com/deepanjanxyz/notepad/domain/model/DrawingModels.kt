package com.deepanjanxyz.notepad.domain.model

enum class DrawingTool {
    SELECT,
    ERASER,
    PEN,
    MARKER,
    HIGHLIGHTER
}

enum class EraserMode {
    SEGMENT, // Erases touched segments
    STROKE   // Erases entire stroke
}

enum class CanvasGridType {
    NONE,
    RULED,
    GRID,
    DOTS
}
