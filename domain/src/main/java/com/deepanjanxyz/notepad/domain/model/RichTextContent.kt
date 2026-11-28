package com.deepanjanxyz.notepad.domain.model

private const val STYLES_SEPARATOR = "\n---RICH_STYLES_V1---\n"
private const val META_KEY_VALUE_PARTS = 2
private const val SPAN_BOUND_PARTS = 2
private const val LINK_CODE = "L"

private val ALLOWED_LINK_SCHEMES = listOf("https://", "http://", "mailto:", "tel:")

enum class RichInlineKind(val code: String) {
    BOLD("B"),
    ITALIC("I"),
    UNDERLINE("U"),
    STRIKETHROUGH("S");

    companion object {
        fun fromCode(code: String): RichInlineKind? =
            entries.firstOrNull { it.code == code }
    }
}

enum class RichBlockKind(val code: String) {
    NORMAL("N"),
    H1("H1"),
    H2("H2");

    companion object {
        fun fromCode(code: String): RichBlockKind? =
            entries.firstOrNull { it.code == code }
    }
}

data class RichInlineSpan(
    val start: Int,
    val end: Int,
    val kind: RichInlineKind
)

data class RichBlockSpan(
    val lineIndex: Int,
    val kind: RichBlockKind
)

data class RichLinkSpan(
    val start: Int,
    val end: Int,
    val url: String
)

data class RichTextDocument(
    val text: String = "",
    val inlineSpans: List<RichInlineSpan> = emptyList(),
    val blockSpans: List<RichBlockSpan> = emptyList(),
    val linkSpans: List<RichLinkSpan> = emptyList()
)

data class TextEditDiff(
    val editStart: Int,
    val oldEditEnd: Int,
    val newEditEnd: Int
)

/**
 * Encodes, decodes, and maintains inline, block, and link formatting spans inside
 * a plain [Note.content] string without changing the database schema.
 */
@Suppress("TooManyFunctions")
object RichTextCodec {

    fun extractPlainText(raw: String): String {
        val idx = raw.lastIndexOf(STYLES_SEPARATOR)
        return if (idx >= 0) raw.substring(0, idx) else raw
    }

    /**
     * Validates and normalizes [rawUrl] to an allowed external link scheme
     * (`https://`, `http://`, `mailto:`, or `tel:`), prepending `https://` for bare
     * hostnames and rejecting control characters or unsafe schemes.
     */
    fun sanitizeLinkUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return null

        val lower = trimmed.lowercase()
        val matchedScheme = ALLOWED_LINK_SCHEMES.firstOrNull { lower.startsWith(it) }
        if (matchedScheme != null) {
            val rest = trimmed.substring(matchedScheme.length)
            if (rest.isBlank()) return null
            if (matchedScheme == "http://" || matchedScheme == "https://") {
                val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
                if (authority.isBlank() || authority.startsWith('.')) return null
            }
            return matchedScheme + rest
        }

        // Reject any explicit scheme not in the allowlist (e.g. javascript:, data:, file:, intent:).
        val firstDelimiter = trimmed.indexOfAny(charArrayOf(':', '/', '?', '#'))
        if (firstDelimiter >= 0 && trimmed[firstDelimiter] == ':') {
            return null
        }

