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
}
