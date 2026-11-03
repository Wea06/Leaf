package com.leaftodo.app

import android.app.Application
import com.leaftodo.app.reminder.Reminders
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leaftodo.app.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LeafDatabase.get(application).taskDao()
    val today = MutableStateFlow(LocalDate.now())
    val tasks = today.flatMapLatest { dao.observeListSections(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val calendarSelected = MutableStateFlow(today.value)
    private var midnightJob: kotlinx.coroutines.Job? = null

    init { scheduleMidnight() }

    private fun scheduleMidnight() {
        midnightJob?.cancel()
        midnightJob = viewModelScope.launch {
            while (true) {
                val now = ZonedDateTime.now()
                delay(Duration.between(now, nextMidnight(now)).toMillis() + 1000)
                updateToday()
            }
        }
    }
    val monthTasks = calendarSelected.map { YearMonth.from(it) }.distinctUntilChanged()
        .flatMapLatest { dao.observeBetween(it.minusMonths(1).atDay(1), it.plusMonths(1).atEndOfMonth()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDay(date: LocalDate) { calendarSelected.value = date }
    fun selectMonth(month: YearMonth) {
        calendarSelected.value = selectionAfterFlip(month, today.value)
    }
    fun flipMonth(by: Int) {
        calendarSelected.value = selectionAfterFlip(shiftMonth(YearMonth.from(calendarSelected.value), by), today.value)
    }
    fun backToToday() { calendarSelected.value = today.value }
    val todayTasks = today.flatMapLatest { dao.observeTasksOn(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentTasks = today.flatMapLatest {
        val range = recentRange(it)
        dao.observeOpenBetween(range.start, range.endInclusive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshToday() {
        updateToday()
        scheduleMidnight()
    }
    private fun updateToday() {
        val oldToday = today.value
        val newToday = LocalDate.now()
        if (calendarSelected.value == oldToday) calendarSelected.value = newToday
        today.value = newToday
    }
    suspend fun load(id: Long): Task? = dao.getById(id)
    suspend fun save(id: Long?, title: String, note: String, date: LocalDate?, time: LocalTime?) = viewModelScope.async {
        if (title.isBlank()) return@async
        if (id == null) {
            dao.insert(newTask(title, note, date, time, Instant.now()))
            Reminders.requestSync(getApplication())
        }
        else dao.getById(id)?.let { latest ->
            if (!latest.sameContent(title, note, date, time)) {
                dao.update(latest.edited(title, note, date, time, Instant.now()))
                Reminders.requestSync(getApplication())
            }
        }
    }.await()
    suspend fun delete(id: Long) = viewModelScope.async { dao.getById(id)?.let { dao.delete(it); Reminders.requestSync(getApplication()) } }.await()
    fun toggleDone(task: Task) = change(task, if (task.status == TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN)
    fun drop(task: Task) = change(task, TaskStatus.DROPPED)
    fun reopen(task: Task) = change(task, TaskStatus.OPEN)
    private fun change(task: Task, status: TaskStatus) {
        viewModelScope.launch { dao.getById(task.id)?.let { dao.update(it.withStatus(status, Instant.now())); Reminders.requestSync(getApplication()) } }
    }
}
