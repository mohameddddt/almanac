package com.almanac.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class Priority { Low, Normal, High }

data class Task(
    val id: Long,
    val title: String,
    val done: Boolean = false,
    val priority: Priority = Priority.Normal,
    val due: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long? = null,
)

data class Event(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val tint: Int = 0,
    val note: String = "",
)

data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    /** Empty means a one-off alarm. */
    val days: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
)

data class Habit(
    val id: Long,
    val name: String,
    val tint: Int = 0,
    val doneDates: Set<LocalDate> = emptySet(),
) {
    fun streak(today: LocalDate = LocalDate.now()): Int {
        var day = if (today in doneDates) today else today.minusDays(1)
        var count = 0
        while (day in doneDates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}

data class AppData(
    val tasks: List<Task> = emptyList(),
    val events: List<Event> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val habits: List<Habit> = emptyList(),
    /** Mood per day, 0 (heavy) .. 4 (bright). */
    val moods: Map<LocalDate, Int> = emptyMap(),
    /** A single free-form note per day. */
    val notes: Map<LocalDate, String> = emptyMap(),
    val worldZones: List<String> = listOf("Europe/London", "Asia/Tokyo", "America/New_York"),
    /** 0 = follow system, 1 = light, 2 = dark. */
    val themeMode: Int = 0,
)
