package com.deepanjanxyz.notepad.backup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.Log
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.NoteSegment
import com.deepanjanxyz.notepad.domain.model.RichBlockKind
import com.deepanjanxyz.notepad.domain.model.RichInlineKind
import com.deepanjanxyz.notepad.domain.model.RichInlineSpan
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import com.deepanjanxyz.notepad.domain.model.lineText
import com.deepanjanxyz.notepad.domain.model.parseNoteSegments
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

private const val TAG = "NotePdfExport"
private const val PDF_SUFFIX = ".pdf"
private const val PDF_MIME_TYPE = "application/pdf"
private const val UNTITLED_FALLBACK_NAME = "Untitled Note"
private const val MAX_NAME_LENGTH = 60
private val ILLEGAL_IN_NAME = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

private const val EDITOR_HORIZONTAL_PADDING_DP = 40f
private const val EDITOR_VERTICAL_PADDING_DP = 24f
private const val DEFAULT_CONTENT_WIDTH = 340f
private const val DEFAULT_CONTENT_HEIGHT = 600f
private const val MIN_CONTENT_WIDTH = 280f
private const val MAX_CONTENT_WIDTH = 520f
private const val MIN_CONTENT_HEIGHT = 420f
private const val MAX_CONTENT_HEIGHT = 820f

private const val PAGE_MARGIN = 36f
private const val FOOTER_RESERVED = 30f
private const val FOOTER_DIVIDER_OFFSET = 6f
private const val FOOTER_BASELINE_OFFSET = 14f
private const val FOOTER_LABEL_GAP = 16f
private const val MIN_FOOTER_TITLE_WIDTH = 40f

private const val TITLE_TEXT_SIZE = 24f
private const val TITLE_LINE_SPACING = 1.15f
private const val BODY_TEXT_SIZE = 18f
private const val BODY_TARGET_LINE_HEIGHT = 26f
private const val MIN_LINE_SPACING_ADD = 2f
private const val FOOTER_TEXT_SIZE = 10f
private const val TITLE_GAP_BEFORE_DIVIDER = 8f
private const val TITLE_GAP_AFTER_DIVIDER = 14f
private const val DIVIDER_STROKE_WIDTH = 1f
private const val FOOTER_DIVIDER_STROKE_WIDTH = 0.5f

private const val COLOR_BODY = 0xFF222222.toInt()
private const val COLOR_LINK = 0xFF1A56DB.toInt()
private const val COLOR_DIVIDER = 0xFFD0D5DD.toInt()
private const val COLOR_FOOTER = 0xFF667085.toInt()
private const val PDF_H1_SCALE = 1.33f
private const val PDF_H2_SCALE = 1.15f

/**
 * Exports a note as one or more phone-screen-sized PDFs: a single PDF when the
 * text fits on one screen, and sequential follow-up PDFs (`Example 2.pdf`, ...)
 * when the text is long enough to scroll beyond a single screen.
 */
@Suppress("TooManyFunctions")
object NotePdfExport {

    private val exportLock = Mutex()

    data class Outcome(
        val message: String,
        val error: Boolean = false
    )

    private sealed interface PageElement {
        data class LineSlice(
            val layout: StaticLayout,
            val lineIndex: Int,
            val y: Float
        ) : PageElement

        data class Divider(val y: Float) : PageElement
    }

    private data class PdfPageSpec(
        val elements: List<PageElement>,
        val pageHeight: Int
    )

