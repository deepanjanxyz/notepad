package com.deepanjanxyz.notepad.domain.usecase.note

import com.deepanjanxyz.notepad.domain.model.Note
import com.deepanjanxyz.notepad.domain.util.NoteFilter

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
