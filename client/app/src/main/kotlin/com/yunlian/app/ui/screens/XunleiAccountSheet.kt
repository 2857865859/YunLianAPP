package com.yunlian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yunlian.app.R
import com.yunlian.app.data.db.XunleiAccountEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 已登录迅雷账号的底部弹窗：昵称、登录时间 + 退出登录（二次确认）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XunleiAccountSheet(
    account: XunleiAccountEntity,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val loginTime = remember(account.updatedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(account.updatedAt))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.drive_xunlei),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(account.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("迅雷网盘 · 已登录", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            LoginTimeCard(loginTime)
            Spacer(modifier = Modifier.height(24.dp))
            LogoutButton { showLogoutConfirm = true }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前迅雷账号吗？") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") } }
        )
    }
}

