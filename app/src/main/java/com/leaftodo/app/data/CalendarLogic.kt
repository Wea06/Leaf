package com.leaftodo.app.data

import java.time.LocalDate
import java.time.YearMonth

enum class DayMark { NONE, TODAY, ALL_CLOSED, OPEN_RECENT, OPEN_STALE, UPCOMING }

fun dayMark(date: LocalDate, tasks: List<Task>, today: LocalDate): DayMark {
    val matching = tasks.filter { it.date == date }
    return when {
        date == today -> DayMark.TODAY
        matching.isEmpty() -> DayMark.NONE
        matching.none { it.status == TaskStatus.OPEN } -> DayMark.ALL_CLOSED
        date > today -> DayMark.UPCOMING
        date in recentRange(today) -> DayMark.OPEN_RECENT
        else -> DayMark.OPEN_STALE
    }
}

fun calendarMarks(tasks: List<Task>, month: YearMonth, today: LocalDate): Map<LocalDate, DayMark> {
    val byDate = tasks.filter { it.date?.let { date -> YearMonth.from(date) == month } == true }
        .groupBy { it.date }
    return (1..month.lengthOfMonth()).associate { day ->
        val date = month.atDay(day)
        date to dayMark(date, byDate[date].orEmpty(), today)
    }
}

fun monthGrid(month: YearMonth): List<LocalDate?> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()
    return List((cells + 6) / 7 * 7) { index ->
        (index - leading + 1).takeIf { it in 1..month.lengthOfMonth() }?.let(month::atDay)
    }
}

fun shiftMonth(month: YearMonth, by: Int): YearMonth = month.plusMonths(by.toLong())

fun selectionAfterFlip(month: YearMonth, today: LocalDate): LocalDate =
    if (YearMonth.from(today) == month) today else month.atDay(1)

fun formatMonth(month: YearMonth): String = "${month.year}年${month.monthValue}月"

fun isStale(task: Task, today: LocalDate): Boolean =
    task.status == TaskStatus.OPEN && task.date?.let { it < recentRange(today).start } == true

fun dayTasks(tasks: List<Task>, date: LocalDate): List<Task> =
    splitToday(tasks.filter { it.date == date }).let { it.open + it.closed }
