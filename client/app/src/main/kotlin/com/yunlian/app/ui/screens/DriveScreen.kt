package com.yunlian.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.yunlian.app.ui.components.TechGradient
import com.yunlian.app.ui.components.TechPageHero
import com.yunlian.app.ui.theme.TechBackground
import com.yunlian.app.ui.theme.TechBlue
import com.yunlian.app.ui.theme.TechCyan
import com.yunlian.app.ui.theme.TechOutline
import com.yunlian.app.ui.theme.TechPurple
import com.yunlian.app.ui.theme.TechSurface
import com.yunlian.app.ui.theme.TechTextPrimary
import com.yunlian.app.ui.theme.TechTextSecondary
import com.yunlian.app.ui.theme.TechWarning
import com.yunlian.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunlian.app.data.db.QuarkAccountEntity
import com.yunlian.app.data.db.UCAccountEntity
import com.yunlian.app.data.db.XunleiAccountEntity
import com.yunlian.app.data.network.model.QuotaInfo
import com.yunlian.app.ui.viewmodel.DriveQuotaViewModel
import com.yunlian.app.ui.viewmodel.QuarkCloudViewModel
import com.yunlian.app.ui.viewmodel.UCCoudViewModel
import com.yunlian.app.ui.viewmodel.XunleiCloudViewModel

/**
 * 网盘账号展示模型。
 * TODO: 迅雷 / UC 后续接入 cookie 登录后，isLoggedIn 由真实登录态驱动。
 */
private data class DriveAccount(
    val id: String,
    val name: String,
    val description: String,
    val avatarText: String,
    val iconRes: Int? = null,
    val isLoggedIn: Boolean = false
)

