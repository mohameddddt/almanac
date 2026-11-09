package com.almanac.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almanac.app.data.Habit
import com.almanac.app.data.Store
import com.almanac.app.ui.components.EmptyNote
import com.almanac.app.ui.components.GhostButton
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.PaperCard
import com.almanac.app.ui.components.RoundIcon
import com.almanac.app.ui.components.ScreenTitle
import com.almanac.app.ui.components.SectionLabel
import com.almanac.app.ui.components.TintPicker
import com.almanac.app.ui.components.p
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitsScreen() {
    val data by Store.data.collectAsState()
    val today = LocalDate.now()
    val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
    var deleting by remember { mutableStateOf<Habit?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        ScreenTitle("Small things, often", "Rituals")

        if (data.habits.isEmpty()) EmptyNote("Begin with one ritual. Keep it small enough to never skip.")

        data.habits.forEach { h ->
            HabitCard(h, week, today, onLongPress = { deleting = h })
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))
        NewHabit()

        Spacer(Modifier.height(34.dp))
        MoodJournal(data.moods, today)
    }

    deleting?.let { h ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = p.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Let go of “${h.name}”?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Its history will be removed too.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { InkButton("Remove", { Store.deleteHabit(h.id); deleting = null }, color = p.terracotta) },
            dismissButton = { GhostButton("Keep", { deleting = null }) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(h: Habit, week: List<LocalDate>, today: LocalDate, onLongPress: () -> Unit) {
    val c = p.tint(h.tint)
    val streak = h.streak(today)
    PaperCard(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(c))
            Spacer(Modifier.width(10.dp))
            Text(h.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (streak > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$streak", style = MaterialTheme.typography.headlineMedium, color = c)
                    Text(if (streak == 1) "DAY" else "DAY STREAK", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { d ->
                val done = d in h.doneDates
                val bg by animateColorAsState(if (done) c else Color.Transparent, label = "day")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (d == today) p.ink else p.muted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .border(1.dp, if (done) c else p.line, CircleShape)
                            .clickable { Store.toggleHabit(h.id, d) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Outlined.Check, null, tint = p.onAccent, modifier = Modifier.size(16.dp))
                        else Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = p.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // the last thirty days, one slim bar each — like tally marks
        Row(Modifier.fillMaxWidth().height(20.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 29 downTo 0) {
                val d = today.minusDays(i.toLong())
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (d in h.doneDates) c else p.line.copy(alpha = 0.8f)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val last30 = (0 until 30).count { today.minusDays(it.toLong()) in h.doneDates }
        Text("$last30 of the last 30 days · hold to remove", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NewHabit() {
    var name by remember { mutableStateOf("") }
    var tint by remember { mutableIntStateOf(0) }
    val add = { if (name.isNotBlank()) { Store.addHabit(name, tint); name = ""; tint = (tint + 1) % 5 } }
    SectionLabel("A new ritual")
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        LineField(name, { name = it }, "Stretch, journal, call home…", Modifier.weight(1f), onDone = add)
        Spacer(Modifier.width(12.dp))
        RoundIcon(
            Icons.Outlined.Add, "Add ritual", add,
            container = if (name.isBlank()) p.line else p.ink,
            tint = if (name.isBlank()) p.muted else p.paper, size = 40.dp,
        )
    }
    Spacer(Modifier.height(14.dp))
    TintPicker(tint) { tint = it }
}

/** Five weeks of moods, one tile per day — a quiet emotional weather report. */
@Composable
private fun MoodJournal(moods: Map<LocalDate, Int>, today: LocalDate) {
    SectionLabel("Mood, five weeks")
    Spacer(Modifier.height(14.dp))
    val start = today.minusDays((today.dayOfWeek.value - 1).toLong()).minusWeeks(4)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            java.time.DayOfWeek.entries.forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (w in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (dow in 0 until 7) {
                    val d = start.plusDays((w * 7 + dow).toLong())
                    val m = moods[d]
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    m != null -> p.moods[m]
                                    d.isAfter(today) -> Color.Transparent
                                    else -> p.card
                                },
                            )
                            .then(if (d == today) Modifier.border(1.5.dp, p.ink, RoundedCornerShape(8.dp)) else Modifier),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MoodWords.forEachIndexed { i, w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(p.moods[i]))
                Spacer(Modifier.width(5.dp))
                Text(w, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val logged = moods.filterKeys { !it.isBefore(today.minusDays(29)) }
    if (logged.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        val avg = logged.values.average()
        val word = MoodWords[kotlin.math.round(avg).toInt().coerceIn(0, 4)]
        PaperCard(Modifier.fillMaxWidth(), padding = 18.dp) {
            Text("This month has felt mostly", style = MaterialTheme.typography.bodySmall)
            Text(word.lowercase() + ".", style = MaterialTheme.typography.headlineMedium, color = p.moods[kotlin.math.round(avg).toInt().coerceIn(0, 4)])
        }
    }
}
