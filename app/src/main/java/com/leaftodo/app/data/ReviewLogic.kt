package com.leaftodo.app.data

import java.time.*

fun reviewDayFor(fireAt: LocalDateTime): LocalDate =
    fireAt.toLocalDate().let { if (fireAt.hour < 4) it.minusDays(1) else it }

fun nextReviewAt(time: LocalTime, now: Instant, zone: ZoneId): Instant {
    val today = now.atZone(zone).toLocalDate()
    val candidate = today.atTime(time).atZone(zone).toInstant()
    return if (candidate > now) candidate else today.plusDays(1).atTime(time).atZone(zone).toInstant()
}

const val REVIEW_CATCH_UP_HOURS = 2L
data class ReviewPlan(val showDay: LocalDate?, val nextAt: Instant?)
fun planReview(enabled: Boolean, time: LocalTime, armedAt: Instant?, lastShownDay: LocalDate?, now: Instant, zone: ZoneId): ReviewPlan {
    if (!enabled) return ReviewPlan(null, null)
    val show = armedAt?.takeIf { it <= now && Duration.between(it, now) <= Duration.ofHours(REVIEW_CATCH_UP_HOURS) }
        ?.let { reviewDayFor(it.atZone(zone).toLocalDateTime()) }?.takeIf { it != lastShownDay }
    return ReviewPlan(show, nextReviewAt(time, now, zone))
}

data class ReviewSummary(val title: String, val text: String, val bigText: String)
fun reviewSummary(tasks: List<Task>, day: LocalDate, today: LocalDate): ReviewSummary? {
    val dated = tasks.filter { it.date == day }
    if (dated.isEmpty()) return null
    val done = dated.count { it.status == TaskStatus.DONE }
    val dropped = dated.count { it.status == TaskStatus.DROPPED }
    val open = dated.filter { it.status == TaskStatus.OPEN }
        .sortedWith(compareBy<Task> { it.time == null }.thenBy { it.time }.thenBy { it.createdAt })
    val word = when (day) { today -> "今天"; today.minusDays(1) -> "昨天"; else -> formatDate(day, today) }
    val text = "${word}完成 $done 条" + (if (dropped > 0) "，划掉 $dropped 条" else "") +
        when { open.isNotEmpty() -> "，还剩 ${open.size} 条没做完"; dropped == 0 -> "，全都做完了"; else -> "，没有剩下的" }
    val big = text + if (open.isEmpty()) "" else "\n没做完：" + open.take(5).joinToString("、") { it.title } +
        if (open.size > 5) " 等 ${open.size} 条" else ""
    return ReviewSummary(formatDate(day, today) + " 回顾", text, big)
}
