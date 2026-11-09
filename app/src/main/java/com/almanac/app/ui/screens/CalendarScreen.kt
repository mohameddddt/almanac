package com.almanac.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almanac.app.data.AppData
import com.almanac.app.data.Event
import com.almanac.app.data.Store
import com.almanac.app.ui.components.Chip
import com.almanac.app.ui.components.Dot
import com.almanac.app.ui.components.EmptyNote
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.GhostButton
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Moon
import com.almanac.app.ui.components.MoonGlyph
import com.almanac.app.ui.components.RoundIcon
import com.almanac.app.ui.components.ScreenTitle
import com.almanac.app.ui.components.SectionLabel
import com.almanac.app.ui.components.TimePickDialog
import com.almanac.app.ui.components.TintPicker
import com.almanac.app.ui.components.p
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen() {
    val data by Store.data.collectAsState()
    val today = LocalDate.now()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var selected by rememberSaveable { mutableStateOf(today) }
    var editing by remember { mutableStateOf<Event?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        ScreenTitle(kicker = month.year.toString(), title = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())) {
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous month", { month = month.minusMonths(1) }, container = p.card, tint = p.inkSoft, size = 42.dp)
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowForward, "Next month", { month = month.plusMonths(1) }, container = p.card, tint = p.inkSoft, size = 42.dp)
        }

        if (month != YearMonth.from(today) || selected != today) {
            Chip("Back to today", selected = false, onClick = { month = YearMonth.from(today); selected = today })
            Spacer(Modifier.height(14.dp))
        }

        // weekday initials
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (d.value >= 6) p.terracotta.copy(alpha = 0.8f) else p.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        var drag by remember { mutableStateOf(0f) }
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(280)) { it / 6 * dir } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 6 * dir } + fadeOut(tween(160)))
            },
            label = "month",
            modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (drag < -120) month = month.plusMonths(1)
                        if (drag > 120) month = month.minusMonths(1)
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                ) { _, delta -> drag += delta }
            },
        ) { m ->
            MonthGrid(m, data, today, selected) { selected = it; if (YearMonth.from(it) != m) month = YearMonth.from(it) }
        }

        Spacer(Modifier.height(26.dp))
        DayDetail(selected, data, onAdd = { editing = Event(Store.newId(), "", selected) }, onEdit = { editing = it })
    }

    editing?.let { e -> EventSheet(e, onDismiss = { editing = null }) }
}