        // Allow bare domain/path inputs such as "example.com" or "docs.kotlinlang.org/api".
        val hostPart = trimmed.substringBefore('/').substringBefore('?').substringBefore('#')
        if (!hostPart.contains('.') || hostPart.startsWith('.') || hostPart.endsWith('.')) {
            return null
        }
        return "https://$trimmed"
    }

    fun decode(raw: String): RichTextDocument {
        val idx = raw.lastIndexOf(STYLES_SEPARATOR)
        if (idx < 0) return RichTextDocument(text = raw)

        val text = raw.substring(0, idx)
        val meta = raw.substring(idx + STYLES_SEPARATOR.length)
        val lineCount = countLines(text)
        val inlineSpans = mutableListOf<RichInlineSpan>()
        val blockSpans = mutableListOf<RichBlockSpan>()
        val linkSpans = mutableListOf<RichLinkSpan>()

        meta.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split(":", limit = META_KEY_VALUE_PARTS)
            if (parts.size != META_KEY_VALUE_PARTS) return@forEach

            val code = parts[0]
            val payload = parts[1]

            if (code == LINK_CODE) {
                val linkParts = payload.split(":", limit = META_KEY_VALUE_PARTS)
                if (linkParts.size == META_KEY_VALUE_PARTS) {
                    val bounds = linkParts[0].split(",", limit = SPAN_BOUND_PARTS)
                    val safeUrl = sanitizeLinkUrl(linkParts[1])
                    if (bounds.size == SPAN_BOUND_PARTS && safeUrl != null) {
                        val start = bounds[0].toIntOrNull()
                        val end = bounds[1].toIntOrNull()
                        if (start != null && end != null && start >= 0 && end > start && end <= text.length) {
                            linkSpans += RichLinkSpan(start, end, safeUrl)
                        }
                    }
                }
                return@forEach
            }

            val inlineKind = RichInlineKind.fromCode(code)
            if (inlineKind != null) {
                val bounds = payload.split(",", limit = SPAN_BOUND_PARTS)
                if (bounds.size == SPAN_BOUND_PARTS) {
                    val start = bounds[0].toIntOrNull()
                    val end = bounds[1].toIntOrNull()
                    if (start != null && end != null && start >= 0 && end > start && end <= text.length) {
                        inlineSpans += RichInlineSpan(start, end, inlineKind)
                    }
                }
                return@forEach
            }

            val blockKind = RichBlockKind.fromCode(code)
            if (blockKind != null && blockKind != RichBlockKind.NORMAL) {
                val lineIndex = payload.toIntOrNull()
                if (lineIndex != null && lineIndex in 0 until lineCount) {
                    blockSpans += RichBlockSpan(lineIndex, blockKind)
                }
            }
        }

        return RichTextDocument(
            text = text,
            inlineSpans = normalizeInlineSpans(inlineSpans, text.length),
            blockSpans = normalizeBlockSpans(blockSpans, lineCount),
            linkSpans = normalizeLinkSpans(linkSpans, text.length)
        )
    }

    fun encode(doc: RichTextDocument): String {
        val inline = normalizeInlineSpans(doc.inlineSpans, doc.text.length)
        val blocks = normalizeBlockSpans(doc.blockSpans, countLines(doc.text))
        val links = normalizeLinkSpans(doc.linkSpans, doc.text.length)
        if (inline.isEmpty() && blocks.isEmpty() && links.isEmpty()) return doc.text

        val metaLines = buildList {
            blocks.forEach { add("${it.kind.code}:${it.lineIndex}") }
            inline.forEach { add("${it.kind.code}:${it.start},${it.end}") }
            links.forEach { add("$LINK_CODE:${it.start},${it.end}:${it.url}") }
        }
        return doc.text + STYLES_SEPARATOR + metaLines.joinToString("\n")
    }

    fun inlineKindsCoveringSelection(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int
    ): Set<RichInlineKind> {
        val s = minOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val e = maxOf(selStart, selEnd).coerceIn(0, doc.text.length)
        if (s >= e) return inlineKindsAtCursor(doc, s)

        val result = mutableSetOf<RichInlineKind>()
        for (kind in RichInlineKind.entries) {
            if (doc.inlineSpans.any { it.kind == kind && it.start <= s && it.end >= e }) {
                result += kind
            }
        }
        return result
    }

    fun inlineKindsAtCursor(doc: RichTextDocument, cursor: Int): Set<RichInlineKind> {
        if (doc.text.isEmpty() || cursor <= 0) return emptySet()
        val probe = (cursor - 1).coerceIn(0, doc.text.lastIndex)
        return doc.inlineSpans
            .asSequence()
            .filter { probe >= it.start && probe < it.end }
            .map { it.kind }
            .toSet()
    }

    fun linkAt(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int
    ): RichLinkSpan? = linkAt(doc.linkSpans, doc.text.length, selStart, selEnd)

    fun linkAt(
        linkSpans: List<RichLinkSpan>,
        textLength: Int,
        selStart: Int,
        selEnd: Int
    ): RichLinkSpan? {
        if (textLength <= 0 || linkSpans.isEmpty()) return null
        val s = minOf(selStart, selEnd).coerceIn(0, textLength)
        val e = maxOf(selStart, selEnd).coerceIn(0, textLength)
        return if (s == e) {
            linkSpans.firstOrNull { s in it.start..it.end }
        } else {
            linkSpans.firstOrNull { it.start < e && it.end > s }
        }
    }

    /**
     * Applies or updates a hyperlink on `[selStart, selEnd)` or inserts a new link at a
     * collapsed cursor, returning the updated document and the resulting cursor offset.
     */
    fun applyLink(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int,
        rawUrl: String,
        displayText: String? = null
    ): Pair<RichTextDocument, Int> {
        val safeUrl = sanitizeLinkUrl(rawUrl) ?: return doc to maxOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val s = minOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val e = maxOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val cleanedDisplay = displayText
            ?.replace('\n', ' ')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (s < e && (cleanedDisplay == null || cleanedDisplay == doc.text.substring(s, e))) {
            val remaining = subtractLinkRange(doc.linkSpans, s, e)
            val updatedLinks = normalizeLinkSpans(remaining + RichLinkSpan(s, e, safeUrl), doc.text.length)
            return doc.copy(linkSpans = updatedLinks) to e
        }

        val replacement = cleanedDisplay ?: safeUrl
        val newText = doc.text.substring(0, s) + replacement + doc.text.substring(e)
        val newCursor = s + replacement.length
        val diff = TextEditDiff(editStart = s, oldEditEnd = e, newEditEnd = newCursor)
        val adjustedInline = adjustInlineSpansForDiff(
            oldSpans = doc.inlineSpans,
            diff = diff,
            newTextLength = newText.length,
            activeTypingKinds = inlineKindsCoveringSelection(doc, s, e)
        )
        val adjustedBlocks = adjustBlockSpansForDiff(
            oldBlocks = doc.blockSpans,
            oldText = doc.text,
            newText = newText,
            diff = diff
        )
        val shiftedLinks = adjustLinkSpansForDiff(
            oldLinks = doc.linkSpans,
            diff = diff,
            newTextLength = newText.length
        )
        val updatedLinks = normalizeLinkSpans(
            subtractLinkRange(shiftedLinks, s, newCursor) + RichLinkSpan(s, newCursor, safeUrl),
            newText.length
        )
        return RichTextDocument(
            text = newText,
            inlineSpans = adjustedInline,
            blockSpans = adjustedBlocks,
            linkSpans = updatedLinks
        ) to newCursor
    }

    fun removeLink(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int
    ): RichTextDocument {
        val s = minOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val e = maxOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val remaining = if (s == e) {
            val target = linkAt(doc, s, e) ?: return doc
            doc.linkSpans.filterNot { it == target }
        } else {
            subtractLinkRange(doc.linkSpans, s, e)
        }
        return doc.copy(linkSpans = normalizeLinkSpans(remaining, doc.text.length))
    }

    fun blockKindAt(doc: RichTextDocument, cursor: Int): RichBlockKind {
        val lineIndex = lineIndexAt(doc.text, cursor)
        return doc.blockSpans.firstOrNull { it.lineIndex == lineIndex }?.kind ?: RichBlockKind.NORMAL
    }

    fun toggleInlineSpan(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int,
        kind: RichInlineKind
    ): RichTextDocument {
        val s = minOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val e = maxOf(selStart, selEnd).coerceIn(0, doc.text.length)
        if (s >= e) return doc

        val alreadyCovered = doc.inlineSpans.any {
            it.kind == kind && it.start <= s && it.end >= e
        }
        val updated = if (alreadyCovered) {
            subtractSpanRange(doc.inlineSpans, s, e) { it.kind == kind }
        } else {
            doc.inlineSpans + RichInlineSpan(s, e, kind)
        }
        return doc.copy(inlineSpans = normalizeInlineSpans(updated, doc.text.length))
    }

    fun applyBlockKind(
        doc: RichTextDocument,
        cursor: Int,
        target: RichBlockKind
    ): RichTextDocument {
        val lineIndex = lineIndexAt(doc.text, cursor)
        val current = blockKindAt(doc, cursor)
        val resolved = if (target == current) RichBlockKind.NORMAL else target
        val filtered = doc.blockSpans.filterNot { it.lineIndex == lineIndex }
        val updated = if (resolved == RichBlockKind.NORMAL) {
            filtered
        } else {
            filtered + RichBlockSpan(lineIndex, resolved)
        }
        return doc.copy(blockSpans = normalizeBlockSpans(updated, countLines(doc.text)))
    }

    fun clearFormatting(
        doc: RichTextDocument,
        selStart: Int,
        selEnd: Int
    ): RichTextDocument {
        val s = minOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val e = maxOf(selStart, selEnd).coerceIn(0, doc.text.length)
        val clearRange = if (s == e) lineBoundsAt(doc.text, s) else s until e
        val startLine = lineIndexAt(doc.text, clearRange.first)
        val endLine = if (clearRange.isEmpty()) {
            startLine
        } else {
            lineIndexAt(doc.text, clearRange.last)
        }

        val remainingInline = if (!clearRange.isEmpty()) {
            subtractSpanRange(doc.inlineSpans, clearRange.first, clearRange.last + 1) { true }
        } else {
            doc.inlineSpans
        }
        val remainingLinks = if (!clearRange.isEmpty()) {
            subtractLinkRange(doc.linkSpans, clearRange.first, clearRange.last + 1)
        } else {
            doc.linkSpans
        }
        val remainingBlocks = doc.blockSpans.filterNot { it.lineIndex in startLine..endLine }
        return doc.copy(
            inlineSpans = normalizeInlineSpans(remainingInline, doc.text.length),
            blockSpans = normalizeBlockSpans(remainingBlocks, countLines(doc.text)),
            linkSpans = normalizeLinkSpans(remainingLinks, doc.text.length)
        )
    }

    fun adjustForTextEdit(
        oldDoc: RichTextDocument,
        newText: String,
        cursorAfterEdit: Int,
        activeTypingKinds: Set<RichInlineKind>
    ): RichTextDocument {
        if (oldDoc.text == newText) return oldDoc
        val diff = computeTextDiff(oldDoc.text, newText, cursorAfterEdit)
        val adjustedInline = adjustInlineSpansForDiff(
            oldSpans = oldDoc.inlineSpans,
            diff = diff,
            newTextLength = newText.length,
            activeTypingKinds = activeTypingKinds
        )
        val adjustedBlocks = adjustBlockSpansForDiff(
            oldBlocks = oldDoc.blockSpans,
            oldText = oldDoc.text,
            newText = newText,
            diff = diff
        )
        val adjustedLinks = adjustLinkSpansForDiff(
            oldLinks = oldDoc.linkSpans,
            diff = diff,
            newTextLength = newText.length
        )
        return RichTextDocument(
            text = newText,
            inlineSpans = adjustedInline,
            blockSpans = adjustedBlocks,
            linkSpans = adjustedLinks
        )
    }

    fun adjustLocalSpansForEdit(
        oldSpans: List<RichInlineSpan>,
        oldText: String,
        newText: String,
        cursorAfterEdit: Int,
        activeTypingKinds: Set<RichInlineKind>
    ): List<RichInlineSpan> {
        if (oldText == newText) return normalizeInlineSpans(oldSpans, newText.length)
        val diff = computeTextDiff(oldText, newText, cursorAfterEdit)
        return adjustInlineSpansForDiff(oldSpans, diff, newText.length, activeTypingKinds)
    }

    fun adjustLocalLinkSpansForEdit(
        oldLinks: List<RichLinkSpan>,
        oldText: String,
        newText: String,
        cursorAfterEdit: Int
    ): List<RichLinkSpan> {
        if (oldText == newText) return normalizeLinkSpans(oldLinks, newText.length)
        val diff = computeTextDiff(oldText, newText, cursorAfterEdit)
        return adjustLinkSpansForDiff(oldLinks, diff, newText.length)
    }

    fun extractSegmentInlineSpans(doc: RichTextDocument): List<List<RichInlineSpan>> {
        val lines = doc.text.split("\n")
        val result = MutableList(lines.size) { emptyList<RichInlineSpan>() }
        var lineStart = 0
        lines.forEachIndexed { index, line ->
            val prefixLen = segmentItemStartOffset(line)
            val itemStart = lineStart + prefixLen
            val itemLen = (line.length - prefixLen).coerceAtLeast(0)
            val itemEnd = itemStart + itemLen
            if (itemLen > 0) {
                val localSpans = doc.inlineSpans.mapNotNull { span ->
                    val s = maxOf(span.start, itemStart)
                    val e = minOf(span.end, itemEnd)
                    if (s < e) RichInlineSpan(s - itemStart, e - itemStart, span.kind) else null
                }
                result[index] = normalizeInlineSpans(localSpans, itemLen)
            }
            lineStart += line.length + 1
        }
        return result
    }

    fun extractSegmentLinkSpans(doc: RichTextDocument): List<List<RichLinkSpan>> {
        val lines = doc.text.split("\n")
        val result = MutableList(lines.size) { emptyList<RichLinkSpan>() }
        var lineStart = 0
        lines.forEachIndexed { index, line ->
            val prefixLen = segmentItemStartOffset(line)
            val itemStart = lineStart + prefixLen
            val itemLen = (line.length - prefixLen).coerceAtLeast(0)
            val itemEnd = itemStart + itemLen
            if (itemLen > 0) {
                val localLinks = doc.linkSpans.mapNotNull { span ->
                    val s = maxOf(span.start, itemStart)
                    val e = minOf(span.end, itemEnd)
                    if (s < e) RichLinkSpan(s - itemStart, e - itemStart, span.url) else null
                }
                result[index] = normalizeLinkSpans(localLinks, itemLen)
            }
            lineStart += line.length + 1
        }
        return result
    }

    fun assembleFromSegments(
        segments: List<NoteSegment>,
        perSegmentSpans: List<List<RichInlineSpan>>,
        blockSpans: List<RichBlockSpan> = emptyList(),
        perSegmentLinkSpans: List<List<RichLinkSpan>> = emptyList()
    ): RichTextDocument {
        val serialized = serializeNoteSegments(segments)
        val globalSpans = mutableListOf<RichInlineSpan>()
        val globalLinks = mutableListOf<RichLinkSpan>()
        var lineStart = 0
        segments.forEachIndexed { index, segment ->
            val serializedLine = serializeNoteSegments(listOf(segment))
            val itemText = segment.lineText()
            val prefixLen = (serializedLine.length - itemText.length).coerceAtLeast(0)
            val itemStart = lineStart + prefixLen
            perSegmentSpans.getOrNull(index).orEmpty().forEach { span ->
                val s = span.start.coerceIn(0, itemText.length)
                val e = span.end.coerceIn(0, itemText.length)
                if (s < e) {
                    globalSpans += RichInlineSpan(itemStart + s, itemStart + e, span.kind)
                }
            }
            perSegmentLinkSpans.getOrNull(index).orEmpty().forEach { span ->
                val s = span.start.coerceIn(0, itemText.length)
                val e = span.end.coerceIn(0, itemText.length)
                if (s < e) {
                    globalLinks += RichLinkSpan(itemStart + s, itemStart + e, span.url)
                }
            }
            lineStart += serializedLine.length + 1
        }
        return RichTextDocument(
            text = serialized,
            inlineSpans = normalizeInlineSpans(globalSpans, serialized.length),
            blockSpans = normalizeBlockSpans(blockSpans, countLines(serialized)),
            linkSpans = normalizeLinkSpans(globalLinks, serialized.length)
        )
    }

    fun segmentItemStartOffset(line: String): Int = when {
        line.startsWith(CHECKLIST_CHECKED_PREFIX, ignoreCase = true) ->
            CHECKLIST_CHECKED_PREFIX.length
        line.startsWith(CHECKLIST_UNCHECKED_PREFIX) ->
            CHECKLIST_UNCHECKED_PREFIX.length
        line.isNumberedLine() ->
            (line.length - line.numberedLineText().length).coerceAtLeast(0)
        line.isBulletLine() ->
            BULLET_PREFIX.length
        else -> 0
    }

    fun computeTextDiff(
        oldText: String,
        newText: String,
        cursorAfterEdit: Int
    ): TextEditDiff {
        val oldLen = oldText.length
        val newLen = newText.length
        val delta = newLen - oldLen

        if (delta > 0 && cursorAfterEdit in delta..newLen) {
            val insStart = cursorAfterEdit - delta
            val candidate = newText.removeRange(insStart, cursorAfterEdit)
            if (candidate == oldText) {
                return TextEditDiff(insStart, insStart, cursorAfterEdit)
            }
        }

        if (delta < 0) {
            val delCount = -delta
            if (cursorAfterEdit in 0..newLen && cursorAfterEdit + delCount <= oldLen) {
                val candidate = oldText.removeRange(cursorAfterEdit, cursorAfterEdit + delCount)
                if (candidate == newText) {
                    return TextEditDiff(cursorAfterEdit, cursorAfterEdit + delCount, cursorAfterEdit)
                }
            }
        }

        var prefix = 0
        val minLen = minOf(oldLen, newLen)
        while (prefix < minLen && oldText[prefix] == newText[prefix]) {
            prefix++
        }

        var suffix = 0
        while (oldLen - suffix > prefix &&
            newLen - suffix > prefix &&
            oldText[oldLen - 1 - suffix] == newText[newLen - 1 - suffix]
        ) {
            suffix++
        }

        return TextEditDiff(
            editStart = prefix,
            oldEditEnd = oldLen - suffix,
            newEditEnd = newLen - suffix
        )
    }

    fun normalizeInlineSpans(
        spans: List<RichInlineSpan>,
        textLength: Int
    ): List<RichInlineSpan> {
        if (textLength <= 0 || spans.isEmpty()) return emptyList()
        val result = mutableListOf<RichInlineSpan>()
        RichInlineKind.entries.forEach { kind ->
            val sorted = spans
                .asSequence()
                .filter { it.kind == kind }
                .map {
                    RichInlineSpan(
                        start = it.start.coerceIn(0, textLength),
                        end = it.end.coerceIn(0, textLength),
                        kind = kind
                    )
                }
                .filter { it.start < it.end }
                .sortedWith(compareBy<RichInlineSpan> { it.start }.thenBy { it.end })
                .toList()

            var current: RichInlineSpan? = null
            sorted.forEach { next ->
                val cur = current
                current = if (cur == null) {
                    next
                } else if (next.start <= cur.end) {
                    cur.copy(end = maxOf(cur.end, next.end))
                } else {
                    result += cur
                    next
                }
            }
            current?.let { result += it }
        }
        return result.sortedWith(compareBy<RichInlineSpan> { it.start }.thenBy { it.end }.thenBy { it.kind.ordinal })
    }

    fun normalizeLinkSpans(
        spans: List<RichLinkSpan>,
        textLength: Int
    ): List<RichLinkSpan> {
        if (textLength <= 0 || spans.isEmpty()) return emptyList()
        val sorted = spans
            .asSequence()
            .mapNotNull { span ->
                val safeUrl = sanitizeLinkUrl(span.url) ?: return@mapNotNull null
                val start = span.start.coerceIn(0, textLength)
                val end = span.end.coerceIn(0, textLength)
                if (start < end) RichLinkSpan(start, end, safeUrl) else null
            }
            .sortedWith(compareBy<RichLinkSpan> { it.start }.thenBy { it.end })
            .toList()

        val result = mutableListOf<RichLinkSpan>()
        sorted.forEach { next ->
            val prev = result.lastOrNull()
            when {
                prev == null -> result += next
                next.start <= prev.end && next.url == prev.url -> {
                    result[result.lastIndex] = prev.copy(end = maxOf(prev.end, next.end))
                }
                next.start < prev.end -> {
                    if (next.end > prev.end) {
                        result += next.copy(start = prev.end)
                    }
                }
                else -> result += next
            }
        }
        return result
    }

    private fun normalizeBlockSpans(
        spans: List<RichBlockSpan>,
        lineCount: Int
    ): List<RichBlockSpan> {
        if (lineCount <= 0 || spans.isEmpty()) return emptyList()
        val byLine = linkedMapOf<Int, RichBlockKind>()
        spans.forEach { span ->
            if (span.lineIndex in 0 until lineCount && span.kind != RichBlockKind.NORMAL) {
                byLine[span.lineIndex] = span.kind
            }
        }
        return byLine.entries
            .sortedBy { it.key }
            .map { RichBlockSpan(it.key, it.value) }
    }

    private fun subtractSpanRange(
        spans: List<RichInlineSpan>,
        removeStart: Int,
        removeEnd: Int,
        predicate: (RichInlineSpan) -> Boolean
    ): List<RichInlineSpan> = spans.flatMap { span ->
        if (!predicate(span) || span.end <= removeStart || span.start >= removeEnd) {
            listOf(span)
        } else {
            buildList {
                if (span.start < removeStart) {
                    add(span.copy(end = removeStart))
                }
                if (span.end > removeEnd) {
                    add(span.copy(start = removeEnd))
                }
            }
        }
    }

    private fun subtractLinkRange(
        spans: List<RichLinkSpan>,
        removeStart: Int,
        removeEnd: Int
    ): List<RichLinkSpan> = spans.flatMap { span ->
        if (span.end <= removeStart || span.start >= removeEnd) {
            listOf(span)
        } else {
            buildList {
                if (span.start < removeStart) {
                    add(span.copy(end = removeStart))
                }
                if (span.end > removeEnd) {
                    add(span.copy(start = removeEnd))
                }
            }
        }
    }

    private fun adjustInlineSpansForDiff(
        oldSpans: List<RichInlineSpan>,
        diff: TextEditDiff,
        newTextLength: Int,
        activeTypingKinds: Set<RichInlineKind>
    ): List<RichInlineSpan> {
        val editStart = diff.editStart
        val oldEditEnd = diff.oldEditEnd
        val newEditEnd = diff.newEditEnd
        val delta = newEditEnd - oldEditEnd
        val hasInsertion = newEditEnd > editStart

        val nextSpans = mutableListOf<RichInlineSpan>()
        oldSpans.forEach { span ->
            when {
                span.end <= editStart -> nextSpans += span
                span.start >= oldEditEnd -> nextSpans += span.copy(
                    start = span.start + delta,
                    end = span.end + delta
                )
                span.start < editStart && span.end > oldEditEnd -> {
                    if (hasInsertion && span.kind !in activeTypingKinds) {
                        nextSpans += span.copy(end = editStart)
                        nextSpans += span.copy(start = newEditEnd, end = span.end + delta)
                    } else {
                        nextSpans += span.copy(end = span.end + delta)
                    }
                }
                span.start < editStart -> nextSpans += span.copy(end = editStart)
                span.end > oldEditEnd -> nextSpans += span.copy(
                    start = newEditEnd,
                    end = span.end + delta
                )
                else -> Unit
            }
        }

        if (hasInsertion) {
            activeTypingKinds.forEach { kind ->
                nextSpans += RichInlineSpan(editStart, newEditEnd, kind)
            }
        }

        return normalizeInlineSpans(nextSpans, newTextLength)
    }

    private fun adjustLinkSpansForDiff(
        oldLinks: List<RichLinkSpan>,
        diff: TextEditDiff,
        newTextLength: Int
    ): List<RichLinkSpan> {
        if (oldLinks.isEmpty()) return emptyList()
        val editStart = diff.editStart
        val oldEditEnd = diff.oldEditEnd
        val newEditEnd = diff.newEditEnd
        val delta = newEditEnd - oldEditEnd

        val nextLinks = mutableListOf<RichLinkSpan>()
        oldLinks.forEach { span ->
            when {
                span.end <= editStart -> nextLinks += span
                span.start >= oldEditEnd -> nextLinks += span.copy(
                    start = span.start + delta,
                    end = span.end + delta
                )
                span.start < editStart && span.end > oldEditEnd -> {
                    nextLinks += span.copy(end = span.end + delta)
                }
                span.start < editStart -> nextLinks += span.copy(end = editStart)
                span.end > oldEditEnd -> nextLinks += span.copy(
                    start = newEditEnd,
                    end = span.end + delta
                )
                else -> Unit
            }
        }
        return normalizeLinkSpans(nextLinks, newTextLength)
    }

    private fun adjustBlockSpansForDiff(
        oldBlocks: List<RichBlockSpan>,
        oldText: String,
        newText: String,
        diff: TextEditDiff
    ): List<RichBlockSpan> {
        if (oldBlocks.isEmpty()) return emptyList()
        val startLine = lineIndexAt(oldText, diff.editStart)
        val removedNewlines = oldText.substring(diff.editStart, diff.oldEditEnd).count { it == '\n' }
        val insertedNewlines = newText.substring(diff.editStart, diff.newEditEnd).count { it == '\n' }
        val lineDelta = insertedNewlines - removedNewlines
        val lineStartOffset = lineStartOf(oldText, startLine)
        val insertedAtLineStart = removedNewlines == 0 && insertedNewlines > 0 && diff.editStart == lineStartOffset

        val updated = oldBlocks.mapNotNull { span ->
            when {
                span.lineIndex < startLine -> span
                span.lineIndex == startLine -> {
                    if (insertedAtLineStart) span.copy(lineIndex = span.lineIndex + insertedNewlines) else span
                }
                span.lineIndex <= startLine + removedNewlines -> null
                else -> span.copy(lineIndex = span.lineIndex + lineDelta)
            }
        }
        return normalizeBlockSpans(updated, countLines(newText))
    }

    private fun countLines(text: String): Int =
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1

    private fun lineIndexAt(text: String, offset: Int): Int {
        val safe = offset.coerceIn(0, text.length)
        var lines = 0
        for (i in 0 until safe) {
            if (text[i] == '\n') lines++
        }
        return lines
    }

    private fun lineStartOf(text: String, targetLine: Int): Int {
        if (targetLine <= 0) return 0
        var line = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                line++
                if (line == targetLine) return i + 1
            }
        }
        return text.length
    }

    private fun lineBoundsAt(text: String, offset: Int): IntRange {
        val safe = offset.coerceIn(0, text.length)
        val prevNewline = text.lastIndexOf('\n', startIndex = (safe - 1).coerceAtLeast(0))
        val lineStart = if (safe == 0 || prevNewline < 0) 0 else prevNewline + 1
        val nextNewline = text.indexOf('\n', startIndex = safe)
        val lineEnd = if (nextNewline < 0) text.length else nextNewline
        return lineStart until lineEnd
    }
}
