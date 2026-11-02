package com.leaftodo.app

import com.leaftodo.app.data.*
import java.time.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class TaskLogicTest {
    private val now = Instant.parse("2026-11-01T09:00:00Z")
    private val today = LocalDate.of(2026, 11, 1)
    @Test fun dates() {
        assertEquals("11月1日 周日", formatDate(today, today))
        assertEquals("2025年10月20日 周一", formatDate(LocalDate.of(2025,10,20), today))
        assertEquals("2027年1月1日 周五", formatDate(LocalDate.of(2027,1,1), today))
        assertEquals("3月5日 周四", formatDate(LocalDate.of(2026,3,5), today))
        assertEquals("12月31日 周四", formatDate(LocalDate.of(2026,12,31), today))
        listOf("一","二","三","四","五","六","日").forEachIndexed { i, day ->
            assertEquals("11月${2+i}日 周$day", formatDate(LocalDate.of(2026,11,2+i), today))
        }
    }
    @Test fun newTask() {
        val task = newListTask("  买牛奶  ", now)
        assertEquals("买牛奶", task.title); assertNull(task.date); assertNull(task.time)
        assertEquals(now, task.createdAt); assertEquals(now, task.updatedAt)
        assertEquals("", task.note); assertEquals(TaskStatus.OPEN, task.status); assertNull(task.closedAt)
    }
    @Test fun transitionsPreserveFields() {
        val original = newListTask("标题", now).copy(note = "内容", date = today, time = LocalTime.of(9,30))
        for (end in listOf(TaskStatus.DONE, TaskStatus.DROPPED)) {
            val closed = original.withStatus(end, now.plusSeconds(1))
            assertEquals(now.plusSeconds(1), closed.closedAt)
            assertEquals(end, closed.status)
            val opened = closed.withStatus(TaskStatus.OPEN, now.plusSeconds(2))
            assertNull(opened.closedAt); assertEquals(TaskStatus.OPEN, opened.status)
            for (changed in listOf(closed, opened)) {
                assertEquals(original, changed.copy(status = original.status, updatedAt = now, closedAt = null))
                assertTrue(changed.updatedAt > now)
            }
        }
    }
    @Test fun ordering() {
        val a = newListTask("a", now)
        val b = newListTask("b", now.plusSeconds(1))
        val c = a.withStatus(TaskStatus.DONE, now.plusSeconds(2))
        val d = b.withStatus(TaskStatus.DROPPED, now.plusSeconds(3))
        val groups = splitList(listOf(b,c,a,d))
        assertEquals(listOf(a,b), groups.open); assertEquals(listOf(d,c), groups.closed)
        assertTrue(splitList(emptyList()).open.isEmpty())
    }
    @Test fun schemaExists() { assertTrue(File("schemas/com.leaftodo.app.data.LeafDatabase/1.json").isFile) }

    @Test fun newTaskNormalizesOnlyUserInput() {
        val dated = newTask("  标题  ", "  内容\n第二行  ", today, LocalTime.of(15, 0), now)
        assertEquals("标题", dated.title)
        assertEquals("  内容\n第二行", dated.note)
        assertEquals(today, dated.date); assertEquals(LocalTime.of(15, 0), dated.time)
        assertEquals(now, dated.createdAt); assertEquals(now, dated.updatedAt)
        assertEquals(TaskStatus.OPEN, dated.status); assertNull(dated.closedAt)
        val undated = newTask("a", "", null, LocalTime.NOON, now)
        assertNull(undated.date); assertNull(undated.time)
    }
    @Test fun editTitlePreservesEveryOtherField() {
        val task = newTask("标题", "内容", today, LocalTime.of(15, 0), now)
            .copy(id = 42, status = TaskStatus.DONE, closedAt = now.plusSeconds(1))
        val edited = task.edited(" 新标题 ", task.note, task.date, task.time, now.plusSeconds(2))
        assertEquals(task.copy(title = "新标题", updatedAt = now.plusSeconds(2)), edited)
    }
    @Test fun changingDateKeepsTime() {
        val task = newTask("标题", "内容", today, LocalTime.of(15, 0), now)
        val edited = task.edited(task.title, task.note, today.plusDays(1), task.time, now.plusSeconds(1))
        assertEquals(today.plusDays(1), edited.date); assertEquals(task.time, edited.time)
    }
    @Test fun clearingDateClearsTime() {
        val task = newTask("标题", "", today, LocalTime.NOON, now)
        val edited = task.edited(task.title, task.note, null, task.time, now.plusSeconds(1))
        assertNull(edited.date); assertNull(edited.time)
    }
    @Test fun addingDateDoesNotFillTime() {
        val task = newListTask("标题", now)
        val edited = task.edited(task.title, task.note, today, null, now.plusSeconds(1))
        assertEquals(today, edited.date); assertNull(edited.time)
    }
    @Test fun contentComparison() {
        val task = newTask("标题", "内容", today, LocalTime.NOON, now)
        assertTrue(task.sameContent("标题", "内容", today, LocalTime.NOON))
        assertTrue(task.sameContent(" 标题 ", "内容", today, LocalTime.NOON))
        assertFalse(task.sameContent("标题", "内容  ", today, LocalTime.NOON))
        assertFalse(task.sameContent("另一个", "内容", today, LocalTime.NOON))
        assertFalse(task.sameContent("标题", "另一行", today, LocalTime.NOON))
        assertFalse(task.sameContent("标题", "内容", today.plusDays(1), LocalTime.NOON))
        assertFalse(task.sameContent("标题", "内容", today, null))
    }
    @Test fun defaultDateOnToday() { assertEquals(today, defaultDateFor(TaskPage.TODAY, today)) }
    @Test fun defaultDateOnList() { assertNull(defaultDateFor(TaskPage.LIST, today)) }
    @Test fun defaultDateOnCalendar() { assertNull(defaultDateFor(TaskPage.CALENDAR, today)) }
    @Test fun todayOrdering() {
        val early = newTask("9点", "", today, LocalTime.of(9, 0), now)
        val late = newTask("15点", "", today, LocalTime.of(15, 0), now.minusSeconds(1))
        val a = newTask("无时间早", "", today, null, now)
        val b = newTask("无时间晚", "", today, null, now.plusSeconds(1))
        val done = early.withStatus(TaskStatus.DONE, now.plusSeconds(2))
        val dropped = late.withStatus(TaskStatus.DROPPED, now.plusSeconds(3))
        val groups = splitToday(listOf(b, dropped, late, a, done, early))
        assertEquals(listOf(early, late, a, b), groups.open)
        assertEquals(listOf(dropped, done), groups.closed)
        assertEquals(TaskGroups(emptyList(), emptyList()), splitToday(emptyList()))
    }
}
