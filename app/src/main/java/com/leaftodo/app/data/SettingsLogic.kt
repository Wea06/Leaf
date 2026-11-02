package com.leaftodo.app.data

import java.time.LocalTime

enum class PaperEffect { FIBER, GRID, NONE }
fun parsePaperEffect(value: String?): PaperEffect = PaperEffect.entries.find { it.name == value } ?: PaperEffect.FIBER
val DEFAULT_REVIEW_TIME: LocalTime = LocalTime.of(23, 0)
fun parseReviewTime(value: String?): LocalTime {
    if (value == null || !value.matches(Regex("[0-9]{2}:[0-9]{2}"))) return DEFAULT_REVIEW_TIME
    return try { LocalTime.of(value.take(2).toInt(), value.takeLast(2).toInt()) }
    catch (_: RuntimeException) { DEFAULT_REVIEW_TIME }
}
