package com.deepanjanxyz.notepad.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.deepanjanxyz.notepad.worker.NoteReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    currentReminderTime: Long?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val defaultInitialMillis = remember(currentReminderTime) {
        defaultReminderMillis(currentReminderTime)
    }

    var selectedMillis by remember { mutableLongStateOf(defaultInitialMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Still proceed so the user can see the reminder scheduled in the app.
        onSetReminder(selectedMillis)
        onDismiss()
    }

    fun submitReminder() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onSetReminder(selectedMillis)
            onDismiss()
        }
    }

    ReminderDialogContent(
        currentReminderTime = currentReminderTime,
        selectedMillis = selectedMillis,
        onSelectedMillisChange = { selectedMillis = it },
        onPickDate = { showDatePicker = true },
        onPickTime = { showTimePicker = true },
        onSubmit = { submitReminder() },
        onClearReminder = onClearReminder,
        onDismiss = onDismiss
    )

    ReminderPickers(
        showDatePicker = showDatePicker,
        showTimePicker = showTimePicker,
        selectedMillis = selectedMillis,
        onSelect = { selectedMillis = it },
        onDismissDate = { showDatePicker = false },
        onDismissTime = { showTimePicker = false }
    )
}

/** The existing reminder if it is still ahead, otherwise an hour from now on the hour. */
private fun defaultReminderMillis(currentReminderTime: Long?): Long =
    if (currentReminderTime != null && currentReminderTime > System.currentTimeMillis()) {
        currentReminderTime
    } else {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

/** The reminder dialog itself. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReminderDialogContent(
    currentReminderTime: Long?,
    selectedMillis: Long,
    onSelectedMillisChange: (Long) -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onSubmit: () -> Unit,
    onClearReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedDateText = remember(selectedMillis) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(selectedMillis))
    }
    val selectedTimeText = remember(selectedMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(selectedMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("reminder_dialog"),
        title = { ReminderTitleRow(isEditing = currentReminderTime != null) },
        text = {
            ReminderDialogBody(
                selectedMillis = selectedMillis,
                selectedDateText = selectedDateText,
                selectedTimeText = selectedTimeText,
                onSelect = onSelectedMillisChange,
                onPickDate = onPickDate,
                onPickTime = onPickTime
            )
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                modifier = Modifier.testTag("reminder_save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            ReminderDismissButtons(
                isEditing = currentReminderTime != null,
                onClearReminder = onClearReminder,
                onDismiss = onDismiss
            )
        }
    )
}

/** The bell, and the title that says whether a reminder is being added or changed. */
@Composable
private fun ReminderTitleRow(isEditing: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = if (isEditing) "Edit Reminder" else "Add Reminder",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** The presets, the custom pickers and the summary. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderDialogBody(
    selectedMillis: Long,
    selectedDateText: String,
    selectedTimeText: String,
    onSelect: (Long) -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Get notified at the scheduled date and time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ReminderSectionLabel("Quick Presets")

        QuickPresetsRow(selectedMillis = selectedMillis, onSelect = onSelect)

        Spacer(modifier = Modifier.height(4.dp))

        ReminderSectionLabel("Custom Date & Time")

        DateTimePickerRow(
            selectedDateText = selectedDateText,
            selectedTimeText = selectedTimeText,
            onPickDate = onPickDate,
            onPickTime = onPickTime
        )

        ReminderSummary(selectedMillis = selectedMillis)
    }
}

@Composable
private fun ReminderSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/** The date and time buttons side by side. */
@Composable
private fun DateTimePickerRow(
    selectedDateText: String,
    selectedTimeText: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DateTimeButton(
            label = "Date",
            value = selectedDateText,
            icon = Icons.Default.CalendarMonth,
            testTag = "reminder_pick_date_button",
            onClick = onPickDate
        )
        DateTimeButton(
            label = "Time",
            value = selectedTimeText,
            icon = Icons.Default.AccessTime,
            testTag = "reminder_pick_time_button",
            onClick = onPickTime
        )
    }
}

@Composable
private fun RowScope.DateTimeButton(
    label: String,
    value: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** The line that repeats when the reminder is set for. */
@Composable
private fun ReminderSummary(selectedMillis: Long) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Reminder: ${NoteReminderScheduler.formatReminderDateTime(selectedMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Delete, when there is a reminder to delete, and Cancel. */
@Composable
private fun ReminderDismissButtons(
    isEditing: Boolean,
    onClearReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isEditing) {
            TextButton(
                onClick = {
                    onClearReminder()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("reminder_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("reminder_cancel_button")
        ) {
            Text("Cancel")
        }
    }
}

