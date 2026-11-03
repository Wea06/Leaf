package com.leaftodo.app.data

import java.time.YearMonth

data class SinkFrame(val scale: Float, val depth: Float, val movement: Float)

fun cubicEaseInOut(value: Float): Float {
    val x = value.coerceIn(0f, 1f)
    return if (x < .5f) 4 * x * x * x else 1 - (-2 * x + 2).let { it * it * it } / 2
}

fun sinkFrame(elapsedMs: Long, durationMs: Int = 700): SinkFrame {
    require(durationMs > 0)
    val p = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val depth = when { p < .2f -> p / .2f; p < .8f -> 1f; else -> (1 - p) / .2f }
    return SinkFrame(1 - .05f * depth, depth, cubicEaseInOut((p - .2f) / .6f))
}

fun monthDirection(old: YearMonth, new: YearMonth): Int = new.compareTo(old).compareTo(0)

/** Layout changes only at these boundaries; the continuous clock is read in graphics layers. */
fun motionStage(elapsedMs: Long, reopening: Boolean, vanishInPlace: Boolean): Int {
    val delay = if (reopening) 60 else 330
    return when {
        elapsedMs < delay -> 0
        elapsedMs < delay + (if (vanishInPlace) 140 else 560) -> 1
        else -> 2
    }
}

fun projectMotionTasks(tasks: List<Task>, original: Task, stage: Int, vanishInPlace: Boolean): List<Task> =
    if (stage == 0 || (vanishInPlace && stage == 1)) tasks.filterNot { it.id == original.id } + original else tasks
