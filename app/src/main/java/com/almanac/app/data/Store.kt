package com.almanac.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Everything lives in one small JSON file in the app's private storage.
 * Reads happen once at start-up; every change is written back on a single
 * background thread so the UI never waits on disk.
 */
object Store {
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private lateinit var file: File
    private val writer = Executors.newSingleThreadExecutor()
    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        file = File(context.applicationContext.filesDir, "almanac.json")
        _data.value = runCatching { decode(JSONObject(file.readText())) }
            .getOrElse { seed() }
        loaded = true
    }

    fun newId(): Long = System.nanoTime()

    /** Latest state waiting to be written; null when the disk is up to date. */
    private val pending = AtomicReference<AppData?>(null)

    fun edit(transform: (AppData) -> AppData) {
        _data.update(transform)
        // Only schedule a write if none is queued; a queued write always picks up the newest state,
        // so a burst of edits (typing a note) costs one serialization, not one per keystroke.
        if (pending.getAndSet(_data.value) == null) writer.execute(::flush)
    }

    private fun flush() {
        val snapshot = pending.getAndSet(null) ?: return
        val json = encode(snapshot).toString()
        val tmp = File(file.parentFile, "almanac.json.tmp")
        tmp.writeText(json)
        tmp.renameTo(file)
    }

    // ---- tasks ----
    fun addTask(title: String, priority: Priority, due: LocalDate?) = edit {
        it.copy(tasks = it.tasks + Task(newId(), title.trim(), priority = priority, due = due))
    }

    fun toggleTask(id: Long) = edit { d ->
        d.copy(tasks = d.tasks.map {
            if (it.id == id) it.copy(done = !it.done, doneAt = if (it.done) null else System.currentTimeMillis()) else it
        })
    }

    fun updateTask(task: Task) = edit { d -> d.copy(tasks = d.tasks.map { if (it.id == task.id) task else it }) }
    fun deleteTask(id: Long) = edit { d -> d.copy(tasks = d.tasks.filterNot { it.id == id }) }
    fun clearDone() = edit { d -> d.copy(tasks = d.tasks.filterNot { it.done }) }

    // ---- events ----
    fun upsertEvent(event: Event) = edit { d ->
        val exists = d.events.any { it.id == event.id }
        d.copy(events = if (exists) d.events.map { if (it.id == event.id) event else it } else d.events + event)
    }

    fun deleteEvent(id: Long) = edit { d -> d.copy(events = d.events.filterNot { it.id == id }) }

    // ---- habits ----
    fun addHabit(name: String, tint: Int) = edit { it.copy(habits = it.habits + Habit(newId(), name.trim(), tint)) }
    fun deleteHabit(id: Long) = edit { d -> d.copy(habits = d.habits.filterNot { it.id == id }) }
    fun toggleHabit(id: Long, day: LocalDate) = edit { d ->
        d.copy(habits = d.habits.map {
            if (it.id != id) it
            else it.copy(doneDates = if (day in it.doneDates) it.doneDates - day else it.doneDates + day)
        })
    }

    // ---- day details ----
    fun setMood(day: LocalDate, mood: Int?) = edit { d ->
        d.copy(moods = if (mood == null) d.moods - day else d.moods + (day to mood))
    }

    fun setNote(day: LocalDate, text: String) = edit { d ->
        d.copy(notes = if (text.isBlank()) d.notes - day else d.notes + (day to text))
    }

    // ---- clock ----
    fun setAlarms(alarms: List<Alarm>) = edit { it.copy(alarms = alarms) }
    fun addZone(zone: String) = edit { if (zone in it.worldZones) it else it.copy(worldZones = it.worldZones + zone) }
    fun removeZone(zone: String) = edit { it.copy(worldZones = it.worldZones - zone) }

    fun setThemeMode(mode: Int) = edit { it.copy(themeMode = mode) }

    // ---- first launch ----
    private fun seed(): AppData {
        val today = LocalDate.now()
        return AppData(
            tasks = listOf(
                Task(1, "Tick the box to finish a task", priority = Priority.High, due = today),
                Task(2, "Swipe sideways to delete", due = today),
                Task(3, "Plan the weekend", priority = Priority.Low),
            ),
            events = listOf(Event(4, "Coffee with Sam", today.plusDays(2), LocalTime.of(10, 30), tint = 1)),
            habits = listOf(
                Habit(5, "Read", 0),
                Habit(6, "Walk", 1),
                Habit(7, "Stretch", 2),
            ),
        )
    }

    // ---- JSON ----
    private fun encode(d: AppData) = JSONObject().apply {
        put("v", 1)
        put("tasks", JSONArray(d.tasks.map {
            JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)
                .put("priority", it.priority.name).put("due", it.due?.toString())
                .put("createdAt", it.createdAt).put("doneAt", it.doneAt)
        }))
        put("events", JSONArray(d.events.map {
            JSONObject().put("id", it.id).put("title", it.title).put("date", it.date.toString())
                .put("time", it.time?.toString()).put("tint", it.tint).put("note", it.note)
        }))
        put("alarms", JSONArray(d.alarms.map {
            JSONObject().put("id", it.id).put("hour", it.hour).put("minute", it.minute)
                .put("label", it.label).put("enabled", it.enabled)
                .put("days", JSONArray(it.days.map { day -> day.value }))
        }))
        put("habits", JSONArray(d.habits.map {
            JSONObject().put("id", it.id).put("name", it.name).put("tint", it.tint)
                .put("done", JSONArray(it.doneDates.map(LocalDate::toString)))
        }))
        put("moods", JSONObject().apply { d.moods.forEach { (k, v) -> put(k.toString(), v) } })
        put("notes", JSONObject().apply { d.notes.forEach { (k, v) -> put(k.toString(), v) } })
        put("zones", JSONArray(d.worldZones))
        put("theme", d.themeMode)
    }

    private fun decode(o: JSONObject) = AppData(
        tasks = o.optJSONArray("tasks").objects().map {
            Task(
                id = it.getLong("id"),
                title = it.getString("title"),
                done = it.optBoolean("done"),
                priority = runCatching { Priority.valueOf(it.getString("priority")) }.getOrDefault(Priority.Normal),
                due = it.optStringOrNull("due")?.let(LocalDate::parse),
                createdAt = it.optLong("createdAt"),
                doneAt = if (it.isNull("doneAt")) null else it.optLong("doneAt"),
            )
        },
        events = o.optJSONArray("events").objects().map {
            Event(
                id = it.getLong("id"),
                title = it.getString("title"),
                date = LocalDate.parse(it.getString("date")),
                time = it.optStringOrNull("time")?.let(LocalTime::parse),
                tint = it.optInt("tint"),
                note = it.optString("note"),
            )
        },
        alarms = o.optJSONArray("alarms").objects().map {
            val days = it.optJSONArray("days")
            Alarm(
                id = it.getLong("id"),
                hour = it.getInt("hour"),
                minute = it.getInt("minute"),
                label = it.optString("label"),
                enabled = it.optBoolean("enabled", true),
                days = (0 until (days?.length() ?: 0)).map { i -> DayOfWeek.of(days!!.getInt(i)) }.toSet(),
            )
        },
        habits = o.optJSONArray("habits").objects().map {
            val done = it.optJSONArray("done")
            Habit(
                id = it.getLong("id"),
                name = it.getString("name"),
                tint = it.optInt("tint"),
                doneDates = (0 until (done?.length() ?: 0)).map { i -> LocalDate.parse(done!!.getString(i)) }.toSet(),
            )
        },
        moods = o.optJSONObject("moods")?.let { m -> m.keys().asSequence().associate { LocalDate.parse(it) to m.getInt(it) } }
            ?: emptyMap(),
        notes = o.optJSONObject("notes")?.let { m -> m.keys().asSequence().associate { LocalDate.parse(it) to m.getString(it) } }
            ?: emptyMap(),
        worldZones = o.optJSONArray("zones")?.let { z -> (0 until z.length()).map(z::getString) } ?: AppData().worldZones,
        themeMode = o.optInt("theme"),
    )

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map(::getJSONObject)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
