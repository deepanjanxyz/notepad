package com.deepanjanxyz.notepad.ui.screens

import com.deepanjanxyz.notepad.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.backup.DownloadsBackup
import com.deepanjanxyz.notepad.backup.NotePdfExport
import com.deepanjanxyz.notepad.domain.model.ChecklistItem
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.domain.model.RichBlockKind
import com.deepanjanxyz.notepad.domain.model.RichBlockSpan
import com.deepanjanxyz.notepad.domain.model.RichInlineKind
import com.deepanjanxyz.notepad.domain.model.RichInlineSpan
import com.deepanjanxyz.notepad.domain.model.RichLinkSpan
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.domain.model.RichTextDocument
import com.deepanjanxyz.notepad.domain.model.containsStructuredLine
import com.deepanjanxyz.notepad.domain.model.hasNoText
import com.deepanjanxyz.notepad.domain.model.lineText
import com.deepanjanxyz.notepad.domain.model.parseNoteSegments
import com.deepanjanxyz.notepad.domain.model.renumberNumberedSegments
import com.deepanjanxyz.notepad.ui.components.ReminderDialog
import com.deepanjanxyz.notepad.ui.components.StatefulBasicTextField
import com.deepanjanxyz.notepad.ui.feature_editor.components.ActiveLinkPill
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorAddSheet
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorBottomBar
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorColorPickerSheet
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorFindBar
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorFormattingBar
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorLabelsSheet
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorLinesContent
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorLinkDialog
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorMoreSheet
import com.deepanjanxyz.notepad.ui.feature_editor.components.EditorTopBar
import com.deepanjanxyz.notepad.ui.feature_editor.components.EmbeddedDrawingCard
import com.deepanjanxyz.notepad.ui.feature_editor.model.BlockStyle
import com.deepanjanxyz.notepad.ui.feature_editor.model.EditorFormat
import com.deepanjanxyz.notepad.ui.feature_editor.model.asSegment
import com.deepanjanxyz.notepad.ui.feature_editor.model.buildBodyOutputTransformation
import com.deepanjanxyz.notepad.ui.feature_editor.model.convertTo
import com.deepanjanxyz.notepad.ui.feature_editor.model.detectBlockStyle
import com.deepanjanxyz.notepad.ui.feature_editor.model.detectInlineFormats
import com.deepanjanxyz.notepad.ui.feature_editor.model.findMatchRanges
import com.deepanjanxyz.notepad.ui.feature_editor.model.toDomainKind
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.theme.OnBackgroundDark
import com.deepanjanxyz.notepad.ui.theme.OnPrimaryContainerDark
import com.deepanjanxyz.notepad.ui.theme.OnPrimaryDark
import com.deepanjanxyz.notepad.ui.theme.OnSurfaceVariantDark
import com.deepanjanxyz.notepad.ui.theme.PrimaryContainerDark
import com.deepanjanxyz.notepad.ui.theme.PrimaryDark
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// How long the editor waits after a change before it saves, in milliseconds.
private const val AUTO_SAVE_DELAY_MS = 500L

// Maximum label chips displayed in the editor bottom bar before collapsing.
private const val MAX_VISIBLE_EDITOR_LABELS = 4

// Sentinel focus index when the "add item" draft row has focus in lines mode.
private const val NEW_LINE_FOCUS_INDEX = -1

private data class EditorLinkDialogState(
    val initialText: String,
    val initialUrl: String,
    val isEditing: Boolean,
    val targetSegmentIndex: Int?,
    val selStart: Int,
    val selEnd: Int
)

