package com.leaftodo.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaftodo.app.data.backupFileName
import com.leaftodo.app.ui.theme.*
import java.time.LocalDateTime

@Composable
internal fun SettingsScreen(onClose: () -> Unit, model: BackupViewModel = viewModel()) {
    val ui by model.ui.collectAsStateWithLifecycle()
    val busy = ui is BackupUi.Busy
    BackHandler { if (!busy) { model.cancelImport(); onClose() } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(model::export)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(model::pickImport)
    }
    Column(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState()).padding(horizontal = 34.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { model.cancelImport(); onClose() }, enabled = !busy,
                modifier = Modifier.width(64.dp), contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = ink, disabledContentColor = soft)) { Text("返回") }
            Text("设置", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = ink, textAlign = TextAlign.Center)
            Spacer(Modifier.width(64.dp))
        }
        Text("备份", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = ink)
        Text("备份是一个 .json 文件，里面是你所有的事。Leaf 不联网，备份只会存在你自己选的位置。重装 App、换手机、装新版本之前，先导出一份，再拷一份到电脑或网盘。",
            style = MaterialTheme.typography.bodyMedium, color = soft)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BackupButton("导出备份", "存成一个文件，自己选放在哪", !busy) { export.launch(backupFileName(LocalDateTime.now())) }
            BackupButton("从备份导入", "用备份替换现在所有的事；替换前自动另存一份到“下载/Leaf”", !busy) { import.launch(arrayOf("*/*")) }
        }
        (ui as? BackupUi.Busy)?.let { Text(it.text, color = soft, style = MaterialTheme.typography.bodyMedium) }
    }
    (ui as? BackupUi.ConfirmImport)?.let { confirm ->
        val message = "这个备份是 ${confirm.exportedAtText} 导出的，里面有 ${confirm.backupCount} 条事。\n" +
            "导入后，Leaf 里现在的 ${confirm.currentCount} 条事会被替换成备份里的。" +
            (if (confirm.currentCount > 0) "\n导入前会先把现在的 ${confirm.currentCount} 条另存到手机的“下载/Leaf”文件夹。" else "") +
            (if (confirm.backupCount == 0) "\n备份里没有事，导入后 Leaf 会变成空的。" else "")
        LeafConfirm(message, "导入", "取消", model::cancelImport, model::confirmImport)
    }
    (ui as? BackupUi.Message)?.let { message ->
        AlertDialog(onDismissRequest = model::dismiss, containerColor = card, tonalElevation = 0.dp,
            textContentColor = ink, text = { Text(message.text) },
            confirmButton = { TextButton(onClick = model::dismiss) { Text("好", color = clayText) } })
    }
}

@Composable
private fun BackupButton(title: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp))
        .background(card).clickable(enabled = enabled, onClick = onClick).padding(16.dp)) {
        Text(title, color = if (enabled) ink else soft, style = MaterialTheme.typography.bodyLarge)
        Text(description, color = soft, fontSize = 13.sp)
    }
}
