package com.leaftodo.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    groups: TaskGroups, emptyText: String,
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        if (groups.open.isEmpty() && groups.closed.isEmpty()) item { Text(emptyText, color = soft) }
        items(groups.open, key = { it.id }) { TaskCard(it, onToggle, onDrop, onReopen, onEdit) }
        if (groups.closed.isNotEmpty()) item { Text("已结束", color = soft, fontSize = 13.sp) }
        items(groups.closed, key = { it.id }) { TaskCard(it, onToggle, onDrop, onReopen, onEdit) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: Task, onToggle: (Task) -> Unit, onDrop: (Task) -> Unit, onReopen: (Task) -> Unit, onEdit: (Task) -> Unit) {
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
                if (task.time != null || task.note.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        task.time?.let { Text(it.format(DateTimeFormatter.ofPattern("HH:mm")), color = clayText,
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
