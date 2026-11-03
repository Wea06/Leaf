package com.leaftodo.app

import com.leaftodo.app.ui.sketch.*

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.leaftodo.app.reminder.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.theme.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun EditTaskScreen(id: Long?, defaultDate: String?, today: LocalDate, model: TaskListViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var notifications by remember { mutableStateOf(canNotify(context)) }
    var now by remember { mutableStateOf(Instant.now()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifications = canNotify(context)
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { notifications = canNotify(context); now = Instant.now() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(defaultDate) }
    var timeText by rememberSaveable { mutableStateOf<String?>(null) }
    var initialTitle by rememberSaveable { mutableStateOf("") }
    var initialNote by rememberSaveable { mutableStateOf("") }
    var initialDate by rememberSaveable { mutableStateOf(defaultDate) }
    var initialTime by rememberSaveable { mutableStateOf<String?>(null) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(id) {
        if (!initialized) {
            if (id != null) {
                val task = model.load(id)
                if (task == null) { onClose(); return@LaunchedEffect }
                title = task.title; note = task.note
                dateText = task.date?.toString(); timeText = task.time?.toString()
            }
            initialTitle = title; initialNote = note; initialDate = dateText; initialTime = timeText
            initialized = true
        }
    }
    val date = dateText?.let(LocalDate::parse)
    val time = timeText?.let(LocalTime::parse)
    val original = newTask(initialTitle, initialNote, initialDate?.let(LocalDate::parse),
        initialTime?.let(LocalTime::parse), Instant.EPOCH)
    val changed = !original.sameContent(title, note, date, time)
    val cancel = { if (!busy) { if (changed) discard = true else { keyboard?.hide(); onClose() } } }
    BackHandler(onBack = cancel)
    if (!initialized) return

    val scroll = rememberScrollState()
    PauseBoilWhileScrolling(scroll)
    Column(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding().verticalScroll(scroll).padding(horizontal = 34.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = cancel, enabled = !busy, contentPadding = PaddingValues(0.dp)) { Text("取消", color = ink) }
            Text(if (id == null) "记一条" else "改一条", modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.titleLarge)
            TextButton(enabled = title.isNotBlank() && !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = clayText, disabledContentColor = soft),
                contentPadding = PaddingValues(0.dp), onClick = {
                    busy = true
                    keyboard?.hide()
                    scope.launch {
                        try { model.save(id, title, note, date, time); onClose() }
                        catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = true }
                        finally { busy = false }
                    }
                }) { Text("保存") }
        }
        val titleFocus = remember { FocusRequester() }
        val noteFocus = remember { FocusRequester() }
        EditField(title, { title = it }, "要做什么？", Modifier.focusRequester(titleFocus),
            singleLine = true, keyboardActions = KeyboardActions(onNext = { noteFocus.requestFocus() }))
        LaunchedEffect(Unit) { if (id == null) { titleFocus.requestFocus(); keyboard?.show() } }
        EditField(note, { note = it }, "详细内容（选填）", Modifier.focusRequester(noteFocus), singleLine = false)
        PickerRow("日期", date?.let { formatDate(it, today) } ?: "无", enabled = true,
            selected = date != null, onClick = { datePicker = true }, onClear = { dateText = null; timeText = null })
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PickerRow("时间", if (date == null) "先选日期" else time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "无",
            enabled = date != null, selected = time != null, isTime = true,
            onClick = { timePicker = true }, onClear = { timeText = null })
        if (time != null && !notifications) {
            Text("通知没打开，到点不会弹出提醒", color = clayText, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable { openNotificationSettings(context) }.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp))
        } else if (isPastReminder(date, time, now, ZoneId.systemDefault())) {
            Text("这个时间已经过了，不会再提醒", color = soft, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        }
        if (id != null) {
            TextButton(onClick = { deleting = true }, enabled = !busy,
                modifier = Modifier.padding(top = 20.dp)) { Text("删除这条", color = ink) }
        }
    }
    if (discard) LeafConfirm("不保存就离开？", "不保存", "继续编辑", { discard = false }) {
        keyboard?.hide(); onClose()
    }
    if (deleting) LeafConfirm("删除后找不回来。确定删除？", "删除", "取消", { deleting = false }) {
        if (!busy) {
            busy = true
            scope.launch {
                try { model.delete(requireNotNull(id)); keyboard?.hide(); onClose() }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = true }
                finally { busy = false; deleting = false }
            }
        }
    }
    if (error) LeafDialog(onDismissRequest = { error = false }, containerColor = card,
        tonalElevation = 0.dp, textContentColor = ink, text = { Text("操作未完成，请重试。") },
        confirmButton = { TextButton(onClick = { error = false }) { Text("确定", color = clayText) } })
    if (datePicker) LeafDatePicker(date, today, onDismiss = { datePicker = false }) {
        dateText = it.toString(); datePicker = false
    }
    if (timePicker) LeafTimePicker(time, onDismiss = { timePicker = false }) {
        timeText = it.toString(); timePicker = false
        now = Instant.now()
        if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted(context)) {
            context.getSharedPreferences("reminders", android.content.Context.MODE_PRIVATE).edit()
                .putBoolean("notificationRequested", true).commit()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier,
    singleLine: Boolean, keyboardActions: KeyboardActions = KeyboardActions.Default) {
    BasicTextField(value, onChange, modifier.fillMaxWidth().heightIn(min = if (singleLine) 60.dp else 120.dp)
        .sketchCard().padding(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink), cursorBrush = SolidColor(ink),
        singleLine = singleLine, keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        keyboardActions = keyboardActions, decorationBox = { field ->
            Box(contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                if (value.isEmpty()) Text(placeholder, color = soft)
                field()
            }
        })
}

