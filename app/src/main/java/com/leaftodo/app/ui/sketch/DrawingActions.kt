package com.leaftodo.app.ui.sketch

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import com.leaftodo.app.data.*
import kotlinx.coroutines.*

/** Owned by a page, so a card moving to another section keeps its drawing only here. */
class DrawingActions(private val scope: CoroutineScope) {
    private data class Event(val status: TaskStatus, val progress: MutableFloatState)
    private val events = mutableMapOf<Long, Event>()
    private val final = mutableStateOf(1f)
    class Motion(val original: Task, val status: TaskStatus, val vanishInPlace: Boolean) {
        val delayMs = if (status == TaskStatus.OPEN) 60 else 330
        val clock = Animatable(0f)
        val elapsed get() = clock.value.toLong()
        val stage by derivedStateOf {
            motionStage(elapsed, status == TaskStatus.OPEN, vanishInPlace)
        }
    }
    var motion by mutableStateOf<Motion?>(null)
        private set
    private var motionJob: Job? = null

    fun project(tasks: List<Task>, include: (Task) -> Boolean = { true }): List<Task> {
        val m = motion ?: return tasks
        if (!include(m.original)) return tasks
        return projectMotionTasks(tasks, m.original, m.stage, m.vanishInPlace)
    }
    fun rendered(task: Task): Task = motion?.takeIf { it.original.id == task.id }
        ?.let { task.copy(status = it.status) } ?: task

    fun cancelMotion() {
        motionJob?.cancel()
        motion = null
        finish()
    }
    fun startMotion(task: Task, status: TaskStatus, enabled: Boolean, vanishInPlace: Boolean) {
        motionJob?.cancel()
        finish()
        motion = null
        if (!enabled) return
        val m = Motion(task, status, vanishInPlace)
        motion = m
        motionJob = scope.launch {
            m.clock.animateTo((m.delayMs + 700).toFloat(), tween(m.delayMs + 700, easing = LinearEasing))
            if (motion === m) motion = null
        }
    }
    fun progress(id: Long, status: TaskStatus): State<Float> = events[id]?.takeIf { it.status == status }?.progress ?: final
    private var drawingJob: Job? = null
    fun finish() { drawingJob?.cancel(); events.values.forEach { it.progress.floatValue = 1f } }
    fun start(task: Task, status: TaskStatus, enabled: Boolean, vanishInPlace: Boolean = false) {
        val id = task.id
        startMotion(task, status, enabled, vanishInPlace)
        events.clear()
        val value = mutableFloatStateOf(if(enabled && status != TaskStatus.OPEN) 0f else 1f)
        events[id] = Event(status,value)
        if (value.floatValue == 1f) return
        drawingJob = scope.launch {
            val start = SystemClock.uptimeMillis()
            while (value.floatValue < 1f) {
                delay(16)
                value.floatValue = steppedProgress(SystemClock.uptimeMillis()-start,330)
            }
        }
    }
}
val LocalDrawingActions = compositionLocalOf<DrawingActions?> { null }

@Composable
fun DrawingPage(scrollState: androidx.compose.foundation.gestures.ScrollableState? = null, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val actions = remember { DrawingActions(scope) }
    CompositionLocalProvider(LocalDrawingActions provides actions) {
        if (scrollState != null) PauseBoilWhileScrolling(scrollState)
        content()
    }
}
