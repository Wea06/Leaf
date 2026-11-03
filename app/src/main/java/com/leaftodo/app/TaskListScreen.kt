package com.leaftodo.app

import com.leaftodo.app.ui.sketch.*

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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
    sourceGroups: TaskGroups, emptyText: String, today: LocalDate,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    showCreatedDate: Boolean = false, sourceRecent: List<DayGroup> = emptyList(),
    onRecentToggle: (Task) -> Unit = {}, onRecentDrop: (Task) -> Unit = {},
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit,
) {
    val scroll = androidx.compose.foundation.lazy.rememberLazyListState()
    PauseBoilWhileScrolling(scroll)
    val actions = LocalDrawingActions.current
    val all = sourceGroups.open + sourceGroups.closed + sourceRecent.flatMap { it.tasks }
    val projected = actions?.project(all) { it.date?.let { d -> d >= today.minusDays(7) && d <= today } == true } ?: all
    val groups = splitToday(projected.filter { it.date == today })
    val recent = recentUnfinished(projected, today)
    val m = actions?.motion
    LaunchedEffect(today) { actions?.cancelMotion() }
    // Keep the viewport at its pixel position instead of following a relocated keyed card.
    LaunchedEffect(m, m?.stage) {
        if (m != null) scroll.requestScrollToItem(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset)
    }
    val leaving = if (!expanded && m != null && !m.vanishInPlace && m.status != TaskStatus.OPEN && m.stage == 1)
    groups.closed.filter { it.id == m.original.id } else emptyList()
    LazyColumn(state = scroll, contentPadding = PaddingValues(bottom = 120.dp)) {
        if (groups.open.isEmpty() && groups.closed.isEmpty()) item(key = "empty") {
            Column(motionPlacement()) { Text(emptyText, color = soft)
            }
        }
        taskCards(groups.open, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit)
        taskCards(leaving, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit, topGap = groups.open.isNotEmpty(), leaving = true)
        if (sourceGroups.closed.isNotEmpty() || groups.closed.isNotEmpty()) {
            item(key = "closed-fold") {
                Column(motionPlacement()) {
                    val foldSource = remember { MutableInteractionSource() }
                    val foldPressed by foldSource.collectIsPressedAsState()
                    if (groups.open.isNotEmpty()) Spacer(Modifier.height(20.dp))
                    Text(
                        "已结束 ${sourceGroups.closed.size} 条 · ${if (expanded) "收起" else "展开"}",
                        color = if(foldPressed) clayText else soft, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(interactionSource=foldSource,indication=null) { onExpandedChange(!expanded) }
                        .padding(vertical = 14.dp),
                    )

                }
            }
            if (expanded) taskCards(groups.closed, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit, topGap = true)
        }
        if (recent.isNotEmpty()) {
            item(key = "recent-title") {
                Column(motionPlacement()) {
                    Spacer(Modifier.height(40.dp))
                    Text("近 7 天没做完", style = MaterialTheme.typography.titleMedium, color = ink)

                }
            }
            recent.forEachIndexed { index, group ->
                item(key = "day-${group.date}") {
                    Column(motionPlacement()) {
                        Spacer(Modifier.height(if (index == 0) 20.dp else 32.dp))
                        Text(formatDate(group.date, today), style = MaterialTheme.typography.bodyLarge, color = soft)
                        Spacer(Modifier.height(12.dp))

                    }
                }
                taskCards(group.tasks, today, false, onRecentToggle, onRecentDrop, onReopen, onEdit, vanishInPlace = true)
            }
        }
    }
}

internal fun LazyListScope.taskCards(
    tasks: List<Task>, today: LocalDate, showCreatedDate: Boolean,
    onToggle: (Task) -> Unit, onDrop: (Task) -> Unit,
    onReopen: (Task) -> Unit, onEdit: (Task) -> Unit, topGap: Boolean = false,
    dimmed: (Task) -> Boolean = { false }, vanishInPlace: Boolean = false, leaving: Boolean = false,
) {
    itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
        val actions = LocalDrawingActions.current
        val m = actions?.motion
        val sinking = m?.original?.id == task.id
        fun frame(): SinkFrame {
            if (!sinking) return SinkFrame(1f, 0f, 0f)
            val elapsed = m!!.elapsed - m.delayMs
            return sinkFrame(if (leaving || vanishInPlace) elapsed.coerceAtMost(140) else elapsed)
        }
        Column(Modifier.animateItem(
            fadeInSpec = if (m?.status == TaskStatus.OPEN && vanishInPlace) tween(140) else null,
            placementSpec = if (m != null && !m.vanishInPlace) tween(420, delayMillis = 140, easing = Easing(::cubicEaseInOut)) else null,
            fadeOutSpec = if (m != null) tween(140) else null)
            .zIndex(if (sinking) -1f else 0f)) {
                if (index > 0 || topGap) Spacer(Modifier.height(20.dp))
                Box(Modifier.graphicsLayer { val f = frame(); scaleX = f.scale; scaleY = f.scale }) {
                    if (sinking) Box(Modifier.matchParentSize().graphicsLayer {
                        val depth = frame().depth
                        translationX = (3 + 8 * depth).dp.toPx()
                        translationY = translationX
                        alpha = .5f * depth
                    }.sketchFill(ink, task.id.toInt() + 500))
                    TaskCard(actions?.rendered(task) ?: task, today, showCreatedDate, onToggle, onDrop, onReopen, onEdit, dimmed(task), vanishInPlace)
                }
            }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TaskCard(task: Task, today: LocalDate, showCreatedDate: Boolean, onToggle: (Task) -> Unit, onDrop: (Task) -> Unit, onReopen: (Task) -> Unit, onEdit: (Task) -> Unit, dimmed: Boolean = false, vanishInPlace: Boolean = false) {
    CompositionProbe("TaskCard-${task.id}")
    var menu by remember { mutableStateOf(false) }
    val actions = LocalDrawingActions.current
    val animations = LocalDrawingAnimations.current
    val final = remember { mutableStateOf(1f) }
    val progress = actions?.progress(task.id,task.status) ?: final
    fun toggle() { actions?.start(task, if(task.status==TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN,animations,vanishInPlace); onToggle(task) }
    fun drop() { actions?.start(task,TaskStatus.DROPPED,animations,vanishInPlace); onDrop(task) }
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
            } else DropdownMenuItem(text = { Text("撤回成待办", color = ink) }, onClick = { actions?.start(task, TaskStatus.OPEN, animations); onReopen(task); menu = false })
        }
    }
}

/** Transparent headings move with the same window, allowing the sinking card underneath. */
@Composable
internal fun LazyItemScope.motionPlacement(): Modifier {
    val m = LocalDrawingActions.current?.motion
    return Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null,
        placementSpec = if (m != null && !m.vanishInPlace)
        tween(420, delayMillis = 140, easing = Easing(::cubicEaseInOut)) else null)
}
