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
}
