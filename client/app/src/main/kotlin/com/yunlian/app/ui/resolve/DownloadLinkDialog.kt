package com.yunlian.app.ui.resolve

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yunlian.app.data.network.model.DownloadLink

/**
 * 下载确认弹窗：仅展示文件名，不暴露临时直链。
 * 点「取消」或弹窗外关闭 = 放弃下载，由上层清理临时转存。
 */
@Composable
fun DownloadLinkDialog(
    link: DownloadLink,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = link.filename,
                style = MaterialTheme.typography.titleMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334A8F),
                    contentColor = Color(0xFFF2F5FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        modifier = modifier
    )
}
