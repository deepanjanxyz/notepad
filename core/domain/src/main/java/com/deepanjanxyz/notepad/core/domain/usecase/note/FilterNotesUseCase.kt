package com.deepanjanxyz.notepad.core.domain.usecase.note

import com.deepanjanxyz.notepad.core.model.Note
import com.deepanjanxyz.notepad.core.domain.util.NoteFilter

class FilterNotesUseCase {
    operator fun invoke(
        notes: List<Note>,
        query: String = "",
        colorFilter: Int? = null,
        tagFilter: String? = null
    ): List<Note> {
        return NoteFilter.filterNotes(
            notes = notes,
            query = query,
            colorFilter = colorFilter,
            tagFilter = tagFilter
        )
    }
}
