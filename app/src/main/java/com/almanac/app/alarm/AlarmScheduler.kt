package com.almanac.app.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.almanac.app.MainActivity
import com.almanac.app.data.Alarm
import com.almanac.app.data.Store
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {
    const val CHANNEL = "almanac_alarms"
    const val ACTION_FIRE = "com.almanac.app.FIRE"
    const val ACTION_DISMISS = "com.almanac.app.DISMISS"
    const val ACTION_SNOOZE = "com.almanac.app.SNOOZE"
    const val EXTRA_ID = "id"
    const val EXTRA_LABEL = "label"
    const val EXTRA_KIND = "kind"
    const val KIND_ALARM = "alarm"
    const val KIND_TIMER = "timer"
    const val KIND_SNOOZE = "snooze"
    const val TIMER_ID = -42L
    const val SNOOZE_MINUTES = 10L

    fun nextTrigger(alarm: Alarm, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val base = now.toLocalDate().atTime(alarm.hour, alarm.minute)
        if (alarm.days.isEmpty()) return if (base.isAfter(now)) base else base.plusDays(1)
        for (offset in 0..7L) {
            val candidate = base.plusDays(offset)
            if (candidate.dayOfWeek in alarm.days && candidate.isAfter(now)) return candidate
        }
        return base.plusDays(7)
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(CHANNEL, "Alarms & timers", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings for your alarms and finished timers"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
        }
        nm.createNotificationChannel(channel)
    }

    private fun fireIntent(context: Context, id: Long, label: String, kind: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_KIND, kind)
        val code = if (kind == KIND_SNOOZE) (id.hashCode() xor 0x5A5A) else id.hashCode()
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun setExact(context: Context, atMillis: Long, operation: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), operation)
        } catch (_: SecurityException) {
            // Exact alarms revoked by the user — fall back to a near-exact window.
            am.setWindow(AlarmManager.RTC_WAKEUP, atMillis, 60_000, operation)
        }
    }

    fun schedule(context: Context, alarm: Alarm) {
        val pi = fireIntent(context, alarm.id, alarm.label, KIND_ALARM)
        if (!alarm.enabled) {
            context.getSystemService(AlarmManager::class.java).cancel(pi)
            return
        }
        val at = nextTrigger(alarm).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setExact(context, at, pi)
    }

    fun cancel(context: Context, alarm: Alarm) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(fireIntent(context, alarm.id, alarm.label, KIND_ALARM))
        am.cancel(fireIntent(context, alarm.id, alarm.label, KIND_SNOOZE))
    }

    fun snooze(context: Context, id: Long, label: String) {
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000
        setExact(context, at, fireIntent(context, id, label, KIND_SNOOZE))
    }

    fun scheduleTimer(context: Context, endMillis: Long) =
        setExact(context, endMillis, fireIntent(context, TIMER_ID, "Timer finished", KIND_TIMER))

    fun cancelTimer(context: Context) =
        context.getSystemService(AlarmManager::class.java)
            .cancel(fireIntent(context, TIMER_ID, "Timer finished", KIND_TIMER))

    fun syncAll(context: Context) {
        Store.init(context)
        Store.data.value.alarms.forEach { schedule(context, it) }
    }

    // ---- edits that keep the store and the system in step ----
    fun save(context: Context, alarm: Alarm) {
        val list = Store.data.value.alarms
        Store.setAlarms(if (list.any { it.id == alarm.id }) list.map { if (it.id == alarm.id) alarm else it } else list + alarm)
        schedule(context, alarm)
    }

    fun delete(context: Context, alarm: Alarm) {
        cancel(context, alarm)
        Store.setAlarms(Store.data.value.alarms.filterNot { it.id == alarm.id })
    }
}
