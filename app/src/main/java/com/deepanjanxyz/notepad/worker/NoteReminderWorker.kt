package com.deepanjanxyz.notepad.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepanjanxyz.notepad.MainActivity
import com.deepanjanxyz.notepad.R
import com.deepanjanxyz.notepad.data.local.database.AppDatabase
import com.deepanjanxyz.notepad.domain.model.DrawingSerializer

class NoteReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "note_reminders_channel"
        const val CHANNEL_NAME = "Note Reminders"
        const val KEY_NOTE_ID = "key_note_id"
        const val KEY_NOTE_TITLE = "key_note_title"
        const val KEY_NOTE_CONTENT = "key_note_content"

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

        val db = AppDatabase.getInstance(applicationContext)
        val noteEntity = db.noteDao().getNoteById(noteId)

        // If note was deleted or moved to trash, don't show notification
        if (noteEntity == null || noteEntity.inTrash) {
            return Result.success()
        }

        val note = noteEntity.toDomain()

        // Create channel on Oreo+
        createNotificationChannel(applicationContext)

        val displayTitle = when {
            note.title.isNotBlank() -> note.title
            inputData.getString(KEY_NOTE_TITLE)?.isNotBlank() == true -> inputData.getString(KEY_NOTE_TITLE)!!
            else -> applicationContext.getString(R.string.app_name)
        }

        val rawContent = if (note.content.isNotBlank()) {
            note.content
        } else {
            inputData.getString(KEY_NOTE_CONTENT) ?: ""
        }

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

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(cleanPreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanPreview))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(noteId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission might have been revoked on Android 13+
        }

        // Clear reminderTime in the database as it has now fired
        db.noteDao().updateReminderTime(noteId, null)

        return Result.success()
    }
}
