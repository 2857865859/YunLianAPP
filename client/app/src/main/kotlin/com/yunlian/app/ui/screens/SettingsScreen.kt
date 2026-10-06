package com.yunlian.app.ui.screens
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yunlian.app.data.download.DownloadSaver
import com.yunlian.app.data.prefs.SettingsRepository
import com.yunlian.app.ui.SnackbarController
import com.yunlian.app.ui.components.SectionHeader
import com.yunlian.app.ui.components.TechPageHero
import com.yunlian.app.ui.theme.TechBackground
import com.yunlian.app.ui.theme.TechCyan
import com.yunlian.app.ui.theme.TechOutline
import com.yunlian.app.ui.theme.TechSurface
import com.yunlian.app.ui.theme.TechTextPrimary
import com.yunlian.app.ui.theme.TechTextSecondary

/** 可选的下载线程数（最高 512） */
private val threadOptions = listOf(8, 16, 32, 64)

/**
 * 设置页：下载设置与关于信息。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    downloadThreads: Int,
    onThreadsChange: (Int) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThreadsDialog by remember { mutableStateOf(false) }
    // 本地状态：修改后立即刷新 UI，同时同步外部保存值
    var threads by remember { mutableStateOf(downloadThreads) }
    LaunchedEffect(downloadThreads) { threads = downloadThreads }
    val context = LocalContext.current
    // 下载保存目录（SAF）：本地状态驱动 UI 刷新，同时同步 SharedPreferences
    val settingsRepo = remember { SettingsRepository(context) }
    var downloadDirUri by remember { mutableStateOf(settingsRepo.downloadDirUri) }
    // 网络与下载策略（本地状态驱动 UI，同时同步 SharedPreferences）
    var maxConcurrent by remember { mutableStateOf(settingsRepo.maxConcurrentDownloads) }
    var showConcurrencyDialog by remember { mutableStateOf(false) }
    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 持久授权：应用重启后仍可写（API19+；Android 10/11+ 分区存储必需）
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            settingsRepo.downloadDirUri = uri.toString()
            downloadDirUri = uri.toString()
            SnackbarController.show("下载保存目录已更新")
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechBackground)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
    ) {
        TechPageHero(title = "设置")
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        SectionLabel("下载")
        SettingsItem(
            icon = Icons.Outlined.Tune,
            title = "下载线程数",
            description = "当前 $threads 线程（分片并发）",
            onClick = { showThreadsDialog = true }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 网络与下载策略
        SettingsItem(
            icon = Icons.Outlined.Layers,
            title = "最大同时下载任务数",
            description = "当前同时下载 $maxConcurrent 个任务",
            onClick = { showConcurrencyDialog = true }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 下载保存目录：系统文件夹选择器（SAF，适配各 Android 版本分区存储）；
        // 已自定义时卡片右侧内嵌「恢复默认」操作（不单独外露按钮）
        SettingsItem(
            icon = Icons.Outlined.FolderOpen,
            title = "下载保存目录",
            description = downloadDirUri?.let { "已自定义：${DownloadSaver.safDirDisplay(it)}" }
                ?: "系统默认目录DownLoad",
            onClick = { dirLauncher.launch(null) },
            trailing = if (downloadDirUri != null) {
                {
                    TextButton(
                        onClick = {
                            downloadDirUri = null
                            settingsRepo.downloadDirUri = null
                            SnackbarController.show("已恢复默认下载目录")
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "恢复默认",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("关于")
        SettingsItem(
            icon = Icons.Outlined.Info,
            title = "关于云链助手",
            description = null,
            onClick = onAboutClick
        )
        }
    }

    // 线程数选择弹窗
    if (showThreadsDialog) {
        AlertDialog(
            onDismissRequest = { showThreadsDialog = false },
            title = { Text("下载线程数") },
            text = {
                Column {
                    Text(
                        text = "线程数越多，分片并行下载越快",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 两列网格：10 个选项 5 行一屏可见，无需滑动就知道有哪些档位；
                    // 仍保留限高 + 滚动，横屏/矮屏时兜底
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        threadOptions.chunked(2).forEach { rowValues ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowValues.forEach { value ->
                                    RadioThreadRow(
                                        value = value,
                                        threads = threads,
                                        onSelect = { v ->
                                            threads = v
                                            onThreadsChange(v)
                                            showThreadsDialog = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // 奇数个时补空占位，保持两列对齐
                                if (rowValues.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThreadsDialog = false }) { Text("取消") }
            }
        )
    }

    // 最大同时下载任务数
    if (showConcurrencyDialog) {
        val options = listOf(2, 4, 6, 8)
        AlertDialog(
            onDismissRequest = { showConcurrencyDialog = false },
            title = { Text("最大同时下载任务数") },
            text = {
                Column {
                    Text(
                        text = "限制后台并发，避免占满带宽",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    options.chunked(2).forEach { rowValues ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowValues.forEach { v ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = maxConcurrent == v,
                                        onClick = {
                                            maxConcurrent = v
                                            settingsRepo.maxConcurrentDownloads = v
                                            showConcurrencyDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${v}个",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConcurrencyDialog = false }) { Text("取消") }
            }
        )
    }

}

@Composable
private fun SectionLabel(text: String) {
    SectionHeader(text = text, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String?,
    onClick: () -> Unit,
    /** 长按回调（隐藏菜单等）；null 时不启用长按 */
    onLongClick: (() -> Unit)? = null,
    /** 自定义尾部内容（如「恢复默认」操作）；null 时显示默认 ChevronRight */
    trailing: @Composable (() -> Unit)? = null
) {
    val shape = MaterialTheme.shapes.large
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = TechSurface
        ),
        border = BorderStroke(1.dp, TechOutline.copy(alpha = .68f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TechCyan
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TechTextPrimary
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TechTextSecondary.copy(alpha = .92f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TechTextSecondary
                )
            }
        }
    }
}

/** 线程数单选行（用于弹窗两列布局，每行占半宽） */
@Composable
private fun RadioThreadRow(
    value: Int,
    threads: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = threads == value,
            onClick = { onSelect(value) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$value 线程",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

