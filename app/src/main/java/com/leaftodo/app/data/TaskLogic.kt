package com.leaftodo.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun formatDate(date: LocalDate, today: LocalDate): String {
    val year = if (date.year == today.year) "" else "${date.year}年"
    val weekday = listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]
    return "$year${date.monthValue}月${date.dayOfMonth}日 周$weekday"
}

fun newListTask(title: String, now: Instant): Task =
    newTask(title, "", null, null, now)

fun newTask(title: String, note: String, date: LocalDate?, time: LocalTime?, now: Instant): Task =
    Task(title = title.trim(), note = note.trimEnd(), date = date,
        time = if (date == null) null else time, createdAt = now, updatedAt = now)

fun Task.edited(title: String, note: String, date: LocalDate?, time: LocalTime?, now: Instant): Task =
    copy(title = title.trim(), note = note.trimEnd(), date = date,
        time = if (date == null) null else time, updatedAt = now)

fun Task.sameContent(title: String, note: String, date: LocalDate?, time: LocalTime?): Boolean =
    this.title == title.trim() && this.note.trimEnd() == note.trimEnd() && this.date == date && this.time == time

enum class TaskPage { TODAY, LIST, CALENDAR }

fun defaultDateFor(page: TaskPage, today: LocalDate): LocalDate? =
    if (page == TaskPage.TODAY) today else null

fun splitToday(tasks: List<Task>): TaskGroups = TaskGroups(
    tasks.filter { it.status == TaskStatus.OPEN }
        .sortedWith(openTaskComparator),
    tasks.filter { it.status != TaskStatus.OPEN }.sortedByDescending { it.closedAt },
)

private val openTaskComparator: Comparator<Task> =
    compareBy<Task> { it.time == null }.thenBy { it.time }.thenBy { it.createdAt }

fun recentRange(today: LocalDate): ClosedRange<LocalDate> = today.minusDays(7)..today.minusDays(1)

data class DayGroup(val date: LocalDate, val tasks: List<Task>)

fun recentUnfinished(tasks: List<Task>, today: LocalDate): List<DayGroup> {
    val range = recentRange(today)
    return tasks.filter { it.status == TaskStatus.OPEN && it.date?.let { date -> date in range } == true }
        .groupBy { requireNotNull(it.date) }.toSortedMap(reverseOrder())
        .map { (date, tasks) -> DayGroup(date, tasks.sortedWith(openTaskComparator)) }
}

fun Task.createdDate(zone: ZoneId): LocalDate = createdAt.atZone(zone).toLocalDate()

fun Task.withStatus(newStatus: TaskStatus, now: Instant): Task = copy(
    status = newStatus, updatedAt = now,
    closedAt = if (newStatus == TaskStatus.OPEN) null else now,
)

data class TaskGroups(val open: List<Task>, val closed: List<Task>)
fun splitList(tasks: List<Task>): TaskGroups = TaskGroups(
    tasks.filter { it.status == TaskStatus.OPEN }.sortedBy { it.createdAt },
    tasks.filter { it.status != TaskStatus.OPEN }.sortedByDescending { it.closedAt },
)
