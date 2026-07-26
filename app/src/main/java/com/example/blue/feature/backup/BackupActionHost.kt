package com.example.blue.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.blue.data.backup.BackupInfo
import com.example.blue.data.backup.BackupManager
import com.example.blue.data.backup.RestoreMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

enum class BackupAction {
    EXPORT,
    RESTORE,
}

/**
 * Owns the system document launchers and safety dialogs for home backup actions.
 * It deliberately emits no destination page: tapping a home button opens the
 * corresponding system picker directly.
 */
@Composable
fun BackupActionHost(
    managerProvider: () -> BackupManager,
    requestedAction: BackupAction?,
    onActionConsumed: () -> Unit,
    onMessage: (String, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var inspected by remember { mutableStateOf<BackupInfo?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }

    fun export(uri: Uri) {
        scope.launch {
            busyMessage = "正在导出备份…"
            runCatching { managerProvider().exportTo(uri) }
                .onSuccess { onMessage("备份导出成功", false) }
                .onFailure { onMessage(it.message ?: "导出失败", true) }
            busyMessage = null
        }
    }

    fun inspect(uri: Uri) {
        scope.launch {
            busyMessage = "正在验证备份…"
            runCatching { managerProvider().inspect(uri) }
                .onSuccess { info ->
                    selectedUri = uri
                    inspected = info
                }
                .onFailure { onMessage(it.message ?: "备份文件无效", true) }
            busyMessage = null
        }
    }

    fun restore(mode: RestoreMode) {
        val uri = selectedUri ?: return
        inspected = null
        scope.launch {
            busyMessage = "正在恢复数据…"
            runCatching { managerProvider().restore(uri, mode) }
                .onSuccess {
                    inspected = null
                    selectedUri = null
                    onMessage(
                        if (mode == RestoreMode.MERGE) "已合并恢复数据" else "已完整恢复数据",
                        false,
                    )
                }
                .onFailure { onMessage(it.message ?: "恢复失败", true) }
            busyMessage = null
        }
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(::export) }
    val selectBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(::inspect) }

    LaunchedEffect(requestedAction) {
        val action = requestedAction ?: return@LaunchedEffect
        onActionConsumed()
        when (action) {
            BackupAction.EXPORT -> createBackup.launch(defaultBackupFileName())
            BackupAction.RESTORE -> selectBackup.launch(
                arrayOf("application/zip", "application/x-zip-compressed"),
            )
        }
    }

    inspected?.let { info ->
        AlertDialog(
            onDismissRequest = {
                inspected = null
                selectedUri = null
            },
            title = { Text("已验证备份") },
            text = {
                Text(
                    "日记 ${info.diaryCount} 篇 · 账目 ${info.accountCount} 条 · " +
                        "睡眠 ${info.sleepCount} 条 · 去来 ${info.timeEventCount} 条 · " +
                        "图片 ${info.imageCount} 张",
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { restore(RestoreMode.MERGE) }) {
                        Text("合并数据")
                    }
                    TextButton(
                        onClick = {
                            inspected = null
                            confirmReplace = true
                        },
                    ) {
                        Text("清空后完整恢复")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        inspected = null
                        selectedUri = null
                    },
                ) { Text("取消") }
            },
        )
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("确认清空当前数据？") },
            text = { Text("这会删除当前日记、账目、睡眠记录和照片，再恢复备份。请再次确认。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplace = false
                        restore(RestoreMode.REPLACE)
                    },
                ) { Text("确认清空并恢复") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = false }) { Text("取消") }
            },
        )
    }

    busyMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(message) },
            text = {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            confirmButton = {},
        )
    }
}

private fun defaultBackupFileName(): String =
    "life_record_backup_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))}.zip"
