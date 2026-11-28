package com.deepanjanxyz.notepad.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.theme.OnSurfaceDark
import com.deepanjanxyz.notepad.ui.theme.OnSurfaceVariantDark
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_ANIMATED_CARDS = 6
private const val BIN_APPEAR_MS = 160
private const val LID_OPEN_MS = 180
private const val CARD_DROP_MS = 350
private const val CARD_STAGGER_MS = 45L
private const val BIN_BOUNCE_UP_MS = 90
private const val BIN_BOUNCE_DOWN_MS = 110
private const val LID_CLOSE_MS = 170
private const val BIN_CLOSED_HOLD_MS = 110L
private const val BIN_EXIT_MS = 130
private const val LID_MAX_ANGLE_DEGREES = -44f

internal data class PendingTrashDrop(
    val notes: List<Note>,
    val boundsInRoot: Map<Long, Rect>
)

/**
 * Plays the home-screen delete sequence: opens the bottom trash bin, arcs each deleted note card
 * down into the bin, closes the bin lid once the note(s) are inside, and only then invokes
 * [onBinClosedCommit] so the countdown/undo snackbar begins afterwards.
 */
@Composable
internal fun TrashDropOverlay(
    pending: PendingTrashDrop,
    rootOffsetInRoot: Offset,
    rootSize: IntSize,
    onBinClosedCommit: (List<Long>) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val latestCommit by rememberUpdatedState(onBinClosedCommit)
    val latestFinished by rememberUpdatedState(onFinished)

    var binCenterLocal by remember(pending) { mutableStateOf<Offset?>(null) }
    var committed by remember(pending) { mutableStateOf(false) }

    val binAlpha = remember(pending) { Animatable(0f) }
    val binScale = remember(pending) { Animatable(0.65f) }
    val lidOpenProgress = remember(pending) { Animatable(0f) }

    val animatedNotes = remember(pending) { pending.notes.take(MAX_ANIMATED_CARDS) }
    val dropProgresses = remember(pending) {
        animatedNotes.map { Animatable(0f) }
    }

    val fallbackWidthPx = with(density) { 164.dp.toPx() }
    val fallbackHeightPx = with(density) { 104.dp.toPx() }
    val arcLiftPx = with(density) { 22.dp.toPx() }
    val binMouthDipPx = with(density) { 6.dp.toPx() }

    val localRects = remember(pending, rootOffsetInRoot, rootSize) {
        animatedNotes.mapIndexed { index, note ->
            val captured = pending.boundsInRoot[note.id]
            if (captured != null && captured.width > 0f && captured.height > 0f) {
                captured.translate(-rootOffsetInRoot.x, -rootOffsetInRoot.y)
            } else {
                val cx = (rootSize.width.takeIf { it > 0 } ?: 1080) / 2f
                val cy = (rootSize.height.takeIf { it > 0 } ?: 1920) * (0.32f + index * 0.03f)
                Rect(
                    left = cx - fallbackWidthPx / 2f,
                    top = cy - fallbackHeightPx / 2f,
                    right = cx + fallbackWidthPx / 2f,
                    bottom = cy + fallbackHeightPx / 2f
                )
            }
        }
    }

    DisposableEffect(pending) {
        onDispose {
            if (!committed) {
                committed = true
                latestCommit(pending.notes.map { it.id })
            }
        }
    }

    LaunchedEffect(pending, binCenterLocal != null) {
        val targetBinCenter = binCenterLocal ?: return@LaunchedEffect

        // Open the bin lid before cards start dropping.
        coroutineScope {
            launch { binAlpha.animateTo(1f, tween(BIN_APPEAR_MS, easing = FastOutSlowInEasing)) }
            launch { binScale.animateTo(1f, tween(BIN_APPEAR_MS, easing = FastOutSlowInEasing)) }
            launch { lidOpenProgress.animateTo(1f, tween(LID_OPEN_MS, easing = FastOutSlowInEasing)) }
        }

        // Arc each card into the open bin mouth.
        val dropEasing = CubicBezierEasing(0.32f, 0f, 0.85f, 0.65f)
        coroutineScope {
            dropProgresses.forEachIndexed { idx, anim ->
                launch {
                    if (idx > 0) delay(idx * CARD_STAGGER_MS)
                    anim.animateTo(1f, tween(CARD_DROP_MS, easing = dropEasing))
                }
            }
        }

        // Bounce the bin slightly and close the lid as the cards settle in.
        coroutineScope {
            launch {
                binScale.animateTo(1.13f, tween(BIN_BOUNCE_UP_MS, easing = FastOutSlowInEasing))
                binScale.animateTo(1f, tween(BIN_BOUNCE_DOWN_MS, easing = FastOutSlowInEasing))
            }
            launch {
                lidOpenProgress.animateTo(0f, tween(LID_CLOSE_MS, easing = FastOutSlowInEasing))
            }
        }

        delay(BIN_CLOSED_HOLD_MS)

        // Fade out the closed bin before committing to Trash and starting the Undo countdown.
        coroutineScope {
            launch { binAlpha.animateTo(0f, tween(BIN_EXIT_MS, easing = FastOutSlowInEasing)) }
            launch { binScale.animateTo(0.84f, tween(BIN_EXIT_MS, easing = FastOutSlowInEasing)) }
        }

        if (!committed) {
            committed = true
            latestCommit(pending.notes.map { it.id })
        }
        latestFinished()
    }

    val binDescription = stringResource(R.string.trash_drop_target)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("trash_drop_overlay")
    ) {
        // Floating dropping note previews
        val targetBin = binCenterLocal
        animatedNotes.forEachIndexed { index, note ->
            val startRect = localRects[index]
            val t = dropProgresses[index].value
            val startCenter = startRect.center
            val endCenter = targetBin?.let { Offset(it.x, it.y + binMouthDipPx) } ?: startCenter

            val arcOffset = sin(t * PI).toFloat() * arcLiftPx
            val currentCenterX = lerp(startCenter.x, endCenter.x, t)
            val currentCenterY = lerp(startCenter.y, endCenter.y, t) - arcOffset
            val scale = if (t < 0.14f) {
                lerp(1f, 1.04f, t / 0.14f)
            } else {
                lerp(1.04f, 0.16f, (t - 0.14f) / 0.86f)
            }
            val tiltSign = if (startCenter.x <= endCenter.x) 1f else -1f
            val rotation = tiltSign * 8f * sin(t * PI).toFloat()
            val cardAlpha = if (t < 0.78f) 1f else (1f - (t - 0.78f) / 0.22f).coerceIn(0f, 1f)

            val widthDp = with(density) { startRect.width.toDp() }
            val heightDp = with(density) { startRect.height.toDp() }

            DroppingNotePreview(
                note = note,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (currentCenterX - startRect.width / 2f).roundToInt(),
                            (currentCenterY - startRect.height / 2f).roundToInt()
                        )
                    }
                    .width(widthDp)
                    .height(heightDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                        alpha = cardAlpha
                    }
            )
        }

        // Bottom-center animated trash bin target
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .graphicsLayer {
                    alpha = binAlpha.value
                    scaleX = binScale.value
                    scaleY = binScale.value
                }
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        val centerRoot = coords.boundsInRoot().center
                        binCenterLocal = centerRoot - rootOffsetInRoot
                    }
                }
                .semantics { contentDescription = binDescription }
                .testTag("home_trash_drop_bin")
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                ),
                shadowElevation = 8.dp,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedTrashBinGlyph(
                        lidOpenProgress = lidOpenProgress.value,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        mouthShadowColor = MaterialTheme.colorScheme.error.copy(alpha = 0.28f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DroppingNotePreview(
    note: Note,
    modifier: Modifier = Modifier
) {
    val hasCustomColor = note.colorIndex in 1 until NoteColorOptions.size
    val cardColor = if (hasCustomColor) {
        NoteColorOptions[note.colorIndex]
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val titleColor = if (hasCustomColor) OnSurfaceDark else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (hasCustomColor) OnSurfaceVariantDark else MaterialTheme.colorScheme.onSurfaceVariant
    val previewBody = remember(note.content) {
        RichTextCodec.extractPlainText(note.content)
            .lineSequence()
            .take(3)
            .joinToString("\n")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = note.title.ifBlank { stringResource(R.string.widget_note_untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (previewBody.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = previewBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Custom Canvas trash bin icon whose lid hinges open, exposes the dark mouth opening,
 * and shuts cleanly once the note lands inside.
 */
@Composable
private fun AnimatedTrashBinGlyph(
    lidOpenProgress: Float,
    tint: Color,
    mouthShadowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.085f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val bodyTop = h * 0.34f
        val bodyBottom = h * 0.90f
        val bodyLeft = w * 0.22f
        val bodyRight = w * 0.78f
        val insetBottom = w * 0.06f

        // Darken the mouth opening in proportion to the lid angle.
        if (lidOpenProgress > 0.02f) {
            drawRoundRect(
                color = mouthShadowColor.copy(alpha = mouthShadowColor.alpha * lidOpenProgress),
                topLeft = Offset(bodyLeft + strokeWidth * 0.3f, bodyTop - strokeWidth * 0.4f),
                size = Size(bodyRight - bodyLeft - strokeWidth * 0.6f, strokeWidth * 1.4f),
                cornerRadius = CornerRadius(strokeWidth, strokeWidth)
            )
        }

        val bucketPath = Path().apply {
            moveTo(bodyLeft, bodyTop)
            lineTo(bodyLeft + insetBottom, bodyBottom - w * 0.05f)
            quadraticTo(
                bodyLeft + insetBottom,
                bodyBottom,
                bodyLeft + insetBottom + w * 0.06f,
                bodyBottom
            )
            lineTo(bodyRight - insetBottom - w * 0.06f, bodyBottom)
            quadraticTo(
                bodyRight - insetBottom,
                bodyBottom,
                bodyRight - insetBottom,
                bodyBottom - w * 0.05f
            )
            lineTo(bodyRight, bodyTop)
            close()
        }
        drawPath(bucketPath, color = tint, style = stroke)

        val ribTop = bodyTop + (bodyBottom - bodyTop) * 0.24f
        val ribBottom = bodyBottom - (bodyBottom - bodyTop) * 0.22f
        drawLine(
            color = tint,
            start = Offset(w * 0.41f, ribTop),
            end = Offset(w * 0.42f, ribBottom),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.59f, ribTop),
            end = Offset(w * 0.58f, ribBottom),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )

        val lidLeft = w * 0.14f
        val lidRight = w * 0.86f
        val lidY = h * 0.26f - h * 0.05f * lidOpenProgress
        val pivot = Offset(lidLeft, lidY)
        rotate(degrees = LID_MAX_ANGLE_DEGREES * lidOpenProgress, pivot = pivot) {
            drawLine(
                color = tint,
                start = Offset(lidLeft, lidY),
                end = Offset(lidRight, lidY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            val handlePath = Path().apply {
                moveTo(w * 0.37f, lidY)
                lineTo(w * 0.39f, lidY - h * 0.13f)
                lineTo(w * 0.61f, lidY - h * 0.13f)
                lineTo(w * 0.63f, lidY)
            }
            drawPath(handlePath, color = tint, style = stroke)
        }
    }
}
