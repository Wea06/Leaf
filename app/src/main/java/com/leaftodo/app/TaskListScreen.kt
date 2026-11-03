package com.leaftodo.app

import com.leaftodo.app.ui.sketch.*

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.theme.*

@Composable
fun TaskList(
    groups: TaskGroups, emptyText: String, today: LocalDate,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    showCreatedDate: Boolean = false, recent: List<DayGroup> = emptyList(),
    onRecentToggle: (Task) -> Unit = {}, onRecentDrop: (Task) -> Unit = {},
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit,
) {
    val scroll = androidx.compose.foundation.lazy.rememberLazyListState()
    PauseBoilWhileScrolling(scroll)
    LazyColumn(state = scroll, contentPadding = PaddingValues(bottom = 120.dp)) {
        if (groups.open.isEmpty() && groups.closed.isEmpty()) item(key = "empty") { Text(emptyText, color = soft) }
        taskCards(groups.open, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit)
        if (groups.closed.isNotEmpty()) {
            item(key = "closed-fold") {
                val foldSource = remember { MutableInteractionSource() }
                val foldPressed by foldSource.collectIsPressedAsState()
                if (groups.open.isNotEmpty()) Spacer(Modifier.height(20.dp))
                Text(
                    "已结束 ${groups.closed.size} 条 · ${if (expanded) "收起" else "展开"}",
                    color = if(foldPressed) clayText else soft, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(interactionSource=foldSource,indication=null) { onExpandedChange(!expanded) }
                        .padding(vertical = 14.dp),
                )
            }
            if (expanded) taskCards(groups.closed, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit, topGap = true)
        }
        if (recent.isNotEmpty()) {
            item(key = "recent-title") {
                Spacer(Modifier.height(40.dp))
                Text("近 7 天没做完", style = MaterialTheme.typography.titleMedium, color = ink)
            }
            recent.forEachIndexed { index, group ->
                item(key = "day-${group.date}") {
                    Spacer(Modifier.height(if (index == 0) 20.dp else 32.dp))
                    Text(formatDate(group.date, today), style = MaterialTheme.typography.bodyLarge, color = soft)
                    Spacer(Modifier.height(12.dp))
                }
                taskCards(group.tasks, today, false, onRecentToggle, onRecentDrop, onReopen, onEdit)
            }
        }
    }
}

internal fun LazyListScope.taskCards(
    tasks: List<Task>, today: LocalDate, showCreatedDate: Boolean,
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit, topGap: Boolean = false,
    dimmed: (Task) -> Boolean = { false },
) {
    itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
        if (index > 0 || topGap) Spacer(Modifier.height(20.dp))
        TaskCard(task, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit, dimmed(task))
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TaskCard(task: Task, today: LocalDate, showCreatedDate: Boolean, onToggle: (Task) -> Unit, onDrop: (Task) -> Unit, onReopen: (Task) -> Unit, onEdit: (Task) -> Unit, dimmed: Boolean = false) {
    CompositionProbe("TaskCard-${task.id}")
    var menu by remember { mutableStateOf(false) }
    val actions = LocalDrawingActions.current
    val animations = LocalDrawingAnimations.current
    val final = remember { mutableStateOf(1f) }
    val progress = actions?.progress(task.id,task.status) ?: final
    fun toggle() { actions?.start(task.id, if(task.status==TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN,animations); onToggle(task) }
    fun drop() { actions?.start(task.id,TaskStatus.DROPPED,animations); onDrop(task) }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hasSubtitle = showCreatedDate || task.time != null || task.note.isNotEmpty()
    Box {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .sketchCard(fill=if(pressed) clayTint else card,seed=task.id.toInt())
            .combinedClickable(interactionSource=source, indication=null, onClick = { onEdit(task) }, onLongClick = { menu = true })
            .padding(start = 4.dp, end = 18.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SketchCheckbox(task.status,task.time!=null,task.id.toInt(),progress) { toggle() }
            Spacer(Modifier.width(6.dp))
            // Balance the visible two-line text, including the font's space below its glyphs.
            Column(Modifier.weight(1f).offset(y = if (hasSubtitle) 4.dp else 0.dp)) {
                SketchStrike(task.title, if(task.status==TaskStatus.OPEN && !dimmed) ink else soft,
                    task.status==TaskStatus.DROPPED,task.id.toInt()+90,progress)
                if (hasSubtitle) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (showCreatedDate) Text("记于 " + formatDate(task.createdDate(ZoneId.systemDefault()), today),
                            color = soft, fontSize = 13.sp, maxLines = 1)
                        if (!showCreatedDate) task.time?.let { Text(it.format(DateTimeFormatter.ofPattern("HH:mm")), color = clayText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")) }
                        if (task.note.isNotEmpty()) Text(task.note.lineSequence().first(), color = soft,
                            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = card, shadowElevation = 0.dp, tonalElevation = 0.dp, border = BorderStroke(1.5.dp,ink)) {
            if (task.status == TaskStatus.OPEN) {
                DropdownMenuItem(text = { Text("完成", color = ink) }, onClick = { toggle(); menu = false })
                DropdownMenuItem(text = { Text("划掉（不做了）", color = ink) }, onClick = { drop(); menu = false })
            } else DropdownMenuItem(text = { Text("撤回成待办", color = ink) }, onClick = { onReopen(task); menu = false })
        }
    }
}
