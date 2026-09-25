package com.deepanjanxyz.notepad.core.domain.util

import com.deepanjanxyz.notepad.core.model.DrawingSerializer
import com.deepanjanxyz.notepad.core.model.Note
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
                val contentLower = if (isDrawing) "" else note.content.lowercase(Locale.getDefault())
                val tagsLower = note.tags.map { it.lowercase(Locale.getDefault()) }

                // Check direct substring match first for exact phrase
                if (titleLower.contains(cleanQuery) ||
                    contentLower.contains(cleanQuery) ||
                    tagsLower.any { it.contains(cleanQuery) }
                ) {
                    true
                } else {
                    // Check that all individual keywords are present in either title, content, or tags
                    keywords.all { kw ->
                        titleLower.contains(kw) ||
                                contentLower.contains(kw) ||
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
