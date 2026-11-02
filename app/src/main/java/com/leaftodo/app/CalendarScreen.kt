package com.leaftodo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(model: TaskListViewModel, today: LocalDate, selected: LocalDate, onEdit: (Task) -> Unit) {
    val tasks by model.monthTasks.collectAsStateWithLifecycle()
    val month = YearMonth.from(selected)
    val marks = remember(tasks, month, today) { calendarMarks(tasks, month, today) }
    val grid = remember(month) { monthGrid(month) }
    val selectedTasks = remember(tasks, selected) { dayTasks(tasks, selected) }
    var monthPicker by rememberSaveable { mutableStateOf(false) }
    if (monthPicker) CalendarMonthPicker(month, onDismiss = { monthPicker = false }) {
        model.selectMonth(it)
        monthPicker = false
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item(key = "month") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(previous = true) { model.flipMonth(-1) }
                Text(formatMonth(month), color = ink, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "选择年月") { monthPicker = true }
                        .padding(vertical = 9.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                MonthArrow(previous = false) { model.flipMonth(1) }
            }
            Spacer(Modifier.height(16.dp))
        }
        item(key = "grid") {
            Column(Modifier.fillMaxWidth().pointerInput(model) {
                val threshold = 56.dp.toPx()
                var dragged = 0f
                var flipped = false
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f; flipped = false },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                        if (!flipped && kotlin.math.abs(dragged) > threshold) {
                            model.flipMonth(if (dragged < 0) 1 else -1)
                            flipped = true
                        }
                    },
                )
            }) {
                Row {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(it, color = soft, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    grid.chunked(7).forEach { week ->
                        Row {
                            week.forEach { date ->
                                if (date == null) Spacer(Modifier.weight(1f).height(52.dp))
                                else CalendarDay(date, today, marks.getValue(date), selected == date,
                                    Modifier.weight(1f)) { model.selectDay(date) }
                            }
                        }
                    }
                }
            }
        }
        item(key = "legend") {
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(DayMark.TODAY, DayMark.ALL_CLOSED, DayMark.OPEN_RECENT, DayMark.OPEN_STALE, DayMark.UPCOMING).forEach { mark ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (mark == DayMark.TODAY) Box(Modifier.size(12.dp).background(clayTint, RoundedCornerShape(3.dp)))
                        else MarkDot(mark)
                        Text(mark.label(), color = soft, fontSize = 13.sp)
                    }
                }
            }
        }
        item(key = "selected-date") {
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(selected, today), color = ink, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f))
                if (selected != today) Box(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { model.backToToday() }.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text("回到今天", color = soft, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (selectedTasks.isEmpty()) item(key = "empty") {
            Text("这天没有事，点右下角 ＋ 记一条", color = soft)
        }
        taskCards(selectedTasks, today, false, model::toggleDone, model::drop, model::reopen, onEdit,
            dimmed = { isStale(it, today) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarMonthPicker(initial: YearMonth, onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
    val years = DatePickerDefaults.YearRange
    var year by rememberSaveable { mutableIntStateOf(initial.year.coerceIn(years)) }
    var month by rememberSaveable { mutableIntStateOf(initial.monthValue) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = card, tonalElevation = 0.dp,
        titleContentColor = ink, textContentColor = ink,
        title = { Text("选择年月") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MonthPickerColumn("年份", years, year, Modifier.weight(1f), { "${it}年" }) { year = it }
                MonthPickerColumn("月份", 1..12, month, Modifier.weight(1f), { "${it}月" }) { month = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(YearMonth.of(year, month)) }) { Text("确定", color = clayText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = ink) } })
}

@Composable
private fun MonthPickerColumn(label: String, values: IntRange, selected: Int, modifier: Modifier,
    format: (Int) -> String, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (selected - values.first - 2).coerceAtLeast(0))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = soft, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.height(240.dp).selectableGroup(), state = state,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(values.count(), key = { it + values.first }) { index ->
                val value = index + values.first
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .then(if (selected == value) Modifier.background(clayTint) else Modifier)
                    .selectable(selected == value, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(format(value), color = ink,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"))
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(previous: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick)
        .semantics { contentDescription = if (previous) "上个月" else "下个月" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(10.dp)) {
            val start = if (previous) size.width else 0f
            val end = size.width - start
            drawLine(ink, Offset(start, 0f), Offset(end, size.height / 2), 2.dp.toPx())
            drawLine(ink, Offset(end, size.height / 2), Offset(start, size.height), 2.dp.toPx())
        }
    }
}

@Composable
private fun CalendarDay(date: LocalDate, today: LocalDate, mark: DayMark, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.height(52.dp).clip(shape)
        .then(if (mark == DayMark.TODAY) Modifier.background(clayTint) else Modifier)
        .then(if (selected) Modifier.border(1.5.dp, ink, shape) else Modifier)
        .clickable(onClick = onClick)
        .semantics { contentDescription = formatDate(date, today) + "，" + mark.label() },
        contentAlignment = Alignment.Center) {
        Text(date.dayOfMonth.toString(), color = if (mark == DayMark.OPEN_STALE) soft else ink,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"))
        // 标记独立放在数字下沿与框底之间，不参与日期数字的居中计算。
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).height(8.dp),
            contentAlignment = Alignment.Center) { MarkDot(mark) }
    }
}

@Composable
private fun MarkDot(mark: DayMark) {
    Canvas(Modifier.size(6.dp)) {
        when (mark) {
            DayMark.ALL_CLOSED -> drawCircle(ink, radius = (size.minDimension - 1.5.dp.toPx()) / 2, style = Stroke(1.5.dp.toPx()))
            DayMark.OPEN_RECENT -> drawCircle(clay)
            DayMark.OPEN_STALE -> drawCircle(soft)
            DayMark.UPCOMING -> drawCircle(ink)
            else -> Unit
        }
    }
}

private fun DayMark.label(): String = when (this) {
    DayMark.NONE -> "没有事"
    DayMark.TODAY -> "今天"
    DayMark.ALL_CLOSED -> "都结束了"
    DayMark.OPEN_RECENT -> "没做完"
    DayMark.OPEN_STALE -> "过期没做完"
    DayMark.UPCOMING -> "以后的事"
}
