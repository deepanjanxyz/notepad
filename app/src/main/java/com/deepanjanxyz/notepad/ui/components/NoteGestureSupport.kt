package com.deepanjanxyz.notepad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.theme.ArchiveGreen
import com.deepanjanxyz.notepad.ui.theme.ArchiveGreenAccent
import com.deepanjanxyz.notepad.ui.theme.ArchiveGreenMuted
import com.deepanjanxyz.notepad.ui.theme.OnArchiveGreen
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class NoteGesturePhase {
    IDLE,
    HELD,
    ARCHIVE_SWIPE,
    REORDER_DRAG
}

private const val HORIZONTAL_SWIPE_RATIO = 1.75f
private const val ARCHIVE_WIDTH_FRACTION = 0.44f
private const val RIGHT_COLUMN_REORDER_ESCAPE_FRACTION = 0.92f
private const val RIGHT_COLUMN_FLICK_VELOCITY_THRESHOLD = 700f
private const val ARCHIVE_OUT_DURATION_MS = 160
private const val ARCHIVE_HOLD_GREEN_MS = 210L
private const val ARCHIVE_COLLAPSE_DURATION_MS = 210
private const val REORDER_SETTLE_DURATION_MS = 175
private val MAX_ARCHIVE_THRESHOLD = 148.dp
private val MIN_ARCHIVE_THRESHOLD = 72.dp
private val DRAG_START_SLOP = 10.dp
private val REORDER_ESCAPE_VERTICAL_SLOP = 26.dp

internal class ReorderDragSession(
    val note: Note,
    initialPointerRootPos: Offset,
    val grabOffsetInCard: Offset,
    val cardSizePx: IntSize
) {
    var pointerRootPos by mutableStateOf(initialPointerRootPos)
    var isSettling by mutableStateOf(false)

    val overlayCenterInRoot: Offset
        get() = Offset(
            x = pointerRootPos.x - grabOffsetInCard.x + cardSizePx.width / 2f,
            y = pointerRootPos.y - grabOffsetInCard.y + cardSizePx.height / 2f
        )
}

