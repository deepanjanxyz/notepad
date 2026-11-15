package com.deepanjanxyz.notepad.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.deepanjanxyz.notepad.domain.model.DrawingData
import com.deepanjanxyz.notepad.domain.model.DrawingStroke
import com.deepanjanxyz.notepad.domain.util.DrawingFit

/** A full-alpha paint channel; stroke alphas arrive as a fraction of it. */
private const val MAX_ALPHA = 255

/** A stroke never thins to nothing, however far the drawing is scaled down. */
private const val MIN_STROKE_WIDTH = 1f

/** Share of the thumbnail's shorter side kept clear, so strokes are not clipped. */
private const val THUMBNAIL_PADDING_FRACTION = 0.04f

/** Gives a single-point stroke a length to draw, so a dot still appears. */
private const val DOT_LENGTH = 0.5f

/**
 * Renders [drawing] into a bitmap the widget can show.
 *
 * A widget cannot host a Compose canvas, so the strokes are replayed onto an
 * off-screen bitmap here. [DrawingFit] does the scaling, which means a sketch made
 * on a full-screen canvas appears whole and centred inside the widget cell rather
 * than cropped to its top-left corner.
 */
internal fun drawingThumbnail(drawing: DrawingData, widthPx: Int, heightPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(
        widthPx.coerceAtLeast(1),
        heightPx.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    canvas.drawColor(drawing.backgroundColor.toInt())

    val padding = minOf(bitmap.width, bitmap.height) * THUMBNAIL_PADDING_FRACTION
    val fit = DrawingFit.of(
        drawing = drawing,
        boxWidth = bitmap.width.toFloat(),
        boxHeight = bitmap.height.toFloat(),
        padding = padding
    )
    val paint = strokePaint()
    drawing.strokes.forEach { stroke ->
        if (stroke.points.isNotEmpty()) {
            canvas.drawPath(pathOf(stroke, fit), paint.appliedTo(stroke, fit.scale))
        }
    }
    return bitmap
}

private fun strokePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
}

/** Takes on one stroke's colour and width, scaled to the thumbnail. */
private fun Paint.appliedTo(stroke: DrawingStroke, scale: Float): Paint = apply {
    color = stroke.color.toInt()
    alpha = (stroke.alpha.coerceIn(0f, 1f) * MAX_ALPHA).toInt()
    strokeWidth = (stroke.strokeWidth * scale).coerceAtLeast(MIN_STROKE_WIDTH)
}

private fun pathOf(stroke: DrawingStroke, fit: DrawingFit): Path = Path().apply {
    val first = stroke.points.first()
    moveTo(fit.x(first), fit.y(first))
    stroke.points.drop(1).forEach { point -> lineTo(fit.x(point), fit.y(point)) }
    if (stroke.points.size == 1) {
        // A one-point stroke is a dot, which a path only draws once it has a
        // length.
        lineTo(fit.x(first), fit.y(first) + DOT_LENGTH)
    }
}
