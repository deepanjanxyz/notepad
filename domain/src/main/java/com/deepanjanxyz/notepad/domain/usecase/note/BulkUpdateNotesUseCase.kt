package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.repository.NoteRepository

private const val MAX_TAG_LENGTH = 30

class BulkUpdateNotesUseCase(private val repository: NoteRepository) {
    suspend fun setColor(ids: List<Long>, colorIndex: Int) {
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return
        repository.updateNotesColor(distinctIds, colorIndex.coerceAtLeast(0))
    }

    suspend fun setTag(ids: List<Long>, tag: String, enabled: Boolean) {
        val distinctIds = ids.distinct()
        val cleanTag = tag.replace("#", "").trim().take(MAX_TAG_LENGTH).trim()
        if (distinctIds.isEmpty() || cleanTag.isEmpty()) return
        repository.setTagForNotes(distinctIds, cleanTag, enabled)
    }
}
