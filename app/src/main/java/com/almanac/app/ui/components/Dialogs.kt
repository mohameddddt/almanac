package com.almanac.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.almanac.app.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun timePickerColors() = TimePickerDefaults.colors(
    clockDialColor = p.shade,
    clockDialSelectedContentColor = p.paper,
    clockDialUnselectedContentColor = p.inkSoft,
    selectorColor = p.signal,
    containerColor = p.paper,
    timeSelectorSelectedContainerColor = p.ink,
    timeSelectorUnselectedContainerColor = p.shade,
    timeSelectorSelectedContentColor = p.paper,
    timeSelectorUnselectedContentColor = p.inkSoft,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacTimePicker(state: TimePickerState) = TimePicker(state = state, colors = timePickerColors())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RectangleShape, color = p.paper, modifier = Modifier.border(1.dp, p.ink)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SET TIME", style = Type.tag, color = p.ink, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                Rule(color = p.ink)
                Spacer(Modifier.height(18.dp))
                AlmanacTimePicker(state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextAction("Cancel", onDismiss)
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
    val colors = DatePickerDefaults.colors(
        containerColor = p.paper,
        selectedDayContainerColor = p.ink,
        selectedDayContentColor = p.paper,
        todayDateBorderColor = p.signal,
        todayContentColor = p.signal,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        colors = colors,
        confirmButton = {
            InkButton("Choose", {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }, Modifier.padding(end = 8.dp, bottom = 8.dp))
        },
        dismissButton = { TextAction("Cancel", onDismiss) },
    ) {
        DatePicker(state = state, showModeToggle = false, colors = colors)
    }
}

/** Square-cornered editor sheet with a ruled title line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.paper,
        shape = RectangleShape,
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                Modifier.padding(top = 12.dp, bottom = 6.dp)
                    .size(36.dp, 3.dp)
                    .background(p.ink),
            )
        },
    ) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding().imePadding()
                .padding(bottom = 16.dp),
        ) {
            Text(title.uppercase(), style = Type.tag, color = p.signal)
            Spacer(Modifier.height(8.dp))
            Rule(color = p.ink)
            content()
        }
    }
}
