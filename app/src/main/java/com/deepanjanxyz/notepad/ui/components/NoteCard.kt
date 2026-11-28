package com.deepanjanxyz.notepad.ui.components

import com.deepanjanxyz.notepad.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.BULLET_PREFIX
import com.deepanjanxyz.notepad.domain.model.DrawingData
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.RichBlockKind
import com.deepanjanxyz.notepad.domain.model.RichInlineKind
import com.deepanjanxyz.notepad.domain.model.RichInlineSpan
import com.deepanjanxyz.notepad.domain.model.RichLinkSpan
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.ui.feature_drawing.util.drawDrawingStroke
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.theme.OnSurfaceDark
import com.deepanjanxyz.notepad.ui.theme.OnSurfaceVariantDark
import com.deepanjanxyz.notepad.ui.theme.OutlineDark
import com.deepanjanxyz.notepad.ui.theme.PrimaryDark
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler

// Thickest a thumbnail stroke is allowed to get when it is scaled down.
private const val MIN_STROKE_WIDTH = 1.5f

// Length of the "[x] " / "[ ] " marker at the start of a checklist line.
private const val CHECKLIST_MARKER_LENGTH = 4

// Length of the "- " marker at the start of a bullet line.
private const val BULLET_MARKER_LENGTH = 2

// Maximum label chips displayed on a note card before collapsing.
private const val MAX_VISIBLE_CARD_LABELS = 3

// Card preview font sizes for H1 / H2 blocks.
private val CARD_H1_SIZE = 18.sp
private val CARD_H2_SIZE = 16.sp

/**
 * Decodes rich-text formatting and converts list markers (`[x] `, `[ ] `, `- `)
 * into card glyphs while preserving inline and block span offsets.
 */
