package com.almanac.app.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almanac.app.alarm.AlarmScheduler
import com.almanac.app.data.Alarm
import com.almanac.app.data.Store
import com.almanac.app.ui.components.AlmanacTimePicker
import com.almanac.app.ui.components.Blank
import com.almanac.app.ui.components.FrameButton
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.LedgerRow
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Masthead
import com.almanac.app.ui.components.Meter
import com.almanac.app.ui.components.Rule
import com.almanac.app.ui.components.SectionHead
import com.almanac.app.ui.components.Segmented
import com.almanac.app.ui.components.Sheet
import com.almanac.app.ui.components.SquareToggle
import com.almanac.app.ui.components.TextAction
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberNow
import com.almanac.app.ui.components.tap
import com.almanac.app.ui.theme.Type
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Panes = listOf("Alarms", "Timer", "Stopwatch", "World")
private val HM = DateTimeFormatter.ofPattern("HH:mm")
private val HMS = DateTimeFormatter.ofPattern("HH:mm:ss")
private val DayStamp = DateTimeFormatter.ofPattern("EEE d MMM")

@Composable
fun ClockScreen() {
    var pane by rememberSaveable { mutableIntStateOf(0) }
    val here = remember { ZonedDateTime.now() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        Masthead("Clock", "${cityOf(here.zone.id)} · UTC${here.offset.id.replace("Z", "+00:00")}")
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnalogFace(Modifier.fillMaxWidth(0.7f).aspectRatio(1f))
        }
        Spacer(Modifier.height(14.dp))
        DigitalLine(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
        Segmented(Panes, pane, { pane = it })
        Spacer(Modifier.height(24.dp))
        when (pane) {
            0 -> AlarmsPane()
            1 -> TimerPane()
            2 -> StopwatchPane()
            else -> WorldPane()
        }
    }
}

// ------------------------------------------------------------------ face

/** Redraws every frame, but only the canvas — nothing recomposes. */
@Composable
private fun AnalogFace(modifier: Modifier = Modifier) {
    var millis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { millis = System.currentTimeMillis() } }
    val ink = p.ink
    val faint = p.muted
    val signal = p.signal

    Canvas(modifier) {
        val t = LocalDateTime.now()
        val sec = t.second + (millis % 1000) / 1000f
        val min = t.minute + sec / 60f
        val hour = (t.hour % 12) + min / 60f
        val r = size.minDimension / 2
        val c = center

        drawCircle(ink, r - 1.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
        for (i in 0 until 60) {
            val a = i / 60f * 2 * PI.toFloat()
            val hourMark = i % 5 == 0
            drawLine(
                color = if (hourMark) ink else faint,
                start = c.polar(r * if (hourMark) 0.74f else 0.84f, a),
                end = c.polar(r * 0.9f, a),
                strokeWidth = (if (hourMark) 3.5.dp else 1.dp).toPx(),
            )
        }
        hand(c, hour / 12f, r * 0.5f, 6.dp.toPx(), ink)
        hand(c, min / 60f, r * 0.82f, 4.dp.toPx(), ink)
        val sa = sec / 60f * 2 * PI.toFloat()
        drawLine(signal, c.polar(-r * 0.2f, sa), c.polar(r * 0.86f, sa), 1.5.dp.toPx())
        drawCircle(signal, 4.dp.toPx(), c)
    }
}

private fun Offset.polar(len: Float, angle: Float) = Offset(x + len * sin(angle), y - len * cos(angle))

private fun DrawScope.hand(c: Offset, turn: Float, len: Float, width: Float, color: Color) {
    val a = turn * 2 * PI.toFloat()
    drawLine(color, c.polar(-len * 0.14f, a), c.polar(len, a), width, StrokeCap.Butt)
}

@Composable
private fun DigitalLine(modifier: Modifier = Modifier) {
    val now = rememberNow(1000)
    Text(
        now.format(HMS) + "  ·  " + now.format(DayStamp).uppercase(),
        style = Type.data.copy(fontSize = 14.sp),
        color = p.ink,
        modifier = modifier,
    )
}

// ------------------------------------------------------------------ alarms

private fun daysLabel(days: Set<DayOfWeek>) = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Daily"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    days.size == 5 && DayOfWeek.SATURDAY !in days && DayOfWeek.SUNDAY !in days -> "Weekdays"
    else -> "Custom"
}

