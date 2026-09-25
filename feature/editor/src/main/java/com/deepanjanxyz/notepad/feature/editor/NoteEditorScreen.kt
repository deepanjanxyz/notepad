package com.deepanjanxyz.notepad.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.core.model.ChecklistItem
import com.deepanjanxyz.notepad.core.model.DrawingSerializer
import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.model.parseChecklist
import com.deepanjanxyz.notepad.core.model.serializeChecklist
import com.deepanjanxyz.notepad.core.designsystem.components.ReminderDialog
import com.deepanjanxyz.notepad.feature.editor.components.EditorChecklistContent
import com.deepanjanxyz.notepad.feature.editor.components.EditorColorPickerSheet
import com.deepanjanxyz.notepad.feature.editor.components.EditorLabelsSheet
import com.deepanjanxyz.notepad.feature.editor.components.EditorTopBar
import com.deepanjanxyz.notepad.feature.editor.components.EmbeddedDrawingCard
import com.deepanjanxyz.notepad.core.designsystem.NoteColorOptions
import com.deepanjanxyz.notepad.core.work.NoteReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    availableTags: List<String>,
    onGetNote: suspend (Long) -> Note?,
    onSaveNote: suspend (Long, String, String, Int, List<String>, Boolean?, Boolean?, Long?) -> Long,
    onMoveToTrash: suspend (Long) -> Unit,
    onMoveToArchive: suspend (Long) -> Unit,
    onAddLabel: suspend (String) -> Unit,
    onOpenDrawing: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

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
    var isChecklistMode by remember { mutableStateOf(false) }
    var checklistItems by remember { mutableStateOf(listOf<ChecklistItem>()) }
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

                val lines = content.lines().filter { it.isNotBlank() }
                val hasChecklist = lines.isNotEmpty() && lines.any { it.startsWith("[ ] ") || it.startsWith("[x] ", ignoreCase = true) }
                if (hasChecklist) {
                    isChecklistMode = true
                    checklistItems = parseChecklist(content)
                }
            }
        }
        isLoaded = true
    }

    fun updateChecklistItems(newItems: List<ChecklistItem>) {
        checklistItems = newItems
        content = serializeChecklist(newItems)
    }

    fun addNewItem() {
        val trimmed = newItemText.trim()
        if (trimmed.isNotBlank()) {
            val updated = checklistItems + ChecklistItem(text = trimmed, isChecked = false)
            updateChecklistItems(updated)
            newItemText = ""
        }
    }

    fun getCombinedContent(textPart: String) = DrawingSerializer.combine(drawingPart, textPart)

    LaunchedEffect(title, content, drawingPart, colorIndex, tags, isPinned, inArchive, reminderTime, isLoaded) {
        if (isLoaded && (title.isNotBlank() || content.isNotBlank() || drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null)) {
            delay(500)
            val combined = getCombinedContent(content)
            val savedId = onSaveNote(currentNoteId, title, combined, colorIndex, tags, isPinned, inArchive, reminderTime)
            if (currentNoteId <= 0L && savedId > 0L) {
                currentNoteId = savedId
            }
        }
    }

    fun forceSave() {
        var effectiveText = content
        if (isChecklistMode) {
            var items = checklistItems
            if (newItemText.isNotBlank()) {
                items = items + ChecklistItem(text = newItemText.trim(), isChecked = false)
            }
            effectiveText = serializeChecklist(items)
        }
        if (isLoaded && (title.isNotBlank() || effectiveText.isNotBlank() || drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null)) {
            val combined = getCombinedContent(effectiveText)
            coroutineScope.launch {
                val savedId = onSaveNote(currentNoteId, title, combined, colorIndex, tags, isPinned, inArchive, reminderTime)
                if (currentNoteId <= 0L && savedId > 0L) {
                    currentNoteId = savedId
                }
            }
        }
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
        Color(0xFF131314)
    }

    val amberAccent = Color(0xFFFFB300)
    val textPrimary = Color(0xFFE2E2E6)
    val textPlaceholder = Color(0xFF6E7179)
    val labelPillContainer = Color(0xFF382952)
    val labelPillContent = Color(0xFFD0BCFF)

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
                isChecklistMode = isChecklistMode,
                onToggleChecklist = {
                    if (!isChecklistMode) {
                        isChecklistMode = true
                        val parsed: List<ChecklistItem> = if (content.isNotBlank()) parseChecklist(content) else emptyList()
                        checklistItems = parsed
                        content = serializeChecklist(parsed)
                    } else {
                        isChecklistMode = false
                        content = checklistItems.joinToString("\n") { it.text }
                    }
                },
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
                        color = if (isPast) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, if (isPast) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
                                tint = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = NoteReminderScheduler.formatReminderDateTime(reminderTime!!),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove reminder",
                                tint = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        reminderTime = null
                                        coroutineScope.launch {
                                            onSaveNote(currentNoteId, title, content, colorIndex, tags, isPinned, inArchive, null)
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

                if (isChecklistMode) {
                    EditorChecklistContent(
                        checklistItems = checklistItems,
                        newItemText = newItemText,
                        onNewItemTextChange = { newItemText = it },
                        onAddNewItem = { addNewItem() },
                        onItemCheckChange = { id, isChecked ->
                            val updated = checklistItems.map {
                                if (it.id == id) it.copy(isChecked = isChecked) else it
                            }
                            updateChecklistItems(updated)
                        },
                        onItemTextChange = { id, newText ->
                            val updated = checklistItems.map {
                                if (it.id == id) it.copy(text = newText) else it
                            }
                            updateChecklistItems(updated)
                        },
                        onItemDelete = { id ->
                            val updated = checklistItems.filterNot { it.id == id }
                            updateChecklistItems(updated)
                        }
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
                                color = Color(0xFF2C2C32),
                                border = BorderStroke(1.dp, Color(0xFF42434D)),
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
                    if (isChecklistMode) {
                        var items = checklistItems
                        if (newItemText.isNotBlank()) {
                            items = items + ChecklistItem(text = newItemText.trim(), isChecked = false)
                        }
                        effectiveContent = serializeChecklist(items)
                    }
                    val savedId = onSaveNote(currentNoteId, title, effectiveContent, colorIndex, tags, isPinned, inArchive, newMillis)
                    if (currentNoteId <= 0L && savedId > 0L) {
                        currentNoteId = savedId
                    }
                }
            },
            onClearReminder = {
                reminderTime = null
                coroutineScope.launch {
                    var effectiveContent = content
                    if (isChecklistMode) {
                        var items = checklistItems
                        if (newItemText.isNotBlank()) {
                            items = items + ChecklistItem(text = newItemText.trim(), isChecked = false)
                        }
                        effectiveContent = serializeChecklist(items)
                    }
                    val savedId = onSaveNote(currentNoteId, title, effectiveContent, colorIndex, tags, isPinned, inArchive, null)
                    if (currentNoteId <= 0L && savedId > 0L) {
                        currentNoteId = savedId
                    }
                }
            },
            onDismiss = { showReminderDialog = false }
        )
    }
}
