package com.leaftodo.app.data

import java.time.*
import java.time.format.DateTimeFormatter

fun Task.reminderAt(zone: ZoneId): Instant? =
    if (status == TaskStatus.OPEN && date != null && time != null)
        date.atTime(time.withSecond(0).withNano(0)).atZone(zone).toInstant() else null

fun nextAlarmAt(tasks: List<Task>, armedAt: Instant?, now: Instant, zone: ZoneId): Instant? =
    tasks.mapNotNull { it.reminderAt(zone) }.filter {
        if (armedAt != null && armedAt <= now) it >= armedAt else it > now
    }.minOrNull()

const val CATCH_UP_HOURS = 24L

fun dueReminders(tasks: List<Task>, from: Instant, now: Instant, zone: ZoneId): List<Task> =
    tasks.filter { task -> task.reminderAt(zone)?.let {
        it >= from && it <= now && it >= now.minusSeconds(CATCH_UP_HOURS * 3600)
    } == true }.sortedWith(compareBy<Task> { it.reminderAt(zone) }.thenBy { it.id })

fun keepNotification(task: Task?, now: Instant, zone: ZoneId): Boolean =
    task?.reminderAt(zone)?.let { it <= now } == true

fun reminderText(task: Task, today: LocalDate): String {
    val date = task.date ?: return ""
    val time = task.time ?: return ""
    return formatDate(date, today) + " " + time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun isPastReminder(date: LocalDate?, time: LocalTime?, now: Instant, zone: ZoneId): Boolean =
    date != null && time != null && date.atTime(time).atZone(zone).toInstant() <= now

data class NextReminder(val at: Instant, val tasks: List<Task>)

fun nextReminder(tasks: List<Task>, now: Instant, zone: ZoneId): NextReminder? {
    val at = nextAlarmAt(tasks, null, now, zone) ?: return null
    return NextReminder(at, tasks.filter { it.reminderAt(zone) == at }.sortedBy { it.id })
}
