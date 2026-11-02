package com.leaftodo.app

import com.leaftodo.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BackupDeviceTest {
    private val now = Instant.parse("2026-11-01T08:00:00.123Z")
    private fun tasks() = listOf(
        Task(1,"清单",createdAt=now,updatedAt=now),
        Task(2,"日期",date=LocalDate.of(2026,11,2),createdAt=now,updatedAt=now),
        Task(3,"时间",date=LocalDate.of(2026,11,2),time=LocalTime.of(9,5),createdAt=now,updatedAt=now),
        Task(4,"完成",status=TaskStatus.DONE,createdAt=now,updatedAt=now,closedAt=now),
        Task(5,"划掉",status=TaskStatus.DROPPED,createdAt=now,updatedAt=now,closedAt=now),
        Task(6,"  \"引号\\/😀中文  ",note="  \n\t😀中文\n  ",createdAt=now,updatedAt=now))
    @Test fun platformJsonRoundTrip() {
        val tasks=tasks()
        val result=decodeBackup(encodeBackup(tasks,now)) as BackupResult.Ok
        assertEquals(tasks,result.backup.tasks)
        assertEquals(now,result.backup.exportedAt)
        assertEquals(1,result.backup.format)
    }
    @Test fun platformNullIsNotString() {
        val text=encodeBackup(tasks(),now)
        val row=JSONObject(text).getJSONArray("tasks").getJSONObject(0)
        assertTrue(row.has("date")); assertTrue(row.isNull("date"))
        assertTrue(row.has("time")); assertTrue(row.isNull("time"))
        val task=(decodeBackup(text) as BackupResult.Ok).backup.tasks[0]
        assertNull(task.date); assertNull(task.time); assertNull(task.closedAt)
    }
}
