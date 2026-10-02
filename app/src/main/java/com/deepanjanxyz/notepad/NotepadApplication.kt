package com.deepanjanxyz.notepad

import android.app.Application
import com.deepanjanxyz.notepad.data.NoteRepositoryProvider
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import com.deepanjanxyz.notepad.domain.usecase.label.AddLabelUseCase
import com.deepanjanxyz.notepad.domain.usecase.label.DeleteLabelUseCase
import com.deepanjanxyz.notepad.domain.usecase.label.GetLabelsUseCase
import com.deepanjanxyz.notepad.domain.usecase.label.LabelUseCases
import com.deepanjanxyz.notepad.domain.usecase.label.RenameLabelUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.ArchiveNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.EmptyTrashUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.FilterNotesUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.GetArchiveNotesUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.GetNoteByIdUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.GetNotesUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.GetTrashNotesUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.NoteUseCases
import com.deepanjanxyz.notepad.domain.usecase.note.PermanentlyDeleteNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.RestoreNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.SaveNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.SetNoteReminderUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.TogglePinUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.TrashNoteUseCase

class NotepadApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * Manual dependency container for the app.
 *
 * The repository is built once here and the domain use cases are wired on top of
 * it, so the presentation layer depends only on the domain layer and never reaches
 * into the data layer directly.
 */
class AppContainer(application: Application) {
    private val repository: NoteRepository = NoteRepositoryProvider.create(application)

    val noteUseCases: NoteUseCases = NoteUseCases(
        getNotes = GetNotesUseCase(repository),
        getArchiveNotes = GetArchiveNotesUseCase(repository),
        getTrashNotes = GetTrashNotesUseCase(repository),
        getNoteById = GetNoteByIdUseCase(repository),
        saveNote = SaveNoteUseCase(repository),
        trashNote = TrashNoteUseCase(repository),
        restoreNote = RestoreNoteUseCase(repository),
        permanentlyDeleteNote = PermanentlyDeleteNoteUseCase(repository),
        emptyTrash = EmptyTrashUseCase(repository),
        togglePin = TogglePinUseCase(repository),
        archiveNote = ArchiveNoteUseCase(repository),
        filterNotes = FilterNotesUseCase(),
        setNoteReminder = SetNoteReminderUseCase(repository)
    )

    val labelUseCases: LabelUseCases = LabelUseCases(
        getLabels = GetLabelsUseCase(repository),
        addLabel = AddLabelUseCase(repository),
        renameLabel = RenameLabelUseCase(repository),
        deleteLabel = DeleteLabelUseCase(repository)
    )
}
