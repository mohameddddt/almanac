package com.almanac.app.ui.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almanac.app.alarm.AlarmScheduler
import com.almanac.app.data.Alarm
import com.almanac.app.data.Store
import com.almanac.app.ui.components.Chip
import com.almanac.app.ui.components.EmptyNote
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.GhostButton
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.PaperCard
import com.almanac.app.ui.components.Ring
import com.almanac.app.ui.components.RoundIcon
import com.almanac.app.ui.components.SectionLabel
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberNow
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ClockPane(val label: String) { Alarms("Alarms"), Timer("Timer"), Stopwatch("Stopwatch"), World("World") }

@Composable
fun ClockScreen() {
    var pane by rememberSaveable { mutableStateOf(ClockPane.Alarms) }
    val now = rememberNow()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnalogClock(Modifier.fillMaxWidth(0.74f).aspectRatio(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "${Fmt.weekday(now.toLocalDate())}, ${now.format(Fmt.dayMonth)}",
            style = MaterialTheme.typography.bodyMedium,
            color = p.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClockPane.entries.forEach { Chip(it.label, pane == it, { pane = it }) }
        }
        Spacer(Modifier.height(22.dp))

        AnimatedContent(pane, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pane") {
            when (it) {
                ClockPane.Alarms -> AlarmsPane()
                ClockPane.Timer -> TimerPane()
                ClockPane.Stopwatch -> StopwatchPane()
                ClockPane.World -> WorldPane(now)
            }
        }
    }
}

// ------------------------------------------------------------------ analog face

@Composable
private fun AnalogClock(modifier: Modifier = Modifier) {
    var millis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { millis = System.currentTimeMillis() } }
    val face = p.card
    val ink = p.ink
    val tick = p.muted
    val brass = p.brass
    val hand = p.terracotta
    val ring = p.line

    Canvas(modifier) {
        val t = LocalDateTime.now()
        val ms = millis % 1000
        val sec = t.second + ms / 1000f
        val min = t.minute + sec / 60f
        val hour = (t.hour % 12) + min / 60f
        val r = size.minDimension / 2
        val c = center

        drawCircle(face, r, c)
        drawCircle(ring, r * 0.97f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))

        for (i in 0 until 60) {
            val major = i % 5 == 0
            val cardinal = i % 15 == 0
            val a = i / 60f * 2 * PI.toFloat()
            val outer = r * 0.88f
            val inner = r * when { cardinal -> 0.76f; major -> 0.80f; else -> 0.85f }
            drawLine(
                color = if (cardinal) brass else tick.copy(alpha = if (major) 0.9f else 0.4f),
                start = c.polar(inner, a), end = c.polar(outer, a),
                strokeWidth = (if (cardinal) 3.dp else if (major) 2.dp else 1.dp).toPx(),
                cap = StrokeCap.Round,
            )
        }
        hand(c, hour / 12f, r * 0.46f, 5.5.dp.toPx(), ink)
        hand(c, min / 60f, r * 0.68f, 3.5.dp.toPx(), ink)
        // seconds hand with counterweight
        val sa = sec / 60f * 2 * PI.toFloat()
        drawLine(hand, c.polar(-r * 0.16f, sa), c.polar(r * 0.78f, sa), 1.6.dp.toPx(), StrokeCap.Round)
        drawCircle(hand, 5.dp.toPx(), c)
        drawCircle(face, 2.dp.toPx(), c)
    }
}

private fun Offset.polar(len: Float, angle: Float) = Offset(x + len * sin(angle), y - len * cos(angle))

private fun DrawScope.hand(c: Offset, turn: Float, len: Float, width: Float, color: Color) {
    val a = turn * 2 * PI.toFloat()
    drawLine(color, c.polar(-len * 0.12f, a), c.polar(len, a), width, StrokeCap.Round)
}

// ------------------------------------------------------------------ alarms

