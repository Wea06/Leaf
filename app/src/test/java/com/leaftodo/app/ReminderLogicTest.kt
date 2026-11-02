package com.leaftodo.app

import com.leaftodo.app.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderLogicTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 11, 2)
    private fun at(hour: Int, minute: Int = 0) = today.atTime(hour, minute).atZone(zone).toInstant()
    private fun task(hour: Int = 15, minute: Int = 0, id: Long = 1) =
        Task(id = id, title = "提醒$id", date = today, time = LocalTime.of(hour, minute), createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)

    @Test fun reminderConvertsShanghai() { assertEquals(Instant.parse("2026-11-02T07:00:00Z"), task().reminderAt(zone)) }
    @Test fun reminderConvertsUtc() { assertEquals(Instant.parse("2026-11-02T15:00:00Z"), task().reminderAt(ZoneOffset.UTC)) }
    @Test fun reminderTruncatesSecondsAndNanos() { assertEquals(at(15), task().copy(time = LocalTime.of(15, 0, 42, 123)).reminderAt(zone)) }
    @Test fun reminderRequiresDate() { assertNull(task().copy(date = null).reminderAt(zone)) }
    @Test fun reminderRequiresTime() { assertNull(task().copy(time = null).reminderAt(zone)) }
    @Test fun reminderIgnoresBothClosedStatuses() {
        for (status in listOf(TaskStatus.DONE, TaskStatus.DROPPED)) assertNull(task().copy(status = status).reminderAt(zone))
    }
    @Test fun nextEmpty() { assertNull(nextAlarmAt(emptyList(), null, at(10), zone)) }
    @Test fun nextAllPast() { assertNull(nextAlarmAt(listOf(task(9)), null, at(10), zone)) }
    @Test fun nextEarliestFuture() { assertEquals(at(15), nextAlarmAt(listOf(task(18), task(9), task(15)), null, at(10), zone)) }
    @Test fun nextExcludesExactlyNow() { assertNull(nextAlarmAt(listOf(task(10)), null, at(10), zone)) }
    @Test fun nextIgnoresClosedAndUntimed() {
        assertEquals(at(18), nextAlarmAt(listOf(task().copy(status = TaskStatus.DONE), task().copy(time = null), task(18)), null, at(10), zone))
    }
    @Test fun overdueArmedAlarmIsPreserved() { assertEquals(at(8), nextAlarmAt(listOf(task(8)), at(8), at(8, 5), zone)) }
    @Test fun armedExactlyNowIsPreserved() { assertEquals(at(8), nextAlarmAt(listOf(task(8)), at(8), at(8), zone)) }
    @Test fun beforeArmedIsNotCaughtUp() { assertEquals(at(9), nextAlarmAt(listOf(task(7, 59), task(9)), at(8), at(8, 5), zone)) }
    @Test fun futureArmedDoesNotHideEarlierTask() { assertEquals(at(11), nextAlarmAt(listOf(task(11), task(18)), at(18), at(10), zone)) }
    @Test fun overdueArmedWithoutCandidatesClears() { assertNull(nextAlarmAt(emptyList(), at(8), at(10), zone)) }
    @Test fun dueSameMinuteWithSubsecondNowSortedById() {
        assertEquals(listOf(1L, 2L), dueReminders(listOf(task(id = 2), task(15, 1, 3), task(id = 1)), at(15), at(15).plusMillis(200), zone).map { it.id })
    }
    @Test fun dueDelayedAlarmIncludesBothEnds() {
        val tasks = listOf(task(8, 8, 5), task(8, 7, 4), task(7, 59, 1), task(8, 3, 3), task(8, 0, 2))
        assertEquals(listOf(2L, 3L, 4L), dueReminders(tasks, at(8), at(8, 7), zone).map { it.id })
    }
    @Test fun dueCatchUpLimitedTo24Hours() {
        val tasks = listOf(task(9, id = 1).copy(date = today.minusDays(1)), task(11, id = 2).copy(date = today.minusDays(1)))
        assertEquals(listOf(2L), dueReminders(tasks, at(10).minusSeconds(3 * 86400), at(10), zone).map { it.id })
    }
    @Test fun dueIncludesExactly24Hours() {
        val t = task(10).copy(date = today.minusDays(1))
        assertEquals(listOf(t), dueReminders(listOf(t), at(10).minusSeconds(3 * 86400), at(10), zone))
    }
    @Test fun dueIgnoresClosedUntimedAndUndated() {
        val t = task(8)
        assertTrue(dueReminders(listOf(t.copy(status = TaskStatus.DONE), t.copy(status = TaskStatus.DROPPED), t.copy(time = null), t.copy(date = null)), at(7), at(10), zone).isEmpty())
    }
    @Test fun keepOnlyOpenDueNotification() {
        assertTrue(keepNotification(task(9), at(10), zone))
        assertTrue(keepNotification(task(10), at(10), zone))
        assertFalse(keepNotification(null, at(10), zone))
        assertFalse(keepNotification(task(11), at(10), zone))
        assertFalse(keepNotification(task(9).copy(status = TaskStatus.DONE), at(10), zone))
        assertFalse(keepNotification(task(9).copy(status = TaskStatus.DROPPED), at(10), zone))
        assertFalse(keepNotification(task(9).copy(time = null), at(10), zone))
        assertFalse(keepNotification(task(9).copy(date = null), at(10), zone))
    }
    @Test fun textUsesCommonDateFormatter() { assertEquals("11月2日 周一 15:00", reminderText(task(), today)) }
    @Test fun textPadsHourAndMinute() { assertEquals("11月2日 周一 09:05", reminderText(task(9, 5), today)) }
    @Test fun textIncludesYearWhenNeeded() { assertEquals("2027年1月1日 周五 08:00", reminderText(task(8).copy(date = LocalDate.of(2027, 1, 1)), today)) }
    @Test fun textMissingDateOrTimeIsEmpty() {
        assertEquals("", reminderText(task().copy(date = null), today))
        assertEquals("", reminderText(task().copy(time = null), today))
    }
    @Test fun pastIncludesEquality() {
        assertTrue(isPastReminder(today, LocalTime.of(9, 59), at(10), zone))
        assertTrue(isPastReminder(today, LocalTime.of(10, 0), at(10), zone))
        assertFalse(isPastReminder(today, LocalTime.of(10, 1), at(10), zone))
    }
    @Test fun pastRequiresBothFields() {
        assertFalse(isPastReminder(null, LocalTime.of(9, 0), at(10), zone))
        assertFalse(isPastReminder(today, null, at(10), zone))
    }
    @Test fun nextReminderGroupsAndSortsSameInstant() {
        val a = task(id = 1); val b = task(id = 2)
        assertEquals(NextReminder(at(15), listOf(a, b)), nextReminder(listOf(b, task(18), a), at(10), zone))
    }
    @Test fun nextReminderAllPastIsNull() { assertNull(nextReminder(listOf(task(9), task(10)), at(10), zone)) }
}
