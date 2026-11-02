package com.leaftodo.app

import com.leaftodo.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BackupLogicTest {
    private val now = Instant.parse("2026-11-01T08:00:00.123Z")
    private fun samples(): List<Task> = listOf(
        Task(1, "清单", createdAt = now, updatedAt = now),
        Task(2, "日期", date = LocalDate.of(2026,11,2), createdAt = now, updatedAt = now),
        Task(3, "时间", date = LocalDate.of(2026,11,2), time = LocalTime.of(9,5), createdAt = now, updatedAt = now),
        Task(4, "完成", status = TaskStatus.DONE, createdAt = now, updatedAt = now, closedAt = now),
        Task(5, "划掉", status = TaskStatus.DROPPED, createdAt = now, updatedAt = now, closedAt = now),
        Task(6, "毫秒", createdAt = now, updatedAt = now))
    private fun root() = JSONObject(encodeBackup(samples(), now))
    private fun ok(text: String) = (decodeBackup(text) as BackupResult.Ok).backup
    private fun reject(text: String, keyword: String = "") {
        val result = decodeBackup(text)
        assertTrue(result.toString(), result is BackupResult.Failed)
        val reason = (result as BackupResult.Failed).reason
        assertTrue(reason.isNotBlank()); assertTrue(reason, reason.contains(keyword))
    }
    private fun badRoot(key: String, value: Any?) { val r=root(); r.put(key,value); reject(r.toString()) }
    private fun badTask(key: String, value: Any?) { val r=root(); r.getJSONArray("tasks").getJSONObject(1).put(key,value); reject(r.toString(), "第 2 条") }
    @Test fun roundTrip() { val b=ok(encodeBackup(samples(),now)); assertEquals(samples(), b.tasks); assertEquals(now,b.exportedAt); assertEquals(1,b.format) }
    @Test fun specialCharacters() {
        val tasks=listOf(samples()[0].copy(title="  双引号\"反斜杠\\/中文  ",note="  \n\t😀中文\n  "))
        assertEquals(tasks,ok(encodeBackup(tasks,now)).tasks)
    }
    @Test fun encodedFieldsAndOrder() {
        val r=JSONObject(encodeBackup(samples().reversed(),now)); assertEquals("Leaf",r.getString("app")); assertEquals(1,r.getInt("format")); assertEquals(1,r.getInt("dbVersion")); assertEquals(6,r.getInt("count"))
        val rows=r.getJSONArray("tasks"); assertTrue(rows.getJSONObject(0).has("date")); assertTrue(rows.getJSONObject(0).isNull("date")); assertTrue(rows.getJSONObject(0).isNull("time")); assertTrue(rows.getJSONObject(0).isNull("closedAt"))
        assertEquals("09:05",rows.getJSONObject(2).getString("time")); assertEquals("DONE",rows.getJSONObject(3).getString("status"))
        assertEquals((1L..6L).toList(),(0..5).map { rows.getJSONObject(it).getLong("id") })
    }
    @Test fun emptyRoundTrip() { assertEquals(emptyList<Task>(),ok(encodeBackup(emptyList(),now)).tasks) }
    @Test fun unknownFieldsAndMissingNote() { val r=root(); r.put("future",true); val row=r.getJSONArray("tasks").getJSONObject(0); row.put("future", JSONObject()); row.remove("note"); assertEquals(samples(),ok(r.toString()).tasks) }
    @Test fun emptyText() = reject("")
    @Test fun nonJson() = reject("不是json")
    @Test fun arrayRoot() = reject("[]")
    @Test fun wrongApp() = badRoot("app","Other")
    @Test fun missingFormat() = badRoot("format",null)
    @Test fun stringFormat() = badRoot("format","1")
    @Test fun fractionFormat() = badRoot("format",1.5)
    @Test fun newerFormat() { val r=root().put("format",2); reject(r.toString(),"更新版本") }
    @Test fun zeroFormat() = badRoot("format",0)
    @Test fun missingTasks() = badRoot("tasks",null)
    @Test fun invalidExportedAt() = badRoot("exportedAt","昨天")
    @Test fun wrongCount() = badRoot("count",9)
    @Test fun missingTitle() = badTask("title",null)
    @Test fun blankTitle() = badTask("title","   ")
    @Test fun wrongStatus() = badTask("status","FOO")
    @Test fun invalidDate() = badTask("date","2026-13-01")
    @Test fun invalidLeapDate() = badTask("date","2026-02-29")
    @Test fun wrongDateFormat() = badTask("date","2026/11/02")
    @Test fun invalidTime() = badTask("time","25:00")
    @Test fun shortTime() = badTask("time","9:00")
    @Test fun timeWithoutDate() { val r=root(); r.getJSONArray("tasks").getJSONObject(0).put("time","09:00"); reject(r.toString(),"第 1 条") }
    @Test fun invalidCreatedAt() = badTask("createdAt","abc")
    @Test fun missingCreatedAt() = badTask("createdAt",null)
    @Test fun invalidUpdatedAt() = badTask("updatedAt","abc")
    @Test fun invalidClosedAt() = badTask("closedAt","abc")
    @Test fun zeroId() = badTask("id",0)
    @Test fun negativeId() = badTask("id",-1)
    @Test fun missingId() = badTask("id",null)
    @Test fun fractionId() = badTask("id",1.5)
    @Test fun duplicateId() { val r=root(); r.getJSONArray("tasks").getJSONObject(1).put("id",1); reject(r.toString(),"重复") }
    @Test fun fileNames() { val time=LocalDateTime.parse("2026-11-02T09:05"); assertEquals("leaf-2026-11-02-0905.json",backupFileName(time)); assertEquals("leaf-before-import-2026-11-02-0905.json",preImportFileName(time)) }
    @Test fun localExportTime() { assertTime("2026-11-01T13:30:00Z","11月1日 周日 21:30") }
    @Test fun localYearBoundary() { assertTime("2025-12-31T16:30:00Z","1月1日 周四 00:30") }
    @Test fun olderYear() { assertTime("2025-06-01T02:00:00Z","2025年6月1日 周日 10:00") }
    private fun assertTime(instant: String, expected: String) { assertEquals(expected,formatBackupTime(Instant.parse(instant),ZoneId.of("Asia/Shanghai"),LocalDate.of(2026,11,2))) }
}