private fun daysLabel(days: Set<DayOfWeek>) = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    days.size == 5 && DayOfWeek.SATURDAY !in days && DayOfWeek.SUNDAY !in days -> "Weekdays"
    else -> DayOfWeek.entries.filter { it in days }.joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private fun untilLabel(alarm: Alarm): String {
    val d = Duration.between(LocalDateTime.now(), AlarmScheduler.nextTrigger(alarm))
    val h = d.toHours()
    val m = d.toMinutesPart()
    return when {
        h >= 24 -> "in ${h / 24}d ${h % 24}h"
        h > 0 -> "in ${h}h ${m}m"
        else -> "in ${m.coerceAtLeast(1)}m"
    }
}

@Composable
private fun AlarmsPane() {
    val context = LocalContext.current
    val data by Store.data.collectAsState()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    rememberNow(30_000) // keep "in 3h 12m" fresh

    Column {
        SectionLabel("Alarms", trailing = {
            RoundIcon(Icons.Outlined.Add, "Add alarm", { editing = Alarm(Store.newId(), 7, 0) }, size = 34.dp)
        })
        Spacer(Modifier.height(8.dp))
        if (data.alarms.isEmpty()) EmptyNote("No alarms yet. Mornings are better planned.")
        data.alarms.sortedBy { it.hour * 60 + it.minute }.forEach { a ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(p.card)
                    .clickable { editing = a }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "%02d:%02d".format(a.hour, a.minute),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (a.enabled) p.ink else p.muted,
                    )
                    Text(
                        listOf(a.label.ifBlank { null }, daysLabel(a.days), if (a.enabled) untilLabel(a) else null)
                            .filterNotNull().joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = a.enabled,
                    onCheckedChange = { AlarmScheduler.save(context, a.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = p.terracotta, checkedThumbColor = p.onAccent,
                        uncheckedTrackColor = p.paper, uncheckedThumbColor = p.muted, uncheckedBorderColor = p.line,
                    ),
                )
            }
        }
    }
    editing?.let { AlarmSheet(it) { editing = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSheet(initial: Alarm, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isNew = Store.data.value.alarms.none { it.id == initial.id }
    val time = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableStateOf(initial.days) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (isNew) "NEW ALARM" else "EDIT ALARM",
                style = MaterialTheme.typography.labelMedium, color = p.terracotta,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(16.dp))
            TimePicker(
                state = time,
                colors = TimePickerDefaults.colors(
                    clockDialColor = p.card,
                    selectorColor = p.terracotta,
                    clockDialSelectedContentColor = p.onAccent,
                    clockDialUnselectedContentColor = p.inkSoft,
                    timeSelectorSelectedContainerColor = p.ink,
                    timeSelectorUnselectedContainerColor = p.card,
                    timeSelectorSelectedContentColor = p.paper,
                    timeSelectorUnselectedContentColor = p.inkSoft,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayOfWeek.entries.forEach { d ->
                    val on = d in days
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (on) p.ink else Color.Transparent)
                            .border(1.dp, if (on) p.ink else p.line, CircleShape)
                            .clickable { days = if (on) days - d else days + d },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) p.paper else p.inkSoft,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(daysLabel(days), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 6.dp))
            LineField(label, { label = it }, "Label (e.g. Wake up, Medicine)")
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) GhostButton("Delete", { AlarmScheduler.delete(context, initial); onDismiss() }, color = p.terracotta)
                Spacer(Modifier.weight(1f))
                GhostButton("Cancel", onDismiss)
                InkButton("Save", {
                    AlarmScheduler.save(
                        context,
                        initial.copy(hour = time.hour, minute = time.minute, label = label.trim(), days = days, enabled = true),
                    )
                    onDismiss()
                })
            }
        }
    }
}

// ------------------------------------------------------------------ timer

/** Survives tab switches; the system alarm is the source of truth for ringing. */
private object TimerState {
    var total by mutableLongStateOf(25 * 60_000L)
    var endsAt by mutableLongStateOf(0L) // wall-clock millis; 0 = not running
    var remainingPaused by mutableLongStateOf(25 * 60_000L)
    val running get() = endsAt > 0
}

