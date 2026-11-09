package com.almanac.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almanac.app.data.AppData
import com.almanac.app.data.Event
import com.almanac.app.data.Store
import com.almanac.app.ui.components.BallotBox
import com.almanac.app.ui.components.Blank
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.InkPicker
import com.almanac.app.ui.components.LedgerRow
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Masthead
import com.almanac.app.ui.components.Moon
import com.almanac.app.ui.components.MoonGlyph
import com.almanac.app.ui.components.MoodWords
import com.almanac.app.ui.components.Rule
import com.almanac.app.ui.components.SectionHead
import com.almanac.app.ui.components.Segmented
import com.almanac.app.ui.components.Sheet
import com.almanac.app.ui.components.TextAction
import com.almanac.app.ui.components.TimePickDialog
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberToday
import com.almanac.app.ui.components.tap
import com.almanac.app.ui.theme.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen() {
    val data by Store.data.collectAsState()
    val today = rememberToday()
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
    var selected by rememberSaveable { mutableStateOf(today) }
    var editing by remember { mutableStateOf<Event?>(null) }
    val inMonth = remember(data.events, month) { data.events.count { YearMonth.from(it.date) == month } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        Masthead(
            title = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            meta = "${month.year} · $inMonth event${if (inMonth == 1) "" else "s"}" +
                if (month != YearMonth.from(today)) "" else " · this month",
        ) {
            SquareIcon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month") { month = month.minusMonths(1) }
            SquareIcon(Icons.AutoMirrored.Filled.ArrowForward, "Next month") { month = month.plusMonths(1) }
        }
        if (month != YearMonth.from(today) || selected != today) {
            TextAction("← Back to today", { month = YearMonth.from(today); selected = today }, color = p.signal,
                modifier = Modifier.padding(top = 4.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }

        var drag by remember { mutableFloatStateOf(0f) }
        AnimatedContent(
            targetState = month,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
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
            MonthTable(m, data, today, selected) {
                selected = it
                if (YearMonth.from(it) != m) month = YearMonth.from(it)
            }
        }

        Spacer(Modifier.height(30.dp))
        DayDetail(selected, today, data, onAdd = { editing = Event(Store.newId(), "", selected) }, onEdit = { editing = it })
    }

    editing?.let { e -> EventSheet(e, onDismiss = { editing = null }) }
}

@Composable
fun SquareIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).border(1.dp, p.ink).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = p.ink, modifier = Modifier.size(18.dp)) }
}

/** The month as a ruled table, Monday first. */
@Composable
private fun MonthTable(month: YearMonth, data: AppData, today: LocalDate, selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val eventsByDay = remember(data.events) { data.events.groupBy { it.date } }
    val dueDays = remember(data.tasks) { data.tasks.filter { it.due != null && !it.done }.mapNotNull { it.due }.toSet() }

    Column {
        Rule(color = p.ink)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase().take(3),
                    style = Type.tag,
                    color = if (d.value >= 6) p.signal else p.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Rule(color = p.ink)
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth().height(58.dp)) {
                for (dow in 0 until 7) {
                    if (dow > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(p.rule))
                    val day = start.plusDays((week * 7 + dow).toLong())
                    DayCell(
                        day = day,
                        inMonth = YearMonth.from(day) == month,
                        isToday = day == today,
                        isSelected = day == selected,
                        events = eventsByDay[day].orEmpty(),
                        hasDue = day in dueDays,
                        mood = data.moods[day],
                        modifier = Modifier.weight(1f).fillMaxHeight().tap { onPick(day) },
                    )
                }
            }
            Rule()
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    events: List<Event>,
    hasDue: Boolean,
    mood: Int?,
    modifier: Modifier,
) {
    Box(
        modifier
            .background(if (isSelected) p.shade else Color.Transparent)
            .then(if (isSelected) Modifier.border(1.5.dp, p.ink) else Modifier)
            .padding(5.dp),
    ) {
        // date numeral; today is set reversed out of a signal block
        Text(
            "${day.dayOfMonth}",
            style = Type.data.copy(fontSize = 13.sp),
            color = when {
                isToday -> p.paper
                !inMonth -> p.muted.copy(alpha = 0.5f)
                else -> p.ink
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(if (isToday) p.signal else Color.Transparent)
                .padding(horizontal = 3.dp),
        )
        val phase = Moon.name(day)
        if (inMonth && (phase == "Full moon" || phase == "New moon") && Moon.name(day.minusDays(1)) != phase) {
            MoonGlyph(day, 8.dp, Modifier.align(Alignment.TopEnd).padding(top = 2.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            events.take(3).forEach { e ->
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (inMonth) p.inkAt(e.tint) else p.rule))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasDue && inMonth) Box(Modifier.size(6.dp).border(1.dp, p.ink))
                Spacer(Modifier.weight(1f))
                if (mood != null && inMonth) Box(Modifier.size(6.dp).background(p.mood(mood)))
            }
        }
    }
}

