package com.almanac.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almanac.app.Tab
import com.almanac.app.data.AppData
import com.almanac.app.data.Store
import com.almanac.app.ui.components.Aphorisms
import com.almanac.app.ui.components.BallotBox
import com.almanac.app.ui.components.Blank
import com.almanac.app.ui.components.DoubleRule
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.LedgerRow
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Meter
import com.almanac.app.ui.components.Moon
import com.almanac.app.ui.components.MoonGlyph
import com.almanac.app.ui.components.MoodWords
import com.almanac.app.ui.components.Rule
import com.almanac.app.ui.components.SectionHead
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberNow
import com.almanac.app.ui.components.rememberToday
import com.almanac.app.ui.components.tap
import com.almanac.app.ui.theme.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

val ScreenPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 36.dp)

@Composable
fun TodayScreen(onOpen: (Tab) -> Unit) {
    val data by Store.data.collectAsState()
    val today = rememberToday()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        // ---- masthead ----
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ALMANAC", style = Type.tag, color = p.ink)
            Spacer(Modifier.width(10.dp))
            Text("NO. ${today.dayOfYear} · ${today.year}", style = Type.tag, color = p.muted, modifier = Modifier.weight(1f))
            Text(
                listOf("AUTO", "LIGHT", "DARK")[data.themeMode],
                style = Type.tag,
                color = p.ink,
                modifier = Modifier
                    .border(1.dp, p.ink)
                    .tap { Store.setThemeMode((data.themeMode + 1) % 3) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        DoubleRule()

        Spacer(Modifier.height(18.dp))
        Text(Fmt.weekday(today).uppercase(), style = MaterialTheme.typography.displayLarge)
        Text(
            "${today.format(Fmt.longDate)} · Week ${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        LiveClock()
        Spacer(Modifier.height(12.dp))
        TimePassing()

        Spacer(Modifier.height(30.dp))
        SectionHead("01", "Almanac")
        Facts(today)

        Spacer(Modifier.height(30.dp))
        SectionHead("02", "Condition", trailing = data.moods[today]?.let { "${it + 1} · ${MoodWords[it].uppercase()}" } ?: "UNRECORDED")
        Spacer(Modifier.height(14.dp))
        MoodScale(data.moods[today]) { Store.setMood(today, it) }

        Spacer(Modifier.height(30.dp))
        Agenda(data, today, onOpen)

        if (data.habits.isNotEmpty()) {
            Spacer(Modifier.height(30.dp))
            val kept = data.habits.count { today in it.doneDates }
            SectionHead("04", "Rituals", trailing = "$kept/${data.habits.size}")
            data.habits.forEach { h ->
                val streak = h.streak(today)
                LedgerRow(
                    lead = if (streak > 0) "${streak}D" else "—",
                    leadColor = if (streak > 0) p.ink else p.muted,
                    trailing = {
                        BallotBox(today in h.doneDates, { Store.toggleHabit(h.id, today) }, color = p.inkAt(h.tint))
                    },
                ) { Text(h.name, style = MaterialTheme.typography.bodyLarge) }
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionHead("05", "Marginalia")
        Spacer(Modifier.height(16.dp))
        val (quote, author) = Aphorisms.forDay(today)
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(p.signal))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(quote, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(author.uppercase(), style = Type.tag)
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionHead("06", "Note")
        var note by remember(today) { mutableStateOf(data.notes[today].orEmpty()) }
        LineField(
            value = note,
            onValueChange = { note = it; Store.setNote(today, it) },
            placeholder = "One line about today",
            singleLine = false,
        )
    }
}

/** The only part of the page that changes every second. */
@Composable
private fun LiveClock() {
    val now = rememberNow(1000)
    Row(verticalAlignment = Alignment.Top) {
        Text(now.format(Fmt.time), style = Type.numeral)
        Text(
            "%02d".format(now.second),
            style = Type.data.copy(fontSize = 20.sp),
            color = p.signal,
            modifier = Modifier.padding(start = 6.dp, top = 14.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            Fmt.period(now.hour).uppercase(),
            style = Type.tag,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun TimePassing() {
    val now = rememberNow(60_000)
    val day = now.toLocalTime().toSecondOfDay() / 86400f
    val week = ((now.dayOfWeek.value - 1) + day) / 7f
    val year = ((now.dayOfYear - 1) + day) / now.toLocalDate().lengthOfYear()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Day" to day, "Week" to week, "Year" to year).forEach { (label, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label.uppercase(), style = Type.tag, modifier = Modifier.width(52.dp))
                Meter(v, if (label == "Day") p.signal else p.ink, Modifier.weight(1f), thickness = 2.dp)
                Text("${(v * 100).toInt()}%", style = Type.tag, color = p.ink, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
            }
        }
    }
}

/** A small table of facts about the day, the way an almanac would print them. */
@Composable
private fun Facts(today: LocalDate) {
    val full = remember(today) { Moon.nextFull(today) }
    val toFull = ChronoUnit.DAYS.between(today, full)
    val toWeekend = (DayOfWeek.SATURDAY.value - today.dayOfWeek.value).coerceAtLeast(0)
    val left = today.lengthOfYear() - today.dayOfYear
    val quarterStart = today.withDayOfMonth(1).withMonth(((today.monthValue - 1) / 3) * 3 + 1)
    val rows = listOf(
        Triple("Moon", Moon.name(today), "${(Moon.illumination(today) * 100).toInt()}% lit"),
        Triple("Next full", full.format(Fmt.shortDate), if (toFull == 0L) "tonight" else "in $toFull d"),
        Triple("Day", "${today.dayOfYear} of ${today.lengthOfYear()}", "$left left"),
        Triple("Quarter", "Q${today.get(IsoFields.QUARTER_OF_YEAR)}", "day ${ChronoUnit.DAYS.between(quarterStart, today) + 1}"),
        Triple("Weekend", if (toWeekend == 0) "Now" else "In $toWeekend days", today.plusDays(toWeekend.toLong()).format(Fmt.shortDate)),
        Triple("Month", "${today.dayOfMonth} of ${today.lengthOfMonth()}", "${today.lengthOfMonth() - today.dayOfMonth} left"),
    )
    rows.chunked(2).forEach { pair ->
        Row(Modifier.height(IntrinsicSize.Min)) {
            pair.forEachIndexed { i, (label, value, aside) ->
                if (i == 1) Box(Modifier.width(1.dp).fillMaxHeight().background(p.rule))
                Column(Modifier.weight(1f).padding(start = if (i == 1) 14.dp else 0.dp, top = 12.dp, bottom = 12.dp)) {
                    Text(label.uppercase(), style = Type.tag)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (label == "Moon") {
                            MoonGlyph(today, 13.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    }
                    Text(aside, style = Type.data, color = p.muted)
                }
            }
        }
        Rule()
    }
}

/** Five boxes, filled with ink by density. Tap again to clear. */
@Composable
fun MoodScale(selected: Int?, onPick: (Int?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 5) {
            val on = selected == i
            val filled = selected != null && i <= selected
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.6f)
                    .background(if (filled) p.mood(i) else Color.Transparent)
                    .border(if (on) 2.dp else 1.dp, p.ink)
                    .tap { onPick(if (on) null else i) },
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    "${i + 1}",
                    style = Type.tag,
                    color = if (filled && i >= 2) p.paper else p.ink,
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(MoodWords.first().uppercase(), style = Type.tag, modifier = Modifier.weight(1f))
        Text(MoodWords.last().uppercase(), style = Type.tag)
    }
}

@Composable
private fun Agenda(data: AppData, today: LocalDate, onOpen: (Tab) -> Unit) {
    val events = remember(data.events, today) { data.events.filter { it.date == today }.sortedBy { it.time } }
    val tasks = remember(data.tasks, today) { data.tasks.filter { !it.done && it.due != null && !it.due.isAfter(today) } }
    SectionHead("03", "Agenda", trailing = "${events.size + tasks.size}")
    if (events.isEmpty() && tasks.isEmpty()) Blank("Nothing scheduled.")
    events.forEach { e ->
        LedgerRow(lead = e.time?.format(Fmt.time) ?: "All day", leadColor = p.ink, onClick = { onOpen(Tab.Calendar) }) {
            Box(Modifier.size(width = 3.dp, height = 18.dp).background(p.inkAt(e.tint)))
            Spacer(Modifier.width(12.dp))
            Text(e.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
    tasks.forEach { t ->
        val late = t.due!!.isBefore(today)
        LedgerRow(
            lead = if (late) "Late" else "Due",
            leadColor = if (late) p.signal else p.muted,
            trailing = { BallotBox(false, { Store.toggleTask(t.id) }) },
        ) { Text(t.title, style = MaterialTheme.typography.bodyLarge) }
    }
}
