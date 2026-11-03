package com.leaftodo.app

import com.leaftodo.app.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ListSectionsTest {
    private val today = LocalDate.of(2027, 1, 1)
    private val now = Instant.parse("2026-11-01T09:00:00Z")
    private fun task(days: Long?, status: TaskStatus = TaskStatus.OPEN, seconds: Long = 0) =
        newTask("task", "", days?.let(today::plusDays), null, now.plusSeconds(seconds))
            .withStatus(status, now.plusSeconds(seconds))
    private fun ListSections.all() = future.flatMap { it.tasks } + noDate +
        overdueRecent.flatMap { it.tasks } + overdueOlder.flatMap { it.tasks } + closed

    @Test fun todayAndPastClosedNeverEnterList() {
        val tasks = TaskStatus.entries.flatMap { listOf(task(0, it), task(-1, it), task(-8, it)) }
        assertEquals(tasks.filter { it.status == TaskStatus.OPEN && it.date != today }.toSet(),
            listSections(tasks, today).all().toSet())
    }
    @Test fun futureDatesAscend() {
        val tasks = listOf(task(30), task(2), task(1))
        assertEquals(listOf(1L, 2L, 30L).map(today::plusDays), listSections(tasks, today).future.map { it.date })
    }
    @Test fun undatedOpenUsesCreationOrder() {
        val a = task(null)
        val b = task(null, seconds = 1)
        assertEquals(listOf(a, b), listSections(listOf(b, task(null, TaskStatus.DONE), a), today).noDate)
    }
    @Test fun overdueBoundariesAndGroupsDescendAcrossYear() {
        val sections = listSections(listOf(-365L, -8L, -7L, -1L).map { task(it) }, today)
        assertEquals(listOf(-1L, -7L).map(today::plusDays), sections.overdueRecent.map { it.date })
        assertEquals(listOf(-8L, -365L).map(today::plusDays), sections.overdueOlder.map { it.date })
        assertEquals(LocalDate.of(2026, 12, 31), sections.overdueRecent.first().date)
    }
    @Test fun recentMatchesTodayPageExactly() {
        val tasks = (-15L..30L).flatMap { day -> TaskStatus.entries.map { task(day, it) } } + task(null)
        assertEquals(recentUnfinished(tasks, today), listSections(tasks, today).overdueRecent)
    }
    @Test fun closedIncludesOnlyUndatedAndFutureAndSortsByClosingTime() {
        val a = task(null, TaskStatus.DONE)
        val b = task(1, TaskStatus.DROPPED, 2)
        val c = task(null, TaskStatus.DROPPED, 1)
        val d = task(2, TaskStatus.DONE, 3)
        assertEquals(listOf(d, b, c, a), listSections(listOf(a, b, task(-1, TaskStatus.DONE),
            task(0, TaskStatus.DROPPED), c, d), today).closed)
    }
    @Test fun datedGroupsUseTimeThenCreationOrder() {
        for (days in listOf(1L, -1L, -8L)) {
            val early = task(days).copy(time = LocalTime.of(9, 0))
            val late = task(days, seconds = -1).copy(time = LocalTime.of(15, 0))
            val a = task(days)
            val b = task(days, seconds = 1)
            assertEquals(listOf(early, late, a, b), listSections(listOf(b, late, a, early), today).all())
        }
    }
    @Test fun emptyInputHasNoSections() { assertTrue(listSections(emptyList(), today).all().isEmpty()) }
    @Test fun groupTitles() {
        assertEquals("明天 · 1月2日 周六", groupTitle(today.plusDays(1), today))
        assertEquals("后天 · 1月3日 周日", groupTitle(today.plusDays(2), today))
        assertEquals("昨天 · 2026年12月31日 周四", groupTitle(today.minusDays(1), today))
        assertEquals("1月31日 周日", groupTitle(today.plusDays(30), today))
        assertEquals("明天 · 2027年1月1日 周五", groupTitle(today, today.minusDays(1)))
        assertEquals("后天 · 2027年1月1日 周五", groupTitle(today, today.minusDays(2)))
    }
    @Test fun midnightOrdinaryDay() { checkMidnight("2026-11-03T15:30:00+08:00[Asia/Shanghai]", "2026-11-04") }
    @Test fun midnightMonthEnd() { checkMidnight("2028-02-29T23:59:59+08:00[Asia/Shanghai]", "2028-03-01") }
    @Test fun midnightYearEnd() { checkMidnight("2026-12-31T00:00:00+08:00[Asia/Shanghai]", "2027-01-01") }
    @Test fun midnightHandlesShortAndLongDstDays() {
        val zone = ZoneId.of("America/New_York")
        for ((day, hours) in listOf("2026-03-08" to 23L, "2026-11-01" to 25L)) {
            val start = LocalDate.parse(day).atStartOfDay(zone)
            assertEquals(hours, Duration.between(start, nextMidnight(start)).toHours())
        }
    }
    private fun checkMidnight(input: String, expected: String) {
        val now = ZonedDateTime.parse(input)
        assertEquals(LocalDate.parse(expected).atStartOfDay(now.zone), nextMidnight(now))
        assertTrue(nextMidnight(now).isAfter(now))
    }
}
