package com.leaftodo.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.theme.*

@Composable
fun TaskList(tasks: List<Task>, model: TaskListViewModel) {
    val groups = splitList(tasks)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        if (tasks.isEmpty()) item { Text("还没有事，点右下角 ＋ 记一条", color = soft) }
        items(groups.open, key = { it.id }) { TaskCard(it, model) }
        if (groups.closed.isNotEmpty()) item { Text("已结束", color = soft, fontSize = 13.sp) }
        items(groups.closed, key = { it.id }) { TaskCard(it, model) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: Task, model: TaskListViewModel) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .background(card, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = {}, onLongClick = { menu = true })
            .padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp)
                .background(if (task.status == TaskStatus.DONE) clay else card, RoundedCornerShape(4.dp))
                .border(2.dp, ink, RoundedCornerShape(4.dp))
                .clickable(onClickLabel = "完成或撤回") { model.toggleDone(task) }, contentAlignment = Alignment.Center) {
                Text(when(task.status) { TaskStatus.OPEN -> ""; TaskStatus.DONE -> "✓"; TaskStatus.DROPPED -> "—" }, color = ink)
            }
            Spacer(Modifier.width(20.dp))
            Text(task.title, color = if (task.status == TaskStatus.OPEN) ink else soft,
                textDecoration = if (task.status == TaskStatus.DROPPED) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = card) {
            if (task.status == TaskStatus.OPEN) {
                DropdownMenuItem(text = { Text("完成", color = ink) }, onClick = { model.toggleDone(task); menu = false })
                DropdownMenuItem(text = { Text("划掉（不做了）", color = ink) }, onClick = { model.drop(task); menu = false })
            } else DropdownMenuItem(text = { Text("撤回成待办", color = ink) }, onClick = { model.reopen(task); menu = false })
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val submit = { if (title.isNotBlank()) { onAdd(title); onDismiss() } }
    AlertDialog(onDismissRequest = onDismiss, containerColor = card, titleContentColor = ink,
        title = { Text("记一条") }, text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true,
                placeholder = { Text("要做什么？") }, modifier = Modifier.focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ink, unfocusedTextColor = ink,
                    cursorColor = ink, focusedBorderColor = clayText, unfocusedBorderColor = rule,
                    focusedPlaceholderColor = soft, unfocusedPlaceholderColor = soft))
        }, confirmButton = { TextButton(onClick = submit, enabled = title.isNotBlank(),
            colors = ButtonDefaults.textButtonColors(contentColor = clayText, disabledContentColor = soft)) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = ink) } })
    LaunchedEffect(Unit) { focus.requestFocus() }
}
