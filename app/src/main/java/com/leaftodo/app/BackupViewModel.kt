package com.leaftodo.app

import android.app.Application
import com.leaftodo.app.reminder.Reminders
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leaftodo.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*

sealed interface BackupUi {
    data object Idle : BackupUi
    data class Busy(val text: String) : BackupUi
    data class ConfirmImport(val exportedAtText: String, val backupCount: Int, val currentCount: Int) : BackupUi
    data class Message(val text: String) : BackupUi
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LeafDatabase.get(application)
    private val files = BackupFiles(application)
    private val state = MutableStateFlow<BackupUi>(BackupUi.Idle)
    val ui = state.asStateFlow()
    private var pending: Backup? = null

    fun export(uri: Uri) {
        if (state.value is BackupUi.Busy) return
        state.value = BackupUi.Busy("正在导出…")
        viewModelScope.launch {
            try {
                val tasks = db.taskDao().getAll()
                val text = withContext(Dispatchers.IO) { encodeBackup(tasks, Instant.now()) }
                files.writeVerified(uri, text, tasks)
                val name = files.displayName(uri, backupFileName(LocalDateTime.now()))
                state.value = BackupUi.Message("已导出 ${tasks.size} 条事到“$name”。建议再拷一份到电脑或网盘。")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                state.value = BackupUi.Message("导出失败，文件没有写好，请重新导出。（${e.message}）")
            }
        }
    }

    fun pickImport(uri: Uri) {
        if (state.value is BackupUi.Busy) return
        pending = null
        state.value = BackupUi.Busy("正在检查备份…")
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { decodeBackup(files.read(uri)) }
                when (result) {
                    is BackupResult.Failed -> state.value = BackupUi.Message("这个文件不能导入：${result.reason}。Leaf 里的事一条都没动。")
                    is BackupResult.Ok -> {
                        pending = result.backup
                        val zone = ZoneId.systemDefault()
                        state.value = BackupUi.ConfirmImport(formatBackupTime(result.backup.exportedAt, zone, LocalDate.now(zone)), result.backup.tasks.size, db.taskDao().count())
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                pending = null
                state.value = BackupUi.Message("这个文件不能导入：${e.message}。Leaf 里的事一条都没动。")
            }
        }
    }

    fun confirmImport() {
        if (state.value !is BackupUi.ConfirmImport) return
        val backup = pending ?: return
        state.value = BackupUi.Busy("正在导入…")
        viewModelScope.launch {
            var saved: String? = null
            val current: List<Task>
            try {
                current = db.taskDao().getAll()
                if (current.isNotEmpty()) {
                    val text = withContext(Dispatchers.IO) { encodeBackup(current, Instant.now()) }
                    saved = files.saveBeforeImport(text, current)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                pending = null
                state.value = BackupUi.Message("导入前另存现在的数据没有成功，为了安全没有导入。Leaf 里的事一条都没动。（${e.message}）")
                return@launch
            }
            try {
                db.replaceAllTasks(backup.tasks)
                Reminders.requestSync(getApplication())
                state.value = BackupUi.Message("已导入 ${backup.tasks.size} 条事。" +
                    (saved?.let { "原来的 ${current.size} 条另存在手机的“下载/Leaf/$it”，需要时可以再导入回来。" } ?: ""))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                state.value = BackupUi.Message("导入失败，Leaf 里的事没有变化。（${e.message}）")
            } finally { pending = null }
        }
    }

    fun cancelImport() { if (state.value !is BackupUi.Busy) { pending = null; state.value = BackupUi.Idle } }
    fun dismiss() = cancelImport()
}
