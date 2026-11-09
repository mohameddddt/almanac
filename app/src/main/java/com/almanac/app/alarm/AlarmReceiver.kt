package com.almanac.app.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.almanac.app.MainActivity
import com.almanac.app.R
import com.almanac.app.alarm.AlarmScheduler.ACTION_DISMISS
import com.almanac.app.alarm.AlarmScheduler.ACTION_FIRE
import com.almanac.app.alarm.AlarmScheduler.ACTION_SNOOZE
import com.almanac.app.alarm.AlarmScheduler.EXTRA_ID
import com.almanac.app.alarm.AlarmScheduler.EXTRA_KIND
import com.almanac.app.alarm.AlarmScheduler.EXTRA_LABEL
import com.almanac.app.alarm.AlarmScheduler.KIND_ALARM
import com.almanac.app.alarm.AlarmScheduler.KIND_TIMER
import com.almanac.app.data.Store
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        val id = intent.getLongExtra(EXTRA_ID, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_ALARM
        val nm = context.getSystemService(NotificationManager::class.java)
        val notifId = id.hashCode()

        when (intent.action) {
            ACTION_FIRE -> {
                if (kind == KIND_ALARM) advance(context, id)
                ring(context, nm, notifId, id, label, kind)
            }
            ACTION_DISMISS -> nm.cancel(notifId)
            ACTION_SNOOZE -> {
                nm.cancel(notifId)
                AlarmScheduler.snooze(context, id, label)
            }
        }
    }

    /** One-off alarms switch themselves off; repeating ones book their next ring. */
    private fun advance(context: Context, id: Long) {
        val alarm = Store.data.value.alarms.firstOrNull { it.id == id } ?: return
        if (alarm.days.isEmpty()) {
            Store.setAlarms(Store.data.value.alarms.map { if (it.id == id) it.copy(enabled = false) else it })
        } else {
            AlarmScheduler.schedule(context, alarm)
        }
    }

    private fun ring(context: Context, nm: NotificationManager, notifId: Int, id: Long, label: String, kind: String) {
        AlarmScheduler.ensureChannel(context)
        fun action(action: String, code: Int) = PendingIntent.getBroadcast(
            context, notifId * 31 + code,
            Intent(context, AlarmReceiver::class.java).setAction(action)
                .putExtra(EXTRA_ID, id).putExtra(EXTRA_LABEL, label).putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val title = when {
            kind == KIND_TIMER -> "Time's up"
            label.isNotBlank() -> label
            else -> "Alarm · $now"
        }
        val text = if (kind == KIND_TIMER) "Your timer has finished." else "It's $now. Rise gently."

        val builder = Notification.Builder(context, AlarmScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_moon)
            .setColor(0xFFB8553A.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setDeleteIntent(action(ACTION_DISMISS, 1))
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "Dismiss", action(ACTION_DISMISS, 1)).build())
        if (kind != KIND_TIMER) {
            builder.addAction(
                Notification.Action.Builder(null, "Snooze ${AlarmScheduler.SNOOZE_MINUTES} min", action(ACTION_SNOOZE, 2)).build(),
            )
        }
        val n = builder.build().apply { flags = flags or Notification.FLAG_INSISTENT }
        runCatching { nm.notify(notifId, n) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.syncAll(context)
    }
}