private fun fmtDuration(ms: Long, withTenths: Boolean = false): String {
    val total = ms.coerceAtLeast(0)
    val h = total / 3_600_000
    val m = total / 60_000 % 60
    val s = total / 1000 % 60
    val cs = total / 10 % 100
    val base = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    return if (withTenths) "$base.%02d".format(cs) else base
}

@Composable
private fun TimerPane() {
    val context = LocalContext.current
    val st = TimerState
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(st.running) {
        nowMs = System.currentTimeMillis()
        while (st.running && nowMs < st.endsAt) withFrameMillis { nowMs = System.currentTimeMillis() }
    }
    val remaining = if (st.running) (st.endsAt - nowMs).coerceAtLeast(0) else st.remainingPaused
    val finished = st.running && remaining == 0L

    fun set(minutes: Int) {
        AlarmScheduler.cancelTimer(context)
        st.endsAt = 0
        st.total = minutes * 60_000L
        st.remainingPaused = st.total
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(0.72f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Ring(remaining / st.total.toFloat(), if (finished) p.sage else p.terracotta, Modifier.fillMaxSize(), stroke = 5.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (finished) "Done" else fmtDuration(remaining + if (st.running) 999 else 0),
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    when {
                        finished -> "well spent"
                        st.running -> "ends " + LocalDateTime.now().plusNanos(remaining * 1_000_000).format(Fmt.time)
                        else -> "of ${fmtDuration(st.total)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Outlined.Replay, "Reset", { set((st.total / 60_000).toInt()) }, container = p.card, tint = p.inkSoft)
            RoundIcon(
                if (st.running && !finished) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                "Start or pause",
                {
                    if (st.running && !finished) {
                        st.remainingPaused = (st.endsAt - System.currentTimeMillis()).coerceAtLeast(0)
                        st.endsAt = 0
                        AlarmScheduler.cancelTimer(context)
                    } else {
                        if (finished || st.remainingPaused <= 0) st.remainingPaused = st.total
                        st.endsAt = System.currentTimeMillis() + st.remainingPaused
                        AlarmScheduler.scheduleTimer(context, st.endsAt)
                    }
                },
                container = p.terracotta, tint = p.onAccent, size = 66.dp,
            )
            RoundIcon(Icons.Outlined.Add, "Add a minute", {
                st.total += 60_000
                if (st.running) {
                    st.endsAt += 60_000
                    AlarmScheduler.scheduleTimer(context, st.endsAt)
                } else st.remainingPaused += 60_000
            }, container = p.card, tint = p.inkSoft)
        }
        Spacer(Modifier.height(26.dp))
        SectionLabel("Presets")
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Plank" to 1, "Tea" to 4, "Breather" to 5, "Short" to 10, "Focus" to 25, "Deep work" to 50, "Nap" to 20).forEach { (name, min) ->
                Chip("$name · $min′", st.total == min * 60_000L && !st.running, { set(min) })
            }
        }
    }
}

// ------------------------------------------------------------------ stopwatch

private object StopwatchState {
    var accumulated by mutableLongStateOf(0L)
    var startedAt by mutableLongStateOf(0L) // elapsedRealtime; 0 = paused
    val laps = mutableStateListOf<Long>() // cumulative times
    val running get() = startedAt > 0
    fun elapsed() = accumulated + if (running) SystemClock.elapsedRealtime() - startedAt else 0
}

