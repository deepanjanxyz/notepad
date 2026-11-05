package com.deepanjanxyz.notepad.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.ChecklistItem
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.domain.model.containsStructuredLine
import com.deepanjanxyz.notepad.domain.model.hasNoText
import com.deepanjanxyz.notepad.domain.model.parseNoteSegments
import com.deepanjanxyz.notepad.domain.model.renumberNumberedSegments
import com.deepanjanxyz.notepad.domain.model.serializeNoteSegments
import com.deepanjanxyz.notepad.ui.components.ReminderDialog
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorLinesContent
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.feature_editor.model.asSegment
import com.deepanjanxyz.notepad.ui.feature_editor.model.convertTo
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorColorPickerSheet
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorLabelsSheet
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorTopBar
import com.deepanjanxyz.notepad.ui.feature_editor.components.EmbeddedDrawingCard
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import com.deepanjanxyz.notepad.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// How long the editor waits after a change before it saves, in milliseconds.
private const val AUTO_SAVE_DELAY_MS = 500L


/**
 * The note editor: the title, the body, the checklist, the labels, the reminder and the colour.
 *
 * `LongMethod` and `CyclomaticComplexMethod` are suppressed here rather than the
 * screen split. Its state, its callbacks and its layout are read together, and the
 * alternatives - threading a dozen parameters through new composables, or hiding the
 * flow behind them - would not make any of it simpler.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    startInChecklist: Boolean = false,
    availableTags: List<String>,
    onGetNote: suspend (Long) -> Note?,
    onSaveNote: suspend (Long, String, String, Int, List<String>, Boolean?, Boolean?, Long?) -> Long,
    onMoveToArchive: suspend (Long) -> Unit,
    onAddLabel: suspend (String) -> Unit,
    onClearReminder: suspend (Long) -> Unit,
    onOpenDrawing: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentNoteId by remember { mutableLongStateOf(noteId) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var drawingPart by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var isPinned by remember { mutableStateOf(false) }
    var inArchive by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLabelsSheet by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    var editorFormat by remember { mutableStateOf(EditorFormat.PlainText) }
    var segments by remember { mutableStateOf(listOf<NoteSegment>()) }
    var newItemText by remember { mutableStateOf("") }

    LaunchedEffect(noteId) {
        if (noteId > 0L) {
            val existing = onGetNote(noteId)
            if (existing != null) {
                title = existing.title
                colorIndex = existing.colorIndex
                tags = existing.tags
                isPinned = existing.isPinned
                inArchive = existing.inArchive
                reminderTime = existing.reminderTime

                val rawContent = existing.content
                if (DrawingSerializer.isDrawing(rawContent)) {
                    drawingPart = DrawingSerializer.extractDrawingPart(rawContent)
                    content = DrawingSerializer.extractTextPart(rawContent)
                } else {
                    drawingPart = ""
                    content = rawContent
                }

                if (content.containsStructuredLine()) {
                    editorFormat = EditorFormat.of(content)
                    segments = parseNoteSegments(content)
                }
            }
        } else if (startInChecklist) {
            // A note started from the checklist card opens already in checklist mode,
            // so the first line typed is an item rather than plain text.
            editorFormat = EditorFormat.Checklist
        }
        isLoaded = true
    }

    /** Keeps the note's text and its lines in step: the text is what gets saved. */
    fun updateSegments(newSegments: List<NoteSegment>) {
        segments = newSegments
        content = serializeNoteSegments(newSegments)
    }

    /** Removes one line, leaving the lines around it in place. */
    fun deleteSegment(index: Int) {
        if (index !in segments.indices) return
        val updated = segments.toMutableList().apply { removeAt(index) }
        updateSegments(renumberNumberedSegments(updated))
    }

    /** Flips one task, leaving every other line - text, bullet or number - untouched. */
    fun toggleTask(index: Int) {
        val task = segments.getOrNull(index) as? NoteSegment.Task ?: return
        val toggled = task.copy(item = task.item.copy(isChecked = !task.item.isChecked))
        updateSegments(segments.toMutableList().apply { this[index] = toggled })
    }

    /** Rewrites the words of one line, leaving every other line untouched. */
    fun setSegmentText(index: Int, newText: String) {
        val segment = segments.getOrNull(index) ?: return
        val rewritten = when (segment) {
            is NoteSegment.Task -> segment.copy(item = segment.item.copy(text = newText))
            is NoteSegment.Bullet -> segment.copy(text = newText)
            is NoteSegment.Numbered -> segment.copy(text = newText)
            is NoteSegment.Text -> segment.copy(text = newText)
        }
        updateSegments(segments.toMutableList().apply { this[index] = rewritten })
    }

    /**
     * Adds a line below [index], of the same kind as the line it follows, so the
     * keyboard's action key keeps a list going instead of ending it.
     */
    fun splitSegment(index: Int) {
        val current = segments.getOrNull(index) ?: return
        if (current.hasNoText()) {
            deleteSegment(index)
            return
        }
        val inserted = when (current) {
            is NoteSegment.Task -> NoteSegment.Task(ChecklistItem(text = ""))
            is NoteSegment.Bullet -> NoteSegment.Bullet(text = "")
            is NoteSegment.Numbered -> current.copy(text = "")
            is NoteSegment.Text -> NoteSegment.Text("")
        }
        val updated = segments.toMutableList().apply { add(index + 1, inserted) }
        updateSegments(renumberNumberedSegments(updated))
    }

    /** Appends the line typed into the add row, as the kind the editor is in. */
    fun addItem() {
        val trimmed = newItemText.trim()
        if (trimmed.isBlank()) return
        val appended = segments + editorFormat.asSegment(trimmed)
        updateSegments(renumberNumberedSegments(appended))
        newItemText = ""
    }

    /** Switches the body to [format], rewriting its lines into that kind. */
    fun selectFormat(format: EditorFormat) {
        // Picking the format already in use has nothing to convert, and converting
        // anyway would rewrite it: a converted task is a new task, so a checklist
        // converted to itself comes back with every tick cleared.
        if (format == editorFormat) return
        // Coming from plain text nothing has read the body into lines yet, so it
        // is read here before it can be rewritten.
        if (!editorFormat.isLinesMode) segments = parseNoteSegments(content)
        // The add row's draft is not in [segments] yet, so it is folded in first:
        // changing format would otherwise throw away a line the user was part-way
        // through typing, and the next save would make that loss permanent.
        val draft = newItemText.trim()
        val lines = if (draft.isEmpty()) {
            segments
        } else {
            newItemText = ""
            renumberNumberedSegments(segments + editorFormat.asSegment(draft))
        }
        editorFormat = format
        updateSegments(lines.convertTo(format))
    }

    fun getCombinedContent(textPart: String) = DrawingSerializer.combine(drawingPart, textPart)

    LaunchedEffect(title, content, drawingPart, colorIndex, tags, isPinned, inArchive, reminderTime, isLoaded) {
        val hasSomethingToSave = title.isNotBlank() || content.isNotBlank() ||
            drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null
        if (isLoaded && hasSomethingToSave) {
            delay(AUTO_SAVE_DELAY_MS)
            val combined = getCombinedContent(content)
            val savedId = onSaveNote(
                currentNoteId, title, combined, colorIndex, tags, isPinned, inArchive, reminderTime
            )
            if (currentNoteId <= 0L && savedId > 0L) {
                currentNoteId = savedId
            }
        }
    }

    fun forceSave() {
        var effectiveText = content
        if (editorFormat.isLinesMode) {
            var pending = segments
            if (newItemText.isNotBlank()) {
                pending = pending + editorFormat.asSegment(newItemText.trim())
            }
            effectiveText = serializeNoteSegments(renumberNumberedSegments(pending))
        }
        val hasSomethingToSave = title.isNotBlank() || effectiveText.isNotBlank() ||
            drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null
        if (isLoaded && hasSomethingToSave) {
            val combined = getCombinedContent(effectiveText)
            coroutineScope.launch {
                val savedId = onSaveNote(
                    currentNoteId, title, combined, colorIndex, tags, isPinned, inArchive, reminderTime
                )
                if (currentNoteId <= 0L && savedId > 0L) {
                    currentNoteId = savedId
                }
            }
        }
    }

    fun shareNote() {
        val textPart = if (editorFormat.isLinesMode) serializeNoteSegments(segments) else content
        val body = buildString {
            if (title.isNotBlank()) {
                append(title)
                append("\n\n")
            }
            if (textPart.isNotBlank()) append(textPart)
        }.trim().ifBlank { "Shared from Elite Memo Pro" }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share note"))
    }

    BackHandler {
        forceSave()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            forceSave()
        }
    }

    val darkBackground = if (colorIndex in 1 until NoteColorOptions.size) {
        NoteColorOptions[colorIndex]
    } else {
        AppColors.PanelDeep
    }

    val amberAccent = AppColors.AccentAmber
    val textPrimary = AppColors.TextPrimary
    val textPlaceholder = AppColors.TextPlaceholder
    val labelPillContainer = AppColors.LabelPillContainer
    val labelPillContent = AppColors.LabelPillContent

    val allUniqueLabels = remember(availableTags, tags) {
        (availableTags + tags)
            .map { it.trim().replace("#", "") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val labelsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = darkBackground,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EditorTopBar(
                isPinned = isPinned,
                onTogglePin = { isPinned = !isPinned },
                hasReminder = reminderTime != null,
                onOpenReminder = { showReminderDialog = true },
                editorFormat = editorFormat,
                onFormatSelected = { format -> selectFormat(format) },
                hasColor = colorIndex > 0,
                onOpenColorPicker = { showColorPicker = true },
                hasDrawing = drawingPart.isNotBlank(),
                onOpenDrawing = {
                    forceSave()
                    onOpenDrawing(currentNoteId)
                },
                onArchive = {
                    inArchive = true
                    forceSave()
                    if (currentNoteId > 0L) {
                        coroutineScope.launch { onMoveToArchive(currentNoteId) }
                    }
                    onNavigateBack()
                },
                onShare = { shareNote() },
                onSaveAndClose = {
                    forceSave()
                    onNavigateBack()
                },
                onNavigateBack = {
                    forceSave()
                    onNavigateBack()
                }
            )

            // MAIN CONTENT BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Embedded Drawing Card
                if (drawingPart.isNotBlank() && DrawingSerializer.isDrawing(drawingPart)) {
                    EmbeddedDrawingCard(
                        drawingPart = drawingPart,
                        onClick = {
                            forceSave()
                            onOpenDrawing(currentNoteId)
                        },
                        onDelete = {
                            drawingPart = ""
                            forceSave()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Active Reminder Indicator Chip
                if (reminderTime != null) {
                    val isPast = reminderTime!! < System.currentTimeMillis()
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPast) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isPast) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showReminderDialog = true }
                            .testTag("active_reminder_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (isPast) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = NoteReminderScheduler.formatReminderDateTime(reminderTime!!),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPast) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove reminder",
                                tint = if (isPast) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                },
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        reminderTime = null
                                        val idToClear = currentNoteId
                                        if (idToClear > 0L) {
                                            coroutineScope.launch { onClearReminder(idToClear) }
                                        }
                                    }
                                    .testTag("remove_reminder_icon")
                            )
                        }
                    }
                }

                // Title Field
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    ),
                    cursorBrush = SolidColor(amberAccent),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPlaceholder
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (editorFormat.isLinesMode) {
                    EditorLinesContent(
                        segments = segments,
                        newItemText = newItemText,
                        newItemFormat = editorFormat,
                        onNewItemTextChange = { newItemText = it },
                        onAddItem = { addItem() },
                        onToggleTask = { index -> toggleTask(index) },
                        onSegmentTextChange = { index, text -> setSegmentText(index, text) },
                        onSplitSegment = { index -> splitSegment(index) },
                        onDeleteSegment = { index -> deleteSegment(index) }
                    )
                } else {
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary,
                            lineHeight = 26.sp
                        ),
                        cursorBrush = SolidColor(amberAccent),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.heightIn(min = 200.dp)) {
                                if (content.isEmpty()) {
                                    Text(
                                        text = "Note",
                                        style = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = textPlaceholder
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_content_input")
                    )
                }
            }

            // BOTTOM BAR AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = labelPillContainer,
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showLabelsSheet = true }
                            .testTag("add_tag_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add label",
                                tint = labelPillContent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add label",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = labelPillContent
                                )
                            )
                        }
                    }

                    tags.forEach { tag ->
                        val cleanTag = tag.trim().replace("#", "")
                        if (cleanTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AppColors.ChipSurface,
                                border = BorderStroke(1.dp, AppColors.FieldOutline),
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showLabelsSheet = true }
                                    .testTag("editor_tag_$cleanTag")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cleanTag,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLabelsSheet) {
        EditorLabelsSheet(
            allUniqueLabels = allUniqueLabels,
            currentTags = tags,
            onToggleLabel = { label ->
                val isChecked = tags.any { it.equals(label, ignoreCase = true) }
                tags = if (isChecked) {
                    tags.filterNot { it.equals(label, ignoreCase = true) }
                } else {
                    (tags + label).distinct()
                }
            },
            onCreateLabel = { clean ->
                coroutineScope.launch { onAddLabel(clean) }
                tags = (tags + clean).distinct()
            },
            onDismiss = { showLabelsSheet = false },
            sheetState = labelsSheetState
        )
    }

    if (showColorPicker) {
        EditorColorPickerSheet(
            selectedColorIndex = colorIndex,
            onColorSelected = { colorIndex = it },
            onDismiss = { showColorPicker = false },
            sheetState = colorSheetState
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            currentReminderTime = reminderTime,
            onSetReminder = { newMillis ->
                reminderTime = newMillis
                coroutineScope.launch {
                    var effectiveContent = content
                    if (editorFormat.isLinesMode) {
                        var pending = segments
                        if (newItemText.isNotBlank()) {
                            pending = pending + editorFormat.asSegment(newItemText.trim())
                        }
                        effectiveContent = serializeNoteSegments(renumberNumberedSegments(pending))
                    }
                    val savedId = onSaveNote(
                        currentNoteId, title, effectiveContent, colorIndex, tags, isPinned, inArchive, newMillis
                    )
                    if (currentNoteId <= 0L && savedId > 0L) {
                        currentNoteId = savedId
                    }
                }
            },
            onClearReminder = {
                reminderTime = null
                val idToClear = currentNoteId
                if (idToClear > 0L) {
                    coroutineScope.launch { onClearReminder(idToClear) }
                }
            },
            onDismiss = { showReminderDialog = false }
        )
    }
}
