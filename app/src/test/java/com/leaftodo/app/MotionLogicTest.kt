package com.leaftodo.app

import com.leaftodo.app.data.*
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MotionLogicTest {
    @Test fun sinkBoundaries() {
        listOf(-1L to SinkFrame(1f, 0f, 0f), 0L to SinkFrame(1f, 0f, 0f),
            140L to SinkFrame(.95f, 1f, 0f), 560L to SinkFrame(.95f, 1f, 1f),
            700L to SinkFrame(1f, 0f, 1f), 999L to SinkFrame(1f, 0f, 1f)).forEach { (t, expected) ->
            val actual = sinkFrame(t)
            assertEquals(expected.scale, actual.scale, .0001f)
            assertEquals(expected.depth, actual.depth, .0001f)
            assertEquals(expected.movement, actual.movement, .0001f)
        }
    }
    @Test fun sinkMidpointsAndCubicMovement() {
        assertEquals(.5f, sinkFrame(70).depth, .0001f)
        assertEquals(.5f, sinkFrame(630).depth, .0001f)
        assertEquals(.5f, sinkFrame(350).movement, .0001f)
        assertEquals(.0625f, sinkFrame(245).movement, .0001f)
        assertEquals(sinkFrame(350), sinkFrame(700, 1400))
    }
    @Test fun layoutWaitsForDrawingAndDisappearsAtCorrectBoundary() {
        assertEquals(0, motionStage(329, false, false))
        assertEquals(1, motionStage(330, false, false))
        assertEquals(1, motionStage(889, false, false))
        assertEquals(2, motionStage(890, false, false))
        assertEquals(0, motionStage(59, true, false))
        assertEquals(1, motionStage(60, true, false))
        assertEquals(1, motionStage(469, false, true))
        assertEquals(2, motionStage(470, false, true))
    }
    @Test fun projectionRetainsOnlyTheActorUntilLayoutChanges() {
        val open = newListTask("事", java.time.Instant.EPOCH).copy(id = 1)
        val closed = open.copy(status = TaskStatus.DONE)
        val other = open.copy(id = 2)
        assertEquals(setOf(open, other), projectMotionTasks(listOf(closed, other), open, 0, false).toSet())
        assertEquals(listOf(closed, other), projectMotionTasks(listOf(closed, other), open, 1, false))
        assertEquals(listOf(other, open), projectMotionTasks(listOf(other), open, 1, true))
        assertEquals(listOf(other), projectMotionTasks(listOf(other), open, 2, true))
    }
    @Test fun monthDirectionAcrossYearsAndJumps() {
        val december = YearMonth.of(2026, 12)
        assertEquals(1, monthDirection(december, december.plusMonths(1)))
        assertEquals(-1, monthDirection(december, december.minusMonths(50)))
        assertEquals(0, monthDirection(december, december))
    }
}
