package com.leaftodo.app

import com.leaftodo.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime
import kotlin.math.roundToInt

class SettingsPaperLogicTest {
    @Test fun parsesPaperWithSafeDefault() {
        PaperEffect.entries.forEach { assertEquals(it, parsePaperEffect(it.name)) }
        listOf(null, "", "fiber", "bogus").forEach { assertEquals(PaperEffect.FIBER, parsePaperEffect(it)) }
    }
    @Test fun parsesStrictMinuteTime() {
        assertEquals(LocalTime.of(0, 0), parseReviewTime("00:00"))
        assertEquals(LocalTime.of(23, 59), parseReviewTime("23:59"))
        listOf(null, "", "9:00", "24:00", "23:60", "12:30:01", "12:30.1", "foo", " 12:30").forEach {
            assertEquals(DEFAULT_REVIEW_TIME, parseReviewTime(it))
        }
        assertEquals(0, parseReviewTime("12:30").second)
        assertEquals(0, parseReviewTime("12:30").nano)
    }
    @Test fun fibersAreStableAndWithinRanges() {
        val fibers = fiberSpecs(400f, 800f)
        assertEquals(fibers, fiberSpecs(400f, 800f))
        assertNotEquals(fibers, fiberSpecs(400f, 800f, 1))
        assertEquals((400f * 800 / 170 * 1.1f).roundToInt(), fibers.size)
        assertEquals(fibers.size / 2, fibers.count { it.soft })
        fibers.forEachIndexed { index, f ->
            assertTrue(f.length in 3f..14f || f.length in 16f..30f)
            assertTrue(f.x in 0f..400f && f.y in 0f..800f)
            assertTrue(f.width in .45f..1.15f)
            assertTrue(f.bend in -.125f.. .125f)
            assertEquals(index < fibers.size / 2, f.soft)
            assertTrue(f.alpha in (if (f.dark) .28f else .6f) * .35f..(if (f.dark) .28f else .6f))
        }
        assertTrue(fiberSpecs(0f, 800f).isEmpty())
    }
}
