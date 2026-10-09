package com.deepanjanxyz.notepad.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object NoteReminderScheduler {

    // SimpleDateFormat is not thread-safe, so each thread keeps its own cached
    // instance instead of allocating a fresh formatter on every call. This is
    // invoked while composing every note card that carries a reminder.
    private val timeFormatter: ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    private val dayFormatter: ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial { SimpleDateFormat("MMM d, ", Locale.getDefault()) }
    private val fullDateFormatter: ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial { SimpleDateFormat("MMM d, yyyy, ", Locale.getDefault()) }

    fun getWorkName(noteId: Long): String = "note_reminder_$noteId"

    fun scheduleReminder(
        context: Context,
        noteId: Long,
        noteTitle: String,
        noteContent: String,
        triggerAtMillis: Long
    ) {
        val delayMillis = triggerAtMillis - System.currentTimeMillis()
        val effectiveDelay = delayMillis.coerceAtLeast(0L)

        val inputData = Data.Builder()
            .putLong(NoteReminderWorker.KEY_NOTE_ID, noteId)
            .putString(NoteReminderWorker.KEY_NOTE_TITLE, noteTitle)
            .putString(NoteReminderWorker.KEY_NOTE_CONTENT, noteContent)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NoteReminderWorker>()
            .setInitialDelay(effectiveDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_note_$noteId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkName(noteId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, noteId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkName(noteId))
    }

    fun formatReminderDateTime(millis: Long): String {
        val calendarTarget = Calendar.getInstance().apply { timeInMillis = millis }
        val calendarNow = Calendar.getInstance()

        val timeFormat = timeFormatter.get() ?: SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(millis))

        val isSameDay = calendarTarget.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR) &&
                calendarTarget.get(Calendar.DAY_OF_YEAR) == calendarNow.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = calendarTarget.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                calendarTarget.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> "Today, $formattedTime"
            isTomorrow -> "Tomorrow, $formattedTime"
            calendarTarget.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR) -> {
                val dayFormat = dayFormatter.get() ?: SimpleDateFormat("MMM d, ", Locale.getDefault())
                dayFormat.format(Date(millis)) + formattedTime
            }
            else -> {
                val fullFormat = fullDateFormatter.get() ?: SimpleDateFormat("MMM d, yyyy, ", Locale.getDefault())
                fullFormat.format(Date(millis)) + formattedTime
            }
        }
    }
}
