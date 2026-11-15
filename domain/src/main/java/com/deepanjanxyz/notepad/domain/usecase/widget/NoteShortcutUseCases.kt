package com.deepanjanxyz.notepad.domain.usecase.widget

data class NoteShortcutUseCases(
    val getLinkedNoteId: GetLinkedNoteIdUseCase,
    val linkNote: LinkNoteUseCase,
    val unlinkNote: UnlinkNoteUseCase
)
