package com.leaftodo.app.data

import java.time.Instant
import java.time.LocalDate

fun formatDate(date: LocalDate, today: LocalDate): String {
    val year = if (date.year == today.year) "" else "${date.year}年"
    val weekday = listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]
    return "$year${date.monthValue}月${date.dayOfMonth}日 周$weekday"
}

fun newListTask(title: String, now: Instant): Task =
    Task(title = title.trim(), createdAt = now, updatedAt = now)

fun Task.withStatus(newStatus: TaskStatus, now: Instant): Task = copy(
    status = newStatus, updatedAt = now,
    closedAt = if (newStatus == TaskStatus.OPEN) null else now,
)

data class TaskGroups(val open: List<Task>, val closed: List<Task>)
fun splitList(tasks: List<Task>): TaskGroups = TaskGroups(
    tasks.filter { it.status == TaskStatus.OPEN }.sortedBy { it.createdAt },
    tasks.filter { it.status != TaskStatus.OPEN }.sortedByDescending { it.closedAt },
)
