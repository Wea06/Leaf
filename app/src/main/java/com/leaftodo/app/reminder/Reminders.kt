package com.leaftodo.app.reminder

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.leaftodo.app.MainActivity
import com.leaftodo.app.R
import com.leaftodo.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*

const val OPEN_TODAY = "com.leaftodo.app.OPEN_TODAY"
const val REMINDER_ALARM = "com.leaftodo.app.REMINDER_ALARM"
const val REMINDER_DONE = "com.leaftodo.app.REMINDER_DONE"
const val REMINDER_CHANNEL = "reminders"

class ReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    var armedAt: Instant?
        get() = if (prefs.contains("armedAt")) Instant.ofEpochMilli(prefs.getLong("armedAt", 0)) else null
        set(value) { prefs.edit().also { if (value == null) it.remove("armedAt") else it.putLong("armedAt", value.toEpochMilli()) }.commit() }
    var preciseMode: Boolean
        get() = prefs.getBoolean("preciseMode", false)
        set(value) { prefs.edit().putBoolean("preciseMode", value).commit() }
}

class LeafApplication : Application() {
    override fun onCreate() { super.onCreate(); createReminderChannel(this) }
}

fun createReminderChannel(context: Context) {
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(REMINDER_CHANNEL, "到点提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "到了你给事情选的时间时提醒你"
            enableVibration(true)
        })
}

fun notificationPermissionGranted(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun canNotify(context: Context): Boolean = notificationPermissionGranted(context) &&
    NotificationManagerCompat.from(context).areNotificationsEnabled() &&
    context.getSystemService(NotificationManager::class.java).getNotificationChannel(REMINDER_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

fun canUseExactAlarm(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

fun openTodayIntent(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(context, requestCode,
    Intent(context, MainActivity::class.java).setAction(OPEN_TODAY)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

object Reminders {
    private val mutex = Mutex()
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun requestSync(context: Context): Job {
        val app = context.applicationContext
        return scope.launch { try { sync(app) } catch (_: Exception) { /* 后续恢复前台或系统广播时重算。 */ } }
    }
    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(context, 1001,
        Intent(context, ReminderAlarmReceiver::class.java).setAction(REMINDER_ALARM),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    suspend fun sync(context: Context, now: Instant = Instant.now()) = mutex.withLock { syncLocked(context, now) }

    private suspend fun syncLocked(context: Context, now: Instant) {
        val tasks = LeafDatabase.get(context).taskDao().getReminderCandidates()
        val store = ReminderStore(context)
        val zone = ZoneId.systemDefault()
        val next = nextAlarmAt(tasks, store.armedAt, now, zone)
        val manager = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        if (next == null) { manager.cancel(pi); store.armedAt = null }
        else {
            // 先持久化边界，进程被结束或过期闹钟马上触发时仍可补发。
            store.armedAt = next
            try {
                when {
                    !canUseExactAlarm(context) -> manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pi)
                    store.preciseMode -> manager.setAlarmClock(AlarmManager.AlarmClockInfo(next.toEpochMilli(), openTodayIntent(context, 1001)), pi)
                    else -> manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pi)
                }
            } catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pi) }
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        val byId = tasks.associateBy { it.id.toInt() }
        notifications.activeNotifications.filter { it.tag == "task" }.forEach {
            if (!keepNotification(byId[it.id], now, zone)) notifications.cancel("task", it.id)
        }
    }

    suspend fun onAlarm(context: Context, now: Instant = Instant.now()) = mutex.withLock {
        val store = ReminderStore(context)
        val zone = ZoneId.systemDefault()
        val tasks = LeafDatabase.get(context).taskDao().getReminderCandidates()
        dueReminders(tasks, store.armedAt ?: now, now, zone).forEach { notifyTask(context, it, now.atZone(zone).toLocalDate(), zone) }
        store.armedAt = null
        syncLocked(context, now)
    }

    suspend fun markDone(context: Context, taskId: Long) = mutex.withLock {
        val dao = LeafDatabase.get(context).taskDao()
        dao.getById(taskId)?.takeIf { it.status == TaskStatus.OPEN }?.let { dao.update(it.withStatus(TaskStatus.DONE, Instant.now())) }
        context.getSystemService(NotificationManager::class.java).cancel("task", taskId.toInt())
        syncLocked(context, Instant.now())
    }

    private fun notifyTask(context: Context, task: Task, today: LocalDate, zone: ZoneId) {
        if (!canNotify(context)) return
        val done = PendingIntent.getBroadcast(context, task.id.toInt(),
            Intent(context, ReminderActionReceiver::class.java).setAction(REMINDER_DONE)
                .setData(Uri.parse("leaf://task/${task.id}")).putExtra("taskId", task.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = reminderText(task, today)
        val builder = baseNotification(context).setContentTitle(task.title).setContentText(text)
            .setWhen(requireNotNull(task.reminderAt(zone)).toEpochMilli()).setShowWhen(true)
            .setContentIntent(openTodayIntent(context, task.id.toInt())).addAction(0, "完成", done)
        if (task.note.isNotBlank()) builder.setStyle(NotificationCompat.BigTextStyle().bigText(text + "\n" + task.note))
        try { NotificationManagerCompat.from(context).notify("task", task.id.toInt(), builder.build()) } catch (_: SecurityException) { }
    }

    fun testNotification(context: Context) {
        if (!canNotify(context)) return
        try { NotificationManagerCompat.from(context).notify("test", 1, baseNotification(context)
            .setContentTitle("Leaf 测试通知").setContentText("到点提醒就会像这样弹出来。")
            .setContentIntent(openTodayIntent(context, 1)).build()) } catch (_: SecurityException) { }
    }
    private fun baseNotification(context: Context) = NotificationCompat.Builder(context, REMINDER_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification).setColor(0xFFD97757.toInt())
        .setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true).setOnlyAlertOnce(false)
}

private fun BroadcastReceiver.runReminder(block: suspend () -> Unit) {
    val pending = goAsync()
    Reminders.scope.launch {
        try { withTimeout(9_000) { block() } } catch (_: Exception) { }
        finally { pending.finish() }
    }
}
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != REMINDER_ALARM) return
        runReminder { Reminders.onAlarm(context.applicationContext) }
    }
}
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("taskId", 0)
        if (intent.action != REMINDER_DONE || id <= 0) return
        runReminder { Reminders.markDone(context.applicationContext, id) }
    }
}
class ReminderSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        runReminder { Reminders.sync(context.applicationContext) }
    }
}
