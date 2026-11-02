package com.leaftodo.app.reminder

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.leaftodo.app.R
import com.leaftodo.app.data.*
import com.leaftodo.app.settings.LeafSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*

const val REVIEW_ALARM = "com.leaftodo.app.REVIEW_ALARM"
const val REVIEW_CHANNEL = "review"

fun createReviewChannel(context: Context) {
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(REVIEW_CHANNEL, "每晚回顾", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "每天晚上告诉你这一天完成了几条、还剩几条"
        })
}

fun canNotifyReview(context: Context): Boolean = notificationPermissionGranted(context) &&
    NotificationManagerCompat.from(context).areNotificationsEnabled() &&
    context.getSystemService(NotificationManager::class.java).getNotificationChannel(REVIEW_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

private class ReviewStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("review", Context.MODE_PRIVATE)
    var armedAt: Instant?
        get() = if (prefs.contains("armedAt")) Instant.ofEpochMilli(prefs.getLong("armedAt", 0)) else null
        set(value) { prefs.edit().also { if (value == null) it.remove("armedAt") else it.putLong("armedAt", value.toEpochMilli()) }.commit() }
    var lastShownDay: LocalDate?
        get() = prefs.getString("lastShownDay", null)?.let(LocalDate::parse)
        set(value) { prefs.edit().putString("lastShownDay", value?.toString()).commit() }
    fun reset() { prefs.edit().remove("armedAt").remove("lastShownDay").commit() }
}

object NightlyReview {
    private val mutex = Mutex()
    fun requestSync(context: Context) = Reminders.scope.launch {
        try { sync(context.applicationContext) } catch (_: Exception) { /* 下次恢复或广播时重算。 */ }
    }
    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(context, 1002,
        Intent(context, ReviewAlarmReceiver::class.java).setAction(REVIEW_ALARM),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    suspend fun sync(context: Context, now: Instant = Instant.now()) = mutex.withLock { syncLocked(context, now) }
    private suspend fun syncLocked(context: Context, now: Instant) {
        val settings = LeafSettings.get(context)
        val store = ReviewStore(context)
        val zone = ZoneId.systemDefault()
        val plan = planReview(settings.reviewEnabled.value, settings.reviewTime.value, store.armedAt, store.lastShownDay, now, zone)
        plan.showDay?.let { day ->
            val summary = reviewSummary(LeafDatabase.get(context).taskDao().getTasksOn(day), day, now.atZone(zone).toLocalDate())
            if (summary != null) notify(context, summary)
            store.lastShownDay = day
        }
        val manager = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        store.armedAt = plan.nextAt
        if (plan.nextAt == null) manager.cancel(pi)
        else {
            val millis = plan.nextAt.toEpochMilli()
            try {
                if (canUseExactAlarm(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi) }
        }
    }
    fun onSettingsChanged(context: Context) = Reminders.scope.launch {
        try {
            mutex.withLock {
                ReviewStore(context.applicationContext).reset()
                syncLocked(context.applicationContext, Instant.now())
            }
        } catch (_: Exception) { }
    }
    suspend fun showNow(context: Context): Boolean {
        val today = LocalDateTime.now()
        val day = reviewDayFor(today)
        val summary = reviewSummary(LeafDatabase.get(context).taskDao().getTasksOn(day), day, today.toLocalDate()) ?: return false
        return notify(context, summary)
    }
    private fun notify(context: Context, summary: ReviewSummary): Boolean {
        if (!canNotifyReview(context)) return false
        val notification = NotificationCompat.Builder(context, REVIEW_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setColor(0xFFD97757.toInt())
            .setContentTitle(summary.title).setContentText(summary.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary.bigText))
            .setAutoCancel(true).setContentIntent(openTodayIntent(context, 1002))
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        return try { NotificationManagerCompat.from(context).notify("review", 1, notification); true }
        catch (_: SecurityException) { false }
    }
}

class ReviewAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != REVIEW_ALARM) return
        runReminder { NightlyReview.sync(context.applicationContext) }
    }
}
