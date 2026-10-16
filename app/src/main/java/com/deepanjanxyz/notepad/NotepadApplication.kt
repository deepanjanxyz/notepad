package com.deepanjanxyz.notepad

import android.app.Application
import com.deepanjanxyz.notepad.backup.DownloadsBackup
import com.deepanjanxyz.notepad.data.NoteRepositoryProvider
import com.deepanjanxyz.notepad.data.SettingsRepositoryProvider
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository
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
import com.deepanjanxyz.notepad.domain.usecase.note.PurgeExpiredTrashUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.RestoreNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.SaveNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.SetNoteReminderUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.TogglePinUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.TrashNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.settings.GetSettingsUseCase
import com.deepanjanxyz.notepad.domain.usecase.settings.SaveSettingsUseCase
import com.deepanjanxyz.notepad.domain.usecase.settings.SettingsUseCases
import com.deepanjanxyz.notepad.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class NotepadApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Starts the background work the app needs for the whole process lifetime:
     * keeping the home-screen widget in sync with the pinned notes, purging
     * trash that has aged out, and clearing stray backup copies left in
     * Downloads by earlier versions.
     */
    override fun onCreate() {
        super.onCreate()

        // The home-screen widget mirrors the pinned checklist notes, so it is
        // rebuilt whenever the active notes change - a pin, unpin, edit, delete
        // or archive is on the home screen without waiting for the app to close.
        // A short pause after each change collapses a burst of edits into one
        // rebuild.
        appScope.launch {
            container.noteUseCases.getNotes()
                .drop(1)
                .conflate()
                .collect {
                    delay(WIDGET_SYNC_DEBOUNCE_MS)
                    TaskWidgetProvider.refresh(this@NotepadApplication)
                }
        }

        // Deleted notes are kept for a fixed window and then dropped, so the
        // purge runs once per launch rather than on every trash action.
        CoroutineScope(Dispatchers.IO).launch { container.noteUseCases.purgeExpiredTrash() }

        // Earlier versions could leave numbered copies of the backup file in
        // Downloads; clear them out once the real backup is present so the folder
        // holds one file again.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { DownloadsBackup.cleanUpCopies(this@NotepadApplication) }
        }
    }

    private companion object {
        /** How long to wait for further note changes before rebuilding the widget. */
        const val WIDGET_SYNC_DEBOUNCE_MS = 400L
    }
}

/**
 * Manual dependency container for the app.
 *
 * The repositories are built once here and the domain use cases are wired on top
 * of them, so the presentation layer depends only on the domain layer and never
 * reaches into the data layer directly.
 */
class AppContainer(application: Application) {
    private val repository: NoteRepository = NoteRepositoryProvider.create(application)
    private val settingsRepository: SettingsRepository = SettingsRepositoryProvider.create(application)

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
        purgeExpiredTrash = PurgeExpiredTrashUseCase(repository),
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

    val settingsUseCases: SettingsUseCases = SettingsUseCases(
        getSettings = GetSettingsUseCase(settingsRepository),
        saveSettings = SaveSettingsUseCase(settingsRepository)
    )
}
