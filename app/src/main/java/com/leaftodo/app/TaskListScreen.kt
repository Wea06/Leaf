package com.leaftodo.app

import androidx.compose.foundation.*
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
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        if (groups.open.isEmpty() && groups.closed.isEmpty()) item(key = "empty") { Text(emptyText, color = soft) }
        taskCards(groups.open, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit)
        if (groups.closed.isNotEmpty()) {
            item(key = "closed-fold") {
                if (groups.open.isNotEmpty()) Spacer(Modifier.height(20.dp))
                Text(
                    "已结束 ${groups.closed.size} 条 · ${if (expanded) "收起" else "展开"}",
                    color = soft, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp)).clickable { onExpandedChange(!expanded) }
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

private fun LazyListScope.taskCards(
    tasks: List<Task>, today: LocalDate, showCreatedDate: Boolean,
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit, topGap: Boolean = false,
) {
    itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
        if (index > 0 || topGap) Spacer(Modifier.height(20.dp))
        TaskCard(task, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit)
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: Task, today: LocalDate, showCreatedDate: Boolean, onToggle: (Task) -> Unit, onDrop: (Task) -> Unit, onReopen: (Task) -> Unit, onEdit: (Task) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp)).background(card)
            .combinedClickable(onClick = { onEdit(task) }, onLongClick = { menu = true })
            .padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (task.status == TaskStatus.DONE) clay else card, RoundedCornerShape(4.dp))
                .border(2.dp, ink, RoundedCornerShape(4.dp))
                .clickable(onClickLabel = "完成或撤回") { onToggle(task) }, contentAlignment = Alignment.Center) {
                Text(when(task.status) { TaskStatus.OPEN -> ""; TaskStatus.DONE -> "✓"; TaskStatus.DROPPED -> "—" }, color = ink)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, color = if (task.status == TaskStatus.OPEN) ink else soft,
                    textDecoration = if (task.status == TaskStatus.DROPPED) TextDecoration.LineThrough else null,
                    style = MaterialTheme.typography.bodyLarge)
                if (showCreatedDate || task.time != null || task.note.isNotEmpty()) {
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
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = card) {
            if (task.status == TaskStatus.OPEN) {
                DropdownMenuItem(text = { Text("完成", color = ink) }, onClick = { onToggle(task); menu = false })
                DropdownMenuItem(text = { Text("划掉（不做了）", color = ink) }, onClick = { onDrop(task); menu = false })
            } else DropdownMenuItem(text = { Text("撤回成待办", color = ink) }, onClick = { onReopen(task); menu = false })
        }
    }
}