private fun untilLabel(alarm: Alarm): String {
    val d = Duration.between(LocalDateTime.now(), AlarmScheduler.nextTrigger(alarm))
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return when {
        h >= 24 -> "in ${h / 24}d ${h % 24}h"
        h > 0 -> "in ${h}h ${m}m"
        else -> "in ${m.coerceAtLeast(1)}m"
    }
}

/** `M T W T F S S` with the active days inked and the rest faint. */
@Composable
private fun DayLetters(days: Set<DayOfWeek>) {
    val on = p.ink
    val off = p.rule
    Text(
        buildAnnotatedString {
            DayOfWeek.entries.forEach { d ->
                withStyle(SpanStyle(color = if (days.isEmpty() || d in days) on else off)) {
                    append(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
                }
                append(" ")
            }
        },
        style = Type.tag,
    )
}

@Composable
private fun AlarmsPane() {
    val context = LocalContext.current
    val data by Store.data.collectAsState()
    var editing by remember { mutableStateOf<Alarm?>(null) }

    SectionHead("A", "Alarms", trailing = "${data.alarms.count { it.enabled }} on")
    if (data.alarms.isEmpty()) Blank("No alarms.")
    AlarmRows(data.alarms) { editing = it }
    Spacer(Modifier.height(14.dp))
    InkButton("+ New alarm", { editing = Alarm(Store.newId(), 7, 0) }, Modifier.fillMaxWidth())
    editing?.let { AlarmSheet(it) { editing = null } }
}

@Composable
private fun AlarmRows(alarms: List<Alarm>, onEdit: (Alarm) -> Unit) {
    val context = LocalContext.current
    rememberNow(30_000) // keeps "in 3h 12m" current without touching the rest of the pane
    alarms.sortedBy { it.hour * 60 + it.minute }.forEach { a ->
        Column(Modifier.fillMaxWidth().tap { onEdit(a) }) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "%02d:%02d".format(a.hour, a.minute),
                        style = MaterialTheme.typography.displayMedium,
                        color = if (a.enabled) p.ink else p.muted,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DayLetters(a.days)
                        Text(
                            listOf(daysLabel(a.days), a.label.ifBlank { null }, if (a.enabled) untilLabel(a) else "off")
                                .filterNotNull().joinToString(" · ").uppercase(),
                            style = Type.tag,
                        )
                    }
                }
                SquareToggle(a.enabled) { AlarmScheduler.save(context, a.copy(enabled = it)) }
            }
            Rule()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSheet(initial: Alarm, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isNew = remember { Store.data.value.alarms.none { it.id == initial.id } }
    val time = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableStateOf(initial.days) }

    Sheet(if (isNew) "New alarm" else "Edit alarm", onDismiss) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AlmanacTimePicker(time) }
        Text("REPEAT · ${daysLabel(days).uppercase()}", style = Type.tag)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { d ->
                val on = d in days
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(if (on) p.ink else Color.Transparent)
                        .border(1.dp, p.ink)
                        .tap { days = if (on) days - d else days + d },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = Type.tag,
                        color = if (on) p.paper else p.ink,
                    )
                }
            }
        }
        LineField(label, { label = it }, "Label")
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isNew) TextAction("Delete", { AlarmScheduler.delete(context, initial); onDismiss() }, color = p.signal)
            Spacer(Modifier.weight(1f))
            TextAction("Cancel", onDismiss)
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

// ------------------------------------------------------------------ timer

/** Survives tab switches; the system alarm is what actually rings. */
private object TimerState {
    var total by mutableLongStateOf(25 * 60_000L)
    var endsAt by mutableLongStateOf(0L) // wall-clock millis; 0 = not running
    var remainingPaused by mutableLongStateOf(25 * 60_000L)
    val running get() = endsAt > 0
}

private fun fmtDuration(ms: Long, withHundredths: Boolean = false): String {
    val total = ms.coerceAtLeast(0)
    val h = total / 3_600_000
    val m = total / 60_000 % 60
    val s = total / 1000 % 60
    val cs = total / 10 % 100
    val base = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    return if (withHundredths) "$base.%02d".format(cs) else base
}