    suspend fun exportNote(
        context: Context,
        title: String,
        content: String,
        viewportWidthDp: Float? = null,
        viewportHeightDp: Float? = null,
        isContentScrollable: Boolean? = null
    ): Outcome {
        val plainContent = RichTextCodec.extractPlainText(DrawingSerializer.extractTextPart(content))
        if (title.isBlank() && plainContent.isBlank()) {
            return Outcome(
                message = "There is nothing in this note to export",
                error = true
            )
        }
        return exportLock.withLock {
            runCatching {
                val renderedPdfs = renderPdfDocuments(
                    title = title,
                    body = formatStyledBodyText(content),
                    viewportWidthDp = viewportWidthDp,
                    viewportHeightDp = viewportHeightDp,
                    isContentScrollable = isContentScrollable
                )
                val baseName = baseNameFor(title)
                val takenNames = mutableSetOf<String>()
                val writtenNames = mutableListOf<String>()
                renderedPdfs.forEach { bytes ->
                    val fileName = nextAvailableFileName(baseName, takenNames) { candidate ->
                        DownloadsBackup.exists(context, candidate)
                    }
                    DownloadsBackup.writeBytes(context, bytes, fileName, PDF_MIME_TYPE)
                    takenNames += fileName.lowercase()
                    writtenNames += fileName
                }
                writtenNames
            }.fold(
                onSuccess = { names ->
                    val msg = if (names.size == 1) {
                        "Exported PDF to ${DownloadsBackup.locationLabel(names.first())}"
                    } else {
                        "Exported ${names.size} PDFs (${names.first()} – ${names.last()}) " +
                            "to Downloads/${BackupFolder.FOLDER_NAME}/"
                    }
                    Outcome(message = msg)
                },
                onFailure = { error ->
                    Log.w(TAG, "PDF export failed", error)
                    Outcome(
                        message = "The PDF could not be exported: ${error.message}",
                        error = true
                    )
                }
            )
        }
    }

    internal fun sourceTitleFor(title: String): String =
        title.trim().ifEmpty { UNTITLED_FALLBACK_NAME }

    internal fun baseNameFor(title: String): String {
        val cleaned = title
            .map { ch -> if (ch in ILLEGAL_IN_NAME || ch.isISOControl()) ' ' else ch }
            .joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(MAX_NAME_LENGTH)
            .trim()
        return cleaned.ifEmpty { UNTITLED_FALLBACK_NAME }
    }

    internal fun fileNameFor(title: String): String = baseNameFor(title) + PDF_SUFFIX

    internal fun nextAvailableFileName(
        base: String,
        takenNames: Set<String> = emptySet(),
        existsOnDisk: (String) -> Boolean
    ): String {
        val first = "$base$PDF_SUFFIX"
        if (first.lowercase() !in takenNames && !existsOnDisk(first)) return first
        var index = 2
        while (true) {
            val candidate = "$base $index$PDF_SUFFIX"
            if (candidate.lowercase() !in takenNames && !existsOnDisk(candidate)) return candidate
            index++
        }
    }

    internal fun formatBodyText(content: String): String {
        val plain = RichTextCodec.extractPlainText(DrawingSerializer.extractTextPart(content))
        if (plain.isBlank()) return ""
        return parseNoteSegments(plain).joinToString("\n") { segment ->
            when (segment) {
                is NoteSegment.Task -> {
                    val marker = if (segment.item.isChecked) "[x] " else "[ ] "
                    marker + segment.item.text
                }
                is NoteSegment.Bullet -> "• ${segment.text}"
                is NoteSegment.Numbered -> "${segment.number}. ${segment.text}"
                is NoteSegment.Text -> segment.text
            }
        }
    }

