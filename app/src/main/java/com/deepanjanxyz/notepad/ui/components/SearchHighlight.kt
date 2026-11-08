package com.deepanjanxyz.notepad.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.Locale

private val SearchHighlightColors = listOf(
    Color(0xFFFFE082), // Amber
    Color(0xFF9AE6FF), // Sky
    Color(0xFFA5F0B4), // Mint
    Color(0xFFFFB3C7), // Rose
    Color(0xFFD6B6FF), // Lilac
    Color(0xFFFFC79A) // Apricot
)

private val SearchHighlightTextColor = Color(0xFF1B1B1F)

/**
 * The [text] with every occurrence of a word in [query] highlighted, each keyword
 * taking the next swatch from [colors].
 */
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

        val merged = if (keywords.isEmpty()) {
            emptyList()
        } else {
            mergeOverlappingMatches(findKeywordMatches(text, keywords))
        }

        if (merged.isEmpty()) {
            return@remember AnnotatedString(text)
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
 * Every place each of [keywords] occurs in [text], as (start, end, keyword index)
 * triples. The keyword index is kept so that a match's colour follows the keyword
 * rather than its position in the note.
 */
private fun findKeywordMatches(text: String, keywords: List<String>): List<Triple<Int, Int, Int>> {
    val lowerText = text.lowercase(Locale.getDefault())
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
    return matches
}

/**
 * Sorts the matches and merges only the overlapping ones (a repeated short
 * keyword, say); the first keyword keeps the colour of the merged span.
 */
private fun mergeOverlappingMatches(
    matches: List<Triple<Int, Int, Int>>
): List<Triple<Int, Int, Int>> {
    val merged = mutableListOf<Triple<Int, Int, Int>>()
    for (match in matches.sortedBy { it.first }) {
        val last = merged.lastOrNull()
        if (last != null && match.first < last.second) {
            if (match.second > last.second) {
                merged[merged.size - 1] = Triple(last.first, match.second, last.third)
            }
        } else {
            merged.add(match)
        }
    }
    return merged
}
