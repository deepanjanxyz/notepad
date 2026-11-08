package com.deepanjanxyz.notepad.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepanjanxyz.notepad.MainActivity
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.NoteRepositoryProvider
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer
import com.deepanjanxyz.notepad.domain.model.Note

private const val TAG = "NoteReminderWorker"

class NoteReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "note_reminders_channel"
        const val CHANNEL_NAME = "Note Reminders"
        const val KEY_NOTE_ID = "key_note_id"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for notes and scheduled tasks"
                    enableLights(true)
                    enableVibration(true)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (noteId == -1L) {
            return Result.failure()
        }
        return deliver(noteId)
    }

    /**
     * Posts the reminder for [noteId], and reports whether it was delivered.
     *
     * The reminder is cleared only once the notification has actually been shown,
     * so anything that stops it being seen leaves the reminder in place to fire
     * again later.
     */
    private suspend fun deliver(noteId: Long): Result {
        val repository = NoteRepositoryProvider.create(applicationContext)
        val note = repository.getNoteById(noteId)

        // If note was deleted or moved to trash, don't show notification
        if (note == null || note.inTrash) {
            return Result.success()
        }

        val shown = showReminder(note, noteId)
        if (shown) {
            // The notification was posted, so the reminder has now fired.
            repository.updateReminderTime(noteId, null)
        }
        return if (shown) Result.success() else Result.retry()
    }

    /**
     * Posts the notification for [note]. Returns false when nothing would be seen,
     * which is when the reminder has to be kept: it is still owed to the user, and
     * retrying means it fires once notifications are allowed again.
     */
    private fun showReminder(note: Note, noteId: Long): Boolean {
        // Create channel on Oreo+
        createNotificationChannel(applicationContext)

        val manager = NotificationManagerCompat.from(applicationContext)
        return canDeliver(manager) && post(manager, noteId, buildNotification(note, noteId))
    }

    /** Posts the notification, reporting false when the post was refused. */
    private fun post(
        manager: NotificationManagerCompat,
        noteId: Long,
        notification: Notification
    ): Boolean = try {
        manager.notify(noteId.toInt(), notification)
        true
    } catch (e: SecurityException) {
        // The permission was revoked between the check and the post, so again
        // nothing was shown and the reminder is kept.
        Log.w(TAG, "The reminder notification could not be posted", e)
        false
    }

    /** Builds the reminder notification for [note]. */
    private fun buildNotification(note: Note, noteId: Long): Notification {
        // The note as it stands now, not as it stood when the reminder was set:
        // text the user has since removed must not come back in a notification.
        val displayTitle = note.title.takeIf { it.isNotBlank() }
            ?: applicationContext.getString(R.string.app_name)

        val rawContent = note.content

        val isDrawing = DrawingSerializer.isDrawing(rawContent)
        val cleanPreview = when {
            isDrawing -> "🎨 Drawing note reminder"
            rawContent.isNotBlank() -> {
                rawContent.lines()
                    .map { line ->
                        line.replace(Regex("^\\[[ xX]\\]\\s*"), "• ")
                    }
                    .filter { it.isNotBlank() }
                    .take(4)
                    .joinToString("\n")
            }
            else -> "Reminder for your saved note"
        }

        // Tap intent to open MainActivity with specific note
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_note_id", noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(cleanPreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanPreview))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    /**
     * True when a notification posted now would actually be seen: the app's
     * notifications are allowed and its channel has not been switched off.
     *
     * On Android 13 and above a revoked permission makes notify() a silent no-op
     * rather than throwing, so this has to be asked before posting, not after.
     */
    private fun canDeliver(manager: NotificationManagerCompat): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        val channelBlocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        return !channelBlocked
    }
}
