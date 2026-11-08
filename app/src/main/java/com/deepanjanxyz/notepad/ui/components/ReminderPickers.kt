package com.deepanjanxyz.notepad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** One of the quick reminder times offered above the custom picker. */
private data class ReminderPreset(val label: String, val millis: Long)

/** The millis for [dayOffset] days from now, at [hourOfDay] on the hour. */
private fun calendarAt(hourOfDay: Int, dayOffset: Int): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** The quick reminder times: later today, tomorrow morning and evening, next week. */
private fun reminderPresets(): List<ReminderPreset> {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val laterTodayHour = if (currentHour < 18) 18 else (currentHour + 2).coerceAtMost(23)
    return listOf(
        ReminderPreset(
            label = if (currentHour < 18) "Later Today (6:00 PM)" else "Later Tonight",
            millis = calendarAt(hourOfDay = laterTodayHour, dayOffset = 0)
        ),
        ReminderPreset(
            label = "Tomorrow Morning (9:00 AM)",
            millis = calendarAt(hourOfDay = 9, dayOffset = 1)
        ),
        ReminderPreset(
            label = "Tomorrow Evening (6:00 PM)",
            millis = calendarAt(hourOfDay = 18, dayOffset = 1)
        ),
        ReminderPreset(
            label = "Next Week (9:00 AM)",
            millis = calendarAt(hourOfDay = 9, dayOffset = 7)
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickPresetsRow(selectedMillis: Long, onSelect: (Long) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        reminderPresets().forEach { preset ->
            PresetChip(
                label = preset.label,
                onClick = { onSelect(preset.millis) },
                isSelected = Math.abs(selectedMillis - preset.millis) < 60_000
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** The date and time picker dialogs, shown when they are asked for. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderPickers(
    showDatePicker: Boolean,
    showTimePicker: Boolean,
    selectedMillis: Long,
    onSelect: (Long) -> Unit,
    onDismissDate: () -> Unit,
    onDismissTime: () -> Unit
) {
    if (showDatePicker) {
        ReminderDatePickerDialog(
            selectedMillis = selectedMillis,
            onSelect = onSelect,
            onDismiss = onDismissDate
        )
    }
    if (showTimePicker) {
        ReminderTimePickerDialog(
            selectedMillis = selectedMillis,
            onSelect = onSelect,
            onDismiss = onDismissTime
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDatePickerDialog(
    selectedMillis: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialCal.timeInMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { newDateUtcMillis ->
                        // Preserve time of day
                        val utcCal = Calendar.getInstance().apply { timeInMillis = newDateUtcMillis }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        }
                        onSelect(newCal.timeInMillis)
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("date_picker_ok_button")
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    selectedMillis: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
    val timePickerState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = selectedMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelect(newCal.timeInMillis)
                    onDismiss()
                },
                modifier = Modifier.testTag("time_picker_ok_button")
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                "Select Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}
