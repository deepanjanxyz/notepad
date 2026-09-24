package com.deepanjanxyz.notepad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.deepanjanxyz.notepad.domain.model.CanvasGridType
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.DrawingStroke
import com.deepanjanxyz.notepad.domain.model.DrawingTool
import com.deepanjanxyz.notepad.domain.model.EraserMode
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.feature_drawing.components.CanvasBackgroundDialog
import com.deepanjanxyz.notepad.ui.feature_drawing.components.DrawingBottomToolbar
import com.deepanjanxyz.notepad.ui.feature_drawing.components.DrawingCanvas
import com.deepanjanxyz.notepad.ui.feature_drawing.components.DrawingColorPaletteSheet
import com.deepanjanxyz.notepad.ui.feature_drawing.components.DrawingEraserDrawer
import com.deepanjanxyz.notepad.ui.feature_drawing.components.DrawingTopBar
import com.deepanjanxyz.notepad.ui.feature_drawing.components.FloatingSelectionBar
import com.deepanjanxyz.notepad.ui.feature_drawing.model.KeepThicknessLevels
import com.deepanjanxyz.notepad.ui.feature_drawing.util.eraseSegmentAt
import com.deepanjanxyz.notepad.ui.feature_drawing.util.getSelectionBounds as utilGetSelectionBounds
import com.deepanjanxyz.notepad.ui.feature_drawing.util.isStrokeHit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Kept as a package-level bridge for callers that predate the drawing utility split.
fun getSelectionBounds(strokes: List<DrawingStroke>, selectedIndices: Set<Int>): Rect? =
    utilGetSelectionBounds(strokes, selectedIndices)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    noteId: Long,
    onGetNote: suspend (Long) -> Note?,
    onSaveNote: suspend (Long, String, String, Int, List<String>, Boolean?, Boolean?) -> Long,
    onNavigateBack: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var currentNoteId by remember { mutableLongStateOf(noteId) }
    var title by remember { mutableStateOf("") }
    var noteDescription by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf("Drawing")) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var isPinned by remember { mutableStateOf(false) }
    var inArchive by remember { mutableStateOf(false) }

    var canvasBackgroundColor by remember { mutableLongStateOf(0xFFFFFFFFL) }
    var canvasGridType by remember { mutableStateOf(CanvasGridType.NONE) }
    var strokes by remember { mutableStateOf<List<DrawingStroke>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<DrawingStroke?>(null) }

    var activeTool by remember { mutableStateOf(DrawingTool.PEN) }
    var penColor by remember { mutableLongStateOf(0xFF00B0FFL) }
    var penThickness by remember { mutableFloatStateOf(6f) }
    var markerColor by remember { mutableLongStateOf(0xFF34A853L) }
    var markerThickness by remember { mutableFloatStateOf(20f) }
    var highlighterColor by remember { mutableLongStateOf(0xFFFBBC04L) }
    var highlighterThickness by remember { mutableFloatStateOf(34f) }
    var showColorPalette by remember { mutableStateOf(false) }
    var showEraserOptions by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }

    val selectedColor = when (activeTool) {
        DrawingTool.PEN -> penColor
        DrawingTool.MARKER -> markerColor
        DrawingTool.HIGHLIGHTER -> highlighterColor
        else -> penColor
    }
    val currentStrokeWidth = when (activeTool) {
        DrawingTool.PEN -> penThickness
        DrawingTool.MARKER -> markerThickness
        DrawingTool.HIGHLIGHTER -> highlighterThickness
        else -> penThickness
    }

    var eraserMode by remember { mutableStateOf(EraserMode.SEGMENT) }
    var eraserRadiusPx by remember { mutableFloatStateOf(with(density) { 24.dp.toPx() }) }

    var selectedStrokeIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectionMarquee by remember { mutableStateOf<Rect?>(null) }

    var undoHistory by remember { mutableStateOf<List<List<DrawingStroke>>>(emptyList()) }
    var redoHistory by remember { mutableStateOf<List<List<DrawingStroke>>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }

    fun pushHistory() {
        undoHistory = undoHistory + listOf(strokes)
        redoHistory = emptyList()
    }

    fun handleUndo() {
        if (undoHistory.isNotEmpty()) {
            val prev = undoHistory.last()
            undoHistory = undoHistory.dropLast(1)
            redoHistory = redoHistory + listOf(strokes)
            strokes = prev
            selectedStrokeIndices = emptySet()
        }
    }

    fun handleRedo() {
        if (redoHistory.isNotEmpty()) {
            val next = redoHistory.last()
            redoHistory = redoHistory.dropLast(1)
            undoHistory = undoHistory + listOf(strokes)
            strokes = next
            selectedStrokeIndices = emptySet()
        }
    }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val existing = onGetNote(noteId)
            if (existing != null) {
                title = existing.title
                colorIndex = existing.colorIndex
                tags = if (existing.tags.isNotEmpty()) existing.tags else listOf("Drawing")
                isPinned = existing.isPinned
                inArchive = existing.inArchive

                val content = existing.content
                if (DrawingSerializer.isDrawing(content)) {
                    val drawingPart = DrawingSerializer.extractDrawingPart(content)
                    noteDescription = DrawingSerializer.extractTextPart(content)
                    val parsed = DrawingSerializer.parse(drawingPart)
                    strokes = parsed.strokes
                    canvasBackgroundColor = parsed.backgroundColor
                } else {
                    noteDescription = content
                }
            }
        }
        isLoaded = true
    }

    LaunchedEffect(strokes, canvasBackgroundColor, title, noteDescription, colorIndex, tags, isPinned, inArchive, isLoaded) {
        if (isLoaded && (strokes.isNotEmpty() || title.isNotBlank() || noteDescription.isNotBlank())) {
            delay(600)
            val drawingJson = DrawingSerializer.serialize(strokes, canvasBackgroundColor)
            val combined = DrawingSerializer.combine(drawingJson, noteDescription)
            val savedId = onSaveNote(currentNoteId, title, combined, colorIndex, tags, isPinned, inArchive)
            if (currentNoteId <= 0L && savedId > 0L) {
                currentNoteId = savedId
            }
        }
    }

    fun forceSaveAndExit() {
        if (isLoaded) {
            val drawingJson = DrawingSerializer.serialize(strokes, canvasBackgroundColor)
            val combined = DrawingSerializer.combine(drawingJson, noteDescription)
            coroutineScope.launch {
                val savedId = onSaveNote(currentNoteId, title, combined, colorIndex, tags, isPinned, inArchive)
                val targetId = if (savedId > 0L) savedId else currentNoteId
                onNavigateBack(targetId)
            }
        } else {
            onNavigateBack(currentNoteId)
        }
    }

    BackHandler {
        forceSaveAndExit()
    }

    val currentAlpha = if (activeTool == DrawingTool.HIGHLIGHTER) 0.40f else 1.0f
    val isCurrentHighlighter = activeTool == DrawingTool.HIGHLIGHTER
    val selectionBounds = remember(strokes, selectedStrokeIndices) {
        utilGetSelectionBounds(strokes, selectedStrokeIndices)
    }

    fun eraseNear(cur: Offset, prev: Offset?) {
        if (strokes.isEmpty()) return
        if (eraserMode == EraserMode.STROKE) {
            val surviving = strokes.filterNot { s ->
                isStrokeHit(s, cur, eraserRadiusPx) || (prev != null && isStrokeHit(s, prev, eraserRadiusPx))
            }
            if (surviving.size != strokes.size) {
                strokes = surviving
            }
        } else {
            val afterCur = eraseSegmentAt(cur, eraserRadiusPx, strokes)
            val finalStrokes = if (prev != null && prev != cur) {
                eraseSegmentAt(prev, eraserRadiusPx, afterCur)
            } else {
                afterCur
            }
            strokes = finalStrokes
        }
    }

    fun duplicateSelectedStrokes() {
        if (selectedStrokeIndices.isEmpty()) return
        pushHistory()
        val offsetDp = with(density) { 20.dp.toPx() }
        val toDuplicate = selectedStrokeIndices.mapNotNull { strokes.getOrNull(it) }
        val newStrokes = toDuplicate.map { s ->
            s.copy(
                points = s.points.map { pt -> DrawingPoint(pt.x + offsetDp, pt.y + offsetDp) }
            )
        }
        val startIdx = strokes.size
        strokes = strokes + newStrokes
        selectedStrokeIndices = (startIdx until startIdx + newStrokes.size).toSet()
    }

    fun deleteSelectedStrokes() {
        if (selectedStrokeIndices.isEmpty()) return
        pushHistory()
        strokes = strokes.filterIndexed { index, _ -> !selectedStrokeIndices.contains(index) }
        selectedStrokeIndices = emptySet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF131314))
            .navigationBarsPadding()
    ) {
        DrawingTopBar(
            title = title,
            onTitleChange = { title = it },
            canUndo = undoHistory.isNotEmpty(),
            canRedo = redoHistory.isNotEmpty(),
            onUndo = { handleUndo() },
            onRedo = { handleRedo() },
            onNavigateBack = { forceSaveAndExit() },
            onClearCanvas = {
                pushHistory()
                strokes = emptyList()
                selectedStrokeIndices = emptySet()
            },
            currentGridType = canvasGridType,
            onGridTypeChange = { canvasGridType = it },
            onOpenBackgroundDialog = { showBackgroundDialog = true }
        )

        if (showBackgroundDialog) {
            CanvasBackgroundDialog(
                selectedColor = canvasBackgroundColor,
                selectedGrid = canvasGridType,
                onColorSelected = { canvasBackgroundColor = it },
                onGridSelected = { canvasGridType = it },
                onDismiss = { showBackgroundDialog = false }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(canvasBackgroundColor))
        ) {
            DrawingCanvas(
                strokes = strokes,
                currentStroke = currentStroke,
                canvasBackgroundColor = canvasBackgroundColor,
                canvasGridType = canvasGridType,
                activeTool = activeTool,
                eraserMode = eraserMode,
                eraserRadiusPx = eraserRadiusPx,
                selectedStrokeIndices = selectedStrokeIndices,
                selectionBounds = selectionBounds,
                selectionMarquee = selectionMarquee,
                currentStrokeWidth = currentStrokeWidth,
                selectedColor = selectedColor,
                currentAlpha = currentAlpha,
                isCurrentHighlighter = isCurrentHighlighter,
                onStrokesChange = { strokes = it },
                onCurrentStrokeChange = { currentStroke = it },
                onSelectedIndicesChange = { selectedStrokeIndices = it },
                onSelectionMarqueeChange = { selectionMarquee = it },
                onPushHistory = { pushHistory() },
                onEraseNear = { cur, prev -> eraseNear(cur, prev) },
                onCanvasTouchDown = {
                    showColorPalette = false
                    showEraserOptions = false
                }
            )

            if (activeTool == DrawingTool.SELECT && selectedStrokeIndices.isNotEmpty()) {
                FloatingSelectionBar(
                    selectedCount = selectedStrokeIndices.size,
                    onDuplicate = { duplicateSelectedStrokes() },
                    onDelete = { deleteSelectedStrokes() },
                    onClearSelection = { selectedStrokeIndices = emptySet() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showColorPalette && (activeTool == DrawingTool.PEN || activeTool == DrawingTool.MARKER || activeTool == DrawingTool.HIGHLIGHTER),
            enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(animationSpec = tween(180))
        ) {
            DrawingColorPaletteSheet(
                activeTool = activeTool,
                selectedColor = selectedColor,
                selectedThickness = currentStrokeWidth,
                onColorSelected = { c ->
                    when (activeTool) {
                        DrawingTool.PEN -> penColor = c
                        DrawingTool.MARKER -> markerColor = c
                        DrawingTool.HIGHLIGHTER -> highlighterColor = c
                        else -> {}
                    }
                },
                onThicknessSelected = { thick ->
                    when (activeTool) {
                        DrawingTool.PEN -> penThickness = thick
                        DrawingTool.MARKER -> markerThickness = thick
                        DrawingTool.HIGHLIGHTER -> highlighterThickness = thick
                        else -> {}
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showEraserOptions && activeTool == DrawingTool.ERASER,
            enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(animationSpec = tween(180))
        ) {
            DrawingEraserDrawer(
                currentMode = eraserMode,
                onModeSelected = { eraserMode = it },
                currentRadiusPx = eraserRadiusPx,
                onRadiusChanged = { eraserRadiusPx = it },
                onClearCanvas = {
                    pushHistory()
                    strokes = emptyList()
                    selectedStrokeIndices = emptySet()
                }
            )
        }

        DrawingBottomToolbar(
            activeTool = activeTool,
            onToolClick = { tool ->
                when (tool) {
                    DrawingTool.SELECT -> {
                        activeTool = DrawingTool.SELECT
                        showColorPalette = false
                        showEraserOptions = false
                    }
                    DrawingTool.ERASER -> {
                        if (activeTool == DrawingTool.ERASER) {
                            showEraserOptions = !showEraserOptions
                        } else {
                            activeTool = DrawingTool.ERASER
                            showEraserOptions = true
                            showColorPalette = false
                            selectedStrokeIndices = emptySet()
                        }
                    }
                    DrawingTool.PEN -> {
                        showEraserOptions = false
                        if (activeTool == DrawingTool.PEN) {
                            showColorPalette = !showColorPalette
                        } else {
                            activeTool = DrawingTool.PEN
                            showColorPalette = true
                            selectedStrokeIndices = emptySet()
                        }
                    }
                    DrawingTool.MARKER -> {
                        showEraserOptions = false
                        if (activeTool == DrawingTool.MARKER) {
                            showColorPalette = !showColorPalette
                        } else {
                            activeTool = DrawingTool.MARKER
                            showColorPalette = true
                            selectedStrokeIndices = emptySet()
                        }
                    }
                    DrawingTool.HIGHLIGHTER -> {
                        showEraserOptions = false
                        if (activeTool == DrawingTool.HIGHLIGHTER) {
                            showColorPalette = !showColorPalette
                        } else {
                            activeTool = DrawingTool.HIGHLIGHTER
                            showColorPalette = true
                            selectedStrokeIndices = emptySet()
                        }
                    }
                }
            },
            selectedColor = selectedColor,
            penColor = penColor,
            markerColor = markerColor,
            highlighterColor = highlighterColor
        )
    }
}
