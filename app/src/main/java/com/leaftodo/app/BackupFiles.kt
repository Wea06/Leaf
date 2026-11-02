package com.leaftodo.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.leaftodo.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

internal class BackupFiles(context: Context) {
    private val resolver = context.contentResolver

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            val output = java.io.ByteArrayOutputStream()
            var total = 0
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                total += size
                require(total <= 20 * 1024 * 1024) { "文件太大，不像 Leaf 的备份" }
                output.write(buffer, 0, size)
            }
            output.toString("UTF-8")
        } ?: error("无法打开文件")
    }

    suspend fun writeVerified(uri: Uri, text: String, tasks: List<Task>) = withContext(Dispatchers.IO) {
        (resolver.openOutputStream(uri, "wt") ?: error("无法写入文件")).use {
            it.write(text.toByteArray(Charsets.UTF_8))
        }
        val result = decodeBackup(read(uri))
        require(result is BackupResult.Ok && result.backup.tasks == tasks.sortedBy { it.id }) { "文件写后校验没有通过" }
    }

    suspend fun displayName(uri: Uri, fallback: String): String = withContext(Dispatchers.IO) {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0)?.takeIf(String::isNotBlank) else null
            } ?: fallback
        } catch (_: Exception) { fallback }
    }

    suspend fun saveBeforeImport(text: String, tasks: List<Task>): String = withContext(Dispatchers.IO) {
        val name = preImportFileName(LocalDateTime.now())
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Leaf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }) ?: error("无法创建导入前的备份")
        try {
            writeVerified(uri, text, tasks)
            require(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) == 1) { "无法保存导入前的备份" }
            displayName(uri, name)
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (cleanup: Exception) { e.addSuppressed(cleanup) }
            throw e
        }
    }
}
