package com.leaftodo.app.ui.sketch

import android.os.SystemClock
import androidx.compose.runtime.*
import com.leaftodo.app.data.*
import kotlinx.coroutines.*

/** Owned by a page, so a card moving to another section keeps its drawing only here. */
class DrawingActions(private val scope: CoroutineScope) {
    private data class Event(val status: TaskStatus, val progress: MutableFloatState)
    private val events = mutableMapOf<Long, Event>()
    private val final = mutableStateOf(1f)
    fun progress(id: Long, status: TaskStatus): State<Float> = events[id]?.takeIf { it.status == status }?.progress ?: final
    fun finish() { events.values.forEach { it.progress.floatValue = 1f } }
    fun start(id: Long, status: TaskStatus, enabled: Boolean) {
        val value = mutableFloatStateOf(if(enabled && status != TaskStatus.OPEN) 0f else 1f)
        events[id] = Event(status,value)
        if (value.floatValue == 1f) return
        scope.launch {
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
fun DrawingPage(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val actions = remember { DrawingActions(scope) }
    CompositionLocalProvider(LocalDrawingActions provides actions,content=content)
}