@Composable
private fun PickerRow(label: String, value: String, enabled: Boolean, selected: Boolean,
    isTime: Boolean = false, onClick: () -> Unit, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).sketchClickable(enabled, onClick).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = soft, modifier = Modifier.weight(1f))
        Text(value, color = if (!selected || !enabled) soft else if (isTime) clayText else ink,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = if (isTime) "tnum" else null))
        if (selected) Box(Modifier.padding(start = 4.dp).size(40.dp).clip(CircleShape)
            .clickable(onClick = onClear).semantics { contentDescription = "清空$label" }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(14.dp).sketchDrawing { _,id -> line(0f,0f,w,h,id,width=2f,amp=.24f); line(0f,h,w,0f,id+1,width=2f,amp=.24f) })
        } else Spacer(Modifier.width(8.dp))
    }
}

@Composable
internal fun LeafConfirm(message: String, confirm: String, cancel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    LeafDialog(onDismissRequest = onDismiss, containerColor = card, tonalElevation = 0.dp,
        textContentColor = ink, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = clayText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancel, color = ink) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeafDatePicker(date: LocalDate?, today: LocalDate, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialDisplayedMonthMillis = (date ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    LeafDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = {
            val ms = state.selectedDateMillis
            if (ms != null) onSelect(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()) else onDismiss()
        }) { Text("确定", color = clayText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = ink) } }, text = {
        DatePicker(state, showModeToggle = false, colors = DatePickerDefaults.colors(
            containerColor = card, titleContentColor = ink, headlineContentColor = ink,
            weekdayContentColor = soft, subheadContentColor = ink, navigationContentColor = ink,
            yearContentColor = ink, currentYearContentColor = clayText,
            selectedYearContentColor = ink, selectedYearContainerColor = clay,
            dayContentColor = ink, disabledDayContentColor = soft,
            selectedDayContentColor = ink, disabledSelectedDayContentColor = soft,
            selectedDayContainerColor = clay, disabledSelectedDayContainerColor = clayTint,
            todayContentColor = clayText, todayDateBorderColor = clayText,
            dayInSelectionRangeContentColor = ink, dayInSelectionRangeContainerColor = clayTint,
            dividerColor = rule))
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeafTimePicker(time: LocalTime?, onDismiss: () -> Unit, onSelect: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = time?.hour ?: 9, initialMinute = time?.minute ?: 0, is24Hour = true)
    LeafDialog(onDismissRequest = onDismiss, containerColor = card, tonalElevation = 0.dp,
        titleContentColor = ink, title = { Text("时间") }, text = {
            MaterialTheme(typography = MaterialTheme.typography.copy(
                displayLarge = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            )) {
            TimePicker(state, colors = TimePickerDefaults.colors(
                clockDialColor = bg, selectorColor = clay, containerColor = card,
                clockDialSelectedContentColor = ink, clockDialUnselectedContentColor = ink,
                periodSelectorBorderColor = rule, periodSelectorSelectedContainerColor = clay,
                periodSelectorUnselectedContainerColor = bg, periodSelectorSelectedContentColor = ink,
                periodSelectorUnselectedContentColor = ink, timeSelectorSelectedContainerColor = clay,
                timeSelectorUnselectedContainerColor = clayTint, timeSelectorSelectedContentColor = ink,
                timeSelectorUnselectedContentColor = ink))
            }
        }, confirmButton = { TextButton(onClick = { onSelect(LocalTime.of(state.hour, state.minute)) }) { Text("确定", color = clayText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = ink) } })
}
