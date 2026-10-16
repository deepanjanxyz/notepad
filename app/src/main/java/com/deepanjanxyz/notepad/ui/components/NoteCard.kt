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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.feature_drawing.util.drawDrawingStroke
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import java.util.Locale

/**
 * Vibrant, high-contrast swatches used for search matches. Every keyword in the
 * query is assigned its own colour, so a note that matches several terms stays
 * easy to scan at a glance instead of every hit sharing a single tint.
 */
private val SearchHighlightColors = listOf(
    Color(0xFFFFE082), // Amber
    Color(0xFF9AE6FF), // Sky
    Color(0xFFA5F0B4), // Mint
    Color(0xFFFFB3C7), // Rose
    Color(0xFFD6B6FF), // Lilac
    Color(0xFFFFC79A) // Apricot
)

/**
 * Foreground painted on top of a highlight swatch. Kept dark and constant so the
 * match stays legible on every swatch, in both light and dark themes.
 */
private val SearchHighlightTextColor = Color(0xFF1B1B1F)

@Composable
fun buildHighlightedText(
    text: String,
    query: String,
    colors: List<Color> = SearchHighlightColors
): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    // An empty palette would make the per-keyword colour lookup divide by zero,
    // so it falls back to the default swatches.
    val palette = colors.ifEmpty { SearchHighlightColors }

    return remember(text, query, palette) {
        val keywords = query.trim().lowercase(Locale.getDefault())
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (keywords.isEmpty()) {
            return@remember AnnotatedString(text)
        }

        val lowerText = text.lowercase(Locale.getDefault())
        // Each match keeps the index of the keyword that produced it, so its
        // colour follows the keyword rather than its position in the note.
        val matches = mutableListOf<Triple<Int, Int, Int>>()
        keywords.forEachIndexed { keywordIndex, keyword ->
            var startIndex = 0
            while (startIndex < text.length) {
                val matchIndex = lowerText.indexOf(keyword, startIndex)
                if (matchIndex == -1) break
                matches.add(Triple(matchIndex, matchIndex + keyword.length, keywordIndex))
                startIndex = matchIndex + 1
            }
        }

        if (matches.isEmpty()) {
            return@remember AnnotatedString(text)
        }

        matches.sortBy { it.first }
        // Merge only overlapping matches (for example a repeated short keyword);
        // the first keyword in the query keeps the colour of the merged span.
        val merged = mutableListOf<Triple<Int, Int, Int>>()
        for (match in matches) {
            val last = merged.lastOrNull()
            if (last != null && match.first < last.second) {
                if (match.second > last.second) {
                    merged[merged.size - 1] = Triple(last.first, match.second, last.third)
                }
            } else {
                merged.add(match)
            }
        }

        buildAnnotatedString {
            var cursor = 0
            for (match in merged) {
                if (match.first > cursor) {
                    append(text.substring(cursor, match.first))
                }
                val swatch = palette[match.third % palette.size]
                withStyle(
                    SpanStyle(
                        background = swatch,
                        color = SearchHighlightTextColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text.substring(match.first, match.second))
                }
                cursor = match.second
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
}

/**
 * A single note shown in a list or grid: title, content preview (or a scaled
 * thumbnail when the note is a drawing), tags and metadata, with selection and
 * swipe/long-press actions wired in for the trash, archive and active lists.
 */
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
    onTogglePin: () -> Unit = {},
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

    val formattedContent = remember(note.content) {
        if (note.content.contains("[ ] ") || note.content.contains("[x] ", ignoreCase = true)) {
            note.content.lines().map { line ->
                when {
                    line.startsWith("[x] ", ignoreCase = true) -> "\u2611 " + line.substring(4)
                    line.startsWith("[ ] ") -> "\u2610 " + line.substring(4)
                    else -> line
                }
            }.joinToString("\n")
        } else {
            note.content
        }
    }

    val highlightedContent = buildHighlightedText(
        text = formattedContent,
        query = searchQuery
    )

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
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else cardColor
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (note.isPinned) 4.dp else 1.dp
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header row: Title + Pin/Selection indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = highlightedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (note.title.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                        color = if (note.title.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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

                // Content snippet or Drawing Thumbnail
                val isDrawing = remember(note.content) {
                    DrawingSerializer.isDrawing(note.content)
                }
                if (isDrawing) {
                    val drawingData = remember(note.content) {
                        DrawingSerializer.parse(note.content)
                    }
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
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (drawingData.strokes.isNotEmpty()) {
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
                                            strokeWidth = (stroke.strokeWidth * scale).coerceAtLeast(1.5f)
                                        )
                                    )
                                }
                            }
                        }

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

                // Vibrant, high-contrast tag chips
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        note.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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

                // Reminder Chip (if set)
                val reminderTime = note.reminderTime
                if (reminderTime != null) {
                    val isPast = reminderTime < System.currentTimeMillis()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPast) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, if (isPast) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
                                tint = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = NoteReminderScheduler.formatReminderDateTime(reminderTime),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom row: Date + Actions
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
                        if (isInTrash) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (onRestore != null) {
                                    ActionTooltip("Restore") {
                                        IconButton(
                                            onClick = onRestore,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestoreFromTrash,
                                                contentDescription = "Restore",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (onDeleteForever != null) {
                                    ActionTooltip("Delete forever") {
                                        IconButton(
                                            onClick = onDeleteForever,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Delete forever",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (isInArchive && onUnarchive != null) {
                            ActionTooltip("Unarchive") {
                                IconButton(
                                    onClick = onUnarchive,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Unarchive,
                                        contentDescription = "Unarchive",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
