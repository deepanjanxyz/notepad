package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.BULLET_PREFIX
import com.deepanjanxyz.notepad.domain.model.DrawingData
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.feature_drawing.util.drawDrawingStroke
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler

// Thickest a thumbnail stroke is allowed to get when it is scaled down.
private const val MIN_STROKE_WIDTH = 1.5f

// Length of the "[x] " / "[ ] " marker at the start of a checklist line.
private const val CHECKLIST_MARKER_LENGTH = 4

// Length of the "- " marker at the start of a bullet line.
private const val BULLET_MARKER_LENGTH = 2

/**
 * The list markers written as the glyphs the card shows, when the note has any.
 *
 * A checklist marker becomes its box and a bullet becomes a dot; a numbered line
 * already reads as "1. " in the raw text, so it is left alone.
 */
private fun formatStructuredLines(content: String): String {
    val hasChecklist =
        content.contains("[ ] ") || content.contains("[x] ", ignoreCase = true)
    if (!hasChecklist && !content.contains(BULLET_PREFIX)) return content
    return content.lines().map { line ->
        when {
            line.startsWith("[x] ", ignoreCase = true) ->
                "\u2611 " + line.substring(CHECKLIST_MARKER_LENGTH)

            line.startsWith("[ ] ") ->
                "\u2610 " + line.substring(CHECKLIST_MARKER_LENGTH)

            line.startsWith(BULLET_PREFIX) ->
                "\u2022 " + line.substring(BULLET_MARKER_LENGTH)

            else -> line
        }
    }.joinToString("\n")
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
    val formattedContent = remember(note.content) { formatStructuredLines(note.content) }
    val highlightedContent = buildHighlightedText(text = formattedContent, query = searchQuery)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
        NoteCardBody(
            note = note,
            highlightedTitle = highlightedTitle,
            highlightedContent = highlightedContent,
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
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                isInTrash = isInTrash
            )

            NoteCardSnippet(note = note, highlightedContent = highlightedContent)

            if (note.tags.isNotEmpty()) {
                NoteTagChips(tags = note.tags)
            }

            note.reminderTime?.let { reminderTime ->
                NoteReminderChip(reminderTime = reminderTime)
            }

            Spacer(modifier = Modifier.height(4.dp))

            NoteCardFooterRow(
                note = note,
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
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isInTrash: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = highlightedTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (note.title.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
            color = if (note.title.isNotBlank()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
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
                    contentDescription = "Pinned note",
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
private fun NoteCardSnippet(note: Note, highlightedContent: AnnotatedString) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    text = "Drawing",
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

/** The note's labels, as chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteTagChips(tags: List<String>) {
    Spacer(modifier = Modifier.height(2.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
            color = MaterialTheme.colorScheme.outline
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
                    tooltip = "Restore",
                    icon = Icons.Default.RestoreFromTrash,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onRestore
                )
            }
            if (onDeleteForever != null) {
                TooltipIconButton(
                    tooltip = "Delete forever",
                    icon = Icons.Default.DeleteForever,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteForever
                )
            }
        }
    } else if (isInArchive && onUnarchive != null) {
        TooltipIconButton(
            tooltip = "Unarchive",
            icon = Icons.Default.Unarchive,
            tint = MaterialTheme.colorScheme.primary,
            onClick = onUnarchive
        )
    }
}
