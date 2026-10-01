package com.deepanjanxyz.notepad.domain.usecase.note

data class NoteUseCases(
    val getNotes: GetNotesUseCase,
    val getArchiveNotes: GetArchiveNotesUseCase,
    val getTrashNotes: GetTrashNotesUseCase,
    val getNoteById: GetNoteByIdUseCase,
    val saveNote: SaveNoteUseCase,
    val trashNote: TrashNoteUseCase,
    val restoreNote: RestoreNoteUseCase,
    val permanentlyDeleteNote: PermanentlyDeleteNoteUseCase,
    val emptyTrash: EmptyTrashUseCase,
    val togglePin: TogglePinUseCase,
    val archiveNote: ArchiveNoteUseCase,
    val filterNotes: FilterNotesUseCase,
    val setNoteReminder: SetNoteReminderUseCase
)