private val Presets = listOf(
    1 to "Plank", 4 to "Tea", 5 to "Pause", 10 to "Short",
    20 to "Nap", 25 to "Focus", 45 to "Class", 50 to "Deep",
)

@Composable
private fun TimerPane() {
    val context = LocalContext.current
    val st = TimerState

    fun set(minutes: Int) {
        AlarmScheduler.cancelTimer(context)
        st.endsAt = 0
        st.total = minutes * 60_000L
        st.remainingPaused = st.total
    }

    SectionHead("A", "Timer", trailing = if (st.running) "RUNNING" else "READY")
    Spacer(Modifier.height(10.dp))
    TimerReadout()
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val ticking = st.running && st.endsAt > System.currentTimeMillis()
        InkButton(
            if (ticking) "Pause" else "Start",
            {
                if (st.running && st.endsAt > System.currentTimeMillis()) {
                    st.remainingPaused = (st.endsAt - System.currentTimeMillis()).coerceAtLeast(0)
                    st.endsAt = 0
                    AlarmScheduler.cancelTimer(context)
                } else {
                    if (st.running || st.remainingPaused <= 0) st.remainingPaused = st.total
                    st.endsAt = System.currentTimeMillis() + st.remainingPaused
                    AlarmScheduler.scheduleTimer(context, st.endsAt)
                }
            },
            Modifier.weight(1f),
            color = if (ticking) p.ink else p.signal,
        )
        FrameButton("Reset", { set((st.total / 60_000).toInt()) })
        FrameButton("+1 min", {
            st.total += 60_000
            if (st.running) {
                st.endsAt += 60_000
                AlarmScheduler.scheduleTimer(context, st.endsAt)
            } else st.remainingPaused += 60_000
        })
    }
    Spacer(Modifier.height(28.dp))
    SectionHead("B", "Presets")
    Spacer(Modifier.height(10.dp))
    Presets.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (min, name) ->
                val on = st.total == min * 60_000L && !st.running
                Column(
                    Modifier
                        .weight(1f)
                        .background(if (on) p.ink else Color.Transparent)
                        .border(1.dp, p.ink)
                        .tap { set(min) }
                        .padding(10.dp),
                ) {
                    Text("$min′", style = MaterialTheme.typography.headlineMedium, color = if (on) p.paper else p.ink)
                    Text(name.uppercase(), style = Type.tag, color = if (on) p.paper else p.muted)
                }
            }
        }
    }
}

/** Reads the frame clock while running; isolated so nothing else recomposes per frame. */
@Composable
private fun TimerReadout() {
    val st = TimerState
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(st.running, st.endsAt) {
        nowMs = System.currentTimeMillis()
        while (st.running && nowMs < st.endsAt) withFrameMillis { nowMs = System.currentTimeMillis() }
    }
    val remaining = if (st.running) (st.endsAt - nowMs).coerceAtLeast(0) else st.remainingPaused
    val finished = st.running && remaining == 0L
    Text(
        if (finished) "00:00" else fmtDuration(remaining + if (st.running) 999 else 0),
        style = Type.numeral,
        color = if (finished) p.signal else p.ink,
    )
    Spacer(Modifier.height(6.dp))
    Meter(1f - remaining / st.total.toFloat(), p.signal, thickness = 4.dp)
    Spacer(Modifier.height(6.dp))
    Text(
        when {
            finished -> "DONE"
            st.running -> "ENDS " + LocalDateTime.now().plusNanos(remaining * 1_000_000).format(HM)
            else -> "OF ${fmtDuration(st.total)}"
        },
        style = Type.tag,
    )
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
    SectionHead("A", "Stopwatch", trailing = "${sw.laps.size} laps")
    Spacer(Modifier.height(10.dp))
    StopwatchReadout()
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InkButton(
            if (sw.running) "Stop" else "Start",
            {
                if (sw.running) { sw.accumulated = sw.elapsed(); sw.startedAt = 0 }
                else sw.startedAt = SystemClock.elapsedRealtime()
            },
            Modifier.weight(1f),
            color = if (sw.running) p.ink else p.signal,
        )
        FrameButton(if (sw.running) "Lap" else "Reset", {
            if (sw.running) sw.laps.add(sw.elapsed())
            else { sw.accumulated = 0; sw.laps.clear() }
        })
    }
    if (sw.laps.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        val splits = sw.laps.mapIndexed { i, t -> t - (if (i == 0) 0 else sw.laps[i - 1]) }
        val best = if (splits.size > 1) splits.min() else -1
        val worst = if (splits.size > 1) splits.max() else -1
        Rule(color = p.ink)
        splits.withIndex().reversed().forEach { (i, s) ->
            LedgerRow(
                lead = "%02d".format(i + 1),
                trailing = { Text(fmtDuration(sw.laps[i], true), style = Type.data, color = p.muted) },
            ) {
                Text(
                    fmtDuration(s, true) + when (s) { best -> "  FASTEST"; worst -> "  SLOWEST"; else -> "" },
                    style = Type.data.copy(fontSize = 15.sp),
                    color = when (s) { best -> p.green; worst -> p.signal; else -> p.ink },
                )
            }
        }
    }
}

