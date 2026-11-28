package com.deepanjanxyz.notepad.ui.feature_editor.model

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.deepanjanxyz.notepad.domain.model.RichBlockKind
import com.deepanjanxyz.notepad.domain.model.RichBlockSpan
import com.deepanjanxyz.notepad.domain.model.RichInlineKind
import com.deepanjanxyz.notepad.domain.model.RichInlineSpan
import com.deepanjanxyz.notepad.domain.model.RichLinkSpan
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.domain.model.RichTextDocument

enum class BlockStyle {
    H1,
    H2,
    Normal
}

data class InlineFormatState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val hasLink: Boolean = false
)

private val H1_FONT_SIZE: TextUnit = 24.sp
private val H2_FONT_SIZE: TextUnit = 20.sp

internal fun BlockStyle.toDomainKind(): RichBlockKind = when (this) {
    BlockStyle.H1 -> RichBlockKind.H1
    BlockStyle.H2 -> RichBlockKind.H2
    BlockStyle.Normal -> RichBlockKind.NORMAL
}

internal fun RichBlockKind.toBlockStyle(): BlockStyle = when (this) {
    RichBlockKind.H1 -> BlockStyle.H1
    RichBlockKind.H2 -> BlockStyle.H2
    RichBlockKind.NORMAL -> BlockStyle.Normal
}

internal fun detectBlockStyle(doc: RichTextDocument, selection: TextRange): BlockStyle =
    RichTextCodec.blockKindAt(doc, selection.start).toBlockStyle()

internal fun detectInlineFormats(
    doc: RichTextDocument,
    selection: TextRange,
    activeTypingKinds: Set<RichInlineKind>
): InlineFormatState {
    val active = if (selection.collapsed) {
        activeTypingKinds
    } else {
        RichTextCodec.inlineKindsCoveringSelection(doc, selection.start, selection.end)
    }
    val hasLink = RichTextCodec.linkAt(doc, selection.start, selection.end) != null
    return active.toInlineFormatState(hasLink = hasLink)
}

internal fun detectInlineFormats(
    itemSpans: List<RichInlineSpan>,
    itemLinks: List<RichLinkSpan> = emptyList(),
    itemTextLength: Int,
    selection: TextRange,
    activeTypingKinds: Set<RichInlineKind>
): InlineFormatState {
    val hasLink = RichTextCodec.linkAt(
        linkSpans = itemLinks,
        textLength = itemTextLength,
        selStart = selection.start,
        selEnd = selection.end
    ) != null
    if (selection.collapsed) return activeTypingKinds.toInlineFormatState(hasLink = hasLink)
    val s = minOf(selection.start, selection.end).coerceIn(0, itemTextLength)
    val e = maxOf(selection.start, selection.end).coerceIn(0, itemTextLength)
    if (s >= e) return activeTypingKinds.toInlineFormatState(hasLink = hasLink)

    val covering = RichInlineKind.entries.filterTo(mutableSetOf()) { kind ->
        itemSpans.any { it.kind == kind && it.start <= s && it.end >= e }
    }
    return covering.toInlineFormatState(hasLink = hasLink)
}

private fun Set<RichInlineKind>.toInlineFormatState(hasLink: Boolean = false): InlineFormatState = InlineFormatState(
    isBold = RichInlineKind.BOLD in this,
    isItalic = RichInlineKind.ITALIC in this,
    isUnderline = RichInlineKind.UNDERLINE in this,
    isStrikethrough = RichInlineKind.STRIKETHROUGH in this,
    hasLink = hasLink
)

internal fun buildBodyOutputTransformation(
    doc: RichTextDocument,
    linkColor: Color,
    showFindBar: Boolean,
    findQuery: String,
    activeMatchIndex: Int,
    findHighlightColor: Color,
    findActiveColor: Color
): OutputTransformation? {
    val hasRichStyles = doc.inlineSpans.isNotEmpty() || doc.blockSpans.isNotEmpty() || doc.linkSpans.isNotEmpty()
    val hasFind = showFindBar && findQuery.isNotBlank()
    if (!hasRichStyles && !hasFind) return null

    return OutputTransformation {
        val displayed = asCharSequence().toString()
        applyRichStylesToBuffer(
            displayedText = displayed,
            inlineSpans = doc.inlineSpans,
            blockSpans = doc.blockSpans,
            linkSpans = doc.linkSpans,
            linkColor = linkColor
        )
        if (hasFind) {
            val ranges = findMatchRanges(displayed, findQuery)
            ranges.forEachIndexed { idx, range ->
                val bg = if (idx == activeMatchIndex) findActiveColor else findHighlightColor
                addStyle(SpanStyle(background = bg), range.first, range.last + 1)
            }
        }
    }
}

internal fun buildItemOutputTransformation(
    itemSpans: List<RichInlineSpan>,
    itemLinks: List<RichLinkSpan> = emptyList(),
    linkColor: Color = Color.Unspecified,
    forceStrikethrough: Boolean = false,
    findQuery: String = "",
    findHighlightColor: Color = Color.Transparent,
    activeMatchRange: IntRange? = null,
    findActiveColor: Color = Color.Transparent
): OutputTransformation? {
    val hasFind = findQuery.isNotBlank()
    if (itemSpans.isEmpty() && itemLinks.isEmpty() && !hasFind) return null

    return OutputTransformation {
        val displayed = asCharSequence().toString()
        applyInlineSpansToBuffer(
            textLength = displayed.length,
            inlineSpans = itemSpans,
            linkSpans = itemLinks,
            linkColor = linkColor,
            forceStrikethrough = forceStrikethrough
        )
        if (hasFind) {
            val ranges = findMatchRanges(displayed, findQuery)
            ranges.forEach { range ->
                val bg = if (range == activeMatchRange) findActiveColor else findHighlightColor
                addStyle(SpanStyle(background = bg), range.first, range.last + 1)
            }
        }
    }
}

