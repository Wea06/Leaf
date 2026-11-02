package com.leaftodo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item(key = "month") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(previous = true) { model.flipMonth(-1) }
                Text(formatMonth(month), color = ink, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
    Column(modifier.height(52.dp).clip(shape)
        .then(if (mark == DayMark.TODAY) Modifier.background(clayTint) else Modifier)
        .then(if (selected) Modifier.border(1.5.dp, ink, shape) else Modifier)
        .clickable(onClick = onClick)
        .semantics { contentDescription = formatDate(date, today) + "，" + mark.label() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(date.dayOfMonth.toString(), color = if (mark == DayMark.OPEN_STALE) soft else ink,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.height(8.dp), contentAlignment = Alignment.Center) { MarkDot(mark) }
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
