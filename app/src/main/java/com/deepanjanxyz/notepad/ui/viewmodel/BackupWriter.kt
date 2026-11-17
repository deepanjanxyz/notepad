package com.deepanjanxyz.notepad.ui.viewmodel

import android.app.Application
import android.util.Log
import com.deepanjanxyz.notepad.backup.BackupFolder
import com.deepanjanxyz.notepad.backup.BackupParseResult
import com.deepanjanxyz.notepad.backup.DownloadsBackup
import com.deepanjanxyz.notepad.backup.buildBackupJson
import com.deepanjanxyz.notepad.backup.isBackupWorthy
import com.deepanjanxyz.notepad.backup.parseBackupJson
import com.deepanjanxyz.notepad.domain.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "BackupWriter"

/** How long a burst of changes is left to settle before the backup is written. */
private const val AUTO_SAVE_DELAY_MS = 600L

/**
 * Writes the JSON backup to Downloads, on the manual action and on every change
 * while automatic backup is on, and keeps the outcome the screen reports.
 *
 * The file work lives here so the view model only hands over the notes and the
 * place to put them back.
 */
class BackupWriter(
    private val context: Application,
    private val scope: CoroutineScope,
    private val backupNotes: StateFlow<List<Note>>,
    private val currentNotes: suspend () -> List<Note>,
    private val importNotes: suspend (List<Note>) -> Unit
) {

    private val _status = MutableStateFlow<AutoBackupStatus?>(null)

    /** The outcome of the most recent backup, for the screen to report. */
    val status: StateFlow<AutoBackupStatus?> = _status.asStateFlow()

    private var autoBackupJob: Job? = null

    // The automatic backup and "Back up now" write the same file from their own
    // coroutines, so they take turns.
    private val writeMutex = Mutex()

    /**
     * Starts or stops the automatic backup to match the persisted setting. While it
     * is on, the file is rewritten whenever the set of notes changes.
     */
    fun followSetting(enabled: Boolean) {
        if (!enabled) {
            autoBackupJob?.cancel()
            autoBackupJob = null
            return
        }
        if (autoBackupJob?.isActive == true) return
        autoBackupJob = scope.launch {
            // Somewhere to write even when there is nothing to write yet.
            withContext(Dispatchers.IO) { DownloadsBackup.ensureFolder(context) }
            writeAutoBackup()
            backupNotes
                // Only the empty value from before the database loads is skipped: a
                // change replayed while the first write runs is written too.
                .dropWhile { it.isEmpty() }
                .collectLatest {
                    delay(AUTO_SAVE_DELAY_MS)
                    writeAutoBackup()
                }
        }
    }

    /** Writes a backup now, used by the manual "Back up now" action. */
    fun backUp(name: String = DownloadsBackup.FILE_NAME) {
        scope.launch { write(name) }
    }

    private suspend fun writeAutoBackup() = write(DownloadsBackup.FILE_NAME)

    private suspend fun write(name: String) {
        writeMutex.withLock { writeLocked(name) }
    }

    private suspend fun writeLocked(name: String) {
        // A folder just handed over may already hold a backup written by an earlier
        // installation. Its notes come in before the file is replaced, and the write
        // is held back while they cannot be read.
        if (BackupFolder.hasAdoption(context)) {
            if (adoptExistingBackup()) {
                _status.value = AutoBackupStatus(
                    location = DownloadsBackup.locationLabel(name),
                    lastModified = null,
                    error = "The backup already in the folder could not be read; " +
                        "delete it in Downloads to back up again"
                )
                return
            }
            BackupFolder.finishAdoption(context)
        }
        // An empty backup is never written: the last one holding notes is kept.
        val notes = currentNotes().filter { it.isBackupWorthy() }
        if (notes.isEmpty()) {
            Log.d(TAG, "Skipped the backup: there are no notes to store")
            return
        }
        runCatching {
            withContext(Dispatchers.IO) {
                DownloadsBackup.write(context, buildBackupJson(notes), name)
            }
        }.onSuccess {
            _status.value = AutoBackupStatus(
                location = DownloadsBackup.locationLabel(name),
                lastModified = System.currentTimeMillis()
            )
        }.onFailure { error ->
            Log.w(TAG, "Backup to Downloads failed", error)
            _status.value = AutoBackupStatus(
                location = DownloadsBackup.locationLabel(name),
                lastModified = null,
                error = error.message
            )
        }
    }

    /**
     * Brings in the notes of a backup already in the folder. True when that backup
     * cannot be read, which is the one case where the write has to wait.
     */
    private suspend fun adoptExistingBackup(): Boolean {
        // The file the earlier installation left, not the newest one: the newest may
        // be a copy this app wrote itself.
        val name = DownloadsBackup.FILE_NAME
        val present = withContext(Dispatchers.IO) {
            DownloadsBackup.list(context).any { it.name == name }
        }
        val text = withContext(Dispatchers.IO) { DownloadsBackup.read(context, name) }
        return when {
            // Nothing under the name and nothing under a numbered copy: no backup.
            text == null && !present -> false
            // There but unreadable: writing over it would throw away notes this app
            // cannot see, so the write is held back.
            text == null -> true
            else -> when (val parsed = parseBackupJson(text)) {
                is BackupParseResult.Success -> {
                    importNotes(parsed.notes)
                    false
                }
                is BackupParseResult.Failure -> {
                    Log.w(TAG, "The backup already in the folder was not read: ${parsed.reason}")
                    true
                }
            }
        }
    }
}
