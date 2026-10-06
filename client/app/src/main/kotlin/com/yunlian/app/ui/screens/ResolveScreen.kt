package com.yunlian.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yunlian.app.data.network.ShareLinkParser
import com.yunlian.app.data.network.SharePlatform
import com.yunlian.app.ui.SnackbarController
import com.yunlian.app.ui.components.GradientButton
import com.yunlian.app.ui.components.TechCard
import com.yunlian.app.ui.components.TechDimens
import com.yunlian.app.ui.components.TechGradient
import com.yunlian.app.ui.components.TechPageHero
import com.yunlian.app.ui.theme.TechBlue
import com.yunlian.app.ui.theme.TechBackground
import com.yunlian.app.ui.theme.TechCyan
import com.yunlian.app.ui.theme.TechError
import com.yunlian.app.ui.theme.TechTextPrimary
import com.yunlian.app.ui.theme.TechTextSecondary
import com.yunlian.app.ui.theme.TechPurple
import com.yunlian.app.R
import com.yunlian.app.ui.resolve.DownloadLinkDialog
import com.yunlian.app.ui.resolve.ShareDetailScreen
import com.yunlian.app.ui.viewmodel.QuarkCloudViewModel
import com.yunlian.app.ui.viewmodel.ResolveUiState
import com.yunlian.app.ui.viewmodel.ResolveViewModel
import com.yunlian.app.ui.viewmodel.UCCoudViewModel
import com.yunlian.app.ui.viewmodel.XunleiCloudViewModel