@Composable
internal fun NoteTouchGestureBox(
    note: Note,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    gesturesEnabled: Boolean,
    isBeingDraggedForReorder: Boolean,
    isHiddenForTrashOrArchive: Boolean,
    hasLeftNeighborInSameSection: Boolean,
    searchQuery: String,
    activeGestureNoteId: Long?,
    onActiveGestureNoteChange: (Long?) -> Unit,
    onCardPositioned: (Rect) -> Unit,
    onCardDisposed: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStartReorderDrag: (Note, Offset, Offset, IntSize) -> Unit,
    onMoveReorderDrag: (Offset) -> Unit,
    onEndReorderDrag: () -> Unit,
    onArchiveCommitted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember(note.id) { MutableInteractionSource() }
    val swipeOffsetAnim = remember(note.id) { Animatable(0f) }
    val collapseFractionAnim = remember(note.id) { Animatable(1f) }

    var cardWidthPx by remember(note.id) { mutableIntStateOf(0) }
    var isHeld by remember(note.id) { mutableStateOf(false) }
    var isSwipingArchive by remember(note.id) { mutableStateOf(false) }
    var isConfirmedArchive by remember(note.id) { mutableStateOf(false) }
    var archiveCommitted by remember(note.id) { mutableStateOf(false) }
    var wasHidden by remember(note.id) { mutableStateOf(isHiddenForTrashOrArchive) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val archiveThresholdPx = remember(cardWidthPx, density) {
        val proportional = cardWidthPx * ARCHIVE_WIDTH_FRACTION
        val maxPx = with(density) { MAX_ARCHIVE_THRESHOLD.toPx() }
        val minPx = with(density) { MIN_ARCHIVE_THRESHOLD.toPx() }
        if (cardWidthPx <= 0) maxPx else minOf(proportional, maxPx).coerceAtLeast(minPx)
    }

    val holdScale by animateFloatAsState(
        targetValue = if (isHeld) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "note_hold_scale"
    )

    val inputActive = gesturesEnabled && !isConfirmedArchive && !isHiddenForTrashOrArchive
    val currentInputActive by rememberUpdatedState(inputActive)
    val currentActiveGestureId by rememberUpdatedState(activeGestureNoteId)
    val currentHasLeftNeighbor by rememberUpdatedState(hasLeftNeighborInSameSection)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnStartReorder by rememberUpdatedState(onStartReorderDrag)
    val currentOnMoveReorder by rememberUpdatedState(onMoveReorderDrag)
    val currentOnEndReorder by rememberUpdatedState(onEndReorderDrag)
    val currentOnArchiveCommitted by rememberUpdatedState(onArchiveCommitted)
    val currentOnCardDisposed by rememberUpdatedState(onCardDisposed)
    val currentArchiveThresholdPx by rememberUpdatedState(archiveThresholdPx)

    val dragStartSlopPx = with(density) { DRAG_START_SLOP.toPx() }
    val verticalEscapeSlopPx = with(density) { REORDER_ESCAPE_VERTICAL_SLOP.toPx() }

    LaunchedEffect(isHiddenForTrashOrArchive) {
        if (wasHidden && !isHiddenForTrashOrArchive) {
            isHeld = false
            isSwipingArchive = false
            isConfirmedArchive = false
            archiveCommitted = false
            swipeOffsetAnim.snapTo(0f)
            collapseFractionAnim.snapTo(1f)
        }
        wasHidden = isHiddenForTrashOrArchive
    }

    // Commit pending archive if navigation/removal disposes this item before the collapse finishes.
    DisposableEffect(note.id) {
        onDispose {
            if (isConfirmedArchive && !archiveCommitted) {
                archiveCommitted = true
                currentOnArchiveCommitted(note.id)
            }
            currentOnCardDisposed()
        }
    }

    val gestureModifier = if (inputActive) {
        Modifier.pointerInput(note.id, inputActive) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                if (!currentInputActive ||
                    (currentActiveGestureId != null && currentActiveGestureId != note.id)
                ) {
                    return@awaitEachGesture
                }

                val downPos = down.position
                val downRootPos = layoutCoordinates
                    ?.takeIf { it.isAttached }
                    ?.localToRoot(downPos)
                    ?: Offset.Zero
                val touchSlop = viewConfiguration.touchSlop
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                val press = PressInteraction.Press(downPos)
                scope.launch { interactionSource.emit(press) }

                val longPressed = withTimeoutOrNull(longPressTimeout) {
                    var trackedId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == trackedId }
                            ?: event.changes.firstOrNull()
                            ?: return@withTimeoutOrNull false
                        trackedId = change.id

                        if (change.isConsumed) {
                            scope.launch { interactionSource.emit(PressInteraction.Cancel(press)) }
                            return@withTimeoutOrNull false
                        }
                        if (!change.pressed) {
                            change.consume()
                            scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                            currentOnClick()
                            return@withTimeoutOrNull false
                        }
                        if ((change.position - downPos).getDistance() > touchSlop) {
                            scope.launch { interactionSource.emit(PressInteraction.Cancel(press)) }
                            return@withTimeoutOrNull false
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    false
                } ?: true

                if (!longPressed) return@awaitEachGesture
                if (!currentInputActive ||
                    (currentActiveGestureId != null && currentActiveGestureId != note.id)
                ) {
                    scope.launch { interactionSource.emit(PressInteraction.Cancel(press)) }
                    return@awaitEachGesture
                }

                scope.launch { interactionSource.emit(PressInteraction.Cancel(press)) }
                onActiveGestureNoteChange(note.id)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isHeld = true

                var phase = NoteGesturePhase.HELD
                var trackedId = down.id
                var releasedNormally = false
                var readyHapticFired = false
                val velocityTracker = VelocityTracker()

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == trackedId }
                            ?: event.changes.firstOrNull { it.pressed }
                            ?: event.changes.firstOrNull()
                            ?: break
                        trackedId = change.id

                        val coords = layoutCoordinates?.takeIf { it.isAttached }
                        val currentRootPos = coords?.localToRoot(change.position) ?: downRootPos
                        val deltaFromStart = currentRootPos - downRootPos

                        if (!change.pressed) {
                            change.consume()
                            releasedNormally = true
                            break
                        }

                        when (phase) {
                            NoteGesturePhase.HELD -> {
                                change.consume()
                                if (deltaFromStart.getDistance() > dragStartSlopPx) {
                                    isHeld = false
                                    val dx = deltaFromStart.x
                                    val dy = deltaFromStart.y
                                    val isHorizontalLeft =
                                        dx < 0f && abs(dx) > abs(dy) * HORIZONTAL_SWIPE_RATIO
                                    if (isHorizontalLeft) {
                                        phase = NoteGesturePhase.ARCHIVE_SWIPE
                                        isSwipingArchive = true
                                        velocityTracker.resetTracking()
                                        velocityTracker.addPosition(change.uptimeMillis, currentRootPos)
                                    } else {
                                        phase = NoteGesturePhase.REORDER_DRAG
                                        val rect = coords?.boundsInRoot() ?: Rect.Zero
                                        val origin = if (downRootPos != Offset.Zero) {
                                            downRootPos
                                        } else {
                                            coords?.localToRoot(downPos) ?: currentRootPos
                                        }
                                        val grabOffset = origin - rect.topLeft
                                        val sizePx = coords?.size ?: IntSize.Zero
                                        currentOnStartReorder(note, currentRootPos, grabOffset, sizePx)
                                    }
                                }
                            }

                            NoteGesturePhase.ARCHIVE_SWIPE -> {
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, currentRootPos)
                                val rawDx = deltaFromStart.x
                                val rawDy = deltaFromStart.y
                                val vx = velocityTracker.calculateVelocity().x
                                val cardW = cardWidthPx.toFloat().coerceAtLeast(1f)

                                // Let slow left drags in the right column transition into cross-column reorder.
                                val movedVertically = abs(rawDy) > verticalEscapeSlopPx
                                val movedSlowlyToLeftColumn = currentHasLeftNeighbor &&
                                    rawDx < -cardW * RIGHT_COLUMN_REORDER_ESCAPE_FRACTION &&
                                    abs(vx) < RIGHT_COLUMN_FLICK_VELOCITY_THRESHOLD

                                if (movedVertically || movedSlowlyToLeftColumn) {
                                    isSwipingArchive = false
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        swipeOffsetAnim.snapTo(0f)
                                    }
                                    phase = NoteGesturePhase.REORDER_DRAG
                                    val rect = coords?.boundsInRoot() ?: Rect.Zero
                                    val origin = if (downRootPos != Offset.Zero) {
                                        downRootPos
                                    } else {
                                        coords?.localToRoot(downPos) ?: currentRootPos
                                    }
                                    val grabOffset = origin - rect.topLeft
                                    val sizePx = coords?.size ?: IntSize.Zero
                                    currentOnStartReorder(note, currentRootPos, grabOffset, sizePx)
                                } else {
                                    val trackedDx = rawDx.coerceIn(-cardW * 1.05f, 0f)
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        swipeOffsetAnim.snapTo(trackedDx)
                                    }
                                    val isReadyNow = abs(trackedDx) >= currentArchiveThresholdPx
                                    if (isReadyNow && !readyHapticFired) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    readyHapticFired = isReadyNow
                                }
                            }

                            NoteGesturePhase.REORDER_DRAG -> {
                                change.consume()
                                currentOnMoveReorder(currentRootPos)
                            }

                            NoteGesturePhase.IDLE -> Unit
                        }
                    }
                } finally {
                    isHeld = false
                    onActiveGestureNoteChange(null)

                    if (!releasedNormally) {
                        if (phase == NoteGesturePhase.REORDER_DRAG) {
                            currentOnEndReorder()
                        }
                        if (swipeOffsetAnim.value != 0f) {
                            scope.launch {
                                swipeOffsetAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                isSwipingArchive = false
                            }
                        } else {
                            isSwipingArchive = false
                        }
                    } else {
                        when (phase) {
                            NoteGesturePhase.HELD -> currentOnLongClick()
                            NoteGesturePhase.ARCHIVE_SWIPE -> {
                                val threshold = currentArchiveThresholdPx
                                if (abs(swipeOffsetAnim.value) >= threshold) {
                                    isConfirmedArchive = true
                                    scope.launch {
                                        val exitDistance = -(cardWidthPx.toFloat().coerceAtLeast(300f) * 1.06f)
                                        swipeOffsetAnim.animateTo(
                                            targetValue = exitDistance,
                                            animationSpec = tween(
                                                durationMillis = ARCHIVE_OUT_DURATION_MS,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        delay(ARCHIVE_HOLD_GREEN_MS)
                                        collapseFractionAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(
                                                durationMillis = ARCHIVE_COLLAPSE_DURATION_MS,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        if (!archiveCommitted) {
                                            archiveCommitted = true
                                            currentOnArchiveCommitted(note.id)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        swipeOffsetAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        isSwipingArchive = false
                                    }
                                }
                            }
                            NoteGesturePhase.REORDER_DRAG -> currentOnEndReorder()
                            NoteGesturePhase.IDLE -> Unit
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    val showArchiveBackdrop =
        (isSwipingArchive || isConfirmedArchive) && !isBeingDraggedForReorder && !isHiddenForTrashOrArchive

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val fraction = collapseFractionAnim.value.coerceIn(0f, 1f)
                val height = (placeable.height * fraction).roundToInt().coerceAtLeast(0)
                layout(placeable.width, height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .graphicsLayer {
                alpha = collapseFractionAnim.value.coerceIn(0f, 1f)
            }
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    layoutCoordinates = coords
                    cardWidthPx = coords.size.width
                    onCardPositioned(coords.boundsInRoot())
                }
            }
            .then(gestureModifier)
    ) {
        if (isBeingDraggedForReorder && !isHiddenForTrashOrArchive) {
            ReorderVacatedSlotPlaceholder(modifier = Modifier.matchParentSize())
        }

        if (showArchiveBackdrop) {
            ArchiveSwipeBackground(
                swipeOffsetProvider = { swipeOffsetAnim.value },
                archiveThresholdPx = archiveThresholdPx,
                isConfirmed = isConfirmedArchive,
                modifier = Modifier.matchParentSize()
            )
        }

        NoteCard(
            note = note,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            searchQuery = searchQuery,
            onClick = onClick,
            onLongClick = onLongClick,
            customGestureEnabled = gesturesEnabled,
            gestureInteractionSource = interactionSource,
            modifier = Modifier.graphicsLayer {
                val tx = swipeOffsetAnim.value
                translationX = tx
                scaleX = holdScale
                scaleY = holdScale
                alpha = when {
                    isHiddenForTrashOrArchive || isBeingDraggedForReorder -> 0f
                    isConfirmedArchive && cardWidthPx > 0 ->
                        (1f - ((-tx / cardWidthPx.toFloat()) * 0.55f)).coerceIn(0f, 1f)
                    else -> 1f
                }
            }
        )
    }
}

@Composable
private fun ReorderVacatedSlotPlaceholder(modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val fillColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.28f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fillColor)
            .drawBehind {
                val strokePx = 1.5.dp.toPx()
                val dashPx = 6.dp.toPx()
                val gapPx = 5.dp.toPx()
                val cornerPx = 16.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
                    )
                )
            }
            .testTag("reorder_slot_placeholder")
    )
}