    internal fun formatStyledBodyText(content: String): CharSequence {
        val doc = RichTextCodec.decode(DrawingSerializer.extractTextPart(content))
        if (doc.text.isBlank()) return ""

        val segments = parseNoteSegments(doc.text)
        val perSegmentSpans = RichTextCodec.extractSegmentInlineSpans(doc)
        val perSegmentLinks = RichTextCodec.extractSegmentLinkSpans(doc)
        val builder = SpannableStringBuilder()

        segments.forEachIndexed { index, segment ->
            val dstLineStart = builder.length
            val prefix = when (segment) {
                is NoteSegment.Task -> if (segment.item.isChecked) "[x] " else "[ ] "
                is NoteSegment.Bullet -> "• "
                is NoteSegment.Numbered -> "${segment.number}. "
                is NoteSegment.Text -> ""
            }
            val itemText = segment.lineText()
            builder.append(prefix)
            val dstItemStart = builder.length
            builder.append(itemText)
            val dstLineEnd = builder.length
            if (index < segments.lastIndex) {
                builder.append('\n')
            }

            val block = doc.blockSpans.firstOrNull { it.lineIndex == index }
            if (block != null && dstLineStart < dstLineEnd) {
                when (block.kind) {
                    RichBlockKind.H1 -> {
                        builder.setSpan(
                            RelativeSizeSpan(PDF_H1_SCALE),
                            dstLineStart,
                            dstLineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            StyleSpan(Typeface.BOLD),
                            dstLineStart,
                            dstLineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    RichBlockKind.H2 -> {
                        builder.setSpan(
                            RelativeSizeSpan(PDF_H2_SCALE),
                            dstLineStart,
                            dstLineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            StyleSpan(Typeface.BOLD),
                            dstLineStart,
                            dstLineEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    RichBlockKind.NORMAL -> Unit
                }
            }

            perSegmentSpans.getOrNull(index).orEmpty().forEach { span: RichInlineSpan ->
                val s = (dstItemStart + span.start).coerceIn(dstItemStart, dstLineEnd)
                val e = (dstItemStart + span.end).coerceIn(dstItemStart, dstLineEnd)
                if (s < e) {
                    val what: Any = when (span.kind) {
                        RichInlineKind.BOLD -> StyleSpan(Typeface.BOLD)
                        RichInlineKind.ITALIC -> StyleSpan(Typeface.ITALIC)
                        RichInlineKind.UNDERLINE -> UnderlineSpan()
                        RichInlineKind.STRIKETHROUGH -> StrikethroughSpan()
                    }
                    builder.setSpan(what, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            perSegmentLinks.getOrNull(index).orEmpty().forEach { linkSpan ->
                val s = (dstItemStart + linkSpan.start).coerceIn(dstItemStart, dstLineEnd)
                val e = (dstItemStart + linkSpan.end).coerceIn(dstItemStart, dstLineEnd)
                if (s < e) {
                    builder.setSpan(URLSpan(linkSpan.url), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(COLOR_LINK), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(UnderlineSpan(), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return builder
    }

    private fun trimTrailingNewlines(source: CharSequence): CharSequence {
        var end = source.length
        while (end > 0 && (source[end - 1] == '\n' || source[end - 1] == '\r')) {
            end--
        }
        return if (end == source.length) source else source.subSequence(0, end)
    }

    private fun resolveContentWidth(viewportWidthDp: Float?): Int {
        val raw = viewportWidthDp?.let { it - EDITOR_HORIZONTAL_PADDING_DP } ?: DEFAULT_CONTENT_WIDTH
        return raw.coerceIn(MIN_CONTENT_WIDTH, MAX_CONTENT_WIDTH).roundToInt()
    }

    private fun resolveContentHeight(viewportHeightDp: Float?): Float {
        val raw = viewportHeightDp?.let { it - EDITOR_VERTICAL_PADDING_DP } ?: DEFAULT_CONTENT_HEIGHT
        return raw.coerceIn(MIN_CONTENT_HEIGHT, MAX_CONTENT_HEIGHT)
    }

    private fun renderPdfDocuments(
        title: String,
        body: CharSequence,
        viewportWidthDp: Float?,
        viewportHeightDp: Float?,
        isContentScrollable: Boolean?
    ): List<ByteArray> {
        val contentWidth = resolveContentWidth(viewportWidthDp)
        val contentHeight = resolveContentHeight(viewportHeightDp)
        val pageWidth = contentWidth + (PAGE_MARGIN * 2).toInt()
        val nominalPageHeight = (contentHeight + PAGE_MARGIN * 2 + FOOTER_RESERVED).roundToInt()

        val cleanTitle = title.trim()
        val titleLayout = cleanTitle.takeIf { it.isNotEmpty() }?.let {
            buildLayout(it, createTitlePaint(), contentWidth, 0f, TITLE_LINE_SPACING)
        }
        val cleanBody = trimTrailingNewlines(body)
        val bodyPaint = createBodyPaint()
        val bodyLayout = cleanBody.takeIf { it.isNotBlank() }?.let { text ->
            val fm = bodyPaint.fontMetrics
            val naturalLineHeight = fm.descent - fm.ascent
            val spacingAdd = (BODY_TARGET_LINE_HEIGHT - naturalLineHeight).coerceAtLeast(MIN_LINE_SPACING_ADD)
            buildLayout(text, bodyPaint, contentWidth, spacingAdd, 1f)
        }

        val specs = paginateToSeparatePdfs(
            titleLayout = titleLayout,
            bodyLayout = bodyLayout,
            cleanBody = cleanBody,
            contentHeight = contentHeight,
            nominalPageHeight = nominalPageHeight,
            isContentScrollable = isContentScrollable
        )

        val sourceTitle = sourceTitleFor(title)
        return specs.mapIndexed { index, spec ->
            drawSinglePdfDocument(
                spec = spec,
                pageWidth = pageWidth,
                contentWidth = contentWidth.toFloat(),
                sourceTitle = sourceTitle,
                partIndex = index,
                totalParts = specs.size
            )
        }
    }

    private fun paginateToSeparatePdfs(
        titleLayout: StaticLayout?,
        bodyLayout: StaticLayout?,
        cleanBody: CharSequence,
        contentHeight: Float,
        nominalPageHeight: Int,
        isContentScrollable: Boolean?
    ): List<PdfPageSpec> {
        val contentBottom = PAGE_MARGIN + contentHeight

        fun appendHeader(elements: MutableList<PageElement>, hasBody: Boolean): Float {
            var cursorY = PAGE_MARGIN
            if (titleLayout != null) {
                for (line in 0 until titleLayout.lineCount) {
                    val lineH = (titleLayout.getLineBottom(line) - titleLayout.getLineTop(line)).toFloat()
                    elements += PageElement.LineSlice(titleLayout, line, cursorY)
                    cursorY += lineH
                }
                if (hasBody) {
                    cursorY += TITLE_GAP_BEFORE_DIVIDER
                    elements += PageElement.Divider(cursorY)
                    cursorY += TITLE_GAP_AFTER_DIVIDER
                }
            }
            return cursorY
        }

        if (bodyLayout == null) {
            val elements = mutableListOf<PageElement>()
            val endY = appendHeader(elements, hasBody = false)
            val height = maxOf(nominalPageHeight, (endY + PAGE_MARGIN + FOOTER_RESERVED).roundToInt())
            return listOf(PdfPageSpec(elements, height))
        }

        var effectiveLineCount = bodyLayout.lineCount
        while (effectiveLineCount > 0) {
            val idx = effectiveLineCount - 1
            val lineText = cleanBody.subSequence(
                bodyLayout.getLineStart(idx),
                bodyLayout.getLineEnd(idx)
            ).toString()
            if (lineText.isNotBlank()) break
            effectiveLineCount--
        }

        if (effectiveLineCount == 0) {
            val elements = mutableListOf<PageElement>()
            val endY = appendHeader(elements, hasBody = false)
            val height = maxOf(nominalPageHeight, (endY + PAGE_MARGIN + FOOTER_RESERVED).roundToInt())
            return listOf(PdfPageSpec(elements, height))
        }

        if (isContentScrollable == false) {
            val elements = mutableListOf<PageElement>()
            var cursorY = appendHeader(elements, hasBody = true)
            for (line in 0 until effectiveLineCount) {
                val lineH = (bodyLayout.getLineBottom(line) - bodyLayout.getLineTop(line)).toFloat()
                elements += PageElement.LineSlice(bodyLayout, line, cursorY)
                cursorY += lineH
            }
            val height = maxOf(nominalPageHeight, (cursorY + PAGE_MARGIN + FOOTER_RESERVED).roundToInt())
            return listOf(PdfPageSpec(elements, height))
        }

        val specs = mutableListOf<PdfPageSpec>()
        var lineIndex = 0
        while (lineIndex < effectiveLineCount) {
            val elements = mutableListOf<PageElement>()
            var cursorY = appendHeader(elements, hasBody = true)
            var placedBodyLines = 0
            while (lineIndex < effectiveLineCount) {
                val lineH = (bodyLayout.getLineBottom(lineIndex) - bodyLayout.getLineTop(lineIndex)).toFloat()
                if (placedBodyLines > 0 && cursorY + lineH > contentBottom) {
                    break
                }
                elements += PageElement.LineSlice(bodyLayout, lineIndex, cursorY)
                cursorY += lineH
                placedBodyLines++
                lineIndex++
            }
            val height = maxOf(nominalPageHeight, (cursorY + PAGE_MARGIN + FOOTER_RESERVED).roundToInt())
            specs += PdfPageSpec(elements, height)
        }
        return specs
    }

    private fun drawSinglePdfDocument(
        spec: PdfPageSpec,
        pageWidth: Int,
        contentWidth: Float,
        sourceTitle: String,
        partIndex: Int,
        totalParts: Int
    ): ByteArray {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = DIVIDER_STROKE_WIDTH
        }
        val footerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = FOOTER_DIVIDER_STROKE_WIDTH
        }
        val footerLeftPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_FOOTER
            textSize = FOOTER_TEXT_SIZE
            textAlign = Paint.Align.LEFT
        }
        val footerRightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_FOOTER
            textSize = FOOTER_TEXT_SIZE
            textAlign = Paint.Align.RIGHT
        }

        val document = PdfDocument()
        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, spec.pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            spec.elements.forEach { element ->
                when (element) {
                    is PageElement.LineSlice -> drawLineSlice(canvas, element, pageWidth)
                    is PageElement.Divider -> canvas.drawLine(
                        PAGE_MARGIN,
                        element.y,
                        pageWidth - PAGE_MARGIN,
                        element.y,
                        dividerPaint
                    )
                }
            }

            drawFooter(
                canvas = canvas,
                pageWidth = pageWidth,
                pageHeight = spec.pageHeight,
                contentWidth = contentWidth,
                sourceTitle = sourceTitle,
                partIndex = partIndex,
                totalParts = totalParts,
                dividerPaint = footerDividerPaint,
                leftPaint = footerLeftPaint,
                rightPaint = footerRightPaint
            )

            document.finishPage(page)
            val output = ByteArrayOutputStream()
            document.writeTo(output)
            output.toByteArray()
        } finally {
            document.close()
        }
    }

    @Suppress("LongParameterList")
    private fun drawFooter(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        contentWidth: Float,
        sourceTitle: String,
        partIndex: Int,
        totalParts: Int,
        dividerPaint: Paint,
        leftPaint: TextPaint,
        rightPaint: TextPaint
    ) {
        val footerDividerY = pageHeight - PAGE_MARGIN - FOOTER_DIVIDER_OFFSET
        canvas.drawLine(PAGE_MARGIN, footerDividerY, pageWidth - PAGE_MARGIN, footerDividerY, dividerPaint)

        val baselineY = pageHeight - PAGE_MARGIN + FOOTER_BASELINE_OFFSET
        val pageLabel = "Page ${partIndex + 1} of $totalParts"
        val rightWidth = rightPaint.measureText(pageLabel)
        val maxTitleWidth = (contentWidth - rightWidth - FOOTER_LABEL_GAP).coerceAtLeast(MIN_FOOTER_TITLE_WIDTH)
        val ellipsizedTitle = TextUtils.ellipsize(
            sourceTitle,
            leftPaint,
            maxTitleWidth,
            TextUtils.TruncateAt.END
        ).toString()

        canvas.drawText(ellipsizedTitle, PAGE_MARGIN, baselineY, leftPaint)
        canvas.drawText(pageLabel, pageWidth - PAGE_MARGIN, baselineY, rightPaint)
    }

    private fun drawLineSlice(canvas: Canvas, slice: PageElement.LineSlice, pageWidth: Int) {
        val top = slice.layout.getLineTop(slice.lineIndex).toFloat()
        val bottom = slice.layout.getLineBottom(slice.lineIndex).toFloat()
        canvas.save()
        canvas.clipRect(PAGE_MARGIN, slice.y, pageWidth - PAGE_MARGIN, slice.y + (bottom - top))
        canvas.translate(PAGE_MARGIN, slice.y - top)
        slice.layout.draw(canvas)
        canvas.restore()
    }

    private fun buildLayout(
        text: CharSequence,
        paint: TextPaint,
        contentWidth: Int,
        spacingAdd: Float,
        spacingMultiplier: Float
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(spacingAdd, spacingMultiplier)
            .setIncludePad(false)
            .build()

    private fun createTitlePaint(): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = TITLE_TEXT_SIZE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun createBodyPaint(): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_BODY
        textSize = BODY_TEXT_SIZE
        typeface = Typeface.DEFAULT
    }
}
