package com.leaftodo.app

import com.leaftodo.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReviewLogicTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val zone = ZoneId.of("Asia/Shanghai")
    private val time = LocalTime.of(23, 0)
    private val armed = day.atTime(time).atZone(zone).toInstant()
    private fun task(title: String = "事", date: LocalDate? = day, status: TaskStatus = TaskStatus.OPEN,
        time: LocalTime? = null, created: Long = 0) = Task(title = title, date = date, status = status,
        time = time, createdAt = Instant.ofEpochSecond(created), updatedAt = Instant.EPOCH)

    @Test fun reviewDayBoundaries() {
        for (clock in listOf("00:00", "03:59", "04:00", "23:00")) {
            val t = LocalTime.parse(clock)
            assertEquals(if (t.hour < 4) day.minusDays(1) else day, reviewDayFor(day.atTime(t)))
        }
    }
    @Test fun nextIsStrictlyFuture() {
        assertEquals(armed, nextReviewAt(time, armed.minusSeconds(1), zone))
        for (now in listOf(armed, armed.plusSeconds(1)))
            assertEquals(day.plusDays(1).atTime(time).atZone(zone).toInstant(), nextReviewAt(time, now, zone))
    }
    @Test fun nextUsesCalendarDaysAcrossDst() {
        val ny = ZoneId.of("America/New_York")
        val now = LocalDate.of(2026, 3, 7).atTime(23, 0).atZone(ny).toInstant()
        assertEquals(Duration.ofHours(23), Duration.between(now, nextReviewAt(time, now, ny)))
    }
    @Test fun reviewPlanBoundaries() {
        assertEquals(ReviewPlan(null, null), planReview(false, time, armed, null, armed, zone))
        for (delay in listOf(0L, 300L, 7200L))
            assertEquals(day, planReview(true, time, armed, null, armed.plusSeconds(delay), zone).showDay)
        assertNull(planReview(true, time, armed, null, armed.plusSeconds(7201), zone).showDay)
        assertNull(planReview(true, time, armed, day, armed, zone).showDay)
        assertNull(planReview(true, time, armed, null, armed.minusSeconds(1), zone).showDay)
        assertNull(planReview(true, time, null, null, armed, zone).showDay)
        val midnight = day.atTime(0, 30).atZone(zone).toInstant()
        assertEquals(day.minusDays(1), planReview(true, LocalTime.of(0, 30), midnight, null, midnight, zone).showDay)
    }
    @Test fun emptyAndUndatedDoNotCount() {
        assertNull(reviewSummary(emptyList(), day, day))
        assertNull(reviewSummary(listOf(task(date = null), task(date = day.minusDays(1))), day, day))
    }
    @Test fun summariesHaveExactWording() {
        assertEquals("今天完成 0 条，还剩 1 条没做完", reviewSummary(listOf(task()), day, day)!!.text)
        assertEquals("今天完成 1 条，全都做完了", reviewSummary(listOf(task(status = TaskStatus.DONE)), day, day)!!.text)
        val mixed = listOf(task(status = TaskStatus.DONE), task(status = TaskStatus.DROPPED))
        assertEquals("昨天完成 1 条，划掉 1 条，没有剩下的", reviewSummary(mixed, day, day.plusDays(1))!!.text)
        assertEquals("今天完成 1 条，划掉 1 条，还剩 1 条没做完", reviewSummary(mixed + task(), day, day)!!.text)
        assertEquals("${formatDate(day, day.plusDays(3))}完成 1 条，划掉 1 条，没有剩下的", reviewSummary(mixed, day, day.plusDays(3))!!.text)
        assertEquals("11月2日 周一 回顾", reviewSummary(mixed, day, day)!!.title)
    }
    @Test fun expansionSortsAndLimitsOpenTasks() {
        val tasks = (1..6).reversed().map { task("无$it", created = it.toLong()) } +
            task("晚", time = LocalTime.of(20, 0)) + task("早", time = LocalTime.of(9, 0))
        val summary = reviewSummary(tasks, day, day)!!
        assertEquals(summary.text + "\n没做完：早、晚、无1、无2、无3 等 8 条", summary.bigText)
        val closed = reviewSummary(listOf(task(status = TaskStatus.DONE)), day, day)!!
        assertEquals(closed.text, closed.bigText)
    }
}
