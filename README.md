# Almanac

An offline Android companion for your days: calendar, clock, tasks and small rituals. It's built with Kotlin and Jetpack Compose and has no network access. Everything is stored in one JSON file in the app's private storage.

## Screens
- **Today**: greeting, a large serif clock, day/week/year progress, the real moon phase (calculated on the device), a mood check-in, today's agenda and rituals, a daily aphorism and "a line for today".
- **Calendar**: month grid (swipe or arrows), coloured event dots, task rings, mood underlines and full/new moon markers. Tap a day to add or edit events.
- **Clock**: hand-drawn analog face, plus
  - **Alarms**: real system alarms with repeat days, snooze and dismiss. They survive reboots and app updates.
  - **Timer**: presets (Plank, Tea, Focus 25, Deep work…). It rings even when the app is closed.
  - **Stopwatch**: laps, with the best and worst splits highlighted.
  - **World**: cities with day/night indicator and time offset.
- **Tasks**: priorities, due dates, grouped Overdue / Today / Upcoming / Someday, swipe to delete.
- **Rituals**: habit tracker with streaks, a 30-day tally and a five-week mood map.

Theme: "Parchment & Ink". Toggle system / light / dark from the button on Today.

## Build
```
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # minified, signed with the debug key for personal installs
```
Requires the Android SDK (compileSdk 36) and JDK 17. minSdk 26.

## Structure
```
data/        Models + Store (JSON persistence)
alarm/       AlarmScheduler, AlarmReceiver, BootReceiver
ui/theme/    Palette, fonts (DM Serif Display + DM Sans, bundled)
ui/components  Shared widgets, moon math, aphorisms, pickers
ui/screens   Today, Calendar, Clock, Tasks, Habits
```