@Composable
private fun DayDetail(day: LocalDate, today: LocalDate, data: AppData, onAdd: () -> Unit, onEdit: (Event) -> Unit) {
    Text(Fmt.relative(day, today).uppercase(), style = Type.tag, color = p.signal)
    Text(
        "${Fmt.weekday(day)} ${day.dayOfMonth} ${day.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}".uppercase(),
        style = MaterialTheme.typography.displaySmall,
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        MoonGlyph(day, 11.dp)
        Spacer(Modifier.width(6.dp))
        val mood = data.moods[day]?.let { " · CONDITION ${it + 1} ${MoodWords[it].uppercase()}" }.orEmpty()
        Text(Moon.name(day).uppercase() + mood, style = Type.tag)
    }
    Spacer(Modifier.height(18.dp))

    val events = data.events.filter { it.date == day }.sortedWith(compareBy(nullsFirst()) { it.time })
    SectionHead("A", "Events", trailing = "${events.size}")
    if (events.isEmpty()) Blank("No events.")
    events.forEach { e ->
        LedgerRow(lead = e.time?.format(Fmt.time) ?: "All day", leadColor = p.ink, onClick = { onEdit(e) }) {
            Box(Modifier.size(width = 3.dp, height = 18.dp).background(p.inkAt(e.tint)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(e.title, style = MaterialTheme.typography.bodyLarge)
                if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    InkButton("+ Add event", onAdd, Modifier.fillMaxWidth())

    val tasks = data.tasks.filter { it.due == day }
    if (tasks.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        SectionHead("B", "Due", trailing = "${tasks.count { it.done }}/${tasks.size}")
        tasks.forEach { t ->
            LedgerRow(lead = "Task", trailing = { BallotBox(t.done, { Store.toggleTask(t.id) }) }) {
                Text(t.title, style = MaterialTheme.typography.bodyLarge, color = if (t.done) p.muted else p.ink)
            }
        }
    }
    data.notes[day]?.let {
        Spacer(Modifier.height(28.dp))
        SectionHead("C", "Note")
        Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EventSheet(initial: Event, onDismiss: () -> Unit) {
    val isNew = remember { Store.data.value.events.none { it.id == initial.id } }
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var time by remember { mutableStateOf(initial.time) }
    var tint by remember { mutableStateOf(initial.tint) }
    var pickTime by remember { mutableStateOf(false) }

    Sheet((if (isNew) "New event · " else "Event · ") + initial.date.format(Fmt.shortDay), onDismiss) {
        LineField(title, { title = it }, "Title", large = true)
        Spacer(Modifier.height(18.dp))
        Segmented(
            listOf("All day", time?.format(Fmt.time) ?: "Set time"),
            selected = if (time == null) 0 else 1,
            onSelect = { if (it == 0) time = null else pickTime = true },
        )
        Spacer(Modifier.height(18.dp))
        Text("MARK", style = Type.tag)
        Spacer(Modifier.height(8.dp))
        InkPicker(tint) { tint = it }
        Spacer(Modifier.height(8.dp))
        LineField(note, { note = it }, "Note")
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isNew) TextAction("Delete", { Store.deleteEvent(initial.id); onDismiss() }, color = p.signal)
            Spacer(Modifier.weight(1f))
            TextAction("Cancel", onDismiss)
            InkButton("Save", {
                Store.upsertEvent(initial.copy(title = title.trim(), note = note.trim(), time = time, tint = tint))
                onDismiss()
            }, enabled = title.isNotBlank())
        }
    }
    if (pickTime) {
        TimePickDialog(time ?: LocalTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1), { pickTime = false }) {
            time = it; pickTime = false
        }
    }
}