@Composable
private fun StopwatchReadout() {
    val sw = StopwatchState
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sw.running) { while (sw.running) withFrameMillis { tick = it } }
    val elapsed = tick.let { sw.elapsed() }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(fmtDuration(elapsed), style = Type.numeral)
        Text(
            ".%02d".format(elapsed / 10 % 100),
            style = Type.data.copy(fontSize = 26.sp),
            color = p.signal,
            modifier = Modifier.padding(bottom = 14.dp),
        )
    }
}

// ------------------------------------------------------------------ world

private fun cityOf(zone: String) = zone.substringAfterLast('/').replace('_', ' ')

@Composable
private fun WorldPane() {
    val data by Store.data.collectAsState()
    var adding by remember { mutableStateOf(false) }

    SectionHead("A", "World", trailing = "${data.worldZones.size} cities")
    if (data.worldZones.isEmpty()) Blank("No cities.")
    WorldRows(data.worldZones)
    Spacer(Modifier.height(14.dp))
    InkButton("+ Add city", { adding = true }, Modifier.fillMaxWidth())
    if (adding) ZoneSheet { adding = false }
}

@Composable
private fun WorldRows(zones: List<String>) {
    rememberNow(30_000)
    val here = ZonedDateTime.now()
    zones.forEach { z ->
        val zt = runCatching { ZonedDateTime.now(ZoneId.of(z)) }.getOrNull() ?: return@forEach
        val diffH = (zt.offset.totalSeconds - here.offset.totalSeconds) / 3600f
        val dayShift = zt.toLocalDate().compareTo(here.toLocalDate())
        val daylight = zt.hour in 6..18
        LedgerRow(
            lead = if (diffH == 0f) "±0" else (if (diffH > 0) "+" else "−") + formatHours(abs(diffH)),
            leadColor = p.ink,
            trailing = {
                Text(zt.format(HM), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "×",
                    style = Type.data.copy(fontSize = 18.sp),
                    color = p.muted,
                    modifier = Modifier.tap { Store.removeZone(z) }.padding(start = 14.dp, end = 2.dp),
                )
            },
        ) {
            Column {
                Text(cityOf(z), style = MaterialTheme.typography.titleMedium)
                Text(
                    (if (daylight) "DAY" else "NIGHT") + when { dayShift > 0 -> " · TOMORROW"; dayShift < 0 -> " · YESTERDAY"; else -> "" },
                    style = Type.tag,
                    color = if (daylight) p.ochre else p.cobalt,
                )
            }
        }
    }
}

private fun formatHours(h: Float): String =
    if (h % 1f == 0f) "${h.toInt()}h" else "${h.toInt()}h${((h % 1f) * 60).toInt()}"

@Composable
private fun ZoneSheet(onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val zones = remember {
        ZoneId.getAvailableZoneIds()
            .filter { it.contains('/') && !it.startsWith("Etc") && !it.startsWith("SystemV") && it.first().isUpperCase() }
            .sortedBy { cityOf(it) }
    }
    val shown = remember(q) { zones.filter { q.isBlank() || cityOf(it).contains(q, true) || it.contains(q, true) }.take(80) }
    Sheet("Add a city", onDismiss) {
        LineField(q, { q = it }, "Search", large = true)
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(shown, key = { it }) { z ->
                LedgerRow(lead = z.substringBefore('/').take(7), onClick = { Store.addZone(z); onDismiss() }, leadWidth = 80.dp) {
                    Text(cityOf(z), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
