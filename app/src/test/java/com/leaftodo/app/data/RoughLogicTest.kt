package com.leaftodo.app.data

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RoughLogicTest {
    @Test fun javascriptReference() {
        val next = rng(42)
        listOf(.6011037519201636, .44829055899754167, .8524657934904099,
            .6697340414393693, .17481389874592423).forEach { assertEquals(it, next(), 0.0) }
    }
    @Test fun pointsAndFrames() {
        fun points(seed: Int) = roughPoints(2f, 3f, 102f, 103f, seed, .8f, 12f, false)
        assertEquals(points(7), points(7)); assertNotEquals(points(7), points(8))
        assertEquals(RoughPoint(2f, 3f), points(7).first())
        assertEquals(RoughPoint(102f, 103f), points(7).last())
        points(7).forEach { assertTrue(abs((it.y - 3 - (it.x - 2)) / sqrt(2f)) <= .80001f) }
        assertEquals(roughCirclePoints(0f, 0f, 26f, 1, .8f, 12f, false),
            roughCirclePoints(0f, 0f, 26f, 1, .8f, 12f, false))
    }
    @Test fun steppedAnimation() {
        assertEquals(0f, steppedProgress(-10, 330), 0f)
        assertEquals(0f, steppedProgress(83, 330), 0f)
        assertEquals((1000f / 12 / 330), steppedProgress(84, 330), .00001f)
        assertEquals(steppedProgress(84, 330), steppedProgress(166, 330), 0f)
        assertEquals(1f, steppedProgress(330, 330), 0f)
        assertEquals(1f, steppedProgress(999, 330), 0f)
        assertEquals(.6f, popScale(0f)); assertEquals(1.15f, popScale(.25f)); assertEquals(1f, popScale(.5f))
    }
}