private fun buildStyledCardContent(rawContent: String, linkColor: Color): AnnotatedString {
    val doc = RichTextCodec.decode(rawContent)
    val lines = doc.text.split("\n")
    val displayBuilder = StringBuilder()
    val mappedInline = mutableListOf<RichInlineSpan>()
    val mappedLinks = mutableListOf<RichLinkSpan>()
    val mappedBlocks = mutableListOf<Pair<IntRange, RichBlockKind>>()

    var srcLineStart = 0
    lines.forEachIndexed { lineIdx, line ->
        val srcLineEnd = srcLineStart + line.length
        val (srcPrefixLen, dstPrefix) = when {
            line.startsWith("[x] ", ignoreCase = true) -> CHECKLIST_MARKER_LENGTH to "\u2611 "
            line.startsWith("[ ] ") -> CHECKLIST_MARKER_LENGTH to "\u2610 "
            line.startsWith(BULLET_PREFIX) -> BULLET_MARKER_LENGTH to "\u2022 "
            else -> 0 to ""
        }

        val dstLineStart = displayBuilder.length
        displayBuilder.append(dstPrefix)
        displayBuilder.append(line, srcPrefixLen, line.length)
        val dstLineEnd = displayBuilder.length
        if (lineIdx < lines.lastIndex) {
            displayBuilder.append('\n')
        }

        val srcItemStart = srcLineStart + srcPrefixLen
        val dstItemStart = dstLineStart + dstPrefix.length
        val delta = dstItemStart - srcItemStart
        doc.inlineSpans.forEach { span ->
            val s = maxOf(span.start, srcItemStart)
            val e = minOf(span.end, srcLineEnd)
            if (s < e) {
                mappedInline += RichInlineSpan(s + delta, e + delta, span.kind)
            }
        }
        doc.linkSpans.forEach { span ->
            val s = maxOf(span.start, srcItemStart)
            val e = minOf(span.end, srcLineEnd)
            if (s < e) {
                mappedLinks += RichLinkSpan(s + delta, e + delta, span.url)
            }
        }

        val block = doc.blockSpans.firstOrNull { it.lineIndex == lineIdx }
        if (block != null && dstLineStart < dstLineEnd && block.kind != RichBlockKind.NORMAL) {
            mappedBlocks += (dstLineStart until dstLineEnd) to block.kind
        }

        srcLineStart = srcLineEnd + 1
    }

    val displayText = displayBuilder.toString()
    if (mappedInline.isEmpty() && mappedBlocks.isEmpty() && mappedLinks.isEmpty()) {
        return AnnotatedString(displayText)
    }

    return buildAnnotatedString {
        append(displayText)
        mappedBlocks.forEach { (range, kind) ->
            val style = when (kind) {
                RichBlockKind.H1 -> SpanStyle(fontSize = CARD_H1_SIZE, fontWeight = FontWeight.Bold)
                RichBlockKind.H2 -> SpanStyle(fontSize = CARD_H2_SIZE, fontWeight = FontWeight.SemiBold)
                RichBlockKind.NORMAL -> null
            }
            if (style != null) {
                addStyle(style, range.first, range.last + 1)
            }
        }
        mappedInline.forEach { span ->
            when (span.kind) {
                RichInlineKind.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), span.start, span.end)
                RichInlineKind.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), span.start, span.end)
                RichInlineKind.UNDERLINE,
                RichInlineKind.STRIKETHROUGH -> Unit
            }
        }
        mappedLinks.forEach { link ->
            addStyle(SpanStyle(color = linkColor), link.start, link.end)
        }
        val underlineSpans = mappedInline.filter { it.kind == RichInlineKind.UNDERLINE }
        val strikeSpans = mappedInline.filter { it.kind == RichInlineKind.STRIKETHROUGH }
        if (underlineSpans.isNotEmpty() || strikeSpans.isNotEmpty() || mappedLinks.isNotEmpty()) {
            val pts = sortedSetOf(0, displayText.length).apply {
                underlineSpans.forEach { add(it.start); add(it.end) }
                strikeSpans.forEach { add(it.start); add(it.end) }
                mappedLinks.forEach { add(it.start); add(it.end) }
            }.toList()
            for (i in 0 until pts.lastIndex) {
                val segStart = pts[i]
                val segEnd = pts[i + 1]
                if (segStart >= segEnd) continue
                val hasU = underlineSpans.any { segStart >= it.start && segEnd <= it.end } ||
                    mappedLinks.any { segStart >= it.start && segEnd <= it.end }
                val hasS = strikeSpans.any { segStart >= it.start && segEnd <= it.end }
                val deco = when {
                    hasU && hasS -> TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                    hasU -> TextDecoration.Underline
                    hasS -> TextDecoration.LineThrough
                    else -> null
                }
                if (deco != null) {
                    addStyle(SpanStyle(textDecoration = deco), segStart, segEnd)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    searchQuery: String = "",
    isInTrash: Boolean = false,
    isInArchive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
    onDeleteForever: (() -> Unit)? = null,
    customGestureEnabled: Boolean = false,
    gestureInteractionSource: MutableInteractionSource? = null,
    modifier: Modifier = Modifier
) {
    val hasCustomColor = note.colorIndex in 1 until NoteColorOptions.size
    val cardColor = if (hasCustomColor) {
        NoteColorOptions[note.colorIndex]
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else if (hasCustomColor) {
        BorderStroke(1.dp, NoteColorOptions[note.colorIndex].copy(alpha = 0.7f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val highlightedTitle = buildHighlightedText(
        text = note.title.ifBlank { "Untitled Note" },
        query = searchQuery
    )
    val linkColor = if (hasCustomColor && !isSelected) PrimaryDark else MaterialTheme.colorScheme.primary
    val styledContent = remember(note.content, linkColor) { buildStyledCardContent(note.content, linkColor) }
    val highlightedContent = buildHighlightedText(text = styledContent, query = searchQuery)

    val effectiveInteractionSource = gestureInteractionSource ?: remember { MutableInteractionSource() }
    val interactionModifier = if (customGestureEnabled) {
        Modifier
            .indication(effectiveInteractionSource, LocalIndication.current)
            .focusable(interactionSource = effectiveInteractionSource)
            .onKeyEvent { event ->
                val isActivationKey = event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.Spacebar ||
                    event.key == Key.DirectionCenter
                val hasModifier = event.isAltPressed ||
                    event.isCtrlPressed ||
                    event.isMetaPressed ||
                    event.isShiftPressed
                if (!isActivationKey || hasModifier) return@onKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> event.nativeKeyEvent.repeatCount == 0
                    KeyEventType.KeyUp -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .semantics(mergeDescendants = false) {
                role = Role.Button
                onClick(label = null) {
                    onClick()
                    true
                }
                onLongClick(label = null) {
                    onLongClick()
                    true
                }
            }
    } else {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(interactionModifier)
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                cardColor
            }
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (note.isPinned) 4.dp else 1.dp
        )
    ) {
        val useLightOnCard = hasCustomColor && !isSelected
        NoteCardBody(
            note = note,
            highlightedTitle = highlightedTitle,
            highlightedContent = highlightedContent,
            useLightOnCard = useLightOnCard,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            isInTrash = isInTrash,
            isInArchive = isInArchive,
            onRestore = onRestore,
            onUnarchive = onUnarchive,
            onDeleteForever = onDeleteForever
        )
    }
}

/** The card's contents, in a padded column. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCardBody(
    note: Note,
    highlightedTitle: AnnotatedString,
    highlightedContent: AnnotatedString,
    useLightOnCard: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isInTrash: Boolean,
    isInArchive: Boolean,
    onRestore: (() -> Unit)?,
    onUnarchive: (() -> Unit)?,
    onDeleteForever: (() -> Unit)?
) {
    Box(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NoteCardHeaderRow(
                note = note,
                highlightedTitle = highlightedTitle,
                useLightOnCard = useLightOnCard,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                isInTrash = isInTrash
            )

            NoteCardSnippet(
                note = note,
                highlightedContent = highlightedContent,
                useLightOnCard = useLightOnCard
            )

            if (note.tags.isNotEmpty()) {
                NoteTagChips(
                    noteId = note.id,
                    tags = note.tags,
                    useLightOnCard = useLightOnCard
                )
            }

            note.reminderTime?.let { reminderTime ->
                NoteReminderChip(reminderTime = reminderTime)
            }

            Spacer(modifier = Modifier.height(4.dp))

            NoteCardFooterRow(
                note = note,
                useLightOnCard = useLightOnCard,
                isSelectionMode = isSelectionMode,
                isInTrash = isInTrash,
                isInArchive = isInArchive,
                onRestore = onRestore,
                onUnarchive = onUnarchive,
                onDeleteForever = onDeleteForever
            )
        }
    }
}

/** The title, and either the selection ring or the pin badge. */
@Composable
private fun NoteCardHeaderRow(
    note: Note,
    highlightedTitle: AnnotatedString,
    useLightOnCard: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isInTrash: Boolean
) {
    val titleTextColor = if (useLightOnCard) OnSurfaceDark else MaterialTheme.colorScheme.onSurface
    val titlePlaceholderColor = if (useLightOnCard) {
        OnSurfaceVariantDark.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = highlightedTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (note.title.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
            color = if (note.title.isNotBlank()) titleTextColor else titlePlaceholderColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(22.dp)
            )
        } else if (note.isPinned && !isInTrash) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = stringResource(R.string.note_card_pinned),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

/** The drawing thumbnail when the note holds a drawing, otherwise the text preview. */
@Composable
private fun NoteCardSnippet(
    note: Note,
    highlightedContent: AnnotatedString,
    useLightOnCard: Boolean
) {
    val isDrawing = remember(note.content) {
        DrawingSerializer.isDrawing(note.content)
    }
    if (isDrawing) {
        val drawingData = remember(note.content) {
            DrawingSerializer.parse(note.content)
        }
        NoteDrawingThumbnail(drawingData = drawingData)
    } else if (note.content.isNotBlank()) {
        Text(
            text = highlightedContent,
            style = MaterialTheme.typography.bodyMedium,
            color = if (useLightOnCard) OnSurfaceVariantDark else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/** A scaled-down preview of the note's drawing, with a badge in the corner. */
@Composable
private fun NoteDrawingThumbnail(drawingData: DrawingData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(drawingData.backgroundColor))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        NoteThumbnailCanvas(drawingData = drawingData)

        // Badge in top right corner
        Surface(
            shape = RoundedCornerShape(bottomStart = 8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.editor_drawing),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

/** Fits the drawing into the thumbnail and draws it. */
@Composable
private fun NoteThumbnailCanvas(drawingData: DrawingData) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (drawingData.strokes.isEmpty()) {
            return@Canvas
        }
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (s in drawingData.strokes) {
            for (pt in s.points) {
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
        }
        val drawingW = (maxX - minX).coerceAtLeast(1f)
        val drawingH = (maxY - minY).coerceAtLeast(1f)
        val padding = 16f
        val scaleX = (size.width - padding * 2) / drawingW
        val scaleY = (size.height - padding * 2) / drawingH
        val scale = minOf(scaleX, scaleY).coerceAtMost(1f)
        val offsetX = (size.width - drawingW * scale) / 2f - minX * scale
        val offsetY = (size.height - drawingH * scale) / 2f - minY * scale

        for (stroke in drawingData.strokes) {
            if (stroke.points.isEmpty()) continue
            val scaledPts = stroke.points.map { pt ->
                DrawingPoint(pt.x * scale + offsetX, pt.y * scale + offsetY)
            }
            drawDrawingStroke(
                stroke.copy(
                    points = scaledPts,
                    strokeWidth = (stroke.strokeWidth * scale).coerceAtLeast(MIN_STROKE_WIDTH)
                )
            )
        }
    }
}

/** The note's labels, as chips, collapsing extra items with a See all toggle below. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteTagChips(noteId: Long, tags: List<String>, useLightOnCard: Boolean) {
    val cleanTags = remember(tags) { tags.filter { it.isNotBlank() } }
    if (cleanTags.isEmpty()) return

    var showAllLabels by remember(noteId) { mutableStateOf(false) }
    val hasExtraLabels = cleanTags.size > MAX_VISIBLE_CARD_LABELS
    val visibleTags = if (showAllLabels || !hasExtraLabels) {
        cleanTags
    } else {
        cleanTags.take(MAX_VISIBLE_CARD_LABELS)
    }

    Spacer(modifier = Modifier.height(2.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            visibleTags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (useLightOnCard) {
                        Color.White.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        1.dp,
                        if (useLightOnCard) {
                            Color.White.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (useLightOnCard) {
                                OnSurfaceDark
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        if (hasExtraLabels) {
            val toggleColor = if (useLightOnCard) {
                OnSurfaceDark
            } else {
                MaterialTheme.colorScheme.primary
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showAllLabels = !showAllLabels }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("note_card_see_all_labels")
            ) {
                Text(
                    text = stringResource(
                        if (showAllLabels) R.string.labels_show_less else R.string.labels_see_all
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = toggleColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = if (showAllLabels) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null,
                    tint = toggleColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** The chip that shows when a reminder will fire. */
@Composable
private fun NoteReminderChip(reminderTime: Long) {
    val isPast = reminderTime < System.currentTimeMillis()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPast) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        },
        border = BorderStroke(
            1.dp,
            if (isPast) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.testTag("note_card_reminder_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isPast) Icons.Default.Alarm else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (isPast) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = NoteReminderScheduler.formatReminderDateTime(reminderTime),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isPast) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

/** The date, and the actions that apply to this card. */
@Composable
private fun NoteCardFooterRow(
    note: Note,
    useLightOnCard: Boolean,
    isSelectionMode: Boolean,
    isInTrash: Boolean,
    isInArchive: Boolean,
    onRestore: (() -> Unit)?,
    onUnarchive: (() -> Unit)?,
    onDeleteForever: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = note.date,
            style = MaterialTheme.typography.labelSmall,
            color = if (useLightOnCard) OutlineDark else MaterialTheme.colorScheme.outline
        )

        if (!isSelectionMode) {
            NoteCardActions(
                isInTrash = isInTrash,
                isInArchive = isInArchive,
                onRestore = onRestore,
                onUnarchive = onUnarchive,
                onDeleteForever = onDeleteForever
            )
        }
    }
}

/** Restore and delete for a trashed note, or unarchive for an archived one. */
@Composable
private fun NoteCardActions(
    isInTrash: Boolean,
    isInArchive: Boolean,
    onRestore: (() -> Unit)?,
    onUnarchive: (() -> Unit)?,
    onDeleteForever: (() -> Unit)?
) {
    if (isInTrash) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (onRestore != null) {
                TooltipIconButton(
                    tooltip = stringResource(R.string.note_card_restore),
                    icon = Icons.Default.RestoreFromTrash,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onRestore
                )
            }
            if (onDeleteForever != null) {
                TooltipIconButton(
                    tooltip = stringResource(R.string.note_card_delete_forever),
                    icon = Icons.Default.DeleteForever,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteForever
                )
            }
        }
    } else if (isInArchive && onUnarchive != null) {
        TooltipIconButton(
            tooltip = stringResource(R.string.note_card_unarchive),
            icon = Icons.Default.Unarchive,
            tint = MaterialTheme.colorScheme.primary,
            onClick = onUnarchive
        )
    }
}