private data class LineFindMatch(
    val segmentIndex: Int,
    val range: IntRange
)


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
    startInChecklist: Boolean,
    availableTags: List<String>,
    onGetNote: suspend (Long) -> Note?,
    onSaveNote: suspend (Long, String, String, Int, List<String>, Boolean?, Boolean?, Long?) -> Long,
    onMoveToArchive: suspend (Long) -> Unit,
    onMoveToTrash: suspend (Long) -> Unit = {},
    onAddLabel: suspend (String) -> Unit,
    onClearReminder: suspend (Long) -> Unit,
    onOpenDrawing: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val snackbarHostState = remember { SnackbarHostState() }
    val bodyScrollState = rememberScrollState()
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    var bodyViewportWidthDp by remember { mutableStateOf<Float?>(null) }
    var bodyViewportHeightDp by remember { mutableStateOf<Float?>(null) }
    var isExportingPdf by remember { mutableStateOf(false) }

    var currentNoteId by remember { mutableLongStateOf(noteId) }
    var title by remember { mutableStateOf("") }
    var richDoc by remember { mutableStateOf(RichTextDocument()) }
    var activeTypingKinds by remember { mutableStateOf(emptySet<RichInlineKind>()) }
    val bodyTextFieldState = rememberTextFieldState(initialText = "")
    var lastPlainSelection by remember { mutableStateOf(TextRange.Zero) }
    var drawingPart by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var isPinned by remember { mutableStateOf(false) }
    var inArchive by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var editedAtMillis by remember { mutableLongStateOf(0L) }
    var isDeleting by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLabelsSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showFormattingBar by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var activeMatchIndex by remember { mutableIntStateOf(0) }
    var isLoaded by remember { mutableStateOf(false) }
    var editorFormat by remember { mutableStateOf(EditorFormat.PlainText) }
    var segments by remember { mutableStateOf(listOf<NoteSegment>()) }
    var segmentInlineSpans by remember { mutableStateOf<List<List<RichInlineSpan>>>(emptyList()) }
    var segmentLinkSpans by remember { mutableStateOf<List<List<RichLinkSpan>>>(emptyList()) }
    var newItemText by remember { mutableStateOf("") }
    var newItemSpans by remember { mutableStateOf<List<RichInlineSpan>>(emptyList()) }
    var newItemLinkSpans by remember { mutableStateOf<List<RichLinkSpan>>(emptyList()) }
    var focusedSegmentIndex by remember { mutableStateOf<Int?>(null) }
    var focusedSegmentSelection by remember { mutableStateOf(TextRange.Zero) }
    var linkDialogState by remember { mutableStateOf<EditorLinkDialogState?>(null) }
    var lastSavedSnapshot by remember { mutableStateOf<String?>(null) }
    val saveMutex = remember { Mutex() }
    var activeSaveJob by remember { mutableStateOf<Job?>(null) }

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
                editedAtMillis = existing.updatedAt

                val rawContent = existing.content
                val rawTextPart = if (DrawingSerializer.isDrawing(rawContent)) {
                    drawingPart = DrawingSerializer.extractDrawingPart(rawContent)
                    DrawingSerializer.extractTextPart(rawContent)
                } else {
                    drawingPart = ""
                    rawContent
                }

                val decoded = RichTextCodec.decode(rawTextPart)
                richDoc = decoded

                if (decoded.text.containsStructuredLine()) {
                    editorFormat = EditorFormat.of(decoded.text)
                    segments = parseNoteSegments(decoded.text)
                    segmentInlineSpans = RichTextCodec.extractSegmentInlineSpans(decoded)
                    segmentLinkSpans = RichTextCodec.extractSegmentLinkSpans(decoded)
                } else {
                    bodyTextFieldState.setTextAndPlaceCursorAtEnd(decoded.text)
                    lastPlainSelection = bodyTextFieldState.selection
                    activeTypingKinds = RichTextCodec.inlineKindsAtCursor(decoded, decoded.text.length)
                }
            }
        } else if (startInChecklist) {
            // A note started from the checklist card opens already in checklist mode,
            // so the first line typed is an item rather than plain text.
            editorFormat = EditorFormat.Checklist
        }
        isLoaded = true
    }

    LaunchedEffect(bodyTextFieldState) {
        snapshotFlow { bodyTextFieldState.text.toString() to bodyTextFieldState.selection }
            .collect { (liveText, liveSelection) ->
                if (isLoaded && !editorFormat.isLinesMode) {
                    if (liveText != richDoc.text) {
                        richDoc = RichTextCodec.adjustForTextEdit(
                            oldDoc = richDoc,
                            newText = liveText,
                            cursorAfterEdit = liveSelection.end,
                            activeTypingKinds = activeTypingKinds
                        )
                        lastPlainSelection = liveSelection
                    } else if (liveSelection != lastPlainSelection) {
                        activeTypingKinds = if (liveSelection.collapsed) {
                            RichTextCodec.inlineKindsAtCursor(richDoc, liveSelection.start)
                        } else {
                            RichTextCodec.inlineKindsCoveringSelection(
                                richDoc,
                                liveSelection.start,
                                liveSelection.end
                            )
                        }
                        lastPlainSelection = liveSelection
                    }
                }
            }
    }

    /** Keeps the note's text, lines, and inline formatting spans in step. */
    fun updateSegmentsWithSpans(
        newSegments: List<NoteSegment>,
        newSpans: List<List<RichInlineSpan>>,
        newLinks: List<List<RichLinkSpan>> = segmentLinkSpans,
        blockSpans: List<RichBlockSpan> = emptyList()
    ) {
        segments = newSegments
        segmentInlineSpans = newSpans
        segmentLinkSpans = newLinks
        richDoc = RichTextCodec.assembleFromSegments(newSegments, newSpans, blockSpans, newLinks)
    }

    /** Removes one line, leaving the lines and spans around it in place. */
    fun deleteSegment(index: Int) {
        if (index !in segments.indices) return
        val updatedSegments = segments.toMutableList().apply { removeAt(index) }
        val updatedSpans = segmentInlineSpans.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
        val updatedLinks = segmentLinkSpans.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
        focusedSegmentIndex = when (val currentFocus = focusedSegmentIndex) {
            null -> null
            index -> null
            else -> if (currentFocus > index) currentFocus - 1 else currentFocus
        }
        updateSegmentsWithSpans(renumberNumberedSegments(updatedSegments), updatedSpans, updatedLinks)
    }

    /** Flips one task, leaving every other line and all inline formatting untouched. */
    fun toggleTask(index: Int) {
        val task = segments.getOrNull(index) as? NoteSegment.Task ?: return
        val toggled = task.copy(item = task.item.copy(isChecked = !task.item.isChecked))
        updateSegmentsWithSpans(
            segments.toMutableList().apply { this[index] = toggled },
            segmentInlineSpans,
            segmentLinkSpans
        )
    }

    /** Rewrites the words of one line, updating only that line's inline spans. */
    fun setSegmentText(index: Int, newText: String, cursorOffset: Int) {
        val segment = segments.getOrNull(index) ?: return
        val oldText = segment.lineText()
        if (oldText == newText) return
        val currentSpans = segmentInlineSpans.getOrNull(index).orEmpty()
        val adjustedSpans = RichTextCodec.adjustLocalSpansForEdit(
            oldSpans = currentSpans,
            oldText = oldText,
            newText = newText,
            cursorAfterEdit = cursorOffset,
            activeTypingKinds = activeTypingKinds
        )
        val currentLinks = segmentLinkSpans.getOrNull(index).orEmpty()
        val adjustedLinks = RichTextCodec.adjustLocalLinkSpansForEdit(
            oldLinks = currentLinks,
            oldText = oldText,
            newText = newText,
            cursorAfterEdit = cursorOffset
        )
        val rewritten = when (segment) {
            is NoteSegment.Task -> segment.copy(item = segment.item.copy(text = newText))
            is NoteSegment.Bullet -> segment.copy(text = newText)
            is NoteSegment.Numbered -> segment.copy(text = newText)
            is NoteSegment.Text -> segment.copy(text = newText)
        }
        val nextSegments = segments.toMutableList().apply { this[index] = rewritten }
        val nextSpansList = segmentInlineSpans.toMutableList().apply {
            while (size <= index) add(emptyList())
            this[index] = adjustedSpans
        }
        val nextLinksList = segmentLinkSpans.toMutableList().apply {
            while (size <= index) add(emptyList())
            this[index] = adjustedLinks
        }
        updateSegmentsWithSpans(nextSegments, nextSpansList, nextLinksList)
    }

    /** Adds a line below [index], keeping existing lines and their spans intact. */
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
        val updatedSegments = segments.toMutableList().apply { add(index + 1, inserted) }
        val updatedSpans = segmentInlineSpans.toMutableList().apply {
            while (size < segments.size) add(emptyList())
            add(index + 1, emptyList())
        }
        val updatedLinks = segmentLinkSpans.toMutableList().apply {
            while (size < segments.size) add(emptyList())
            add(index + 1, emptyList())
        }
        updateSegmentsWithSpans(renumberNumberedSegments(updatedSegments), updatedSpans, updatedLinks)
    }

    fun trimLeadingSpans(raw: String, spans: List<RichInlineSpan>): Pair<String, List<RichInlineSpan>> {
        val leadingTrim = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) raw.length else it }
        val trimmed = raw.trim()
        val shifted = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            val adjusted = spans.map {
                it.copy(start = it.start - leadingTrim, end = it.end - leadingTrim)
            }
            RichTextCodec.normalizeInlineSpans(adjusted, trimmed.length)
        }
        return trimmed to shifted
    }

    fun trimLeadingLinkSpans(raw: String, links: List<RichLinkSpan>): List<RichLinkSpan> {
        val leadingTrim = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) raw.length else it }
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        val adjusted = links.map {
            it.copy(start = it.start - leadingTrim, end = it.end - leadingTrim)
        }
        return RichTextCodec.normalizeLinkSpans(adjusted, trimmed.length)
    }

    /** Appends the line typed into the add row, preserving any inline formatting on it. */
    fun addItem() {
        val (trimmed, trimmedSpans) = trimLeadingSpans(newItemText, newItemSpans)
        if (trimmed.isBlank()) return
        val trimmedLinks = trimLeadingLinkSpans(newItemText, newItemLinkSpans)
        val appended = segments + editorFormat.asSegment(trimmed)
        val appendedSpans = segmentInlineSpans.toMutableList().apply {
            while (size < segments.size) add(emptyList())
            add(trimmedSpans)
        }
        val appendedLinks = segmentLinkSpans.toMutableList().apply {
            while (size < segments.size) add(emptyList())
            add(trimmedLinks)
        }
        newItemText = ""
        newItemSpans = emptyList()
        newItemLinkSpans = emptyList()
        updateSegmentsWithSpans(renumberNumberedSegments(appended), appendedSpans, appendedLinks)
    }

    /** Switches the body to [format], keeping each line's inline formatting spans. */
    fun selectFormat(format: EditorFormat) {
        if (format == editorFormat) return
        val sourceSegments: List<NoteSegment>
        val sourceSpans: List<List<RichInlineSpan>>
        val sourceLinks: List<List<RichLinkSpan>>
        if (!editorFormat.isLinesMode) {
            sourceSegments = parseNoteSegments(richDoc.text)
            sourceSpans = RichTextCodec.extractSegmentInlineSpans(richDoc)
            sourceLinks = RichTextCodec.extractSegmentLinkSpans(richDoc)
        } else {
            sourceSegments = segments
            sourceSpans = segmentInlineSpans
            sourceLinks = segmentLinkSpans
        }

        val (draft, draftSpans) = trimLeadingSpans(newItemText, newItemSpans)
        val draftLinks = trimLeadingLinkSpans(newItemText, newItemLinkSpans)
        val combinedSegments: List<NoteSegment>
        val combinedSpans: List<List<RichInlineSpan>>
        val combinedLinks: List<List<RichLinkSpan>>
        if (draft.isEmpty()) {
            combinedSegments = sourceSegments
            combinedSpans = sourceSpans
            combinedLinks = sourceLinks
        } else {
            newItemText = ""
            newItemSpans = emptyList()
            newItemLinkSpans = emptyList()
            combinedSegments = renumberNumberedSegments(sourceSegments + editorFormat.asSegment(draft))
            combinedSpans = sourceSpans.toMutableList().apply {
                while (size < sourceSegments.size) add(emptyList())
                add(draftSpans)
            }
            combinedLinks = sourceLinks.toMutableList().apply {
                while (size < sourceSegments.size) add(emptyList())
                add(draftLinks)
            }
        }

        val converted = combinedSegments.convertTo(format)
        if (format == EditorFormat.PlainText) {
            val plainDoc = RichTextCodec.assembleFromSegments(
                converted,
                combinedSpans,
                richDoc.blockSpans,
                combinedLinks
            )
            bodyTextFieldState.setTextAndPlaceCursorAtEnd(plainDoc.text)
            lastPlainSelection = bodyTextFieldState.selection
            activeTypingKinds = RichTextCodec.inlineKindsAtCursor(plainDoc, plainDoc.text.length)
            richDoc = plainDoc
            segments = converted
            segmentInlineSpans = combinedSpans
            segmentLinkSpans = combinedLinks
            focusedSegmentIndex = null
            editorFormat = format
        } else {
            editorFormat = format
            updateSegmentsWithSpans(converted, combinedSpans, combinedLinks)
        }
    }

    fun getCombinedContent(textPart: String) = DrawingSerializer.combine(drawingPart, textPart)

    /** The serialized body (text plus any encoded formatting spans), add-row draft included. */
    fun effectiveContent(): String {
        if (!editorFormat.isLinesMode) return RichTextCodec.encode(richDoc)
        if (newItemText.isBlank()) return RichTextCodec.encode(richDoc)

        val (trimmed, trimmedSpans) = trimLeadingSpans(newItemText, newItemSpans)
        val trimmedLinks = trimLeadingLinkSpans(newItemText, newItemLinkSpans)
        val pendingSegments = renumberNumberedSegments(segments + editorFormat.asSegment(trimmed))
        val pendingSpans = segmentInlineSpans.toMutableList().apply {
            while (size < segments.size) add(emptyList())
            add(trimmedSpans)
        }
        val pendingLinks = segmentLinkSpans.toMutableList().apply {
            while (size < segments.size) add(emptyList())
            add(trimmedLinks)
        }
        return RichTextCodec.encode(
            RichTextCodec.assembleFromSegments(
                segments = pendingSegments,
                perSegmentSpans = pendingSpans,
                perSegmentLinkSpans = pendingLinks
            )
        )
    }

    fun buildSnapshotFor(
        id: Long,
        noteTitle: String,
        combinedContent: String,
        noteColorIndex: Int,
        noteTags: List<String>,
        noteIsPinned: Boolean,
        noteInArchive: Boolean,
        noteReminderTime: Long?
    ): String = listOf(
        id.toString(),
        noteTitle,
        combinedContent,
        noteColorIndex.toString(),
        noteTags.joinToString("\u0000"),
        noteIsPinned.toString(),
        noteInArchive.toString(),
        noteReminderTime?.toString().orEmpty()
    ).joinToString("\u0001")

    fun launchSerializedSave(): Job? {
        if (isDeleting || !isLoaded) return null
        val effectiveText = effectiveContent()
        val hasSomethingToSave = title.isNotBlank() || effectiveText.isNotBlank() ||
            drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null
        if (!hasSomethingToSave) return null

        val job = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                saveMutex.withLock {
                    if (isDeleting) return@withLock
                    val latestEffective = effectiveContent()
                    val latestHasContent = title.isNotBlank() || latestEffective.isNotBlank() ||
                        drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null
                    if (!latestHasContent) return@withLock

                    val targetId = currentNoteId
                    val noteTitle = title
                    val combined = getCombinedContent(latestEffective)
                    val noteColorIndex = colorIndex
                    val noteTags = tags
                    val noteIsPinned = isPinned
                    val noteInArchive = inArchive
                    val noteReminderTime = reminderTime

                    val snapshot = buildSnapshotFor(
                        id = targetId,
                        noteTitle = noteTitle,
                        combinedContent = combined,
                        noteColorIndex = noteColorIndex,
                        noteTags = noteTags,
                        noteIsPinned = noteIsPinned,
                        noteInArchive = noteInArchive,
                        noteReminderTime = noteReminderTime
                    )
                    if (snapshot == lastSavedSnapshot) return@withLock

                    val savedId = onSaveNote(
                        targetId,
                        noteTitle,
                        combined,
                        noteColorIndex,
                        noteTags,
                        noteIsPinned,
                        noteInArchive,
                        noteReminderTime
                    )
                    val resolvedId = if (targetId <= 0L && savedId > 0L) {
                        currentNoteId = savedId
                        savedId
                    } else {
                        targetId
                    }
                    editedAtMillis = System.currentTimeMillis()
                    lastSavedSnapshot = buildSnapshotFor(
                        id = resolvedId,
                        noteTitle = noteTitle,
                        combinedContent = combined,
                        noteColorIndex = noteColorIndex,
                        noteTags = noteTags,
                        noteIsPinned = noteIsPinned,
                        noteInArchive = noteInArchive,
                        noteReminderTime = noteReminderTime
                    )
                }
            }
        }
        activeSaveJob = job
        return job
    }

    LaunchedEffect(
        title, richDoc, newItemText, newItemSpans, newItemLinkSpans, drawingPart, colorIndex, tags, isPinned,
        inArchive, reminderTime, isLoaded
    ) {
        val effectiveText = effectiveContent()
        val hasSomethingToSave = title.isNotBlank() || effectiveText.isNotBlank() ||
            drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null
        if (isLoaded && hasSomethingToSave && !isDeleting) {
            delay(AUTO_SAVE_DELAY_MS)
            launchSerializedSave()
        }
    }

    fun forceSave() {
        launchSerializedSave()
    }

    fun shareNote() {
        val textPart = RichTextCodec.extractPlainText(effectiveContent())
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

    fun runPdfExport() {
        if (isExportingPdf) return
        isExportingPdf = true
        val noteTitle = title
        val textPart = effectiveContent()
        val viewportWidthDp = bodyViewportWidthDp
        val viewportHeightDp = bodyViewportHeightDp
        val scrollOverflow = if (imeBottomPx == 0) bodyScrollState.maxValue > 0 else null
        coroutineScope.launch {
            try {
                val outcome = withContext(Dispatchers.IO) {
                    NotePdfExport.exportNote(
                        context = context,
                        title = noteTitle,
                        content = textPart,
                        viewportWidthDp = viewportWidthDp,
                        viewportHeightDp = viewportHeightDp,
                        isContentScrollable = scrollOverflow
                    )
                }
                snackbarHostState.showSnackbar(outcome.message)
            } finally {
                isExportingPdf = false
            }
        }
    }

    val pdfPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runPdfExport()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Storage permission is needed to export PDF to Downloads")
            }
        }
    }

    fun exportPdf() {
        val missingPermission = DownloadsBackup.needsLegacyPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (missingPermission) {
            pdfPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            runPdfExport()
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

    val hasCustomColor = colorIndex in 1 until NoteColorOptions.size
    val editorBackground = if (hasCustomColor) {
        NoteColorOptions[colorIndex]
    } else {
        MaterialTheme.colorScheme.background
    }

    val accentColor = if (hasCustomColor) PrimaryDark else MaterialTheme.colorScheme.primary
    val onAccentColor = if (hasCustomColor) OnPrimaryDark else MaterialTheme.colorScheme.onPrimary
    val accentContainer = if (hasCustomColor) {
        PrimaryContainerDark
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val onAccentContainer = if (hasCustomColor) {
        OnPrimaryContainerDark
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val textPrimary = if (hasCustomColor) OnBackgroundDark else MaterialTheme.colorScheme.onBackground
    val textMuted = if (hasCustomColor) {
        OnSurfaceVariantDark
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textPlaceholder = textMuted.copy(alpha = 0.65f)
    val chipContainer = if (hasCustomColor) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val chipBorder = if (hasCustomColor) {
        Color.White.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val allUniqueLabels = remember(availableTags, tags) {
        (availableTags + tags)
            .map { it.replace("#", "").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val modalSheetEnabledValues = remember { setOf(SheetValue.Hidden, SheetValue.Expanded) }
    val labelsSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = modalSheetEnabledValues
    )
    val colorSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = modalSheetEnabledValues
    )
    val addSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = modalSheetEnabledValues
    )
    val moreSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = modalSheetEnabledValues
    )

    val matchRanges = remember(richDoc.text, findQuery, showFindBar, editorFormat) {
        if (showFindBar && !editorFormat.isLinesMode) {
            findMatchRanges(richDoc.text, findQuery)
        } else {
            emptyList()
        }
    }
    val lineMatches = remember(segments, newItemText, findQuery, showFindBar, editorFormat) {
        if (!showFindBar || findQuery.isBlank() || !editorFormat.isLinesMode) {
            emptyList()
        } else {
            buildList {
                segments.forEachIndexed { idx, segment ->
                    findMatchRanges(segment.lineText(), findQuery).forEach { range ->
                        add(LineFindMatch(segmentIndex = idx, range = range))
                    }
                }
                if (newItemText.isNotBlank()) {
                    findMatchRanges(newItemText, findQuery).forEach { range ->
                        add(LineFindMatch(segmentIndex = NEW_LINE_FOCUS_INDEX, range = range))
                    }
                }
            }
        }
    }
    val matchCount = if (editorFormat.isLinesMode) lineMatches.size else matchRanges.size
    LaunchedEffect(matchCount) {
        activeMatchIndex = if (matchCount == 0) {
            0
        } else {
            activeMatchIndex.coerceIn(0, matchCount - 1)
        }
    }
    var activeMatchJumpToken by remember { mutableIntStateOf(0) }
    val activeLineMatch = if (editorFormat.isLinesMode) lineMatches.getOrNull(activeMatchIndex) else null
    val findHighlightColor = accentContainer
    val findActiveColor = accentColor.copy(alpha = 0.45f)
    val bodyOutputTransformation = remember(
        richDoc,
        accentColor,
        showFindBar,
        findQuery,
        activeMatchIndex,
        findHighlightColor,
        findActiveColor
    ) {
        buildBodyOutputTransformation(
            doc = richDoc,
            linkColor = accentColor,
            showFindBar = showFindBar,
            findQuery = findQuery,
            activeMatchIndex = activeMatchIndex,
            findHighlightColor = findHighlightColor,
            findActiveColor = findActiveColor
        )
    }

    val linkOpenErrorMessage = stringResource(R.string.link_open_error)
    fun openExternalUrl(rawUrl: String) {
        val safeUrl = RichTextCodec.sanitizeLinkUrl(rawUrl)
        if (safeUrl == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(linkOpenErrorMessage) }
            return
        }
        val opened = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
        if (!opened) {
            coroutineScope.launch { snackbarHostState.showSnackbar(linkOpenErrorMessage) }
        }
    }

    fun currentActiveLink(): RichLinkSpan? = when {
        !editorFormat.isLinesMode -> RichTextCodec.linkAt(
            richDoc,
            bodyTextFieldState.selection.start,
            bodyTextFieldState.selection.end
        )
        focusedSegmentIndex == NEW_LINE_FOCUS_INDEX -> RichTextCodec.linkAt(
            linkSpans = newItemLinkSpans,
            textLength = newItemText.length,
            selStart = focusedSegmentSelection.start,
            selEnd = focusedSegmentSelection.end
        )
        focusedSegmentIndex != null && focusedSegmentIndex in segments.indices -> {
            val idx = focusedSegmentIndex!!
            RichTextCodec.linkAt(
                linkSpans = segmentLinkSpans.getOrNull(idx).orEmpty(),
                textLength = segments[idx].lineText().length,
                selStart = focusedSegmentSelection.start,
                selEnd = focusedSegmentSelection.end
            )
        }
        else -> null
    }

    fun openLinkDialog() {
        if (!editorFormat.isLinesMode) {
            val sel = bodyTextFieldState.selection
            val s = minOf(sel.start, sel.end).coerceIn(0, richDoc.text.length)
            val e = maxOf(sel.start, sel.end).coerceIn(0, richDoc.text.length)
            val existing = RichTextCodec.linkAt(richDoc, s, e)
            val spanStart = existing?.start ?: s
            val spanEnd = existing?.end ?: e
            val initialText = if (spanStart < spanEnd) {
                richDoc.text.substring(spanStart, spanEnd).replace('\n', ' ')
            } else {
                ""
            }
            linkDialogState = EditorLinkDialogState(
                initialText = initialText,
                initialUrl = existing?.url.orEmpty(),
                isEditing = existing != null,
                targetSegmentIndex = null,
                selStart = spanStart,
                selEnd = spanEnd
            )
            return
        }

        val targetIdx = focusedSegmentIndex ?: NEW_LINE_FOCUS_INDEX
        if (targetIdx == NEW_LINE_FOCUS_INDEX) {
            val sel = if (focusedSegmentIndex == NEW_LINE_FOCUS_INDEX) {
                focusedSegmentSelection
            } else {
                TextRange(newItemText.length)
            }
            val s = minOf(sel.start, sel.end).coerceIn(0, newItemText.length)
            val e = maxOf(sel.start, sel.end).coerceIn(0, newItemText.length)
            val existing = RichTextCodec.linkAt(newItemLinkSpans, newItemText.length, s, e)
            val spanStart = existing?.start ?: s
            val spanEnd = existing?.end ?: e
            linkDialogState = EditorLinkDialogState(
                initialText = if (spanStart < spanEnd) newItemText.substring(spanStart, spanEnd) else "",
                initialUrl = existing?.url.orEmpty(),
                isEditing = existing != null,
                targetSegmentIndex = NEW_LINE_FOCUS_INDEX,
                selStart = spanStart,
                selEnd = spanEnd
            )
            return
        }

        val lineText = segments.getOrNull(targetIdx)?.lineText().orEmpty()
        val lineLinks = segmentLinkSpans.getOrNull(targetIdx).orEmpty()
        val s = minOf(focusedSegmentSelection.start, focusedSegmentSelection.end).coerceIn(0, lineText.length)
        val e = maxOf(focusedSegmentSelection.start, focusedSegmentSelection.end).coerceIn(0, lineText.length)
        val existing = RichTextCodec.linkAt(lineLinks, lineText.length, s, e)
        val spanStart = existing?.start ?: s
        val spanEnd = existing?.end ?: e
        linkDialogState = EditorLinkDialogState(
            initialText = if (spanStart < spanEnd) lineText.substring(spanStart, spanEnd) else "",
            initialUrl = existing?.url.orEmpty(),
            isEditing = existing != null,
            targetSegmentIndex = targetIdx,
            selStart = spanStart,
            selEnd = spanEnd
        )
    }

    fun applyLinkFromDialog(state: EditorLinkDialogState, displayText: String, safeUrl: String) {
        val segIdx = state.targetSegmentIndex
        if (segIdx == null) {
            val (updated, newCursor) = RichTextCodec.applyLink(
                doc = richDoc,
                selStart = state.selStart,
                selEnd = state.selEnd,
                rawUrl = safeUrl,
                displayText = displayText
            )
            richDoc = updated
            bodyTextFieldState.edit {
                if (toString() != updated.text) {
                    replace(0, length, updated.text)
                }
                selection = TextRange(newCursor.coerceIn(0, updated.text.length))
            }
            lastPlainSelection = bodyTextFieldState.selection
            return
        }

        if (segIdx == NEW_LINE_FOCUS_INDEX) {
            val itemDoc = RichTextDocument(
                text = newItemText,
                inlineSpans = newItemSpans,
                linkSpans = newItemLinkSpans
            )
            val (updated, newCursor) = RichTextCodec.applyLink(
                doc = itemDoc,
                selStart = state.selStart,
                selEnd = state.selEnd,
                rawUrl = safeUrl,
                displayText = displayText
            )
            newItemText = updated.text
            newItemSpans = updated.inlineSpans
            newItemLinkSpans = updated.linkSpans
            focusedSegmentIndex = NEW_LINE_FOCUS_INDEX
            focusedSegmentSelection = TextRange(newCursor.coerceIn(0, updated.text.length))
            return
        }

        if (segIdx in segments.indices) {
            val segment = segments[segIdx]
            val itemDoc = RichTextDocument(
                text = segment.lineText(),
                inlineSpans = segmentInlineSpans.getOrNull(segIdx).orEmpty(),
                linkSpans = segmentLinkSpans.getOrNull(segIdx).orEmpty()
            )
            val (updated, newCursor) = RichTextCodec.applyLink(
                doc = itemDoc,
                selStart = state.selStart,
                selEnd = state.selEnd,
                rawUrl = safeUrl,
                displayText = displayText
            )
            val rewritten = when (segment) {
                is NoteSegment.Task -> segment.copy(item = segment.item.copy(text = updated.text))
                is NoteSegment.Bullet -> segment.copy(text = updated.text)
                is NoteSegment.Numbered -> segment.copy(text = updated.text)
                is NoteSegment.Text -> segment.copy(text = updated.text)
            }
            val nextSegments = segments.toMutableList().apply { this[segIdx] = rewritten }
            val nextSpans = segmentInlineSpans.toMutableList().apply {
                while (size <= segIdx) add(emptyList())
                this[segIdx] = updated.inlineSpans
            }
            val nextLinks = segmentLinkSpans.toMutableList().apply {
                while (size <= segIdx) add(emptyList())
                this[segIdx] = updated.linkSpans
            }
            updateSegmentsWithSpans(nextSegments, nextSpans, nextLinks)
            focusedSegmentIndex = segIdx
            focusedSegmentSelection = TextRange(newCursor.coerceIn(0, updated.text.length))
        }
    }

    fun removeLinkAtTarget(targetSegmentIndex: Int?, selStart: Int, selEnd: Int) {
        if (targetSegmentIndex == null) {
            richDoc = RichTextCodec.removeLink(richDoc, selStart, selEnd)
            return
        }
        if (targetSegmentIndex == NEW_LINE_FOCUS_INDEX) {
            newItemLinkSpans = RichTextCodec.removeLink(
                RichTextDocument(text = newItemText, linkSpans = newItemLinkSpans),
                selStart,
                selEnd
            ).linkSpans
            return
        }
        if (targetSegmentIndex in segments.indices) {
            val lineText = segments[targetSegmentIndex].lineText()
            val existingLinks = segmentLinkSpans.getOrNull(targetSegmentIndex).orEmpty()
            val updatedLinks = RichTextCodec.removeLink(
                RichTextDocument(text = lineText, linkSpans = existingLinks),
                selStart,
                selEnd
            ).linkSpans
            val nextLinks = segmentLinkSpans.toMutableList().apply {
                while (size <= targetSegmentIndex) add(emptyList())
                this[targetSegmentIndex] = updatedLinks
            }
            updateSegmentsWithSpans(segments, segmentInlineSpans, nextLinks)
        }
    }

    fun removeCurrentActiveLink() {
        if (!editorFormat.isLinesMode) {
            val sel = bodyTextFieldState.selection
            removeLinkAtTarget(null, sel.start, sel.end)
        } else {
            removeLinkAtTarget(
                focusedSegmentIndex,
                focusedSegmentSelection.start,
                focusedSegmentSelection.end
            )
        }
    }

    fun toggleInlineFormatting(kind: RichInlineKind) {
        if (!editorFormat.isLinesMode) {
            val sel = bodyTextFieldState.selection
            if (sel.collapsed) {
                activeTypingKinds = if (kind in activeTypingKinds) {
                    activeTypingKinds - kind
                } else {
                    activeTypingKinds + kind
                }
            } else {
                val updated = RichTextCodec.toggleInlineSpan(richDoc, sel.start, sel.end, kind)
                richDoc = updated
                activeTypingKinds = RichTextCodec.inlineKindsCoveringSelection(updated, sel.start, sel.end)
            }
            return
        }

        val targetIdx = focusedSegmentIndex
        val sel = focusedSegmentSelection
        if (targetIdx == NEW_LINE_FOCUS_INDEX) {
            if (sel.collapsed) {
                activeTypingKinds = if (kind in activeTypingKinds) {
                    activeTypingKinds - kind
                } else {
                    activeTypingKinds + kind
                }
            } else {
                val updatedSpans = RichTextCodec.toggleInlineSpan(
                    RichTextDocument(newItemText, newItemSpans),
                    sel.start,
                    sel.end,
                    kind
                ).inlineSpans
                newItemSpans = updatedSpans
                activeTypingKinds = RichTextCodec.inlineKindsCoveringSelection(
                    RichTextDocument(newItemText, updatedSpans),
                    sel.start,
                    sel.end
                )
            }
            return
        }

        if (targetIdx != null && targetIdx in segments.indices) {
            if (sel.collapsed) {
                activeTypingKinds = if (kind in activeTypingKinds) {
                    activeTypingKinds - kind
                } else {
                    activeTypingKinds + kind
                }
            } else {
                val lineText = segments[targetIdx].lineText()
                val existing = segmentInlineSpans.getOrNull(targetIdx).orEmpty()
                val updatedLineSpans = RichTextCodec.toggleInlineSpan(
                    RichTextDocument(lineText, existing),
                    sel.start,
                    sel.end,
                    kind
                ).inlineSpans
                val nextList = segmentInlineSpans.toMutableList().apply {
                    while (size <= targetIdx) add(emptyList())
                    this[targetIdx] = updatedLineSpans
                }
                updateSegmentsWithSpans(segments, nextList)
                activeTypingKinds = RichTextCodec.inlineKindsCoveringSelection(
                    RichTextDocument(lineText, updatedLineSpans),
                    sel.start,
                    sel.end
                )
            }
            return
        }

        activeTypingKinds = if (kind in activeTypingKinds) {
            activeTypingKinds - kind
        } else {
            activeTypingKinds + kind
        }
    }

    fun clearEditorFormatting() {
        activeTypingKinds = emptySet()
        if (!editorFormat.isLinesMode) {
            val sel = bodyTextFieldState.selection
            richDoc = RichTextCodec.clearFormatting(richDoc, sel.start, sel.end)
            return
        }
        val targetIdx = focusedSegmentIndex
        val sel = focusedSegmentSelection
        if (targetIdx == NEW_LINE_FOCUS_INDEX) {
            val clearedDoc = RichTextCodec.clearFormatting(
                RichTextDocument(
                    text = newItemText,
                    inlineSpans = newItemSpans,
                    linkSpans = newItemLinkSpans
                ),
                sel.start,
                sel.end
            )
            newItemSpans = clearedDoc.inlineSpans
            newItemLinkSpans = clearedDoc.linkSpans
        } else if (targetIdx != null && targetIdx in segments.indices) {
            val lineText = segments[targetIdx].lineText()
            val existing = segmentInlineSpans.getOrNull(targetIdx).orEmpty()
            val existingLinks = segmentLinkSpans.getOrNull(targetIdx).orEmpty()
            val clearedDoc = RichTextCodec.clearFormatting(
                RichTextDocument(
                    text = lineText,
                    inlineSpans = existing,
                    linkSpans = existingLinks
                ),
                sel.start,
                sel.end
            )
            val nextList = segmentInlineSpans.toMutableList().apply {
                while (size <= targetIdx) add(emptyList())
                this[targetIdx] = clearedDoc.inlineSpans
            }
            val nextLinks = segmentLinkSpans.toMutableList().apply {
                while (size <= targetIdx) add(emptyList())
                this[targetIdx] = clearedDoc.linkSpans
            }
            updateSegmentsWithSpans(segments, nextList, nextLinks)
        }
    }

    fun selectEditorBlockStyle(style: BlockStyle) {
        if (!editorFormat.isLinesMode) {
            richDoc = RichTextCodec.applyBlockKind(
                richDoc,
                bodyTextFieldState.selection.start,
                style.toDomainKind()
            )
            return
        }
        if (style == BlockStyle.Normal) return
        val targetLine = when {
            focusedSegmentIndex == NEW_LINE_FOCUS_INDEX && newItemText.trim().isNotEmpty() -> segments.size
            focusedSegmentIndex != null && focusedSegmentIndex in segments.indices -> focusedSegmentIndex!!
            else -> 0
        }
        selectFormat(EditorFormat.PlainText)
        val lines = richDoc.text.split("\n")
        var lineOffset = 0
        for (i in 0 until minOf(targetLine, lines.size)) {
            lineOffset += lines[i].length + 1
        }
        val lineEnd = (lineOffset + (lines.getOrNull(targetLine)?.length ?: 0)).coerceIn(0, richDoc.text.length)
        richDoc = RichTextCodec.applyBlockKind(
            richDoc,
            lineOffset.coerceIn(0, richDoc.text.length),
            style.toDomainKind()
        )
        bodyTextFieldState.edit {
            selection = TextRange(lineEnd)
        }
        lastPlainSelection = bodyTextFieldState.selection
    }

    fun jumpToMatch(index: Int) {
        val count = if (editorFormat.isLinesMode) lineMatches.size else matchRanges.size
        if (index !in 0 until count) return
        activeMatchIndex = index
        activeMatchJumpToken++
        if (!editorFormat.isLinesMode) {
            val range = matchRanges[index]
            bodyTextFieldState.edit {
                selection = TextRange(
                    range.first.coerceIn(0, length),
                    (range.last + 1).coerceIn(0, length)
                )
            }
        }
    }

    val copiedMessage = stringResource(R.string.editor_note_copied)
    fun makeCopy() {
        val combined = getCombinedContent(effectiveContent())
        coroutineScope.launch {
            onSaveNote(0L, title, combined, colorIndex, tags, false, false, null)
            snackbarHostState.showSnackbar(copiedMessage)
        }
    }

    val editorDeleteAlpha by animateFloatAsState(
        targetValue = if (isDeleting) 0f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "editor_delete_alpha"
    )
    val editorDeleteScale by animateFloatAsState(
        targetValue = if (isDeleting) 0.95f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "editor_delete_scale"
    )

    Scaffold(
        containerColor = editorBackground,
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .graphicsLayer {
                alpha = editorDeleteAlpha
                scaleX = editorDeleteScale
                scaleY = editorDeleteScale
            }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EditorTopBar(
                isPinned = isPinned,
                onTogglePin = { isPinned = !isPinned },
                onArchive = {
                    inArchive = true
                    forceSave()
                    val existingId = currentNoteId
                    val hasContent = title.isNotBlank() || effectiveContent().isNotBlank() ||
                        drawingPart.isNotBlank() || tags.isNotEmpty() || reminderTime != null
                    if (existingId > 0L && !hasContent) {
                        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            withContext(NonCancellable) { onMoveToArchive(existingId) }
                        }
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
                },
                textPrimary = textPrimary,
                accentColor = accentColor,
                accentContainer = accentContainer
            )

            AnimatedVisibility(visible = showFindBar) {
                EditorFindBar(
                    query = findQuery,
                    matchCount = matchCount,
                    activeMatchIndex = activeMatchIndex,
                    hasCustomNoteColor = hasCustomColor,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    accentColor = accentColor,
                    onQueryChange = {
                        findQuery = it
                        activeMatchIndex = 0
                        activeMatchJumpToken++
                    },
                    onPreviousMatch = {
                        if (matchCount > 0) {
                            val prev = if (activeMatchIndex <= 0) {
                                matchCount - 1
                            } else {
                                activeMatchIndex - 1
                            }
                            jumpToMatch(prev)
                        }
                    },
                    onNextMatch = {
                        if (matchCount > 0) {
                            val next = (activeMatchIndex + 1) % matchCount
                            jumpToMatch(next)
                        }
                    },
                    onClose = {
                        showFindBar = false
                        findQuery = ""
                        activeMatchIndex = 0
                    }
                )
            }

            // MAIN CONTENT BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        with(density) {
                            bodyViewportWidthDp = size.width.toDp().value
                            val measuredHeight = size.height.toDp().value
                            if (imeBottomPx == 0 || bodyViewportHeightDp == null) {
                                bodyViewportHeightDp = measuredHeight
                            } else {
                                bodyViewportHeightDp = maxOf(bodyViewportHeightDp ?: measuredHeight, measuredHeight)
                            }
                        }
                    }
                    .verticalScroll(bodyScrollState)
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
                            accentContainer.copy(alpha = 0.75f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isPast) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                accentColor.copy(alpha = 0.5f)
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
                                    accentColor
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
                                    onAccentContainer
                                }
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.editor_remove_reminder),
                                tint = if (isPast) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    onAccentContainer.copy(alpha = 0.8f)
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
                StatefulBasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    ),
                    cursorBrush = SolidColor(accentColor),
                    decorator = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_title_hint),
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
                        segmentInlineSpans = segmentInlineSpans,
                        segmentLinkSpans = segmentLinkSpans,
                        newItemText = newItemText,
                        newItemSpans = newItemSpans,
                        newItemLinkSpans = newItemLinkSpans,
                        newItemFormat = editorFormat,
                        findQuery = if (showFindBar) findQuery else "",
                        findHighlightColor = findHighlightColor,
                        findActiveColor = findActiveColor,
                        activeMatchSegmentIndex = activeLineMatch?.segmentIndex,
                        activeMatchRange = activeLineMatch?.range,
                        activeMatchJumpToken = activeMatchJumpToken,
                        newLineFocusIndex = NEW_LINE_FOCUS_INDEX,
                        onNewItemTextChange = { updatedText, cursorOffset ->
                            val oldText = newItemText
                            if (oldText != updatedText) {
                                newItemSpans = RichTextCodec.adjustLocalSpansForEdit(
                                    oldSpans = newItemSpans,
                                    oldText = oldText,
                                    newText = updatedText,
                                    cursorAfterEdit = cursorOffset,
                                    activeTypingKinds = activeTypingKinds
                                )
                                newItemLinkSpans = RichTextCodec.adjustLocalLinkSpansForEdit(
                                    oldLinks = newItemLinkSpans,
                                    oldText = oldText,
                                    newText = updatedText,
                                    cursorAfterEdit = cursorOffset
                                )
                                newItemText = updatedText
                            }
                        },
                        onNewItemSelectionChange = { sel ->
                            if (focusedSegmentIndex == NEW_LINE_FOCUS_INDEX) {
                                focusedSegmentSelection = sel
                                activeTypingKinds = if (sel.collapsed) {
                                    RichTextCodec.inlineKindsAtCursor(
                                        RichTextDocument(newItemText, newItemSpans),
                                        sel.start
                                    )
                                } else {
                                    RichTextCodec.inlineKindsCoveringSelection(
                                        RichTextDocument(newItemText, newItemSpans),
                                        sel.start,
                                        sel.end
                                    )
                                }
                            }
                        },
                        onNewItemFocusChange = { sel ->
                            focusedSegmentIndex = NEW_LINE_FOCUS_INDEX
                            focusedSegmentSelection = sel
                            activeTypingKinds = if (sel.collapsed) {
                                RichTextCodec.inlineKindsAtCursor(
                                    RichTextDocument(newItemText, newItemSpans),
                                    sel.start
                                )
                            } else {
                                RichTextCodec.inlineKindsCoveringSelection(
                                    RichTextDocument(newItemText, newItemSpans),
                                    sel.start,
                                    sel.end
                                )
                            }
                        },
                        onAddItem = { addItem() },
                        onToggleTask = { index -> toggleTask(index) },
                        onSegmentTextChange = { index, text, cursorOffset ->
                            setSegmentText(index, text, cursorOffset)
                        },
                        onSegmentSelectionChange = { index, sel ->
                            if (focusedSegmentIndex == index) {
                                focusedSegmentSelection = sel
                                val lineText = segments.getOrNull(index)?.lineText().orEmpty()
                                val lineSpans = segmentInlineSpans.getOrNull(index).orEmpty()
                                val lineDoc = RichTextDocument(lineText, lineSpans)
                                activeTypingKinds = if (sel.collapsed) {
                                    RichTextCodec.inlineKindsAtCursor(lineDoc, sel.start)
                                } else {
                                    RichTextCodec.inlineKindsCoveringSelection(lineDoc, sel.start, sel.end)
                                }
                            }
                        },
                        onSegmentFocusChange = { index, sel ->
                            focusedSegmentIndex = index
                            focusedSegmentSelection = sel
                            val lineText = segments.getOrNull(index)?.lineText().orEmpty()
                            val lineSpans = segmentInlineSpans.getOrNull(index).orEmpty()
                            val lineDoc = RichTextDocument(lineText, lineSpans)
                            activeTypingKinds = if (sel.collapsed) {
                                RichTextCodec.inlineKindsAtCursor(lineDoc, sel.start)
                            } else {
                                RichTextCodec.inlineKindsCoveringSelection(lineDoc, sel.start, sel.end)
                            }
                        },
                        onSplitSegment = { index -> splitSegment(index) },
                        onDeleteSegment = { index -> deleteSegment(index) },
                        textPrimary = textPrimary,
                        textPlaceholder = textPlaceholder,
                        textMuted = textMuted,
                        accentColor = accentColor,
                        onAccentColor = onAccentColor
                    )
                } else {
                    StatefulBasicTextField(
                        value = richDoc.text,
                        onValueChange = {},
                        externalState = bodyTextFieldState,
                        outputTransformation = bodyOutputTransformation,
                        lineLimits = TextFieldLineLimits.MultiLine(),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary,
                            lineHeight = 26.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        decorator = { innerTextField ->
                            Box(modifier = Modifier.heightIn(min = 200.dp)) {
                                if (richDoc.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.editor_note_label),
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

            val cleanTags = remember(tags) {
                tags.map { it.replace("#", "").trim() }.filter { it.isNotBlank() }
            }
            var showAllEditorLabels by remember(currentNoteId) { mutableStateOf(false) }
            val hasExtraLabels = cleanTags.size > MAX_VISIBLE_EDITOR_LABELS
            val visibleEditorTags = if (showAllEditorLabels || !hasExtraLabels) {
                cleanTags
            } else {
                cleanTags.take(MAX_VISIBLE_EDITOR_LABELS)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                if (cleanTags.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            visibleEditorTags.forEach { cleanTag ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = chipContainer,
                                    border = BorderStroke(1.dp, chipBorder),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { showLabelsSheet = true }
                                        .testTag("editor_tag_$cleanTag")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = cleanTag,
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (hasExtraLabels) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAllEditorLabels = !showAllEditorLabels }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("editor_see_all_labels")
                            ) {
                                Text(
                                    text = stringResource(
                                        if (showAllEditorLabels) {
                                            R.string.labels_show_less
                                        } else {
                                            R.string.labels_see_all
                                        }
                                    ),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (showAllEditorLabels) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                val activeLink = currentActiveLink()
                AnimatedVisibility(visible = activeLink != null) {
                    activeLink?.let { link ->
                        ActiveLinkPill(
                            url = link.url,
                            accentColor = accentColor,
                            textPrimary = textPrimary,
                            hasCustomNoteColor = hasCustomColor,
                            onOpen = { openExternalUrl(link.url) },
                            onEdit = { openLinkDialog() },
                            onRemove = { removeCurrentActiveLink() }
                        )
                    }
                }

                AnimatedVisibility(visible = showFormattingBar) {
                    val activeBlockStyle = if (!editorFormat.isLinesMode) {
                        detectBlockStyle(richDoc, bodyTextFieldState.selection)
                    } else {
                        BlockStyle.Normal
                    }
                    val activeInlineFormats = when {
                        !editorFormat.isLinesMode -> detectInlineFormats(
                            doc = richDoc,
                            selection = bodyTextFieldState.selection,
                            activeTypingKinds = activeTypingKinds
                        )
                        focusedSegmentIndex == NEW_LINE_FOCUS_INDEX -> detectInlineFormats(
                            itemSpans = newItemSpans,
                            itemLinks = newItemLinkSpans,
                            itemTextLength = newItemText.length,
                            selection = focusedSegmentSelection,
                            activeTypingKinds = activeTypingKinds
                        )
                        focusedSegmentIndex != null && focusedSegmentIndex in segments.indices -> {
                            val idx = focusedSegmentIndex!!
                            detectInlineFormats(
                                itemSpans = segmentInlineSpans.getOrNull(idx).orEmpty(),
                                itemLinks = segmentLinkSpans.getOrNull(idx).orEmpty(),
                                itemTextLength = segments[idx].lineText().length,
                                selection = focusedSegmentSelection,
                                activeTypingKinds = activeTypingKinds
                            )
                        }
                        else -> detectInlineFormats(
                            itemSpans = emptyList(),
                            itemLinks = emptyList(),
                            itemTextLength = 0,
                            selection = TextRange.Zero,
                            activeTypingKinds = activeTypingKinds
                        )
                    }
                    EditorFormattingBar(
                        blockStyle = activeBlockStyle,
                        inlineFormats = activeInlineFormats,
                        hasCustomNoteColor = hasCustomColor,
                        textPrimary = textPrimary,
                        accentContainer = accentContainer,
                        onAccentContainer = onAccentContainer,
                        onSelectBlockStyle = { style -> selectEditorBlockStyle(style) },
                        onToggleBold = { toggleInlineFormatting(RichInlineKind.BOLD) },
                        onToggleItalic = { toggleInlineFormatting(RichInlineKind.ITALIC) },
                        onToggleUnderline = { toggleInlineFormatting(RichInlineKind.UNDERLINE) },
                        onToggleStrikethrough = { toggleInlineFormatting(RichInlineKind.STRIKETHROUGH) },
                        onInsertOrEditLink = { openLinkDialog() },
                        onClearFormatting = { clearEditorFormatting() },
                        onClose = { showFormattingBar = false }
                    )
                }

                EditorBottomBar(
                    hasColor = colorIndex > 0,
                    isFormattingVisible = showFormattingBar,
                    hasCustomNoteColor = hasCustomColor,
                    textPrimary = textPrimary,
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    onAccentContainer = onAccentContainer,
                    onOpenAddMenu = { showAddSheet = true },
                    onOpenColorPicker = { showColorPicker = true },
                    onToggleFormatting = { showFormattingBar = !showFormattingBar },
                    onOpenMoreMenu = { showMoreSheet = true }
                )
            }
        }
    }

    if (showAddSheet) {
        EditorAddSheet(
            currentFormat = editorFormat,
            onOpenDrawing = {
                forceSave()
                onOpenDrawing(currentNoteId)
            },
            onSelectFormat = { format -> selectFormat(format) },
            onDismiss = { showAddSheet = false },
            sheetState = addSheetState
        )
    }

    if (showMoreSheet) {
        EditorMoreSheet(
            editedAtMillis = editedAtMillis,
            hasReminder = reminderTime != null,
            onOpenReminder = { showReminderDialog = true },
            onFindInNote = { showFindBar = true },
            onMakeCopy = { makeCopy() },
            onSend = { shareNote() },
            onExportPdf = { exportPdf() },
            onOpenLabels = { showLabelsSheet = true },
            onDelete = {
                if (!isDeleting) {
                    isDeleting = true
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            withContext(NonCancellable) {
                                activeSaveJob?.join()
                                val idToTrash = currentNoteId
                                if (idToTrash > 0L) {
                                    onMoveToTrash(idToTrash)
                                }
                            }
                            runCatching { moreSheetState.hide() }
                            showMoreSheet = false
                            delay(150L)
                        } finally {
                            onNavigateBack()
                        }
                    }
                }
            },
            onDismiss = { showMoreSheet = false },
            sheetState = moreSheetState
        )
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
                launchSerializedSave()
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

    linkDialogState?.let { dialogState ->
        EditorLinkDialog(
            initialText = dialogState.initialText,
            initialUrl = dialogState.initialUrl,
            isEditing = dialogState.isEditing,
            onSave = { displayText, safeUrl ->
                applyLinkFromDialog(dialogState, displayText, safeUrl)
                linkDialogState = null
            },
            onOpen = if (dialogState.isEditing) {
                { safeUrl -> openExternalUrl(safeUrl) }
            } else {
                null
            },
            onRemove = if (dialogState.isEditing) {
                {
                    removeLinkAtTarget(
                        dialogState.targetSegmentIndex,
                        dialogState.selStart,
                        dialogState.selEnd
                    )
                    linkDialogState = null
                }
            } else {
                null
            },
            onDismiss = { linkDialogState = null }
        )
    }
}
