package com.leaftodo.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

const val BACKUP_APP = "Leaf"
const val BACKUP_FORMAT = 1
// 改表升版本时这里也要改，并按 SPEC 7.1 处理旧备份。
const val BACKUP_DB_VERSION = 1
private val minuteFormat = DateTimeFormatter.ofPattern("HH:mm")

data class Backup(val format: Int, val exportedAt: Instant, val tasks: List<Task>)
sealed interface BackupResult {
    data class Ok(val backup: Backup) : BackupResult
    data class Failed(val reason: String) : BackupResult
}

fun encodeBackup(tasks: List<Task>, exportedAt: Instant): String = JSONObject().apply {
    put("app", BACKUP_APP); put("format", BACKUP_FORMAT); put("dbVersion", BACKUP_DB_VERSION)
    put("exportedAt", exportedAt.toString()); put("count", tasks.size)
    put("tasks", JSONArray().apply {
        tasks.sortedBy { it.id }.forEach { task ->
            put(JSONObject().apply {
                put("id", task.id); put("title", task.title); put("note", task.note)
                put("date", task.date?.toString() ?: JSONObject.NULL)
                put("time", task.time?.format(minuteFormat) ?: JSONObject.NULL)
                put("status", task.status.name)
                put("createdAt", task.createdAt.toString()); put("updatedAt", task.updatedAt.toString())
                put("closedAt", task.closedAt?.toString() ?: JSONObject.NULL)
            })
        }
    })
}.toString(2)

private fun JSONObject.string(key: String): String = (get(key) as? String)
    ?: error("$key 格式不对")
private fun JSONObject.integer(key: String): Long {
    val value = get(key)
    require(value is Int || value is Long) { "$key 必须是整数" }
    return (value as Number).toLong()
}
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else string(key)

fun decodeBackup(text: String): BackupResult {
    val root = try { JSONObject(text) } catch (_: Exception) {
        return BackupResult.Failed("不是 Leaf 的备份文件")
    }
    if (root.opt("app") != BACKUP_APP) return BackupResult.Failed("不是 Leaf 的备份文件")
    return try {
        val format = root.integer("format")
        if (format > BACKUP_FORMAT) return BackupResult.Failed("这个备份来自更新版本的 Leaf，请先升级 App 再导入")
        require(format >= 1)
        val exportedAt = Instant.parse(root.string("exportedAt"))
        val rows = root.getJSONArray("tasks")
        require(root.integer("count") == rows.length().toLong())
        val ids = mutableSetOf<Long>()
        val tasks = (0 until rows.length()).map { index ->
            try {
                val row = rows.getJSONObject(index)
                val id = row.integer("id")
                require(id > 0) { "编号必须是正整数" }
                if (!ids.add(id)) return BackupResult.Failed("第 ${index + 1} 条和前面的编号重复")
                val title = row.string("title")
                require(title.isNotBlank()) { "标题不能为空" }
                val date = row.nullableString("date")?.let {
                    require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(it)) { "日期必须是 yyyy-MM-dd" }
                    try { LocalDate.parse(it) } catch (_: Exception) { error("日期不是真实日期") }
                }
                val time = row.nullableString("time")?.let {
                    require(Regex("[0-9]{2}:[0-9]{2}").matches(it)) { "时间必须是 HH:mm" }
                    try { LocalTime.parse(it) } catch (_: Exception) { error("时间不正确") }
                }
                require(time == null || date != null) { "有时间时必须有日期" }
                val status = try { TaskStatus.valueOf(row.string("status")) } catch (_: Exception) { error("状态必须是 OPEN、DONE 或 DROPPED") }
                fun instant(key: String): Instant = try { Instant.parse(row.string(key)) } catch (_: Exception) { error("$key 时刻不正确或缺失") }
                Task(id, title, if (row.has("note")) row.string("note") else "", date, time, status,
                    instant("createdAt"), instant("updatedAt"),
                    if (row.isNull("closedAt")) null else instant("closedAt"))
            } catch (e: Exception) {
                return BackupResult.Failed("第 ${index + 1} 条有问题：${e.message ?: "记录损坏"}")
            }
        }
        BackupResult.Ok(Backup(format.toInt(), exportedAt, tasks))
    } catch (_: Exception) { BackupResult.Failed("备份文件已损坏") }
}

fun backupFileName(now: LocalDateTime): String = "leaf-${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))}.json"
fun preImportFileName(now: LocalDateTime): String = "leaf-before-import-${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))}.json"
fun formatBackupTime(instant: Instant, zone: ZoneId, today: LocalDate): String {
    val local = instant.atZone(zone)
    return formatDate(local.toLocalDate(), today) + " " + local.format(minuteFormat)
}
