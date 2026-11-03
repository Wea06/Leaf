package com.leaftodo.app.data

import java.time.LocalDate
import java.time.ZonedDateTime

data class ListSections(
    val future: List<DayGroup>,
    val noDate: List<Task>,
    val overdueRecent: List<DayGroup>,
    val overdueOlder: List<DayGroup>,
    val closed: List<Task>,
)

fun listSections(tasks: List<Task>, today: LocalDate): ListSections {
    val open = tasks.filter { it.status == TaskStatus.OPEN }
    val range = recentRange(today)
    fun grouped(tasks: List<Task>, order: Comparator<LocalDate>) = tasks
        .groupBy { requireNotNull(it.date) }.toSortedMap(order)
        .map { (date, items) -> DayGroup(date, items.sortedWith(openTaskComparator)) }
    return ListSections(
        future = grouped(open.filter { it.date?.isAfter(today) == true }, naturalOrder()),
        noDate = open.filter { it.date == null }.sortedBy { it.createdAt },
        overdueRecent = recentUnfinished(tasks, today),
        overdueOlder = grouped(open.filter { it.date?.isBefore(range.start) == true }, reverseOrder()),
        closed = tasks.filter {
            it.status != TaskStatus.OPEN && (it.date == null || it.date.isAfter(today))
        }.sortedByDescending { it.closedAt },
    )
}

fun groupTitle(date: LocalDate, today: LocalDate): String {
    val prefix = when (date) {
        today.plusDays(1) -> "明天 · "
        today.plusDays(2) -> "后天 · "
        today.minusDays(1) -> "昨天 · "
        else -> ""
    }
    return prefix + formatDate(date, today)
}

fun nextMidnight(now: ZonedDateTime): ZonedDateTime =
    now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
