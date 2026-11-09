package com.almanac.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almanac.app.Tab
import com.almanac.app.data.Store
import com.almanac.app.ui.components.Aphorisms
import com.almanac.app.ui.components.Bar
import com.almanac.app.ui.components.Dot
import com.almanac.app.ui.components.EmptyNote
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Moon
import com.almanac.app.ui.components.MoonGlyph
import com.almanac.app.ui.components.PaperCard
import com.almanac.app.ui.components.RoundIcon
import com.almanac.app.ui.components.SectionLabel
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberNow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val ScreenPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 120.dp)
val MoodWords = listOf("Heavy", "Low", "Even", "Good", "Bright")

@Composable
fun TodayScreen(onOpen: (Tab) -> Unit) {
    val data by Store.data.collectAsState()
    val now = rememberNow()
    val today = now.toLocalDate()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        // ---- masthead ----
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.greeting(now.hour).uppercase(), style = MaterialTheme.typography.labelMedium, color = p.terracotta)
                Spacer(Modifier.height(6.dp))
                Text(Fmt.weekday(today), style = MaterialTheme.typography.displayMedium)
                Text(
                    today.format(Fmt.dayMonth) + " · week " + today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                    style = MaterialTheme.typography.bodyMedium, color = p.muted,
                )
            }
            val themeIcon = when (data.themeMode) { 1 -> Icons.Outlined.LightMode; 2 -> Icons.Outlined.DarkMode; else -> Icons.Outlined.BrightnessAuto }
            RoundIcon(
                themeIcon, "Theme", onClick = { Store.setThemeMode((data.themeMode + 1) % 3) },
                container = p.card, tint = p.inkSoft, size = 42.dp,
            )
        }

        Spacer(Modifier.height(26.dp))

        // ---- the hour ----
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                now.format(Fmt.time),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 92.sp, lineHeight = 92.sp),
            )
            Text(
                "%02d".format(now.second),
                style = MaterialTheme.typography.headlineMedium,
                color = p.brass,
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        TimePassing(now)

        Spacer(Modifier.height(28.dp))

        // ---- moon + mood ----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaperCard(Modifier.weight(1f)) {
                MoonGlyph(today, 46.dp)
                Spacer(Modifier.height(14.dp))
                Text(Moon.name(today), style = MaterialTheme.typography.titleMedium)
                Text("${(Moon.illumination(today) * 100).toInt()}% lit", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                val full = Moon.nextFull(today)
                val days = ChronoUnit.DAYS.between(today, full)
                Text(
                    if (days == 0L) "Full tonight" else "Full in $days day${if (days == 1L) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall, color = p.brass,
                )
            }
            PaperCard(Modifier.weight(1.25f)) {
                Text("How does today feel?", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(14.dp))
                MoodPicker(data.moods[today]) { Store.setMood(today, it) }
                Spacer(Modifier.height(10.dp))
                Text(
                    data.moods[today]?.let { MoodWords[it] } ?: "Tap a tone",
                    style = MaterialTheme.typography.bodySmall,
                    color = data.moods[today]?.let { p.moods[it] } ?: p.muted,
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // ---- agenda ----
        SectionLabel("On the page today")
        Spacer(Modifier.height(8.dp))
        val events = data.events.filter { it.date == today }.sortedBy { it.time }
        val tasks = data.tasks.filter { !it.done && it.due != null && !it.due.isAfter(today) }
        if (events.isEmpty() && tasks.isEmpty()) EmptyNote("A clear page. Nothing scheduled.")
        events.forEach { e ->
            AgendaRow(
                lead = e.time?.format(Fmt.time) ?: "All day",
                title = e.title,
                color = p.tint(e.tint),
                onClick = { onOpen(Tab.Calendar) },
            )
        }
        tasks.forEach { t ->
            AgendaRow(
                lead = if (t.due!!.isBefore(today)) "Overdue" else "Task",
                title = t.title,
                color = if (t.due.isBefore(today)) p.terracotta else p.sage,
                onClick = { Store.toggleTask(t.id) },
                hollow = true,
            )
        }

        Spacer(Modifier.height(26.dp))

        // ---- rituals ----
        if (data.habits.isNotEmpty()) {
            SectionLabel("Rituals", trailing = {
                val done = data.habits.count { today in it.doneDates }
                Text("$done/${data.habits.size}", style = MaterialTheme.typography.labelMedium)
            })
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                data.habits.forEach { h ->
                    val done = today in h.doneDates
                    val c = p.tint(h.tint)
                    val bg by animateColorAsState(if (done) c else p.paper, label = "habit")
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(bg)
                            .border(1.dp, if (done) c else p.line, CircleShape)
                            .clickable { Store.toggleHabit(h.id, today) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(if (done) p.onAccent else c, 7.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(h.name, style = MaterialTheme.typography.labelLarge, color = if (done) p.onAccent else p.inkSoft)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }

        // ---- aphorism ----
        val (quote, author) = Aphorisms.forDay(today)
        PaperCard(Modifier.fillMaxWidth(), color = if (p.isDark) p.card else p.ink, padding = 26.dp) {
            Text("“", style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 30.sp), color = p.terracotta)
            Text(
                quote,
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic, lineHeight = 30.sp),
                color = if (p.isDark) p.ink else p.paper,
            )
            Spacer(Modifier.height(14.dp))
            Text("— " + author.uppercase(), style = MaterialTheme.typography.labelMedium, color = p.brass)
        }

        Spacer(Modifier.height(30.dp))

        // ---- a line for today ----
        SectionLabel("A line for today")
        Spacer(Modifier.height(4.dp))
        var note by remember(today) { mutableStateOf(data.notes[today].orEmpty()) }
        LineField(
            value = note,
            onValueChange = { note = it; Store.setNote(today, it) },
            placeholder = "Write one sentence you'll want to remember…",
            singleLine = false,
        )
    }
}

@Composable
private fun TimePassing(now: java.time.LocalDateTime) {
    val dayProgress = now.toLocalTime().toSecondOfDay() / 86400f
    val weekProgress = ((now.dayOfWeek.value - 1) + dayProgress) / 7f
    val yearLen = now.toLocalDate().lengthOfYear()
    val yearProgress = ((now.dayOfYear - 1) + dayProgress) / yearLen
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(
            Triple("Day", dayProgress, p.terracotta),
            Triple("Week", weekProgress, p.sage),
            Triple("Year", yearProgress, p.brass),
        ).forEach { (label, v, c) ->
            Column(Modifier.weight(1f)) {
                Row {
                    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${(v * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = p.inkSoft)
                }
                Spacer(Modifier.height(6.dp))
                Bar(v, c)
            }
        }
    }
}

@Composable
fun MoodPicker(selected: Int?, onPick: (Int?) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        p.moods.forEachIndexed { i, c ->
            val size by animateDpAsState(if (selected == i) 26.dp else 18.dp, label = "mood")
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(if (selected == null || selected == i) c else c.copy(alpha = 0.3f))
                        .clickable { onPick(if (selected == i) null else i) },
                )
            }
        }
    }
}

@Composable
private fun AgendaRow(lead: String, title: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit, hollow: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(lead.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(62.dp))
        if (hollow) Box(Modifier.size(9.dp).border(1.5.dp, color, CircleShape)) else Dot(color, 9.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

