package com.leaftodo.app

import com.leaftodo.app.ui.sketch.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.theme.ink
import com.leaftodo.app.ui.theme.soft
import java.time.LocalDate

@Composable
fun SectionedTaskList(
    sourceSections: ListSections, today: LocalDate,
    futureExpanded: Boolean, onFutureExpanded: (Boolean) -> Unit,
    noDateExpanded: Boolean, onNoDateExpanded: (Boolean) -> Unit,
    overdueExpanded: Boolean, onOverdueExpanded: (Boolean) -> Unit,
    olderExpanded: Boolean, onOlderExpanded: (Boolean) -> Unit,
    closedExpanded: Boolean, onClosedExpanded: (Boolean) -> Unit,
    onOverdueToggle: (Task) -> Unit, onOverdueDrop: (Task) -> Unit,
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit,
) {
    val scroll = androidx.compose.foundation.lazy.rememberLazyListState()
    PauseBoilWhileScrolling(scroll)
    val actions = LocalDrawingActions.current
    val all = (sourceSections.future + sourceSections.overdueRecent + sourceSections.overdueOlder).flatMap { it.tasks } + sourceSections.noDate + sourceSections.closed
    val sections = listSections(actions?.project(all) { it.date != today } ?: all, today)
    val m = actions?.motion
    LaunchedEffect(today) { actions?.cancelMotion() }
    // Keep the viewport at its pixel position instead of following a relocated keyed card.
    LaunchedEffect(m, m?.stage) {
        if (m != null) scroll.requestScrollToItem(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset)
    }
    val leaving = if (!closedExpanded && m != null && !m.vanishInPlace && m.status != TaskStatus.OPEN && m.stage == 1)
    sections.closed.filter { it.id == m.original.id } else emptyList()
    LazyColumn(state = scroll, contentPadding = PaddingValues(bottom = 120.dp)) {
        var hasBlock = false
        fun header(key: String, title: String, expanded: Boolean, change: (Boolean) -> Unit, small: Boolean = false) {
            val gap = hasBlock
            item(key = key) {
                Column(motionPlacement()) {
                    if (gap) Spacer(Modifier.height(40.dp))
                    SectionHeader(title, expanded, { change(!expanded) }, small)

                }
            }
            hasBlock = true
        }
        fun dated(prefix: String, groups: List<DayGroup>, overdue: Boolean = false, dimmed: Boolean = false) {
            groups.forEachIndexed { index, group ->
                item(key = "$prefix-day-${group.date}") {
                    Column(motionPlacement()) {
                        Spacer(Modifier.height(if (index == 0) 20.dp else 32.dp))
                        Text(groupTitle(group.date, today), style = MaterialTheme.typography.bodyLarge, color = soft)
                        Spacer(Modifier.height(12.dp))

                    }
                }
                taskCards(group.tasks, today, false,
                    if (overdue) onOverdueToggle else onToggle,
                    if (overdue) onOverdueDrop else onDrop, onReopen, onEdit, dimmed = { dimmed }, vanishInPlace = overdue)
            }
        }
        if (sections.future.isNotEmpty()) {
            header("future-title", "以后要做 ${sections.future.sumOf { it.tasks.size }} 条", futureExpanded, onFutureExpanded)
            if (futureExpanded) dated("future", sections.future)
        }
        if (sections.noDate.isNotEmpty()) {
            header("no-date-title", "没有日期 ${sections.noDate.size} 条", noDateExpanded, onNoDateExpanded)
            if (noDateExpanded) taskCards(sections.noDate, today, true, onToggle, onDrop, onReopen, onEdit, topGap = true)
        }
        val overdueCount = (sections.overdueRecent + sections.overdueOlder).sumOf { it.tasks.size }
        if (overdueCount > 0) {
            header("overdue-title", "过期没做完 $overdueCount 条", overdueExpanded, onOverdueExpanded)
            if (overdueExpanded) {
                dated("recent", sections.overdueRecent, overdue = true)
                if (sections.overdueOlder.isNotEmpty()) {
                    item(key = "older-title") {
                        Column(motionPlacement()) {
                            Spacer(Modifier.height(32.dp))
                            SectionHeader("更早（超过 7 天）${sections.overdueOlder.sumOf { it.tasks.size }} 条",
                                olderExpanded, { onOlderExpanded(!olderExpanded) }, small = true)

                        }
                    }
                    if (olderExpanded) dated("older", sections.overdueOlder, overdue = true, dimmed = true)
                }
            }
        }
        taskCards(leaving, today, leaving.firstOrNull()?.date == null, onToggle, onDrop, onReopen, onEdit, topGap = true, leaving = true)
        if (sourceSections.closed.isNotEmpty() || sections.closed.isNotEmpty()) {
            header("closed-title", "已结束 ${sourceSections.closed.size} 条", closedExpanded, onClosedExpanded, small = true)
            if (closedExpanded) {
                // Preserve stable item keys across all sections so placement crosses headings.
                sections.closed.forEach { task ->
                    taskCards(listOf(task), today, task.date == null, onToggle, onDrop, onReopen, onEdit, topGap = true)
                }
            }
        }
        if (!hasBlock) item(key = "empty") {
            Column(motionPlacement()) { Text("还没有事，点右下角 ＋ 记一条", color = soft)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, expanded: Boolean, onClick: () -> Unit, small: Boolean = false) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState().value
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .clickable(interactionSource=interaction,indication=null,onClick=onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (small) {
            Text("$title · ${if (expanded) "收起" else "展开"}", color = if(pressed) com.leaftodo.app.ui.theme.clayText else soft, fontSize = 13.sp)
        } else {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if(pressed) com.leaftodo.app.ui.theme.clayText else ink)
            Spacer(Modifier.width(8.dp))
            Text(if (expanded) "收起" else "展开", color = soft, fontSize = 13.sp)
        }
    }
}
