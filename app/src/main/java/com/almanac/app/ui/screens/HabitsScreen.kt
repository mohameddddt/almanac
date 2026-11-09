package com.almanac.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almanac.app.data.Habit
import com.almanac.app.data.Store
import com.almanac.app.ui.components.BallotBox
import com.almanac.app.ui.components.Blank
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.InkPicker
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Masthead
import com.almanac.app.ui.components.MoodWords
import com.almanac.app.ui.components.Rule
import com.almanac.app.ui.components.SectionHead
import com.almanac.app.ui.components.TextAction
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberToday
import com.almanac.app.ui.theme.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val NameColumn = 112.dp

@Composable
fun HabitsScreen() {
    val data by Store.data.collectAsState()
    val today = rememberToday()
    val week = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    var deleting by remember { mutableStateOf<Habit?>(null) }
    val kept = data.habits.count { today in it.doneDates }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        Masthead("Rituals", "$kept of ${data.habits.size} kept today")
        Spacer(Modifier.height(22.dp))

        SectionHead("A", "This week", trailing = "HOLD A NAME TO REMOVE")
        if (data.habits.isEmpty()) Blank("No rituals yet.")
        else {
            WeekHeader(week, today)
            data.habits.forEach { h -> HabitWeekRow(h, week, today, onLongPress = { deleting = h }) }
        }

        if (data.habits.isNotEmpty()) {
            Spacer(Modifier.height(30.dp))
            SectionHead("B", "Thirty days")
            data.habits.forEach { h -> ThirtyDays(h, today) }
        }

        Spacer(Modifier.height(30.dp))
        SectionHead("C", "New ritual")
        NewHabit()

        Spacer(Modifier.height(30.dp))
        MoodRecord(data.moods, today)
    }

    deleting?.let { h ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = p.paper,
            shape = RectangleShape,
            title = { Text("Remove “${h.name}”?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Its record will be deleted too.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { InkButton("Remove", { Store.deleteHabit(h.id); deleting = null }, color = p.signal) },
            dismissButton = { TextAction("Keep", { deleting = null }) },
        )
    }
}

@Composable
private fun WeekHeader(week: List<LocalDate>, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Spacer(Modifier.width(NameColumn))
        week.forEach { d ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = Type.tag,
                    color = if (d == today) p.signal else p.muted,
                )
                Text("${d.dayOfMonth}", style = Type.tag, color = if (d == today) p.signal else p.ink)
            }
        }
        Text("RUN", style = Type.tag, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
    }
    Rule(color = p.ink)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitWeekRow(h: Habit, week: List<LocalDate>, today: LocalDate, onLongPress: () -> Unit) {
    val c = p.inkAt(h.tint)
    val streak = h.streak(today)
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .width(NameColumn)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onLongPress,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(width = 3.dp, height = 16.dp).background(c))
                Spacer(Modifier.width(8.dp))
                Text(h.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
            week.forEach { d ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BallotBox(d in h.doneDates, { Store.toggleHabit(h.id, d) }, color = c, size = 24.dp)
                }
            }
            Text(
                if (streak > 0) "$streak" else "—",
                style = Type.data,
                color = if (streak > 0) p.ink else p.muted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp),
            )
        }
        Rule()
    }
}

/** One slim mark per day — a tally rather than a chart. */
@Composable
private fun ThirtyDays(h: Habit, today: LocalDate) {
    val c = p.inkAt(h.tint)
    val count = (0 until 30).count { today.minusDays(it.toLong()) in h.doneDates }
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(h.name.uppercase(), style = Type.tag, color = p.ink, modifier = Modifier.weight(1f))
            Text("$count/30", style = Type.tag)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 29 downTo 0) {
                val done = today.minusDays(i.toLong()) in h.doneDates
                Box(Modifier.weight(1f).fillMaxSize().background(if (done) c else p.rule))
            }
        }
    }
}

@Composable
private fun NewHabit() {
    var name by remember { mutableStateOf("") }
    var tint by remember { mutableIntStateOf(0) }
    val add = { if (name.isNotBlank()) { Store.addHabit(name, tint); name = ""; tint = (tint + 1) % 5 } }
    Row(verticalAlignment = Alignment.Bottom) {
        LineField(name, { name = it }, "Name", Modifier.weight(1f), onDone = add)
        Spacer(Modifier.width(10.dp))
        InkButton("Add", add, enabled = name.isNotBlank())
    }
    Spacer(Modifier.height(12.dp))
    InkPicker(tint) { tint = it }
}

/** Five weeks of recorded condition, printed as ink density. */
@Composable
private fun MoodRecord(moods: Map<LocalDate, Int>, today: LocalDate) {
    val start = today.minusDays((today.dayOfWeek.value - 1).toLong()).minusWeeks(4)
    val recent = moods.filterKeys { !it.isBefore(today.minusDays(29)) && !it.isAfter(today) }
    val avg = if (recent.isEmpty()) null else recent.values.average().roundToInt().coerceIn(0, 4)
    SectionHead("D", "Condition, five weeks", trailing = avg?.let { "MOSTLY ${MoodWords[it].uppercase()}" } ?: "NO RECORD")
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach {
            Text(
                it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = Type.tag,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (w in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (dow in 0 until 7) {
                    val d = start.plusDays((w * 7 + dow).toLong())
                    val m = moods[d]
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.4f)
                            .background(if (m != null) p.mood(m) else Color.Transparent)
                            .border(1.dp, if (d == today) p.signal else if (d.isAfter(today)) Color.Transparent else p.rule),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        if (!d.isAfter(today)) {
                            Text(
                                "${d.dayOfMonth}",
                                style = Type.tag,
                                color = if (m != null && m >= 2) p.paper else p.muted,
                                modifier = Modifier.padding(3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MoodWords.forEachIndexed { i, w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(p.mood(i)))
                Spacer(Modifier.width(4.dp))
                Text("${i + 1} ${w.uppercase()}", style = Type.tag)
            }
        }
    }
}
