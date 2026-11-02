package com.leaftodo.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leaftodo.app.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LeafDatabase.get(application).taskDao()
    val tasks = dao.observeListTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val today = MutableStateFlow(LocalDate.now())
    val todayTasks = today.flatMapLatest { dao.observeTasksOn(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshToday() { today.value = LocalDate.now() }
    suspend fun load(id: Long): Task? = dao.getById(id)
    suspend fun save(id: Long?, title: String, note: String, date: LocalDate?, time: LocalTime?) {
        if (title.isBlank()) return
        if (id == null) dao.insert(newTask(title, note, date, time, Instant.now()))
        else dao.getById(id)?.let { latest ->
            if (!latest.sameContent(title, note, date, time)) {
                dao.update(latest.edited(title, note, date, time, Instant.now()))
            }
        }
    }
    suspend fun delete(id: Long) { dao.getById(id)?.let { dao.delete(it) } }
    fun toggleDone(task: Task) = change(task, if (task.status == TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN)
    fun drop(task: Task) = change(task, TaskStatus.DROPPED)
    fun reopen(task: Task) = change(task, TaskStatus.OPEN)
    private fun change(task: Task, status: TaskStatus) {
        viewModelScope.launch { dao.getById(task.id)?.let { dao.update(it.withStatus(status, Instant.now())) } }
    }
}
