package com.leaftodo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import com.leaftodo.app.data.*
import com.leaftodo.app.reminder.*
import com.leaftodo.app.settings.LeafSettings
import com.leaftodo.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun ReviewSettingsBlock() {
    val context = LocalContext.current
    val settings = remember { LeafSettings.get(context) }
    val enabled by settings.reviewEnabled.collectAsState()
    val time by settings.reviewTime.collectAsState()
    var revision by remember { mutableIntStateOf(0) }
    val requestNotifications = rememberEnableNotifications { revision++ }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val notifications = remember(revision) { canNotifyReview(context) }
    val now = remember(revision, enabled, time) { Instant.now() }
    val zone = ZoneId.systemDefault()
    val next = nextReviewAt(time, now, zone).atZone(zone)
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var picker by rememberSaveable { mutableStateOf(false) }
    var empty by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Text("每晚回顾", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = ink)
    Text("每天到这个时间，Leaf 告诉你这一天完成了几条、还剩几条。这一天没有带日期的事就不弹。", style = MaterialTheme.typography.bodyMedium, color = soft)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsButton("每晚回顾", if (enabled) "已打开" else "已关闭，不会弹", true, trailing = {
            Switch(checked = enabled, onCheckedChange = null, colors = SwitchDefaults.colors(
                checkedThumbColor = card, checkedTrackColor = clay, checkedBorderColor = clay,
                uncheckedThumbColor = card, uncheckedTrackColor = rule, uncheckedBorderColor = rule))
        }) {
            settings.setReviewEnabled(!enabled)
            if (!enabled && !canNotifyReview(context)) requestNotifications()
            NightlyReview.onSettingsChanged(context)
        }
        SettingsButton("时间", time.format(clock), enabled, tabular = true) { picker = true }
        if (enabled) {
            Text("下一次回顾：${formatDate(next.toLocalDate(), now.atZone(zone).toLocalDate())} ${next.format(clock)}" +
                if (next.hour < 4) "，回顾前一天" else "", style = MaterialTheme.typography.bodyMedium, color = ink)
            if (!notifications) SettingsButton("通知没打开", "回顾弹不出来，点这里打开", true,
                descriptionColor = clayText, onClick = requestNotifications)
        }
        SettingsButton("现在弹一次今天的回顾", "按今天真实的数据马上弹一条，看看长什么样", true) {
            if (!canNotifyReview(context)) requestNotifications()
            else scope.launch { if (!NightlyReview.showNow(context)) empty = true }
        }
    }
    if (picker) LeafTimePicker(time, { picker = false }) { selected ->
        picker = false
        settings.setReviewTime(selected)
        if (!canNotifyReview(context)) requestNotifications()
        NightlyReview.onSettingsChanged(context)
    }
    if (empty) AlertDialog(onDismissRequest = { empty = false }, containerColor = card, tonalElevation = 0.dp,
        textContentColor = ink, text = { Text("今天还没有带日期的事，晚上不会弹回顾。") },
        confirmButton = { TextButton(onClick = { empty = false }) { Text("好", color = clayText) } })
}

@Composable
internal fun PaperSettingsBlock() {
    val settings = LeafSettings.get(LocalContext.current)
    val effect by settings.paperEffect.collectAsState()
    Text("纸面效果", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = ink)
    Text("盖在整个画面上的一层很淡的纹理。", style = MaterialTheme.typography.bodyMedium, color = soft)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PaperEffect.entries.forEach { option ->
            val selected = effect == option
            Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
                .background(if (selected) clayTint else card)
                .selectable(selected, role = Role.RadioButton, onClick = { settings.setPaperEffect(option) })
                .padding(vertical = 14.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(when (option) { PaperEffect.FIBER -> "纸纤维"; PaperEffect.GRID -> "方格线"; PaperEffect.NONE -> "无" },
                    color = if (selected) ink else soft, textAlign = TextAlign.Center)
            }
        }
    }
}
