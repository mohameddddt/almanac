package com.almanac.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(30.dp), color = p.surface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SET THE HOUR", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(20.dp))
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = p.card,
                        clockDialSelectedContentColor = p.onAccent,
                        clockDialUnselectedContentColor = p.inkSoft,
                        selectorColor = p.terracotta,
                        containerColor = p.surface,
                        timeSelectorSelectedContainerColor = p.ink,
                        timeSelectorUnselectedContainerColor = p.card,
                        timeSelectorSelectedContentColor = p.paper,
                        timeSelectorUnselectedContentColor = p.inkSoft,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GhostButton("Cancel", onDismiss)
                    InkButton("Set", { onPick(LocalTime.of(state.hour, state.minute)) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        colors = DatePickerDefaults.colors(containerColor = p.surface),
        confirmButton = {
            InkButton("Choose", {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }, Modifier.padding(end = 8.dp, bottom = 8.dp))
        },
        dismissButton = { GhostButton("Cancel", onDismiss) },
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = p.surface,
                selectedDayContainerColor = p.terracotta,
                selectedDayContentColor = p.onAccent,
                todayDateBorderColor = p.terracotta,
                todayContentColor = p.terracotta,
            ),
        )
    }
}
