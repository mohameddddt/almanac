package com.almanac.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.PI

/**
 * Current time, refreshed every [periodMs] and aligned to the period boundary.
 * Read it in the smallest composable that needs it — whoever reads it recomposes on every tick.
 */
@Composable
fun rememberNow(periodMs: Long = 1000L): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(periodMs) {
        while (true) {
            now = LocalDateTime.now()
            delay(periodMs - System.currentTimeMillis() % periodMs)
        }
    }
    return now
}

/** Today's date; only changes at midnight, so screens built on it stay still. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            val d = LocalDate.now()
            if (d != today) today = d
        }
    }
    return today
}

object Fmt {
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
    val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    val shortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

    fun weekday(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    fun weekdayShort(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun relative(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(shortDay)
    }

    fun period(hour: Int) = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..21 -> "Evening"
        else -> "Night"
    }
}

/** Mood levels, 1–5, written plainly. */
val MoodWords = listOf("Low", "Flat", "Fine", "Good", "Great")

/** Lunar phase computed from the mean synodic month — accurate to within a day, no network needed. */
object Moon {
    private const val SYNODIC = 29.530588853
    private const val REFERENCE_NEW_MOON_EPOCH_DAY = 10962.76 // 2000-01-06 18:14 UTC

    /** Age in days since the last new moon. */
    fun age(date: LocalDate): Double {
        val day = date.atTime(12, 0).toEpochSecond(ZoneOffset.UTC) / 86400.0
        val a = (day - REFERENCE_NEW_MOON_EPOCH_DAY) % SYNODIC
        return if (a < 0) a + SYNODIC else a
    }

    /** 0.0 = new, 0.5 = full, back to 1.0. */
    fun fraction(date: LocalDate) = age(date) / SYNODIC

    fun illumination(date: LocalDate) = (1 - cos(2 * PI * fraction(date))) / 2

    fun name(date: LocalDate): String {
        val f = fraction(date)
        return when {
            f < 0.0339 || f > 0.9661 -> "New moon"
            f < 0.216 -> "Waxing crescent"
            f < 0.284 -> "First quarter"
            f < 0.466 -> "Waxing gibbous"
            f < 0.534 -> "Full moon"
            f < 0.716 -> "Waning gibbous"
            f < 0.784 -> "Last quarter"
            else -> "Waning crescent"
        }
    }

    fun nextFull(from: LocalDate): LocalDate {
        var d = from
        repeat(31) {
            if (name(d) == "Full moon" && name(d.minusDays(1)) != "Full moon") return d
            d = d.plusDays(1)
        }
        return d
    }
}

object Aphorisms {
    private val all = listOf(
        "Well begun is half done." to "Aristotle",
        "It is not that we have a short time to live, but that we waste a lot of it." to "Seneca",
        "The impediment to action advances action. What stands in the way becomes the way." to "Marcus Aurelius",
        "Nature does not hurry, yet everything is accomplished." to "Lao Tzu",
        "Our life is frittered away by detail. Simplify, simplify." to "Henry David Thoreau",
        "How we spend our days is, of course, how we spend our lives." to "Annie Dillard",
        "The secret of getting ahead is getting started." to "Mark Twain",
        "Adopt the pace of nature: her secret is patience." to "Ralph Waldo Emerson",
        "Little by little, one travels far." to "J. R. R. Tolkien",
        "Lost time is never found again." to "Benjamin Franklin",
        "Do what you can, with what you have, where you are." to "Theodore Roosevelt",
        "He who has a why to live can bear almost any how." to "Friedrich Nietzsche",
        "Begin at once to live, and count each separate day as a separate life." to "Seneca",
        "The best time to plant a tree was twenty years ago. The second best time is now." to "Proverb",
        "Rest is not idleness." to "John Lubbock",
        "You must live in the present, launch yourself on every wave." to "Henry David Thoreau",
        "What we do every day matters more than what we do once in a while." to "Gretchen Rubin",
        "Waste no more time arguing what a good person should be. Be one." to "Marcus Aurelius",
        "A year from now you may wish you had started today." to "Karen Lamb",
        "Light tomorrow with today." to "Elizabeth Barrett Browning",
        "In the middle of difficulty lies opportunity." to "Albert Einstein",
        "Each morning we are born again. What we do today matters most." to "Buddha",
        "Small deeds done are better than great deeds planned." to "Peter Marshall",
        "Be patient with all that is unsolved in your heart." to "Rainer Maria Rilke",
        "The day is what you make it." to "Proverb",
        "Slow is smooth, and smooth is fast." to "Proverb",
        "To find yourself, think for yourself." to "Socrates",
        "Every moment is a fresh beginning." to "T. S. Eliot",
        "The mind is everything. What you think you become." to "Buddha",
        "One today is worth two tomorrows." to "Benjamin Franklin",
        "Keep your face always toward the sunshine." to "Walt Whitman",
    )

    fun forDay(d: LocalDate): Pair<String, String> = all[(d.toEpochDay() % all.size).toInt()]
}
