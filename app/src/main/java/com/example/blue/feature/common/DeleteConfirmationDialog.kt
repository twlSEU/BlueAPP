package com.example.blue.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DialogTitle = Color(0xFF2D4555)
private val DialogBody = Color(0xFF748895)
private val DialogDanger = Color(0xFFD95C5C)
private val DialogDangerSoft = Color(0xFFFFEEEE)
private val DialogBorder = Color(0xFFDCE7EE)

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "删除",
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFEFFFF),
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DialogDangerSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DialogDanger,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DialogTitle,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DialogBody,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(26.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, DialogBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DialogTitle),
                    ) {
                        Text("取消", fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DialogDanger,
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    ) {
                        Text(confirmLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
