package com.leaftodo.app

import com.leaftodo.app.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class CalendarLogicTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val now = Instant.parse("2026-11-02T09:00:00Z")
    private fun task(date: String?, status: TaskStatus = TaskStatus.OPEN): Task =
        newTask("事", "", date?.let(LocalDate::parse), null, now).withStatus(status, now)

    @Test fun novemberGrid() {
        val grid = monthGrid(YearMonth.of(2026, 11))
        assertEquals(42, grid.size)
        assertEquals(List<LocalDate?>(6) { null }, grid.take(6))
        assertEquals(LocalDate.of(2026, 11, 1), grid[6])
        assertEquals(LocalDate.of(2026, 11, 30), grid.filterNotNull().last())
    }
    @Test fun fourWeekFebruary() {
        val grid = monthGrid(YearMonth.of(2027, 2))
        assertEquals(28, grid.size)
        assertFalse(grid.contains(null))
    }
    @Test fun leapFebruary() {
        val grid = monthGrid(YearMonth.of(2028, 2))
        assertEquals(35, grid.size)
        assertNull(grid.first())
        assertEquals(LocalDate.of(2028, 2, 1), grid[1])
        assertTrue(grid.contains(LocalDate.of(2028, 2, 29)))
    }
    @Test fun decemberGrid() {
        val grid = monthGrid(YearMonth.of(2026, 12))
        assertEquals(35, grid.size)
        assertNull(grid.first())
        assertEquals(LocalDate.of(2026, 12, 1), grid[1])
    }
    @Test fun gridsHaveContinuousUniqueDatesAndCompleteWeeks() {
        listOf(YearMonth.of(2026, 11), YearMonth.of(2027, 2), YearMonth.of(2028, 2), YearMonth.of(2026, 12)).forEach { month ->
            val grid = monthGrid(month)
            assertEquals(0, grid.size % 7)
            assertEquals((1..month.lengthOfMonth()).map(month::atDay), grid.filterNotNull())
        }
    }
    @Test fun todayOverridesTasksAndStatuses() {
        assertEquals(DayMark.TODAY, dayMark(today, emptyList(), today))
        assertEquals(DayMark.TODAY, dayMark(today, listOf(task("2026-11-02"), task("2026-11-02", TaskStatus.DONE)), today))
    }
    @Test fun emptyPastAndFuture() {
        listOf("2026-10-30", "2026-11-05").forEach {
            assertEquals(DayMark.NONE, dayMark(LocalDate.parse(it), emptyList(), today))
        }
    }
    @Test fun allClosedIncludingDroppedAndFuture() {
        assertEquals(DayMark.ALL_CLOSED, dayMark(LocalDate.parse("2026-10-30"),
            listOf(task("2026-10-30", TaskStatus.DONE), task("2026-10-30", TaskStatus.DROPPED)), today))
        assertEquals(DayMark.ALL_CLOSED, dayMark(LocalDate.parse("2026-10-30"), listOf(task("2026-10-30", TaskStatus.DROPPED)), today))
        assertEquals(DayMark.ALL_CLOSED, dayMark(LocalDate.parse("2026-11-05"), listOf(task("2026-11-05", TaskStatus.DONE)), today))
    }
    @Test fun recentBoundariesAndMixedStatuses() {
        listOf("2026-11-01", "2026-10-26", "2026-10-30").forEach {
            assertEquals(DayMark.OPEN_RECENT, dayMark(LocalDate.parse(it), listOf(task(it), task(it, TaskStatus.DONE)), today))
        }
    }
    @Test fun staleAndUpcoming() {
        listOf("2026-10-25", "2026-10-01").forEach {
            assertEquals(DayMark.OPEN_STALE, dayMark(LocalDate.parse(it), listOf(task(it)), today))
        }
        assertEquals(DayMark.UPCOMING, dayMark(LocalDate.parse("2026-11-05"), listOf(task("2026-11-05")), today))
    }
    @Test fun ignoresOtherDatesAndUndatedTasks() {
        assertEquals(DayMark.NONE, dayMark(LocalDate.parse("2026-10-30"), listOf(task(null), task("2026-10-25")), today))
    }
    @Test fun markTracksCompletionAndReopening() {
        val open = task("2026-10-25")
        val done = open.withStatus(TaskStatus.DONE, now.plusSeconds(1))
        assertEquals(DayMark.ALL_CLOSED, dayMark(open.date!!, listOf(done), today))
        assertEquals(DayMark.OPEN_STALE, dayMark(open.date!!, listOf(done.withStatus(TaskStatus.OPEN, now.plusSeconds(2))), today))
    }
    @Test fun monthMarksIncludeEveryDayAndIgnoreOutsideTasks() {
        val marks = calendarMarks(listOf(task("2026-10-30"), task(null), task("2026-11-05")), YearMonth.of(2026, 11), today)
        assertEquals(30, marks.size)
        assertEquals((1..30).map { LocalDate.of(2026, 11, it) }.toSet(), marks.keys)
        assertEquals(DayMark.TODAY, marks[today])
        assertEquals(DayMark.UPCOMING, marks[LocalDate.of(2026, 11, 5)])
        assertEquals(DayMark.NONE, marks[LocalDate.of(2026, 11, 1)])
    }
    @Test fun shiftsAcrossYearAndZero() {
        assertEquals(YearMonth.of(2027, 1), shiftMonth(YearMonth.of(2026, 12), 1))
        assertEquals(YearMonth.of(2026, 12), shiftMonth(YearMonth.of(2027, 1), -1))
        assertEquals(YearMonth.of(2026, 11), shiftMonth(YearMonth.of(2026, 11), 0))
    }
    @Test fun selectionAfterFlip() {
        assertEquals(today, selectionAfterFlip(YearMonth.of(2026, 11), today))
        assertEquals(LocalDate.of(2026, 12, 1), selectionAfterFlip(YearMonth.of(2026, 12), today))
        assertEquals(LocalDate.of(2026, 10, 1), selectionAfterFlip(YearMonth.of(2026, 10), today))
    }
    @Test fun monthFormatting() {
        assertEquals("2026年11月", formatMonth(YearMonth.of(2026, 11)))
        assertEquals("2027年1月", formatMonth(YearMonth.of(2027, 1)))
    }
    @Test fun staleRequiresOldOpenDatedTask() {
        assertTrue(isStale(task("2026-10-25"), today))
        assertFalse(isStale(task("2026-10-26"), today))
        assertFalse(isStale(task("2026-10-25", TaskStatus.DONE), today))
        assertFalse(isStale(task("2026-10-25", TaskStatus.DROPPED), today))
        assertFalse(isStale(task(null), today))
    }
    @Test fun dayTasksReuseOpenAndClosedOrdering() {
        val early = task("2026-11-02").copy(title = "09", time = LocalTime.of(9, 0))
        val late = early.copy(title = "15", time = LocalTime.of(15, 0))
        val untimed = early.copy(title = "无时间", time = null)
        val untimedLater = untimed.copy(title = "后来", createdAt = now.plusSeconds(1))
        val done = early.withStatus(TaskStatus.DONE, now.plusSeconds(1))
        val dropped = late.withStatus(TaskStatus.DROPPED, now.plusSeconds(2))
        assertEquals(listOf(early, late, untimed, untimedLater, dropped, done),
            dayTasks(listOf(done, untimedLater, late, task("2026-11-03"), dropped, untimed, early), today))
        assertTrue(dayTasks(emptyList(), today).isEmpty())
    }
    @Test fun pageDefaultDates() {
        val selected = LocalDate.of(2026, 11, 20)
        assertEquals(today, defaultDateFor(TaskPage.TODAY, today, selected))
        assertNull(defaultDateFor(TaskPage.LIST, today, selected))
        assertEquals(selected, defaultDateFor(TaskPage.CALENDAR, today, selected))
        assertNull(defaultDateFor(TaskPage.CALENDAR, today, null))
    }
}
