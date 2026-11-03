package com.leaftodo.app

import com.leaftodo.app.ui.sketch.*

import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.leaftodo.app.data.*
import com.leaftodo.app.reminder.*
import com.leaftodo.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*

@Composable
internal fun ReminderSettingsBlock() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var notifications by remember { mutableStateOf(canNotify(context)) }
    var exact by remember { mutableStateOf(canUseExactAlarm(context)) }
    var battery by remember { mutableStateOf(false) }
    var precise by remember { mutableStateOf(ReminderStore(context).preciseMode) }
    var next by remember { mutableStateOf<NextReminder?>(null) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val enableNotifications = rememberEnableNotifications { revision++ }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(revision) {
        notifications = canNotify(context)
        exact = canUseExactAlarm(context)
        battery = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        precise = ReminderStore(context).preciseMode
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        today = now.atZone(zone).toLocalDate()
        next = nextReminder(withContext(Dispatchers.IO) { LeafDatabase.get(context).taskDao().getReminderCandidates() }, now, zone)
    }
    Text("到点提醒", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = ink)
    Text("给一条事选了时间，到那一分钟手机会弹通知。很多手机会自动清理后台，下面几项都要打开，提醒才靠得住。", style = MaterialTheme.typography.bodyMedium, color = soft)
    val upcoming = next
    Text(if (upcoming == null) "现在没有要提醒的事" else
        "下一次提醒：${reminderText(upcoming.tasks.first(), today)} ${upcoming.tasks.first().title}" +
            if (upcoming.tasks.size > 1) " 等 ${upcoming.tasks.size} 条" else "",
        style = MaterialTheme.typography.bodyMedium, color = if (upcoming == null) soft else ink)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsButton("通知", if (notifications) "已打开" else "没打开，点这里打开", true,
            descriptionColor = if (notifications) soft else clayText, onClick = enableNotifications)
        SettingsButton("闹钟和提醒", if (exact) "已允许" else "没允许，提醒可能会晚，点这里打开", true,
            descriptionColor = if (exact) soft else clayText) { openExactAlarmSettings(context) }
        SettingsButton("后台运行不受限制", if (battery) "已允许" else "没允许，点这里打开", true,
            descriptionColor = if (battery) soft else clayText) { openBatterySettings(context) }
        val guide = remember { autoStartGuide(Build.MANUFACTURER, Build.BRAND) }
        SettingsButton("自启动和后台活动（${guide.vendor}）", "Leaf 检查不到这一项，请自己确认一次：${guide.steps}", true) { openAutoStartSettings(context) }
        SettingsButton("更准时", "打开后用闹钟的方式登记，最不容易被推迟；有待提醒的事时，状态栏会显示一个小闹钟图标", true,
            trailing = {
                SketchSwitch(checked = precise)
            }) {
            precise = !precise
            ReminderStore(context).preciseMode = precise
            Reminders.requestSync(context)
            revision++
        }
        SettingsButton("发一条测试通知", "能看到这条，到点提醒就能弹出来", true) {
            if (canNotify(context)) Reminders.testNotification(context) else enableNotifications()
        }
    }
}