@Composable
private fun StopwatchPane() {
    val sw = StopwatchState
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sw.running) { while (sw.running) withFrameMillis { tick = it } }
    val elapsed = tick.let { sw.elapsed() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        PaperCard(Modifier.fillMaxWidth(), padding = 28.dp) {
            Text(
                fmtDuration(elapsed, withTenths = true),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp, fontFeatureSettings = "tnum"),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (sw.laps.isNotEmpty()) {
                Text(
                    "lap ${sw.laps.size + 1} · " + fmtDuration(elapsed - sw.laps.last(), true),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(
                if (sw.running) Icons.Outlined.Flag else Icons.Outlined.Replay,
                if (sw.running) "Lap" else "Reset",
                {
                    if (sw.running) sw.laps.add(sw.elapsed())
                    else { sw.accumulated = 0; sw.laps.clear() }
                },
                container = p.card, tint = p.inkSoft,
            )
            RoundIcon(
                if (sw.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                "Start or pause",
                {
                    if (sw.running) { sw.accumulated = sw.elapsed(); sw.startedAt = 0 }
                    else sw.startedAt = SystemClock.elapsedRealtime()
                },
                container = p.ink, tint = p.paper, size = 66.dp,
            )
        }
        if (sw.laps.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionLabel("Laps")
            val splits = sw.laps.mapIndexed { i, t -> t - (if (i == 0) 0 else sw.laps[i - 1]) }
            val best = if (splits.size > 1) splits.min() else -1
            val worst = if (splits.size > 1) splits.max() else -1
            splits.withIndex().reversed().forEach { (i, s) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text("Lap ${i + 1}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        fmtDuration(s, true),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        color = when (s) { best -> p.sage; worst -> p.terracotta; else -> p.ink },
                    )
                    Text(
                        fmtDuration(sw.laps[i], true),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        modifier = Modifier.width(90.dp).padding(start = 16.dp),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ world

private fun cityOf(zone: String) = zone.substringAfterLast('/').replace('_', ' ')

@Composable
private fun WorldPane(now: LocalDateTime) {
    val data by Store.data.collectAsState()
    var adding by remember { mutableStateOf(false) }
    val here = ZonedDateTime.now()

    Column {
        SectionLabel("Around the world", trailing = {
            RoundIcon(Icons.Outlined.Add, "Add city", { adding = true }, size = 34.dp)
        })
        if (data.worldZones.isEmpty()) EmptyNote("Add a city to keep its hour close.")
        data.worldZones.forEach { z ->
            val zt = runCatching { ZonedDateTime.now(ZoneId.of(z)) }.getOrNull() ?: return@forEach
            val diffH = (zt.offset.totalSeconds - here.offset.totalSeconds) / 3600f
            val dayShift = zt.toLocalDate().compareTo(now.toLocalDate())
            val daylight = zt.hour in 6..18
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (daylight) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode, null,
                    tint = if (daylight) p.brass else p.slate, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(cityOf(z), style = MaterialTheme.typography.titleMedium)
                    val rel = when {
                        diffH == 0f -> "Same time"
                        else -> (if (diffH > 0) "+" else "−") + formatHours(kotlin.math.abs(diffH))
                    }
                    val dayWord = when { dayShift > 0 -> " · tomorrow"; dayShift < 0 -> " · yesterday"; else -> "" }
                    Text(rel + dayWord, style = MaterialTheme.typography.bodySmall)
                }
                Text(zt.format(Fmt.time), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.Close, "Remove",
                    tint = p.muted,
                    modifier = Modifier.size(18.dp).clip(CircleShape).clickable { Store.removeZone(z) },
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        }
    }
    if (adding) ZoneSheet { adding = false }
}

private fun formatHours(h: Float): String =
    if (h % 1f == 0f) "${h.toInt()}h" else "${h.toInt()}h ${((h % 1f) * 60).toInt()}m"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneSheet(onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val zones = remember {
        ZoneId.getAvailableZoneIds()
            .filter { it.contains('/') && !it.startsWith("Etc") && !it.startsWith("SystemV") && it.first().isUpperCase() }
            .sortedBy { cityOf(it) }
    }
    val shown = zones.filter { q.isBlank() || cityOf(it).contains(q, true) || it.contains(q, true) }.take(80)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().imePadding()) {
            Text("ADD A CITY", style = MaterialTheme.typography.labelMedium, color = p.terracotta)
            LineField(q, { q = it }, "Search — Paris, Lagos, Seoul…", large = true)
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(shown, key = { it }) { z ->
                    Row(
                        Modifier.fillMaxWidth().clickable { Store.addZone(z); onDismiss() }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(cityOf(z), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(z.substringBefore('/'), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
