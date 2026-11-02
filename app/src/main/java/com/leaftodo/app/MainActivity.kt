package com.leaftodo.app

import android.os.Bundle
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.leaftodo.app.reminder.*
import com.leaftodo.app.settings.LeafSettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaftodo.app.data.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.leaftodo.app.ui.theme.*

class MainActivity : ComponentActivity() {
    private var openTodayRequests by mutableIntStateOf(0)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == OPEN_TODAY) openTodayRequests++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.action == OPEN_TODAY) openTodayRequests++
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        window.isNavigationBarContrastEnforced = false
        setContent { LeafTheme { Surface(color = bg, contentColor = ink) {
            val effect by LeafSettings.get(this).paperEffect.collectAsState()
            Box(Modifier.fillMaxSize()) {
                LeafApp(openTodayRequests)
                PaperOverlay(effect)
            }
        } } }
    }
}

@Composable
private fun LeafApp(openTodayRequests: Int) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(openTodayRequests) { if (openTodayRequests > 0) selected = 0 }
    val model: TaskListViewModel = viewModel()
    val tasks by model.tasks.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val todayTasks by model.todayTasks.collectAsStateWithLifecycle()
    val recentTasks by model.recentTasks.collectAsStateWithLifecycle()
    val calendarSelected by model.calendarSelected.collectAsStateWithLifecycle()
    var todayExpanded by rememberSaveable { mutableStateOf(false) }
    var listExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    fun dismissSnackbar() {
        snackbarJob?.cancel()
        snackbarHost.currentSnackbarData?.dismiss()
    }
    fun recentClosed(task: Task, dropped: Boolean) {
        if (dropped) model.drop(task) else model.toggleDone(task)
        dismissSnackbar()
        snackbarJob = scope.launch {
            val result = snackbarHost.showSnackbar(
                message = "${if (dropped) "已划掉" else "已完成"}：${task.title}",
                actionLabel = "撤回", duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) model.reopen(task)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { model.refreshToday(); Reminders.requestSync(context); NightlyReview.requestSync(context) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var defaultDate by rememberSaveable { mutableStateOf<String?>(null) }
    if (editing) {
        EditTaskScreen(editingId, defaultDate, today, model, onClose = { editing = false })
        return
    }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    if (settingsOpen) {
        SettingsScreen(onClose = { settingsOpen = false })
        return
    }
    val pages = listOf("今天", "清单", "日历")
    Column(
        Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 34.dp, vertical = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pages[selected], Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                    Box(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .clickable { dismissSnackbar(); settingsOpen = true }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text("设置", style = MaterialTheme.typography.bodyMedium, color = soft)
                    }
                }
                if (selected == 2) {
                    Spacer(Modifier.height(20.dp))
                    CalendarScreen(model, today, calendarSelected,
                        onEdit = { dismissSnackbar(); editingId = it.id; editing = true })
                }
                if (selected == 1) {
                    Spacer(Modifier.height(20.dp))
                    TaskList(splitList(tasks), "还没有事，点右下角 ＋ 记一条", today,
                        expanded = listExpanded, onExpandedChange = { listExpanded = it }, showCreatedDate = true,
                        onToggle = model::toggleDone, onDrop = model::drop, onReopen = model::reopen,
                        onEdit = { dismissSnackbar(); editingId = it.id; editing = true })
                }
                if (selected == 0) {
                    Spacer(Modifier.height(20.dp))
                    Text(formatDate(today, today), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(20.dp))
                    TaskList(splitToday(todayTasks), "今天还没有事，点右下角 ＋ 记一条", today,
                        expanded = todayExpanded, onExpandedChange = { todayExpanded = it },
                        recent = recentUnfinished(recentTasks, today),
                        onRecentToggle = { recentClosed(it, false) }, onRecentDrop = { recentClosed(it, true) },
                        onToggle = model::toggleDone, onDrop = model::drop, onReopen = model::reopen,
                        onEdit = { dismissSnackbar(); editingId = it.id; editing = true })
                }
            }
            FloatingActionButton(
                onClick = {
                    dismissSnackbar()
                    editingId = null
                    defaultDate = defaultDateFor(TaskPage.entries[selected], today, calendarSelected)?.toString()
                    editing = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 42.dp, bottom = 42.dp)
                    .size(56.dp).semantics { contentDescription = "新建一条事" },
                shape = CircleShape,
                containerColor = clay, contentColor = ink,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Text("+", style = MaterialTheme.typography.headlineLarge)
            }
            SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter)
                .padding(start = 34.dp, end = 34.dp, bottom = 114.dp)) { data ->
                Snackbar(shape = RoundedCornerShape(16.dp), containerColor = ink, contentColor = card,
                    action = {
                        TextButton(onClick = { data.performAction() },
                            colors = ButtonDefaults.textButtonColors(contentColor = clay)) { Text("撤回") }
                    }) {
                    Text(data.visuals.message, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            pages.forEachIndexed { index, label ->
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected == index) clayTint else Color.Transparent, RoundedCornerShape(16.dp))
                        .selectable(selected == index, role = Role.Tab, onClick = { dismissSnackbar(); selected = index })
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (selected == index) ink else soft,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}


