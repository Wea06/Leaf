package com.leaftodo.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leaftodo.app.data.*
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LeafDatabase.get(application).taskDao()
    val tasks = dao.observeListTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun add(title: String) { if (title.isNotBlank()) viewModelScope.launch { dao.insert(newListTask(title, Instant.now())) } }
    fun toggleDone(task: Task) = change(task, if (task.status == TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN)
    fun drop(task: Task) = change(task, TaskStatus.DROPPED)
    fun reopen(task: Task) = change(task, TaskStatus.OPEN)
    private fun change(task: Task, status: TaskStatus) {
        viewModelScope.launch { dao.getById(task.id)?.let { dao.update(it.withStatus(status, Instant.now())) } }
    }
}
