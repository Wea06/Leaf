package com.leaftodo.app.settings

import android.content.Context
import com.leaftodo.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LeafSettings private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val enabled = MutableStateFlow(prefs.getBoolean("reviewEnabled", true))
    private val time = MutableStateFlow(parseReviewTime(prefs.getString("reviewTime", null)))
    private val paper = MutableStateFlow(parsePaperEffect(prefs.getString("paperEffect", null)))
    val reviewEnabled = enabled.asStateFlow()
    val reviewTime = time.asStateFlow()
    val paperEffect = paper.asStateFlow()
    @Synchronized fun setReviewEnabled(value: Boolean) {
        if (prefs.edit().putBoolean("reviewEnabled", value).commit()) enabled.value = value
    }
    @Synchronized fun setReviewTime(value: LocalTime) {
        val text = value.format(DateTimeFormatter.ofPattern("HH:mm"))
        if (prefs.edit().putString("reviewTime", text).commit()) time.value = parseReviewTime(text)
    }
    @Synchronized fun setPaperEffect(value: PaperEffect) {
        if (prefs.edit().putString("paperEffect", value.name).commit()) paper.value = value
    }
    companion object {
        @Volatile private var instance: LeafSettings? = null
        fun get(context: Context): LeafSettings = instance ?: synchronized(this) {
            instance ?: LeafSettings(context).also { instance = it }
        }
    }
}