internal fun TextFieldBuffer.applyRichStylesToBuffer(
    displayedText: String,
    inlineSpans: List<RichInlineSpan>,
    blockSpans: List<RichBlockSpan>,
    linkSpans: List<RichLinkSpan> = emptyList(),
    linkColor: Color = Color.Unspecified
) {
    if (displayedText.isEmpty()) return
    applyBlockSpansToBuffer(displayedText, blockSpans)
    applyInlineSpansToBuffer(
        textLength = displayedText.length,
        inlineSpans = inlineSpans,
        linkSpans = linkSpans,
        linkColor = linkColor,
        forceStrikethrough = false
    )
}

private fun TextFieldBuffer.applyBlockSpansToBuffer(
    text: String,
    blockSpans: List<RichBlockSpan>
) {
    if (blockSpans.isEmpty()) return
    val lineRanges = computeLineRanges(text)
    blockSpans.forEach { block ->
        val range = lineRanges.getOrNull(block.lineIndex) ?: return@forEach
        if (range.first < range.last) {
            val style = when (block.kind) {
                RichBlockKind.H1 -> SpanStyle(fontSize = H1_FONT_SIZE, fontWeight = FontWeight.Bold)
                RichBlockKind.H2 -> SpanStyle(fontSize = H2_FONT_SIZE, fontWeight = FontWeight.SemiBold)
                RichBlockKind.NORMAL -> null
            }
            if (style != null) {
                addStyle(style, range.first, range.last)
            }
        }
    }
}

private fun TextFieldBuffer.applyInlineSpansToBuffer(
    textLength: Int,
    inlineSpans: List<RichInlineSpan>,
    linkSpans: List<RichLinkSpan>,
    linkColor: Color,
    forceStrikethrough: Boolean
) {
    if (textLength <= 0 || (inlineSpans.isEmpty() && linkSpans.isEmpty())) return
    inlineSpans.forEach { span ->
        val start = span.start.coerceIn(0, textLength)
        val end = span.end.coerceIn(0, textLength)
        if (start >= end) return@forEach
        when (span.kind) {
            RichInlineKind.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            RichInlineKind.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            RichInlineKind.UNDERLINE,
            RichInlineKind.STRIKETHROUGH -> Unit
        }
    }

    if (linkColor != Color.Unspecified) {
        linkSpans.forEach { link ->
            val start = link.start.coerceIn(0, textLength)
            val end = link.end.coerceIn(0, textLength)
            if (start < end) {
                addStyle(SpanStyle(color = linkColor), start, end)
            }
        }
    }

    applyDecorationsToBuffer(textLength, inlineSpans, linkSpans, forceStrikethrough)
}

private fun TextFieldBuffer.applyDecorationsToBuffer(
    textLength: Int,
    inlineSpans: List<RichInlineSpan>,
    linkSpans: List<RichLinkSpan>,
    forceStrikethrough: Boolean
) {
    val underlineSpans = inlineSpans.filter { it.kind == RichInlineKind.UNDERLINE }
    val strikeSpans = inlineSpans.filter { it.kind == RichInlineKind.STRIKETHROUGH }
    if (underlineSpans.isEmpty() && strikeSpans.isEmpty() && linkSpans.isEmpty()) return

    val boundaries = sortedSetOf(0, textLength)
    underlineSpans.forEach {
        boundaries += it.start.coerceIn(0, textLength)
        boundaries += it.end.coerceIn(0, textLength)
    }
    strikeSpans.forEach {
        boundaries += it.start.coerceIn(0, textLength)
        boundaries += it.end.coerceIn(0, textLength)
    }
    linkSpans.forEach {
        boundaries += it.start.coerceIn(0, textLength)
        boundaries += it.end.coerceIn(0, textLength)
    }

    val pts = boundaries.toList()
    for (i in 0 until pts.lastIndex) {
        val segStart = pts[i]
        val segEnd = pts[i + 1]
        if (segStart >= segEnd) continue

        val hasUnderline = underlineSpans.any { segStart >= it.start && segEnd <= it.end } ||
            linkSpans.any { segStart >= it.start && segEnd <= it.end }
        val hasStrike = forceStrikethrough || strikeSpans.any { segStart >= it.start && segEnd <= it.end }
        val decoration = when {
            hasUnderline && hasStrike -> TextDecoration.combine(
                listOf(TextDecoration.Underline, TextDecoration.LineThrough)
            )
            hasUnderline -> TextDecoration.Underline
            hasStrike -> TextDecoration.LineThrough
            else -> null
        }
        if (decoration != null) {
            addStyle(SpanStyle(textDecoration = decoration), segStart, segEnd)
        }
    }
}

internal fun findMatchRanges(text: String, query: String): List<IntRange> {
    if (query.isBlank() || text.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var index = text.indexOf(query, startIndex = 0, ignoreCase = true)
    while (index >= 0) {
        ranges.add(index until (index + query.length))
        index = text.indexOf(query, startIndex = index + query.length, ignoreCase = true)
    }
    return ranges
}

private fun computeLineRanges(text: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var lineStart = 0
    for (i in text.indices) {
        if (text[i] == '\n') {
            ranges += lineStart until i
            lineStart = i + 1
        }
    }
    ranges += lineStart until text.length
    return ranges
}
