package com.leaftodo.app.data

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

data class Fiber(val x: Float, val y: Float, val length: Float, val angle: Float,
    val bend: Float, val dark: Boolean, val alpha: Float, val width: Float, val soft: Boolean)

fun fiberSpecs(widthDp: Float, heightDp: Float, seed: Int = 2024): List<Fiber> {
    val random = Random(seed)
    val n = (widthDp * heightDp / 170f * 1.1f).roundToInt().coerceAtLeast(0)
    return List(n) { index ->
        val rare = random.nextFloat() < .05f
        val length = if (rare) 16f + random.nextFloat() * 14f else 3f + random.nextFloat() * 11f
        val dark = random.nextFloat() < .6f
        Fiber(random.nextFloat() * widthDp, random.nextFloat() * heightDp, length,
            random.nextFloat() * (2 * PI).toFloat(), (random.nextFloat() - .5f) * .25f,
            dark, (if (dark) .28f else .6f) * (.35f + random.nextFloat() * .65f),
            .45f + random.nextFloat() * .7f, index < n / 2)
    }
}
