package com.yunlian.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yunlian.app.R
import com.yunlian.app.data.db.QuarkAccountEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 已登录夸克账号的底部弹窗：品牌、昵称、登录时间与退出登录。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuarkAccountSheet(account: QuarkAccountEntity, onLogout: () -> Unit, onDismiss: () -> Unit) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val loginTime = remember(account.updatedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(account.updatedAt))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp)) {
            AccountHeader(account.nickname, "夸克网盘 · 已登录", R.drawable.drive_quark)
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
            text = { Text("确定要退出当前夸克账号吗？退出后将清除本地登录凭证。") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
internal fun AccountHeader(nickname: String, status: String, logoRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(logoRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun LoginTimeCard(loginTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "登录时间",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(60.dp)
            )
            Text(loginTime, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB3263E),
            contentColor = Color.White
        )
    ) {
        Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("退出登录")
    }
}
