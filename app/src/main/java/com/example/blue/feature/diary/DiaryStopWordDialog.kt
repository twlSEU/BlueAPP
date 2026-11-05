package com.example.blue.feature.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blue.R
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedBlue
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.ui.theme.BlueTheme

@Composable
internal fun DiaryStopWordDialog(
    words: Set<String>,
    onAdd: (String) -> String?,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理停用词") },
        text = { DiaryStopWordEditor(words, onAdd, onRemove) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成", color = RefinedBlue) }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White,
        titleContentColor = RefinedInk,
        textContentColor = RefinedInk,
    )
}

@Composable
private fun DiaryStopWordEditor(words: Set<String>, onAdd: (String) -> String?, onRemove: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun add() {
        if (input.isBlank()) return
        error = onAdd(input)
        if (error == null) input = ""
    }

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("输入与高频词一致的词语，所有年份都会排除这些词。", style = MaterialTheme.typography.bodyMedium, color = RefinedMuted)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("停用词") },
            placeholder = { Text("例如：工作") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RefinedBlue,
                focusedLabelColor = RefinedBlue,
                cursorColor = RefinedBlue,
            ),
        )
        TextButton(onClick = ::add, enabled = input.isNotBlank(), modifier = Modifier.align(Alignment.End)) {
            Text("添加")
        }
        Text("已添加（${words.size}）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = RefinedInk)
        if (words.isEmpty()) {
            Text("暂无自定义停用词", style = MaterialTheme.typography.bodySmall, color = RefinedMuted)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                words.sorted().forEach { word ->
                    Surface(onClick = { onRemove(word) }, modifier = Modifier.widthIn(max = 220.dp), shape = CircleShape, color = RefinedBackground) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(word, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = RefinedInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "移除 $word", modifier = Modifier.size(14.dp), tint = RefinedMuted)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "日记总结 · 管理停用词", widthDp = 390, heightDp = 420, showBackground = true)
@Preview(name = "日记总结 · 管理停用词大字体", widthDp = 320, heightDp = 500, fontScale = 1.5f, showBackground = true)
@Composable
private fun DiaryStopWordEditorPreview() {
    BlueTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().background(RefinedBackground).padding(16.dp)) {
            RefinedCard(title = "管理停用词", iconRes = R.drawable.ic_tag) {
                DiaryStopWordEditor(setOf("工作", "咖啡", "weekend"), { null }, {})
            }
        }
    }
}
