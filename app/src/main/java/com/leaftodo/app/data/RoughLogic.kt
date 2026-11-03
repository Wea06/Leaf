package com.leaftodo.app.data

import kotlin.math.*

data class RoughPoint(val x: Float, val y: Float)

/** mulberry32: JavaScript Int overflow and unsigned shifts are intentional. */
fun rng(seed: Int): () -> Double {
    var a = seed
    return {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }
}

fun roughPoints(x1: Float, y1: Float, x2: Float, y2: Float, seed: Int,
    amplitude: Float, step: Float, overshoot: Boolean, unit: Float = 1f): List<RoughPoint> {
    require(step > 0)
    val r = rng(seed)
    val dx = x2 - x1; val dy = y2 - y1
    val len = hypot(dx, dy).takeIf { it != 0f } ?: 1f
    val ux = dx / len; val uy = dy / len
    val o1 = if (overshoot) ((r() * 2 - .6) * 2 * unit).toFloat() else 0f
    val o2 = if (overshoot) ((r() * 2 - .6) * 2 * unit).toFloat() else 0f
    val length = len + o1 + o2
    val n = max(2, floor(length / step + .5f).toInt())
    return (0..n).map { i ->
        val off = if ((i == 0 || i == n) && !overshoot) 0f else ((r() * 2 - 1) * amplitude).toFloat()
        RoughPoint(x1 - ux * o1 + ux * length * i / n - uy * off,
            y1 - uy * o1 + uy * length * i / n + ux * off)
    }
}

fun roughCirclePoints(cx: Float, cy: Float, radius: Float, seed: Int,
    amplitude: Float, step: Float, closed: Boolean): List<RoughPoint> {
    require(step > 0)
    val r = rng(seed); val n = max(12, floor(6.28 * radius / step + .5).toInt())
    val start = r() * 6.28; val overlap = if (closed) 0.0 else .2 + r() * .2
    return (0..n).map { i ->
        val t = start + (6.28 + overlap) * i / n
        val rr = radius + (r() * 2 - 1) * amplitude
        RoughPoint((cx + cos(t) * rr).toFloat(), (cy + sin(t) * rr).toFloat())
    }
}

fun steppedProgress(elapsedMs: Long, durationMs: Long, fps: Int = 12): Float {
    require(durationMs > 0 && fps > 0)
    if (elapsedMs >= durationMs) return 1f
    val frame = 1000.0 / fps
    return (floor(elapsedMs / frame) * frame / durationMs).toFloat().coerceIn(0f, 1f)
}
fun popScale(progress: Float): Float = when { progress < .25f -> .6f; progress < .5f -> 1.15f; else -> 1f }
