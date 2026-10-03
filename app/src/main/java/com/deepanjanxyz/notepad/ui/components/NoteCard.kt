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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClickLabel
import androidx.compose.ui.semantics.onLongClickLabel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.domain.model.DrawingPoint
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.ui.feature_drawing.util.drawDrawingStroke
import com.deepanjanxyz.notepad.ui.theme.NoteColorOptions
import com.deepanjanxyz.notepad.ui.theme.NoteTintChip
import com.deepanjanxyz.notepad.ui.theme.NoteTintContent
import com.deepanjanxyz.notepad.ui.theme.NoteTintContentMuted
import com.deepanjanxyz.notepad.ui.theme.NoteTintOutline
import com.deepanjanxyz.notepad.ui.theme.Spacing
import com.deepanjanxyz.notepad.ui.util.NoteUiFormat
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import java.util.Locale

/**
 * Highlights every keyword occurrence in [text].
 *
 * Highlight colours default to the active theme; tinted note cards override them
 * with the high-contrast note-tint tokens.
 */
@Composable
fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    highlightTextColor: Color = MaterialTheme.colorScheme.primary
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)

    return remember(text, query, highlightColor, highlightTextColor) {
        val keywords = query.trim().lowercase(Locale.getDefault())
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (keywords.isEmpty()) return@remember AnnotatedString(text)

        val lowerText = text.lowercase(Locale.getDefault())
        val ranges = mutableListOf<IntRange>()
        for (kw in keywords) {
            var startIndex = 0
            while (startIndex < text.length) {
                val matchIndex = lowerText.indexOf(kw, startIndex)
                if (matchIndex == -1) break
                ranges.add(matchIndex until matchIndex + kw.length)
                startIndex = matchIndex + 1
            }
        }

        if (ranges.isEmpty()) return@remember AnnotatedString(text)

        ranges.sortBy { it.first }
        val mergedRanges = mutableListOf<IntRange>()
        var currentRange = ranges[0]
        for (i in 1 until ranges.size) {
            val r = ranges[i]
            if (r.first <= currentRange.last + 1) {
                currentRange = currentRange.first until maxOf(currentRange.last + 1, r.last + 1)
            } else {
                mergedRanges.add(currentRange)
                currentRange = r
            }
        }
        mergedRanges.add(currentRange)

        buildAnnotatedString {
            var cursor = 0
            for (r in mergedRanges) {
                if (r.first > cursor) append(text.substring(cursor, r.first))
                withStyle(
                    SpanStyle(
                        background = highlightColor,
                        color = highlightTextColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text.substring(r.first, r.last + 1))
                }
                cursor = r.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }
}

/**
 * A single note in the grid/list.
 *
 * Accessibility notes:
 * * the card is a single focus stop with a spoken description and an explicit
 *   selected/not-selected state, so TalkBack no longer reads just the clipped
 *   snippet of body text;
 * * on a tinted card the text switches to the [NoteTintContent] tokens, because
 *   the card background is dark in both themes and inheriting the light-theme
 *   onSurface colour produced unreadable near-black text on a dark tint.
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
    val hasCustomColor = NoteUiFormat.isCustomTint(note.colorIndex)
    val tint = if (hasCustomColor) NoteColorOptions[note.colorIndex] else null
    val cardColor = tint ?: MaterialTheme.colorScheme.surface

    val titleColor = if (hasCustomColor) NoteTintContent else MaterialTheme.colorScheme.onSurface
    val titlePlaceholderColor = if (hasCustomColor) {
        NoteTintContentMuted
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bodyColor = if (hasCustomColor) {
        NoteTintContentMuted
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val metaColor = if (hasCustomColor) NoteTintContentMuted else MaterialTheme.colorScheme.outline

    val borderStroke = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        tint != null -> BorderStroke(1.dp, tint.copy(alpha = 0.7f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val displayTitle = note.title.ifBlank { stringResource(R.string.note_untitled) }
    val isDrawing = remember(note.content) { DrawingSerializer.isDrawing(note.content) }

    val spokenDescription = NoteUiFormat.noteContentDescription(
        title = note.title,
        isPinned = note.isPinned,
        isSelected = isSelected,
        isDrawing = isDrawing,
        isInArchive = isInArchive,
        isInTrash = isInTrash
    )

    val highlightedTitle = buildHighlightedText(
        text = displayTitle,
        query = searchQuery,
        highlightTextColor = if (hasCustomColor) {
            NoteTintContent
        } else {
            MaterialTheme.colorScheme.primary
        }
    )

    val formattedContent = remember(note.content) {
        if (note.content.contains("[ ] ") || note.content.contains("[x] ", ignoreCase = true)) {
            note.content.lines().joinToString("\n") { line ->
                when {
                    line.startsWith("[x] ", ignoreCase = true) -> "\u2611 " + line.substring(4)
                    line.startsWith("[ ] ") -> "\u2610 " + line.substring(4)
                    else -> line
                }
            }
        } else {
            note.content
        }
    }

    val highlightedContent = buildHighlightedText(
        text = formattedContent,
        query = searchQuery,
        highlightTextColor = if (hasCustomColor) {
            NoteTintContent
        } else {
            MaterialTheme.colorScheme.primary
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = stringResource(R.string.a11y_open_note),
                onLongClickLabel = stringResource(R.string.a11y_select_note)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = spokenDescription
                stateDescription = if (isSelected) "selected" else "not selected"
            }
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
        Box(modifier = Modifier.padding(Spacing.lg)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = highlightedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (note.title.isNotBlank()) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (note.title.isNotBlank()) titleColor else titlePlaceholderColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.Circle
                            },
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                metaColor
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (note.isPinned && !isInTrash) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = stringResource(R.string.note_pinned),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(Spacing.xs)
                                    .size(Spacing.iconSmall)
                            )
                        }
                    }
                }

                if (isDrawing) {
                    // Thumbnail of the sketch. The card description already says
                    // "drawing", so the canvas itself stays decorative.
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
                                var maxX = Float.MIN_VALUE
                                var maxY = Float.MIN_VALUE
                                for (stroke in drawingData.strokes) {
                                    for (pt in stroke.points) {
                                        if (pt.x < minX) minX = pt.x
                                        if (pt.y < minY) minY = pt.y
                                        if (pt.x > maxX) maxX = pt.x
                                        if (pt.y > maxY) maxY = pt.y
                                    }
                                }
                                val drawingW = (maxX - minX).coerceAtLeast(1f)
                                val drawingH = (maxY - minY).coerceAtLeast(1f)
                                val padding = 16f
                                val scale = minOf(
                                    (size.width - padding * 2) / drawingW,
                                    (size.height - padding * 2) / drawingH
                                ).coerceAtMost(1f)
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
                                            strokeWidth = (stroke.strokeWidth * scale)
                                                .coerceAtLeast(1.5f)
                                        )
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(bottomStart = 8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.xs + 2.dp,
                                    vertical = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = stringResource(R.string.note_drawing_badge),
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
                        color = bodyColor,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        note.tags.forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                // A light chip colour on a dark tinted card made the
                                // chips the brightest element on screen; the chip is
                                // now a translucent lift of the card itself.
                                color = if (hasCustomColor) {
                                    NoteTintChip
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (hasCustomColor) {
                                        NoteTintOutline
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    }
                                )
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasCustomColor) {
                                        NoteTintContentMuted
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.sm,
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                    }
                }

                val reminderTime = note.reminderTime
                if (reminderTime != null) {
                    val isPast = reminderTime < System.currentTimeMillis()
                    Surface(
                        shape = MaterialTheme.shapes.small,
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
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isPast) {
                                    Icons.Default.Alarm
                                } else {
                                    Icons.Default.NotificationsActive
                                },
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

                Spacer(modifier = Modifier.height(Spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = metaColor
                    )

                    if (!isSelectionMode) {
                        if (isInTrash) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (onRestore != null) {
                                    CardActionButton(
                                        icon = Icons.Default.RestoreFromTrash,
                                        contentDescription = stringResource(R.string.action_restore),
                                        tint = MaterialTheme.colorScheme.primary,
                                        testTag = "restore_note_${note.id}",
                                        onClick = onRestore
                                    )
                                }
                                if (onDeleteForever != null) {
                                    CardActionButton(
                                        icon = Icons.Default.DeleteForever,
                                        contentDescription = stringResource(R.string.action_delete_forever),
                                        tint = MaterialTheme.colorScheme.error,
                                        testTag = "delete_forever_note_${note.id}",
                                        onClick = onDeleteForever
                                    )
                                }
                            }
                        } else if (isInArchive && onUnarchive != null) {
                            CardActionButton(
                                icon = Icons.Default.Unarchive,
                                contentDescription = stringResource(R.string.action_unarchive),
                                tint = MaterialTheme.colorScheme.primary,
                                testTag = "unarchive_note_${note.id}",
                                onClick = onUnarchive
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Icon button used inside a note card.
 *
 * It keeps the default (48 dp) touch container and only shrinks the painted
 * icon. These buttons previously forced a 28 dp box, below the minimum target
 * size, which made restore/delete hard to hit.
 */
@Composable
private fun CardActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(Spacing.iconMedium)
        )
    }
}
