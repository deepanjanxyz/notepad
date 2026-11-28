package com.deepanjanxyz.notepad.domain.util

import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.model.RichTextCodec
import java.util.Locale

object NoteFilter {

    /**
     * Filters notes based on a search query (keywords), color tint filter, and tag filter.
     * Matches keywords against note title, content, or tags.
     */
    fun filterNotes(
        notes: List<Note>,
        query: String,
        colorFilter: Int? = null,
        tagFilter: String? = null
    ): List<Note> {
        val cleanQuery = query.trim().lowercase(Locale.getDefault())
        val keywords = if (cleanQuery.isBlank()) {
            emptyList()
        } else {
            cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        }

        return notes.filter { note ->
            val matchesQuery = if (keywords.isEmpty()) {
                true
            } else {
                val titleLower = note.title.lowercase(Locale.getDefault())
                val isDrawing = DrawingSerializer.isDrawing(note.content)
                // A drawing keeps the caption the user typed inside its content,
                // after the serialized strokes. That text is the user's own and is
                // searched; the stroke data is not.
                val rawBody = if (isDrawing) {
                    DrawingSerializer.extractTextPart(note.content)
                } else {
                    note.content
                }
                val decoded = RichTextCodec.decode(rawBody)
                val contentLower = decoded.text.lowercase(Locale.getDefault())
                val linkUrlsLower = decoded.linkSpans
                    .joinToString(" ") { it.url }
                    .lowercase(Locale.getDefault())
                val tagsLower = note.tags.map { it.lowercase(Locale.getDefault()) }

                // Check direct substring match first for exact phrase
                if (titleLower.contains(cleanQuery) ||
                    contentLower.contains(cleanQuery) ||
                    linkUrlsLower.contains(cleanQuery) ||
                    tagsLower.any { it.contains(cleanQuery) }
                ) {
                    true
                } else {
                    // Check that all individual keywords are present in title, content, link URLs, or tags
                    keywords.all { kw ->
                        titleLower.contains(kw) ||
                            contentLower.contains(kw) ||
                            linkUrlsLower.contains(kw) ||
                            tagsLower.any { it.contains(kw) }
                    }
                }
            }

            val matchesColor = if (colorFilter == null) {
                true
            } else {
                note.colorIndex == colorFilter
            }

            val matchesTag = if (tagFilter == null) {
                true
            } else {
                note.tags.any { it.equals(tagFilter, ignoreCase = true) }
            }

            matchesQuery && matchesColor && matchesTag
        }
    }
}
