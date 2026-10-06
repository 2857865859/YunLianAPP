package com.yunlian.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yunlian.app.R
import com.yunlian.app.data.db.UCAccountEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 已登录 UC 账号的底部弹窗：品牌、昵称、登录时间与退出登录。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UCAccountSheet(account: UCAccountEntity, onLogout: () -> Unit, onDismiss: () -> Unit) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val loginTime = remember(account.updatedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(account.updatedAt))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp)) {
            AccountHeader(account.nickname, "UC网盘 · 已登录", R.drawable.drive_uc)
            Spacer(Modifier.height(18.dp))
            LoginTimeCard(loginTime)
            Spacer(Modifier.height(24.dp))
            LogoutButton { showLogoutConfirm = true }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前 UC 账号吗？退出后将清除本地登录凭证。") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") } }
        )
    }
}
