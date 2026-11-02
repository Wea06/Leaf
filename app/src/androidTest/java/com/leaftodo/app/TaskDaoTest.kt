package com.leaftodo.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.leaftodo.app.data.*
import java.time.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class TaskDaoTest {
    private lateinit var db: LeafDatabase
    private val now = Instant.parse("2026-11-01T09:00:00Z")
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, LeafDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun onlyUndatedTasks() = runBlocking {
        val dao = db.taskDao()
        val a = dao.insert(newListTask("a", now))
        val b = dao.insert(newListTask("b", now))
        dao.insert(newListTask("dated", now).copy(date = LocalDate.of(2026,11,1)))
        assertEquals(setOf(a,b), dao.observeListTasks().first().map { it.id }.toSet())
    }
    @Test fun statusPersists() = runBlocking {
        val dao = db.taskDao()
        val id = dao.insert(newListTask("a", now))
        dao.update(dao.getById(id)!!.withStatus(TaskStatus.DONE, now))
        assertEquals(TaskStatus.DONE, dao.getById(id)!!.status)
        assertEquals(now, dao.getById(id)!!.closedAt)
    }
    @Test fun dateTimeRoundTrip() = runBlocking {
        val task = newListTask("dated", now).copy(date = LocalDate.of(2026,11,1), time = LocalTime.of(9,30))
        val id = db.taskDao().insert(task)
        assertEquals(task.copy(id = id), db.taskDao().getById(id))
    }

    @Test fun onlyTasksOnSelectedDate() = runBlocking {
        val dao = db.taskDao()
        val date = LocalDate.of(2026, 11, 2)
        val id = dao.insert(newTask("今天", "", date, null, now))
        dao.insert(newTask("明天", "", date.plusDays(1), null, now))
        dao.insert(newListTask("无日期", now))
        assertEquals(listOf(id), dao.observeTasksOn(date).first().map { it.id })
    }
    @Test fun addingDateMovesTaskFromList() = runBlocking {
        val dao = db.taskDao()
        val date = LocalDate.of(2026, 11, 2)
        val id = dao.insert(newListTask("待办", now))
        assertEquals(listOf(id), dao.observeListTasks().first().map { it.id })
        val original = dao.getById(id)!!
        dao.update(original.edited(original.title, original.note, date, null, now.plusSeconds(1)))
        assertTrue(dao.observeListTasks().first().isEmpty())
        assertEquals(listOf(id), dao.observeTasksOn(date).first().map { it.id })
    }
    @Test fun titleEditPreservesStoredDateAndTime() = runBlocking {
        val dao = db.taskDao()
        val original = newTask("标题", "内容", LocalDate.of(2026, 11, 2), LocalTime.of(15, 0), now)
        val id = dao.insert(original)
        val loaded = dao.getById(id)!!
        dao.update(loaded.edited("改标题", loaded.note, loaded.date, loaded.time, now.plusSeconds(1)))
        val edited = dao.getById(id)!!
        assertEquals("改标题", edited.title)
        assertEquals(original.date, edited.date); assertEquals(original.time, edited.time)
    }
    @Test fun deletionRemovesTask() = runBlocking {
        val dao = db.taskDao()
        val id = dao.insert(newListTask("删除", now))
        dao.delete(dao.getById(id)!!)
        assertNull(dao.getById(id))
    }
}