@Composable
private fun ArchiveSwipeBackground(
    swipeOffsetProvider: () -> Float,
    archiveThresholdPx: Float,
    isConfirmed: Boolean,
    modifier: Modifier = Modifier
) {
    val neutralTrack = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    val shape = RoundedCornerShape(16.dp)
    val revealThresholdPx = with(LocalDensity.current) { 24.dp.toPx() }
    val swipePx = swipeOffsetProvider()
    val revealedPx = (-swipePx).coerceAtLeast(0f)
    val progress = if (archiveThresholdPx > 0f) {
        (revealedPx / archiveThresholdPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isReady = progress >= 1f || isConfirmed

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(color = neutralTrack)

                val currentRevealed = (-swipeOffsetProvider()).coerceAtLeast(0f)
                if (currentRevealed > 0.5f) {
                    val thresholdProgress = if (archiveThresholdPx > 0f) {
                        (currentRevealed / archiveThresholdPx).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val fillWidth = currentRevealed.coerceAtMost(size.width)
                    val greenFill = if (isConfirmed) {
                        ArchiveGreen
                    } else {
                        lerp(ArchiveGreenMuted, ArchiveGreen, thresholdProgress)
                    }
                    // Grow the green fill leftward from the right edge behind the sliding card.
                    drawRect(
                        color = greenFill,
                        topLeft = Offset(size.width - fillWidth, 0f),
                        size = Size(fillWidth, size.height)
                    )

                    val barHeight = 3.dp.toPx()
                    drawRect(
                        color = ArchiveGreenAccent,
                        topLeft = Offset(size.width - fillWidth, size.height - barHeight),
                        size = Size(fillWidth, barHeight)
                    )

                    val strokePx = 1.5.dp.toPx()
                    drawRoundRect(
                        color = ArchiveGreenAccent.copy(alpha = 0.25f + 0.65f * thresholdProgress),
                        topLeft = Offset(strokePx / 2f, strokePx / 2f),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        style = Stroke(width = strokePx)
                    )
                }
            }
            .testTag("archive_swipe_indicator")
    ) {
        if (!isConfirmed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        val currentRevealed = (-swipeOffsetProvider()).coerceAtLeast(0f)
                        alpha = ((currentRevealed - revealThresholdPx) / (revealThresholdPx * 1.5f))
                            .coerceIn(0f, 1f)
                    }
            ) {
                Text(
                    text = stringResource(
                        if (isReady) R.string.release_to_archive else R.string.swipe_to_archive
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isReady) FontWeight.Bold else FontWeight.Medium,
                    color = OnArchiveGreen,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val currentRevealed = (-swipeOffsetProvider()).coerceAtLeast(0f)
                        val p = if (archiveThresholdPx > 0f) {
                            (currentRevealed / archiveThresholdPx).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val strokeWidth = 2.5.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        drawArc(
                            color = OnArchiveGreen.copy(alpha = 0.28f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = OnArchiveGreen,
                            startAngle = -90f,
                            sweepAngle = 360f * p,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Icon(
                        imageVector = if (isReady) Icons.Filled.Archive else Icons.Outlined.Archive,
                        contentDescription = stringResource(R.string.swipe_to_archive),
                        tint = OnArchiveGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isConfirmed,
            enter = fadeIn(tween(130)) + scaleIn(
                initialScale = 0.72f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = OnArchiveGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Archive,
                            contentDescription = stringResource(R.string.note_archived),
                            tint = OnArchiveGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.swipe_to_archive),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnArchiveGreen,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun ReorderDragOverlay(
    session: ReorderDragSession,
    targetBoundsProvider: () -> Rect?,
    searchQuery: String,
    rootOffsetInRoot: Offset,
    onSettleFinished: () -> Unit
) {
    val density = LocalDensity.current
    val cardWidthDp = with(density) { session.cardSizePx.width.toDp() }
    val settleProgress = remember(session.note.id) { Animatable(0f) }
    var settleStartTopLeft by remember(session.note.id) { mutableStateOf(Offset.Zero) }
    val currentOnSettleFinished by rememberUpdatedState(onSettleFinished)

    val liftScale by animateFloatAsState(
        targetValue = if (session.isSettling) 1f else 1.04f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "reorder_overlay_scale"
    )
    val shadowDp by animateFloatAsState(
        targetValue = if (session.isSettling) 2f else 14f,
        animationSpec = tween(durationMillis = REORDER_SETTLE_DURATION_MS),
        label = "reorder_overlay_shadow"
    )

    LaunchedEffect(session.isSettling) {
        if (session.isSettling) {
            settleStartTopLeft = session.pointerRootPos - rootOffsetInRoot - session.grabOffsetInCard
            settleProgress.snapTo(0f)
            settleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REORDER_SETTLE_DURATION_MS,
                    easing = FastOutSlowInEasing
                )
            )
            currentOnSettleFinished()
        }
    }

    val shadowPx = with(density) { shadowDp.dp.toPx() }
    val borderAlpha = if (session.isSettling) 0.25f else 0.7f

    Box(
        modifier = Modifier
            .offset {
                val topLeft = if (session.isSettling) {
                    val destTopLeft = targetBoundsProvider()?.let { it.topLeft - rootOffsetInRoot }
                        ?: settleStartTopLeft
                    androidx.compose.ui.geometry.lerp(
                        settleStartTopLeft,
                        destTopLeft,
                        settleProgress.value
                    )
                } else {
                    session.pointerRootPos - rootOffsetInRoot - session.grabOffsetInCard
                }
                IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
            }
            .width(cardWidthDp)
            .graphicsLayer {
                scaleX = liftScale
                scaleY = liftScale
                shadowElevation = shadowPx
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("reorder_drag_overlay")
    ) {
        NoteCard(
            note = session.note,
            isSelected = false,
            isSelectionMode = false,
            searchQuery = searchQuery,
            onClick = {},
            onLongClick = {}
        )
    }
}
