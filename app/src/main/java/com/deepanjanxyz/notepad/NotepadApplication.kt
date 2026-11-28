package com.deepanjanxyz.notepad

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.deepanjanxyz.notepad.backup.DownloadsBackup
import com.deepanjanxyz.notepad.data.NoteRepositoryProvider
import com.deepanjanxyz.notepad.data.NoteShortcutRepositoryProvider
import com.deepanjanxyz.notepad.data.SettingsRepositoryProvider
import com.deepanjanxyz.notepad.domain.repository.NoteRepository
import com.deepanjanxyz.notepad.domain.repository.NoteShortcutRepository
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository
import com.deepanjanxyz.notepad.domain.usecase.label.AddLabelUseCase
import com.deepanjanxyz.notepad.domain.usecase.label.DeleteLabelUseCase
import com.deepanjanxyz.notepad.domain.usecase.label.GetLabelsUseCase
import com.deepanjanxyz.notepad.domain.usecase.label.LabelUseCases
import com.deepanjanxyz.notepad.domain.usecase.label.RenameLabelUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.ArchiveNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.note.BulkUpdateNotesUseCase
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
import com.deepanjanxyz.notepad.domain.usecase.widget.GetLinkedNoteIdUseCase
import com.deepanjanxyz.notepad.domain.usecase.widget.LinkNoteUseCase
import com.deepanjanxyz.notepad.domain.usecase.widget.NoteShortcutUseCases
import com.deepanjanxyz.notepad.domain.usecase.widget.UnlinkNoteUseCase
import com.deepanjanxyz.notepad.widget.NoteShortcutWidgetProvider
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

    internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // The home-screen widgets mirror the notes, so they are rebuilt whenever
        // the active notes change - a pin, unpin, edit, delete or archive is on the
        // home screen without waiting for the app to close. A short pause after
        // each change collapses a burst of edits into one rebuild.
        appScope.launch {
            container.noteUseCases.getNotes()
                .drop(1)
                .conflate()
                .collect {
                    delay(WIDGET_SYNC_DEBOUNCE_MS)
                    TaskWidgetProvider.refresh(this@NotepadApplication)
                    NoteShortcutWidgetProvider.refresh(this@NotepadApplication)
                }
        }

        // A shortcut widget reads the portrait and the landscape size the host
        // reports, so a rotation changes which pair applies and the widget has to
        // be redrawn at its new size. The host sends no resize for this, so the
        // app listens for the configuration change itself.
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    NoteShortcutWidgetProvider.refresh(context)
                }
            },
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

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
    private val shortcutRepository: NoteShortcutRepository =
        NoteShortcutRepositoryProvider.create(application)

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
        setNoteReminder = SetNoteReminderUseCase(repository),
        bulkUpdate = BulkUpdateNotesUseCase(repository)
    )

    val labelUseCases: LabelUseCases = LabelUseCases(
        getLabels = GetLabelsUseCase(repository),
        addLabel = AddLabelUseCase(repository),
        renameLabel = RenameLabelUseCase(repository),
        deleteLabel = DeleteLabelUseCase(repository)
    )

    val noteShortcutUseCases: NoteShortcutUseCases = NoteShortcutUseCases(
        getLinkedNoteId = GetLinkedNoteIdUseCase(shortcutRepository),
        linkNote = LinkNoteUseCase(shortcutRepository),
        unlinkNote = UnlinkNoteUseCase(shortcutRepository)
    )

    val settingsUseCases: SettingsUseCases = SettingsUseCases(
        getSettings = GetSettingsUseCase(settingsRepository),
        saveSettings = SaveSettingsUseCase(settingsRepository)
    )
}