@Composable
private fun MonthGrid(month: YearMonth, data: AppData, today: LocalDate, selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val eventsByDay = remember(data.events) { data.events.groupBy { it.date } }
    val tasksByDay = remember(data.tasks) { data.tasks.filter { it.due != null && !it.done }.groupBy { it.due!! } }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (dow in 0 until 7) {
                    val day = start.plusDays((week * 7 + dow).toLong())
                    val inMonth = YearMonth.from(day) == month
                    val isToday = day == today
                    val isSel = day == selected
                    val bg = when {
                        isSel -> p.ink
                        isToday -> p.terracotta.copy(alpha = 0.14f)
                        else -> Color.Transparent
                    }
                    val fg = when {
                        isSel -> p.paper
                        isToday -> p.terracotta
                        !inMonth -> p.muted.copy(alpha = 0.45f)
                        else -> p.ink
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.86f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .clickable { onPick(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        val phase = Moon.name(day)
                        if (inMonth && (phase == "Full moon" || phase == "New moon") && Moon.name(day.minusDays(1)) != phase) {
                            MoonGlyph(day, 7.dp, Modifier.align(Alignment.TopEnd).padding(6.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = fg,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(5.dp)) {
                                val evs = eventsByDay[day].orEmpty().take(3)
                                evs.forEach { Dot(if (isSel) p.paper else p.tint(it.tint), 5.dp) }
                                if (evs.size < 3 && tasksByDay[day] != null) {
                                    Box(Modifier.size(5.dp).border(1.dp, if (isSel) p.paper else p.sage, CircleShape))
                                }
                            }
                            // mood leaves a quiet underline
                            val mood = data.moods[day]
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.width(14.dp).height(2.dp).clip(CircleShape)
                                    .background(if (mood != null && inMonth) p.moods[mood] else Color.Transparent),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetail(day: LocalDate, data: AppData, onAdd: () -> Unit, onEdit: (Event) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Fmt.relative(day).uppercase(), style = MaterialTheme.typography.labelMedium, color = p.terracotta)
            Text("${Fmt.weekday(day)} ${day.dayOfMonth}", style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoonGlyph(day, 12.dp)
                Spacer(Modifier.width(6.dp))
                Text(Moon.name(day), style = MaterialTheme.typography.bodySmall)
                data.moods[day]?.let {
                    Text("  ·  ", style = MaterialTheme.typography.bodySmall)
                    Dot(p.moods[it], 7.dp)
                    Text(" " + MoodWords[it], style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        RoundIcon(Icons.Outlined.Add, "Add event", onAdd, container = p.terracotta, tint = p.onAccent)
    }
    Spacer(Modifier.height(18.dp))
    SectionLabel("Events")
    val events = data.events.filter { it.date == day }.sortedWith(compareBy(nullsFirst()) { it.time })
    if (events.isEmpty()) EmptyNote("Nothing planned. Tap + to pencil something in.")
    events.forEach { e ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(p.card)
                .clickable { onEdit(e) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(4.dp).height(34.dp).clip(CircleShape).background(p.tint(e.tint)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleMedium)
                if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text(e.time?.format(Fmt.time) ?: "All day", style = MaterialTheme.typography.labelLarge, color = p.inkSoft)
        }
    }
    val tasks = data.tasks.filter { it.due == day }
    if (tasks.isNotEmpty()) {
        Spacer(Modifier.height(22.dp))
        SectionLabel("Due")
        tasks.forEach { t ->
            Row(
                Modifier.fillMaxWidth().clickable { Store.toggleTask(t.id) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(16.dp).clip(CircleShape)
                        .background(if (t.done) p.sage else Color.Transparent)
                        .border(1.5.dp, p.sage, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text(t.title, style = MaterialTheme.typography.bodyLarge, color = if (t.done) p.muted else p.ink)
            }
        }
    }
    data.notes[day]?.let {
        Spacer(Modifier.height(22.dp))
        SectionLabel("Line of the day")
        Text("“$it”", style = MaterialTheme.typography.bodyLarge, color = p.inkSoft, modifier = Modifier.padding(top = 10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSheet(initial: Event, onDismiss: () -> Unit) {
    val isNew = Store.data.value.events.none { it.id == initial.id }
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var time by remember { mutableStateOf(initial.time) }
    var tint by remember { mutableStateOf(initial.tint) }
    var pickTime by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp)) {
            Text(
                (if (isNew) "New event · " else "Event · ") + initial.date.format(Fmt.shortDay),
                style = MaterialTheme.typography.labelMedium, color = p.terracotta,
            )
            Spacer(Modifier.height(8.dp))
            LineField(title, { title = it }, "What's happening?", large = true)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("All day", time == null, { time = null })
                Chip(time?.format(Fmt.time) ?: "Set a time", time != null, { pickTime = true })
            }
            Spacer(Modifier.height(20.dp))
            TintPicker(tint) { tint = it }
            Spacer(Modifier.height(10.dp))
            LineField(note, { note = it }, "Add a note (optional)")
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) GhostButton("Delete", { Store.deleteEvent(initial.id); onDismiss() }, color = p.terracotta)
                Spacer(Modifier.weight(1f))
                GhostButton("Cancel", onDismiss)
                InkButton("Save", {
                    Store.upsertEvent(initial.copy(title = title.trim(), note = note.trim(), time = time, tint = tint))
                    onDismiss()
                }, enabled = title.isNotBlank())
            }
        }
    }
    if (pickTime) {
        TimePickDialog(time ?: LocalTime.now().withMinute(0).plusHours(1), { pickTime = false }) {
            time = it; pickTime = false
        }
    }
}