/**
 * 网盘页：
 * - 夸克未登录：点击进入登录页；
 * - 夸克已登录：副标题显示昵称，点击弹出账号信息底部弹窗（可查看 Cookie / 退出登录）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    quarkAccount: QuarkAccountEntity?,
    ucAccount: UCAccountEntity?,
    xunleiAccount: XunleiAccountEntity?,
    /** 夸克云盘浏览 ViewModel（网盘 Tab 内切换展示，非全屏） */
    quarkCloudViewModel: QuarkCloudViewModel,
    /** UC 网盘云盘浏览 ViewModel */
    ucCloudViewModel: UCCoudViewModel,
    /** 迅雷网盘云盘浏览 ViewModel */
    xunleiCloudViewModel: XunleiCloudViewModel,
    /** 网盘空间详情 ViewModel（顶部空间总览） */
    driveQuotaViewModel: DriveQuotaViewModel,
    onQuarkLogin: () -> Unit,
    onQuarkLogout: () -> Unit,
    /** 夸克云盘下载入队后切换到「下载」Tab */
    onDownloadStarted: () -> Unit = {},
    onUCLogin: () -> Unit,
    onUCLogout: () -> Unit,
    onXunleiLogin: () -> Unit,
    onXunleiLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuarkSheet by remember { mutableStateOf(false) }
    var showUCSheet by remember { mutableStateOf(false) }
    var showXunleiSheet by remember { mutableStateOf(false) }
    // 夸克云盘浏览：网盘 Tab 内切换（非全屏），切 Tab 再回来仍保留
    var showCloud by rememberSaveable { mutableStateOf(false) }
    // UC 网盘云盘浏览：网盘 Tab 内切换（非全屏）
    var showUCCloud by rememberSaveable { mutableStateOf(false) }
    // 迅雷网盘云盘浏览：网盘 Tab 内切换（非全屏）
    var showXunleiCloud by rememberSaveable { mutableStateOf(false) }

    // 夸克：登录态由数据库驱动；已登录则副标题显示昵称
    val quark = DriveAccount(
        id = "quark",
        name = "夸克网盘",
        description = quarkAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "夸",
        iconRes = R.drawable.drive_quark,
        isLoggedIn = quarkAccount != null
    )
    val uc = DriveAccount(
        id = "uc",
        name = "UC网盘",
        description = ucAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "UC",
        iconRes = R.drawable.drive_uc,
        isLoggedIn = ucAccount != null
    )
    val xunlei = DriveAccount(
        id = "xunlei",
        name = "迅雷网盘",
        description = xunleiAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "迅",
        iconRes = R.drawable.drive_xunlei,
        isLoggedIn = xunleiAccount != null
    )
    val others = remember {
        emptyList<DriveAccount>()
    }

    // 进入网盘页加载空间详情（仅已登录平台）
    LaunchedEffect(Unit) {
        driveQuotaViewModel.loadAll()
    }
    // 下拉刷新状态：绑定空间配额加载中状态
    val isRefreshing by driveQuotaViewModel.loading.collectAsState()

    // 账号列表 ↔ 夸克云盘 ↔ UC 云盘 ↔ 迅雷云盘：平滑过渡。
    AnimatedContent(
        targetState = when {
            showCloud -> 1
            showUCCloud -> 2
            showXunleiCloud -> 3
            else -> 0
        },
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f))
                .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.98f))
        },
        label = "driveContent"
    ) { target ->
        when (target) {
            1 -> CloudDriveScreen(
                viewModel = quarkCloudViewModel,
                onExit = { showCloud = false },
                onDownloadStarted = onDownloadStarted
        )
        2 -> UCCoudScreen(
            viewModel = ucCloudViewModel,
            onExit = { showUCCloud = false },
            onDownloadStarted = onDownloadStarted
        )
        3 -> XunleiCloudScreen(
            viewModel = xunleiCloudViewModel,
            onExit = { showXunleiCloud = false },
            onDownloadStarted = onDownloadStarted
        )
            else -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { driveQuotaViewModel.loadAll() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(TechBackground)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TechPageHero(
                        title = "网盘",
                        subtitle = "登录后即可高速解析下载"
                    )
                }
                item(key = quark.id) {
                    DriveAccountCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        account = quark,
                        quota = driveQuotaViewModel.quarkQuota.collectAsState().value,
                        onClick = if (quark.isLoggedIn) {
                            {
                                quarkCloudViewModel.loadRoot()
                                showCloud = true
                            }
                        } else {
                            onQuarkLogin
                        },
                        onMoreClick = if (quark.isLoggedIn) {
                            { showQuarkSheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = uc.id) {
                    DriveAccountCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        account = uc,
                        quota = driveQuotaViewModel.ucQuota.collectAsState().value,
                        onClick = if (uc.isLoggedIn) {
                            {
                                ucCloudViewModel.loadRoot()
                                showUCCloud = true
                            }
                        } else {
                            onUCLogin
                        },
                        onMoreClick = if (uc.isLoggedIn) {
                            { showUCSheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = xunlei.id) {
                    DriveAccountCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        account = xunlei,
                        quota = driveQuotaViewModel.xunleiQuota.collectAsState().value,
                        onClick = if (xunlei.isLoggedIn) {
                            { showXunleiCloud = true }
                        } else {
                            onXunleiLogin
                        },
                        onMoreClick = if (xunlei.isLoggedIn) {
                            { showXunleiSheet = true }
                        } else {
                            null
                        }
                    )
                }
                items(others, key = { it.id }) { account ->
                    DriveAccountCard(
                        account = account,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            }
        }
    }

    // 已登录夸克：点击卡片弹出账号信息底部弹窗
    if (showQuarkSheet && quarkAccount != null) {
        QuarkAccountSheet(
            account = quarkAccount,
            onLogout = {
                onQuarkLogout()
                showQuarkSheet = false
            },
            onDismiss = { showQuarkSheet = false }
        )
    }

    // 已登录 UC：点击卡片弹出账号信息底部弹窗
    if (showUCSheet && ucAccount != null) {
        UCAccountSheet(
            account = ucAccount,
            onLogout = {
                onUCLogout()
                showUCSheet = false
            },
            onDismiss = { showUCSheet = false }
        )
    }

    // 已登录迅雷：点击卡片弹出账号信息底部弹窗
    if (showXunleiSheet && xunleiAccount != null) {
        XunleiAccountSheet(
            account = xunleiAccount,
            onLogout = {
                onXunleiLogout()
                showXunleiSheet = false
            },
            onDismiss = { showXunleiSheet = false }
        )
    }

}

@Composable
private fun DriveAccountCard(
    account: DriveAccount,
    modifier: Modifier = Modifier,
    /** 网盘空间详情（已登录且有数据时在卡片内显示进度条）；null 不显示 */
    quota: QuotaInfo? = null,
    onClick: (() -> Unit)? = null,
    /** 已登录时右侧「三个点」更多按钮（打开账号弹窗）；null 则不显示 */
    onMoreClick: (() -> Unit)? = null
) {
    val cardShape = MaterialTheme.shapes.large
    val accent = when (account.id) {
        "quark" -> TechPurple
        "uc" -> TechBlue
        "xunlei" -> TechWarning
        else -> TechCyan
    }
    val cardColors = CardDefaults.cardColors(
        containerColor = TechSurface
    )
    val content: @Composable () -> Unit = {
        DriveAccountCardContent(
            account = account,
            quota = quota,
            clickable = onClick != null,
            onMoreClick = onMoreClick
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors,
            border = BorderStroke(1.dp, accent.copy(alpha = .42f))
        ) { content() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors,
            border = BorderStroke(1.dp, accent.copy(alpha = .42f))
        ) { content() }
    }
}

@Composable
private fun DriveAccountCardContent(
    account: DriveAccount,
    quota: QuotaInfo? = null,
    clickable: Boolean,
    onMoreClick: (() -> Unit)? = null
) {
    val accent = when (account.id) {
        "quark" -> TechPurple
        "uc" -> TechBlue
        "xunlei" -> TechWarning
        else -> TechCyan
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 品牌头像（暂用首字母，后续可替换为品牌图标）
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            if (account.iconRes != null) {
                Image(
                    painter = painterResource(account.iconRes),
                    contentDescription = account.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Text(
                    text = account.avatarText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TechTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = account.description,
                style = MaterialTheme.typography.bodySmall,
                color = TechTextSecondary
            )
            // 已登录且有空间数据：卡片内展示剩余空间进度条（出现时淡入 + 纵向展开，避免突兀）
            AnimatedVisibility(
                visible = account.isLoggedIn && quota != null,
                enter = fadeIn(tween(300)) + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300)
                ),
                exit = fadeOut(tween(200)) + shrinkVertically(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    quota?.let { QuotaInlineBar(it) }
                }
            }
        }

        when {
            account.isLoggedIn && onMoreClick != null -> IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "更多",
                    tint = TechTextSecondary
                )
            }
            account.isLoggedIn -> LoginBadge(isLoggedIn = true)
            clickable -> Text(
                text = "去登录",
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            else -> LoginBadge(isLoggedIn = false)
        }
    }
}

/** 网盘卡片内空间进度条：已用 / 总容量 + 细进度条 */
@Composable
private fun QuotaInlineBar(quota: QuotaInfo) {
    val ratio = if (quota.total > 0) {
        (quota.used.toFloat() / quota.total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column {
        Text(
            text = "已用 ${formatBytes(quota.used)} / ${formatBytes(quota.total)}",
            style = MaterialTheme.typography.labelSmall,
            color = TechTextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).background(TechOutline, RoundedCornerShape(3.dp))) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = ratio.coerceAtLeast(if (ratio > 0f) .025f else 0f))
                    .height(6.dp)
                    .background(
                        if (ratio > .9f) Brush.horizontalGradient(listOf(TechWarning, Color(0xFFFF647C))) else TechGradient,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
private fun LoginBadge(isLoggedIn: Boolean) {
    val (label, color) = if (isLoggedIn) {
        "已登录" to MaterialTheme.colorScheme.primary
    } else {
        "未登录" to MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 字节数格式化：B / KB / MB / GB / TB */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.size - 1) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "${bytes} B"
    else String.format("%.1f %s", value, units[unit])
}