/**
 * 解析页：输入分享链接与提取码 → 解析 → 展示分享详情 → 获取下载直链。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ResolveViewModel,
    /** 夸克云盘浏览 ViewModel（分享文件转存目录选择用） */
    quarkCloudViewModel: QuarkCloudViewModel,
    /** 迅雷网盘云盘浏览 ViewModel（迅雷分享转存目录选择用） */
    xunleiCloudViewModel: XunleiCloudViewModel,
    /** UC 网盘云盘浏览 ViewModel（UC 分享转存目录选择用） */
    ucCloudViewModel: UCCoudViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val downloadLink = viewModel.downloadLink
    val downloadError = viewModel.downloadError
    val context = LocalContext.current

    // 输入框状态提升到页面层：进入详情/文件夹再返回时不清空
    var link by rememberSaveable { mutableStateOf("") }
    var pwd by rememberSaveable { mutableStateOf("") }
    var pwdEdited by rememberSaveable { mutableStateOf(false) }

    // 剪贴板分享链接提示状态：待提示的剪贴板文本 + 已忽略的文本
    // 用 rememberSaveable：切换 Tab 后返回仍保留（避免「忽略后切页回来又弹」）
    var clipboardSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
    var ignoredClipboard by rememberSaveable { mutableStateOf<String?>(null) }

    // 检测函数：读取剪贴板，满足条件则设置提示（三重触发：组合时 / ON_RESUME / 剪贴板变化）
    val maybeSuggestClipboard: () -> Unit = {
        val text = readClipboardSafely(context)
        if (text != null &&
            state is ResolveUiState.Idle &&
            text.isNotBlank() &&
            text != link &&
            text != ignoredClipboard &&
            ShareLinkParser.parse(text) != null
        ) {
            clipboardSuggestion = text
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    DisposableEffect(lifecycleOwner, clipboard) {
        // 剪贴板变化立即检测（前台最灵敏，复制即提示）
        val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            maybeSuggestClipboard()
            // 部分 ROM 剪贴板内容写入有延迟，300ms 后重试一次
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                maybeSuggestClipboard()
            }, 300)
        }
        clipboard.addPrimaryClipChangedListener(clipListener)
        // 打开应用 / 从后台切回时检测
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) maybeSuggestClipboard()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 冷启动兜底：组合完成立即检测一次（避免 ON_RESUME 早于 observer 注册导致漏检）
        maybeSuggestClipboard()
        onDispose {
            clipboard.removePrimaryClipChangedListener(clipListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 11 及以下：轻量轮询兜底（2s 一次）。
    // 部分 ROM（如 vivo）剪贴板监听不触发时仍能识别；Android 12+ 读剪贴板会弹系统提示，不轮询。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                maybeSuggestClipboard()
            }
        }
    }

    // 链接变化时自动匹配提取码（用户未手动输入时）
    LaunchedEffect(link) {
        if (!pwdEdited && pwd.isEmpty()) {
            ShareLinkParser.parse(link)?.pwd?.let { pwd = it }
        }
    }

    // 下载错误提示
    LaunchedEffect(downloadError) {
        downloadError?.let {
            // 下载验证被后台关闭或用户取消时静默返回解析页，不显示取消提示。
            if (it != "下载验证已取消") SnackbarController.show(it)
            viewModel.consumeDownloadError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TechBackground)
    ) {
        // 状态切换过渡：输入态/加载/详情/错误之间平滑淡入淡出 + 轻微位移
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 20 })
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "resolveState"
        ) { s ->
            when (s) {
                is ResolveUiState.Detail -> ShareDetailScreen(
            session = s.session,
            files = s.files,
            viewModel = viewModel,
            quarkCloudViewModel = quarkCloudViewModel,
            xunleiCloudViewModel = xunleiCloudViewModel,
            ucCloudViewModel = ucCloudViewModel,
            scrollBehavior = scrollBehavior,
                    // 顶部左上角返回：退出文件页回到输入页（输入框内容保留）
                    onExit = { viewModel.backToInput() },
                    // 列表「返回上一级」：子目录回上级，根目录回输入页
                    onBack = { viewModel.navigateBack() }
                )
                is ResolveUiState.Loading -> LoadingContent()
                else -> ResolveInputContent(
                    viewModel = viewModel,
                    scrollBehavior = scrollBehavior,
                    state = s,
                    link = link,
                    onLinkChange = { link = it },
                    pwd = pwd,
                    onPwdChange = {
                        pwd = it
                        pwdEdited = true
                    },
                    onClearPwd = { pwd = "" }
                )
            }
        }

        // 剪贴板分享链接提示卡片（仅输入页、有待提示内容时显示，带弹出动画）
        // animatedSuggestion 保留最后提示内容，保证退出动画期间卡片不消失
        var animatedSuggestion by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(clipboardSuggestion) {
            clipboardSuggestion?.let { animatedSuggestion = it }
        }
        AnimatedVisibility(
            visible = state is ResolveUiState.Idle && clipboardSuggestion != null,
            enter = fadeIn(tween(200)) +
                slideInVertically(tween(250)) { -it / 2 } +
                scaleIn(tween(250, delayMillis = 60)),
            exit = fadeOut(tween(150)) +
                slideOutVertically(tween(200)) { -it / 2 } +
                scaleOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            animatedSuggestion?.let { suggestion ->
                val parsed = ShareLinkParser.parse(suggestion)
                ClipboardSuggestCard(
                    platformName = parsed?.platform?.let { platformLabel(it) } ?: "网盘",
                    onPaste = {
                        link = suggestion
                        pwd = parsed?.pwd.orEmpty()
                        pwdEdited = true
                        clipboardSuggestion = null
                        viewModel.startResolve(suggestion, parsed?.pwd)
                    },
                    onDismiss = {
                        ignoredClipboard = suggestion
                        clipboardSuggestion = null
                    }
                )
            }
        }
    }

    // 获取下载直链：轻量阻塞层防止重复提交，超时由 ViewModel 统一处理。
    if (viewModel.isFetchingDownloadLink) {
        BackHandler { }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(42.dp), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("加载中", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // 下载直链弹窗
    downloadLink?.let { link ->
        DownloadLinkDialog(
            link = link,
            onDownload = { viewModel.startDownload(link) },
            onDismiss = { viewModel.dismissDownloadDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolveInputContent(
    viewModel: ResolveViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    state: ResolveUiState,
    link: String,
    onLinkChange: (String) -> Unit,
    pwd: String,
    onPwdChange: (String) -> Unit,
    onClearPwd: () -> Unit
) {
    val isLoading = state is ResolveUiState.Loading
    var linkFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(link, selection = TextRange.Zero))
    }
    LaunchedEffect(link) {
        if (linkFieldValue.text != link) {
            linkFieldValue = TextFieldValue(link, selection = TextRange.Zero)
        }
    }
    val linkInteractionSource = remember { MutableInteractionSource() }
    val pwdInteractionSource = remember { MutableInteractionSource() }
    val linkFocused by linkInteractionSource.collectIsFocusedAsState()
    val pwdFocused by pwdInteractionSource.collectIsFocusedAsState()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TechTextPrimary,
        unfocusedTextColor = TechTextPrimary,
        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedPlaceholderColor = TechTextSecondary,
        unfocusedPlaceholderColor = TechTextSecondary,
        focusedLabelColor = TechTextSecondary,
        unfocusedLabelColor = TechTextSecondary,
        focusedLeadingIconColor = TechCyan,
        unfocusedLeadingIconColor = TechTextSecondary,
        errorBorderColor = androidx.compose.ui.graphics.Color.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
    ) {
        TechPageHero(
            title = "解析",
            subtitle = "粘贴分享链接，一键解析下载"
        )

        Column(
            modifier = Modifier.padding(
                horizontal = TechDimens.PageHorizontal,
                vertical = 22.dp
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "分享链接",
                color = TechTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
            TechCard(
                modifier = Modifier.fillMaxWidth(),
                accent = if (state is ResolveUiState.Error) TechError else TechBlue,
                highlighted = linkFocused,
                padding = 4.dp
            ) {
                OutlinedTextField(
                    value = linkFieldValue,
                    onValueChange = {
                        linkFieldValue = it
                        onLinkChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    placeholder = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("例如：", color = TechTextSecondary, fontSize = 14.sp)
                            Text(
                                "https://pan.quark.cn/s/xxxx",
                                color = TechTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    },
                    leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    singleLine = true,
                    isError = state is ResolveUiState.Error,
                    interactionSource = linkInteractionSource,
                    colors = fieldColors,
                    shape = MaterialTheme.shapes.large
                )
            }
            if (state is ResolveUiState.Error) {
                Text(state.message, color = TechError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }

        TechCard(
            modifier = Modifier.fillMaxWidth(),
            highlighted = pwdFocused,
            padding = 2.dp
        ) {
            OutlinedTextField(
                value = pwd,
                onValueChange = onPwdChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { Text("提取码（可选）") },
                trailingIcon = {
                    if (pwd.isNotEmpty()) {
                        IconButton(onClick = onClearPwd) {
                            Icon(Icons.Filled.Close, contentDescription = "清空提取码")
                        }
                    }
                },
                singleLine = true,
                interactionSource = pwdInteractionSource,
                colors = fieldColors,
                shape = MaterialTheme.shapes.large
            )
        }

        GradientButton(
            text = if (isLoading) "正在解析" else "开始解析",
            onClick = { viewModel.startResolve(link, pwd) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            enabled = link.isNotBlank() && !isLoading,
            loading = isLoading,
            icon = Icons.Outlined.ArrowForward
        )
        Spacer(Modifier.height(48.dp))
        }
    }
}

/** 全屏加载中（进入文件夹/解析中展示，避免闪回输入页） */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "加载中…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 安全读取剪贴板最新文本；失败返回 null（部分 ROM 可能限制剪贴板访问） */
private fun readClipboardSafely(context: Context): String? = runCatching {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
}.getOrNull()

/** 平台名称（提示卡片展示） */
private fun platformLabel(platform: SharePlatform): String = when (platform) {
    SharePlatform.QUARK -> "夸克网盘"
    SharePlatform.UC -> "UC 网盘"
    SharePlatform.XUNLEI -> "迅雷网盘"
}

/** 剪贴板分享链接提示卡片：检测到分享链接时，询问是否粘贴解析 */
@Composable
private fun ClipboardSuggestCard(
    platformName: String,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    TechCard(
        modifier = modifier.fillMaxWidth(),
        accent = TechPurple,
        padding = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = TechCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "检测到 $platformName 分享链接",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = TechTextPrimary
                    )
                    Text(
                        text = "是否粘贴到解析框并开始解析？",
                        style = MaterialTheme.typography.bodySmall,
                        color = TechTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("忽略", color = TechTextSecondary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onPaste,
                    modifier = Modifier
                        .height(44.dp)
                        .background(TechGradient, RoundedCornerShape(12.dp)),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("粘贴并解析", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
